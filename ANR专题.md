#  ANR专题


+ ContentProvider
+ ContentProvider
+ BroadcastQueue
+ ContentProvider流程
+ AppNotResponding流程


参考：
https://juejin.cn/post/6973564044351373326

## BroadcastQueue流程




## ContentProvider流程

ContentProvider使用
```
    Uri uri = Uri.parse("content://com.example.content/table");
    ContentProviderClient client = getContentResolver().acquireUnstableContentProviderClient(
            "com.example.content");
	client.query(uri,null,null,null,null);
```

源码分析：

ContentResolver
```
    public final @Nullable ContentProviderClient acquireUnstableContentProviderClient(
            @NonNull Uri uri) {
        Objects.requireNonNull(uri, "uri");
        IContentProvider provider = acquireUnstableProvider(uri);
        if (provider != null) {
            return new ContentProviderClient(this, provider, uri.getAuthority(), false);
        }

        return null;
    }
```

ContentProviderClient

```
public @Nullable Cursor query(@NonNull Uri uri, @Nullable String[] projection,
            Bundle queryArgs, @Nullable CancellationSignal cancellationSignal)
                    throws RemoteException {
        Objects.requireNonNull(uri, "url");

        beforeRemote();//postDelayed一个延时任务,当前query()操作如果超时，那延时任务仍然在主线程的Handler队列中，将拿出来执行，弹出ANR弹框
        try {
            ICancellationSignal remoteCancellationSignal = null;
            if (cancellationSignal != null) {
                cancellationSignal.throwIfCanceled();
                remoteCancellationSignal = mContentProvider.createCancellationSignal();
                cancellationSignal.setRemote(remoteCancellationSignal);
            }
            final Cursor cursor = mContentProvider.query(
                    mAttributionSource, uri, projection, queryArgs,
                    remoteCancellationSignal);
            if (cursor == null) {
                return null;
            }
            return new CursorWrapperInner(cursor);
        } catch (DeadObjectException e) {
            if (!mStable) {
                mContentResolver.unstableProviderDied(mContentProvider);
            }
            throw e;
        } finally {
            afterRemote();//移除postDelayed生成的延时任务
        }
    }
	
	//如果没有自定义ANR时间，默认ContentProvider超时20000ms发生ANR，在当前UI线程中postDelayed一个延时任务。
    private void beforeRemote() {
        if(mAnrRunnable == null){
            setDetectNotResponding(20000);
        }
        if (mAnrRunnable != null) {
            sAnrHandler.postDelayed(mAnrRunnable, mAnrTimeout);
        }
    }

    //移除postDelayed生成的延时任务
    private void afterRemote() {
        if (mAnrRunnable != null) {
            sAnrHandler.removeCallbacks(mAnrRunnable);
        }
    }
	
	//设置ContentProvider超时时间，默认20000ms，属于SystemApi
    public void setDetectNotResponding(@DurationMillisLong long timeoutMillis) {
        synchronized (ContentProviderClient.class) {
            mAnrTimeout = timeoutMillis;

            if (timeoutMillis > 0) {
                if (mAnrRunnable == null) {
                    mAnrRunnable = new NotRespondingRunnable();
                }
                if (sAnrHandler == null) {
                    sAnrHandler = new Handler(Looper.getMainLooper(), null, true /* async */);
                }

                // If the remote process hangs, we're going to kill it, so we're
                // technically okay doing blocking calls.
                Binder.allowBlocking(mContentProvider.asBinder());
            } else {
                mAnrRunnable = null;

                // If we're no longer watching for hangs, revert back to default
                // blocking behavior.
                Binder.defaultBlocking(mContentProvider.asBinder());
            }
        }
    }
	//由于getContentResolver()拿到的是ApplicationContentResolver对象，因此接下来调用的是ContextImpl的内部类ApplicationContentResolver的方法
    private class NotRespondingRunnable implements Runnable {
        @Override
        public void run() {
            Log.w(TAG, "Detected provider not responding: " + mContentProvider);
            mContentResolver.appNotRespondingViaProvider(mContentProvider);
        }
    }
```


## AppNotResponding流程

ApplicationContentResolver

```
        private final ActivityThread mMainThread;

        public ApplicationContentResolver(Context context, ActivityThread mainThread) {
            super(context);
            mMainThread = Objects.requireNonNull(mainThread);
        }
        public void appNotRespondingViaProvider(IContentProvider icp) {
            mMainThread.appNotRespondingViaProvider(icp.asBinder());
        }
```

ActivityThread

```
    final void appNotRespondingViaProvider(IBinder provider) {
        synchronized (mProviderMap) {
            ProviderRefCount prc = mProviderRefCountMap.get(provider);
            if (prc != null) {
                try {
                    ActivityManager.getService()
                            .appNotRespondingViaProvider(prc.holder.connection);
                } catch (RemoteException e) {
                    throw e.rethrowFromSystemServer();
                }
            }
        }
    }
```

ActivityManagerService

```
    public void appNotRespondingViaProvider(IBinder connection) {
        mCpHelper.appNotRespondingViaProvider(connection);
    }
	

    final class UiHandler extends Handler {
        public UiHandler() {
            super(com.android.server.UiThread.get().getLooper(), null, true);
        }

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case SHOW_ERROR_UI_MSG: {
                    mAppErrors.handleShowAppErrorUi(msg);
                    ensureBootCompleted();
                } break;
                case SHOW_NOT_RESPONDING_UI_MSG: {//ANR弹框
                    mAppErrors.handleShowAnrUi(msg);
                    ensureBootCompleted();
                } break;
                case SHOW_STRICT_MODE_VIOLATION_UI_MSG: {
                    HashMap<String, Object> data = (HashMap<String, Object>) msg.obj;
                    synchronized (mProcLock) {
                        ProcessRecord proc = (ProcessRecord) data.get("app");
                        if (proc == null) {
                            Slog.e(TAG, "App not found when showing strict mode dialog.");
                            break;
                        }
                        if (proc.mErrorState.getDialogController().hasViolationDialogs()) {
                            Slog.e(TAG, "App already has strict mode dialog: " + proc);
                            return;
                        }
                        AppErrorResult res = (AppErrorResult) data.get("result");
                        if (mAtmInternal.showStrictModeViolationDialog()) {
                            proc.mErrorState.getDialogController().showViolationDialogs(res);
                        } else {
                            // The device is asleep, so just pretend that the user
                            // saw a crash dialog and hit "force quit".
                            res.set(0);
                        }
                    }
                    ensureBootCompleted();
                } break;
                case WAIT_FOR_DEBUGGER_UI_MSG: {
                    synchronized (mProcLock) {
                        ProcessRecord app = (ProcessRecord) msg.obj;
                        if (msg.arg1 != 0) {
                            if (!app.hasWaitedForDebugger()) {
                                app.mErrorState.getDialogController().showDebugWaitingDialogs();
                                app.setWaitedForDebugger(true);
                            }
                        } else {
                            app.mErrorState.getDialogController().clearWaitingDialog();
                        }
                    }
                } break;
                case DISPATCH_PROCESSES_CHANGED_UI_MSG: {
                    mProcessList.dispatchProcessesChanged();
                    break;
                }
                case DISPATCH_PROCESS_DIED_UI_MSG: {
                    if (false) { // DO NOT SUBMIT WITH TRUE
                        maybeTriggerWatchdog();
                    }
                    final int pid = msg.arg1;
                    final int uid = msg.arg2;
                    mProcessList.dispatchProcessDied(pid, uid);
                    break;
                }
                case DISPATCH_OOM_ADJ_OBSERVER_MSG: {
                    dispatchOomAdjObserver((String) msg.obj);
                } break;
                case PUSH_TEMP_ALLOWLIST_UI_MSG: {
                    pushTempAllowlist();
                } break;
            }
        }
    }
```

ContentProviderHelper
```
    void appNotRespondingViaProvider(IBinder connection) {
        mService.enforceCallingPermission(android.Manifest.permission.REMOVE_TASKS,
                "appNotRespondingViaProvider()");

        final ContentProviderConnection conn = (ContentProviderConnection) connection;
        if (conn == null) {
            Slog.w(TAG, "ContentProviderConnection is null");
            return;
        }

        ActivityManagerService.traceBegin(Trace.TRACE_TAG_ACTIVITY_MANAGER,
                "appNotRespondingViaProvider: ",
                (conn.provider != null && conn.provider.info != null
                ? conn.provider.info.authority : ""));
        try {
            final ProcessRecord host = conn.provider.proc;
            if (host == null) {
                Slog.w(TAG, "Failed to find hosting ProcessRecord");
                return;
            }

            mService.mAnrHelper.appNotResponding(host, "ContentProvider not responding");
        } finally {
            Trace.traceEnd(Trace.TRACE_TAG_ACTIVITY_MANAGER);
        }
    }
```

AnrHelper

```
    void appNotResponding(ProcessRecord anrProcess, String annotation) {
        appNotResponding(anrProcess, null /* activityShortComponentName */, null /* aInfo */,
                null /* parentShortComponentName */, null /* parentProcess */,
                false /* aboveSystem */, annotation);
    }

    void appNotResponding(ProcessRecord anrProcess, String activityShortComponentName,
            ApplicationInfo aInfo, String parentShortComponentName,
            WindowProcessController parentProcess, boolean aboveSystem, String annotation) {
        synchronized (mAnrRecords) {//把所有的ANR记录在AnrRecord中，多个AnrRecord保存在一个ArrayList中
            mAnrRecords.add(new AnrRecord(anrProcess, activityShortComponentName, aInfo,
                    parentShortComponentName, parentProcess, aboveSystem, annotation));
        }
        startAnrConsumerIfNeeded();
    }

    private void startAnrConsumerIfNeeded() {
        if (mRunning.compareAndSet(false, true)) {
            new AnrConsumerThread().start();//开启一个线程去遍历处理ArrayList中的AnrRecord
        }
    }
    private class AnrConsumerThread extends Thread {
        AnrConsumerThread() {
            super("AnrConsumer");
        }

        private AnrRecord next() {
            synchronized (mAnrRecords) {
                return mAnrRecords.isEmpty() ? null : mAnrRecords.remove(0);
            }
        }

        @Override
        public void run() {
            AnrRecord r;
            while ((r = next()) != null) {
                scheduleBinderHeavyHitterAutoSamplerIfNecessary();
                final long startTime = SystemClock.uptimeMillis();
                // If there are many ANR at the same time, the latency may be larger. If the latency
                // is too large, the stack trace might not be meaningful.
                final long reportLatency = startTime - r.mTimestamp;
                final boolean onlyDumpSelf = reportLatency > EXPIRED_REPORT_TIME_MS;
                r.appNotResponding(onlyDumpSelf);
                final long endTime = SystemClock.uptimeMillis();
                Slog.d(TAG, "Completed ANR of " + r.mApp.processName + " in "
                        + (endTime - startTime) + "ms, latency " + reportLatency
                        + (onlyDumpSelf ? "ms (expired, only dump ANR app)" : "ms"));
            }

            mRunning.set(false);
            synchronized (mAnrRecords) {
                // The race should be unlikely to happen. Just to make sure we don't miss.
                if (!mAnrRecords.isEmpty()) {
                    startAnrConsumerIfNeeded();
                }
            }
        }

    }
	
   private static class AnrRecord {
        final ProcessRecord mApp;
        final String mActivityShortComponentName;
        final String mParentShortComponentName;
        final String mAnnotation;
        final ApplicationInfo mAppInfo;
        final WindowProcessController mParentProcess;
        final boolean mAboveSystem;
        final long mTimestamp = SystemClock.uptimeMillis();

        AnrRecord(ProcessRecord anrProcess, String activityShortComponentName,
                ApplicationInfo aInfo, String parentShortComponentName,
                WindowProcessController parentProcess, boolean aboveSystem, String annotation) {
            mApp = anrProcess;
            mActivityShortComponentName = activityShortComponentName;
            mParentShortComponentName = parentShortComponentName;
            mAnnotation = annotation;
            mAppInfo = aInfo;
            mParentProcess = parentProcess;
            mAboveSystem = aboveSystem;
        }

        void appNotResponding(boolean onlyDumpSelf) {
            mApp.mErrorState.appNotResponding(mActivityShortComponentName, mAppInfo,
                    mParentShortComponentName, mParentProcess, mAboveSystem, mAnnotation,
                    onlyDumpSelf);
        }
    }
```

ProcessErrorStateRecord

```
    void appNotResponding(String activityShortComponentName, ApplicationInfo aInfo,
            String parentShortComponentName, WindowProcessController parentProcess,
            boolean aboveSystem, String annotation, boolean onlyDumpSelf) {
        ArrayList<Integer> firstPids = new ArrayList<>(5);
        SparseArray<Boolean> lastPids = new SparseArray<>(20);

        mApp.getWindowProcessController().appEarlyNotResponding(annotation, () -> {
            synchronized (mService) {
                mApp.killLocked("anr", ApplicationExitInfo.REASON_ANR, true);
            }
        });

        long anrTime = SystemClock.uptimeMillis();
        if (isMonitorCpuUsage()) {
            mService.updateCpuStatsNow();
        }

        final boolean isSilentAnr;
        final int pid = mApp.getPid();
        final UUID errorId;
		//特殊情况下即使发生ANR也不记录，比如关机，crash，app被异常关闭等
        synchronized (mService) {
            // PowerManager.reboot() can block for a long time, so ignore ANRs while shutting down.
            if (mService.mAtmInternal.isShuttingDown()) {
                Slog.i(TAG, "During shutdown skipping ANR: " + this + " " + annotation);
                return;
            } else if (isNotResponding()) {
                Slog.i(TAG, "Skipping duplicate ANR: " + this + " " + annotation);
                return;
            } else if (isCrashing()) {
                Slog.i(TAG, "Crashing app skipping ANR: " + this + " " + annotation);
                return;
            } else if (mApp.isKilledByAm()) {
                Slog.i(TAG, "App already killed by AM skipping ANR: " + this + " " + annotation);
                return;
            } else if (mApp.isKilled()) {
                Slog.i(TAG, "Skipping died app ANR: " + this + " " + annotation);
                return;
            }

            // In case we come through here for the same app before completing
            // this one, mark as anring now so we will bail out.
            synchronized (mProcLock) {
                setNotResponding(true);
            }

            //输出anr信息到event log中
            // Log the ANR to the event log.
            EventLog.writeEvent(EventLogTags.AM_ANR, mApp.userId, pid, mApp.processName,
                    mApp.info.flags, annotation);

            ......

        //输出anr信息到main log中
        // Log the ANR to the main log.
        StringBuilder info = new StringBuilder();
        info.setLength(0);
        info.append("ANR in ").append(mApp.processName);
        if (activityShortComponentName != null) {
            info.append(" (").append(activityShortComponentName).append(")");
        }
        info.append("\n");
        info.append("PID: ").append(pid).append("\n");
        if (annotation != null) {
            info.append("Reason: ").append(annotation).append("\n");
        }
        if (parentShortComponentName != null
                && parentShortComponentName.equals(activityShortComponentName)) {
            info.append("Parent: ").append(parentShortComponentName).append("\n");
        }
        if (errorId != null) {
            info.append("ErrorId: ").append(errorId.toString()).append("\n");
        }
        info.append("Frozen: ").append(mApp.mOptRecord.isFrozen()).append("\n");

        ......

        synchronized (mService) {
            // mBatteryStatsService can be null if the AMS is constructed with injector only. This
            // will only happen in tests.
            if (mService.mBatteryStatsService != null) {
                mService.mBatteryStatsService.noteProcessAnr(mApp.processName, mApp.uid);
            }

            if (isSilentAnr() && !mApp.isDebugging()) {
                mApp.killLocked("bg anr", ApplicationExitInfo.REASON_ANR, true);
                return;
            }

            synchronized (mProcLock) {
                // Set the app's notResponding state, and look up the errorReportReceiver
                makeAppNotRespondingLSP(activityShortComponentName,
                        annotation != null ? "ANR " + annotation : "ANR", info.toString());
                mDialogController.setAnrController(anrController);
            }

            // mUiHandler can be null if the AMS is constructed with injector only. This will only
            // happen in tests.
            if (mService.mUiHandler != null) {
                // Bring up the infamous App Not Responding dialog
                Message msg = Message.obtain();
                msg.what = ActivityManagerService.SHOW_NOT_RESPONDING_UI_MSG;
                msg.obj = new AppNotRespondingDialog.Data(mApp, aInfo, aboveSystem);

                mService.mUiHandler.sendMessageDelayed(msg, anrDialogDelayMs);//通过AMS通知UI线程弹出ANR弹框
            }
        }
    }
```


AMS中的UiHandler接收到ProcessErrorStateRecord发出的Handler消息，切换到了UI线程，之后调用AppNotRespondingDialog来显示ANR弹框页面。
```
10-27 11:28:18.157 W/(  805): AppNotRespondingDialog=================Thread[android.ui,5,main]  805   java.lang.Throwable
10-27 11:28:18.157 W/(  805): 	at com.android.server.am.AppNotRespondingDialog.<init>(AppNotRespondingDialog.java:55)
10-27 11:28:18.157 W/(  805): 	at com.android.server.am.ErrorDialogController.showAnrDialogs(ErrorDialogController.java:193)
10-27 11:28:18.157 W/(  805): 	at com.android.server.am.AppErrors.handleShowAnrUi(AppErrors.java:1077)
10-27 11:28:18.157 W/(  805): 	at com.android.server.am.ActivityManagerService$UiHandler.handleMessage(ActivityManagerService.java:1567)
10-27 11:28:18.157 W/(  805): 	at android.os.Handler.dispatchMessage(Handler.java:106)
10-27 11:28:18.157 W/(  805): 	at android.os.Looper.loopOnce(Looper.java:201)
10-27 11:28:18.157 W/(  805): 	at android.os.Looper.loop(Looper.java:288)
10-27 11:28:18.157 W/(  805): 	at android.os.HandlerThread.run(HandlerThread.java:67)
10-27 11:28:18.157 W/(  805): 	at com.android.server.ServiceThread.run(ServiceThread.java:44)
10-27 11:28:18.157 W/(  805): 	at com.android.server.UiThread.run(UiThread.java:45)
```
