#Android Binder源码分析


一. bindService流程

二. getService()流程

三. addService()流程

参考：

https://blog.csdn.net/luoshengyang/article/details/6642463

https://blog.csdn.net/tkwxty/article/details/108086456

https://blog.csdn.net/tkwxty/article/details/107916160

https://www.jianshu.com/p/327f583f970b

https://zhuanlan.zhihu.com/p/35519585


------

ContextWrapper.java
```
    @Override
    public boolean bindService(Intent service, ServiceConnection conn,
            int flags) {
        return mBase.bindService(service, conn, flags);
    }
```

ContextImpl.java
```
    @Override
    public boolean bindServiceAsUser(Intent service, ServiceConnection conn, int flags,
            Handler handler, UserHandle user) {
        if (handler == null) {
            throw new IllegalArgumentException("handler must not be null.");
        }
        return bindServiceCommon(service, conn, flags, null, handler, null, user);
    }

    private boolean bindServiceCommon(Intent service, ServiceConnection conn, int flags,
            String instanceName, Handler handler, Executor executor, UserHandle user) {
        // Keep this in sync with DevicePolicyManager.bindDeviceAdminServiceAsUser.
        IServiceConnection sd;
        if (conn == null) {
            throw new IllegalArgumentException("connection is null");
        }
        if (handler != null && executor != null) {
            throw new IllegalArgumentException("Handler and Executor both supplied");
        }
        if (mPackageInfo != null) {
            if (executor != null) {
                sd = mPackageInfo.getServiceDispatcher(conn, getOuterContext(), executor, flags);
            } else {
                sd = mPackageInfo.getServiceDispatcher(conn, getOuterContext(), handler, flags);
            }
        } else {
            throw new RuntimeException("Not supported in system context");
        }
        validateServiceIntent(service);
        try {
            IBinder token = getActivityToken();
            if (token == null && (flags&BIND_AUTO_CREATE) == 0 && mPackageInfo != null
                    && mPackageInfo.getApplicationInfo().targetSdkVersion
                    < android.os.Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
                flags |= BIND_WAIVE_PRIORITY;
            }
            service.prepareToLeaveProcess(this);
            int res = ActivityManager.getService().bindIsolatedService(
                mMainThread.getApplicationThread(), getActivityToken(), service,
                service.resolveTypeIfNeeded(getContentResolver()),
                sd, flags, instanceName, getOpPackageName(), user.getIdentifier());
            if (res < 0) {
                throw new SecurityException(
                        "Not allowed to bind to service " + service);
            }
            return res != 0;
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }
```
这里有两点需要注意：
1. conn是我们在aidl定义的ServiceConnection，这里用ServiceDispatcher中的内部类InnerConnection把conn封装起来了。
2. ActivityManager.getService()的过程：通过ServiceManager.getService()来获取C++层返回的BinderProxy对象，并getService()这个BinderProxy对象当成AMS这个binder实体的引用。详情可看addService()和getService()的过程。有了这个BinderProxy，就可以跨进程访问bindIsolatedService()方法

从ActivityManagerService.java执行bindIsolatedService()方法开始，这里就从应用进程进入到了System进程。

ActivityManagerService.java
```
    public int bindIsolatedService(IApplicationThread caller, IBinder token, Intent service,
            String resolvedType, IServiceConnection connection, int flags, String instanceName,
            String callingPackage, int userId) throws TransactionTooLargeException {
        ......
        synchronized(this) {
            return mServices.bindServiceLocked(caller, token, service,
                    resolvedType, connection, flags, instanceName, callingPackage, userId);
        }
    }
```
ActiveServices.java
```
    int     (IApplicationThread caller, IBinder token, Intent service,
            String resolvedType, final IServiceConnection connection, int flags,
            String instanceName, String callingPackage, final int userId)
            throws TransactionTooLargeException {
        ......
        try {
            .......
            ConnectionRecord c = new ConnectionRecord(b, activity,
                    connection, flags, clientLabel, clientIntent,
                    callerApp.uid, callerApp.processName, callingPackage);

            if (s.app != null && b.intent.received) {
                // Service is already running, so we can immediately
                // publish the connection.
                try {
                    c.conn.connected(s.name, b.intent.binder, false);
                } catch (Exception e) {
                    Slog.w(TAG, "Failure sending service " + s.shortInstanceName
                            + " to connection " + c.conn.asBinder()
                            + " (in " + c.binding.client.processName + ")", e);
                }

                // If this is the first app connected back to this binding,
                // and the service had previously asked to be told when
                // rebound, then do so.
                if (b.intent.apps.size() == 1 && b.intent.doRebind) {
                    requestServiceBindingLocked(s, b.intent, callerFg, true);
                }
            } else if (!b.intent.requested) {
                //第一次打开Server
                requestServiceBindingLocked(s, b.intent, callerFg, false);
            }

            getServiceMapLocked(s.userId).ensureNotStartingBackgroundLocked(s);

        } finally {
            Binder.restoreCallingIdentity(origId);
        }

        return 1;
    }
    private final boolean requestServiceBindingLocked(ServiceRecord r, IntentBindRecord i,
            boolean execInFg, boolean rebind) throws TransactionTooLargeException {
        if (r.app == null || r.app.thread == null) {
            // If service is not currently running, can't yet bind.
            return false;
        }
        if (DEBUG_SERVICE) Slog.d(TAG_SERVICE, "requestBind " + i + ": requested=" + i.requested
                + " rebind=" + rebind);
        if ((!i.requested || rebind) && i.apps.size() > 0) {
            try {
                bumpServiceExecutingLocked(r, execInFg, "bind");
                r.app.forceProcessStateUpTo(ActivityManager.PROCESS_STATE_SERVICE);
                r.app.thread.scheduleBindService(r, i.intent.getIntent(), rebind,
                        r.app.getReportedProcState());
                if (!rebind) {
                    i.requested = true;
                }
                i.hasBound = true;
                i.doRebind = false;
            } catch (TransactionTooLargeException e) {
                // Keep the executeNesting count accurate.
                if (DEBUG_SERVICE) Slog.v(TAG_SERVICE, "Crashed while binding " + r, e);
                final boolean inDestroying = mDestroyingServices.contains(r);
                serviceDoneExecutingLocked(r, inDestroying, inDestroying);
                throw e;
            } catch (RemoteException e) {
                if (DEBUG_SERVICE) Slog.v(TAG_SERVICE, "Crashed while binding " + r);
                // Keep the executeNesting count accurate.
                final boolean inDestroying = mDestroyingServices.contains(r);
                serviceDoneExecutingLocked(r, inDestroying, inDestroying);
                return false;
            }
        }
        return true;
    }
```

scheduleBindService()方法的调用过程也是使用了Binder，这里跨进程通信了。从System进程又回到应用进程。

ActivityThread.java
```
        public final void scheduleBindService(IBinder token, Intent intent,
                boolean rebind, int processState) {
            updateProcessState(processState, false);
            BindServiceData s = new BindServiceData();
            s.token = token;
            s.intent = intent;
            s.rebind = rebind;

            if (DEBUG_SERVICE)
                Slog.v(TAG, "scheduleBindService token=" + token + " intent=" + intent + " uid="
                        + Binder.getCallingUid() + " pid=" + Binder.getCallingPid());
            sendMessage(H.BIND_SERVICE, s);
        }

    private void handleBindService(BindServiceData data) {
        Service s = mServices.get(data.token);
        if (DEBUG_SERVICE)
            Slog.v(TAG, "handleBindService s=" + s + " rebind=" + data.rebind);
        if (s != null) {
            try {
                data.intent.setExtrasClassLoader(s.getClassLoader());
                data.intent.prepareToEnterProcess();
                try {
                    if (!data.rebind) {
                        IBinder binder = s.onBind(data.intent);
                        ActivityManager.getService().publishService(
                                data.token, data.intent, binder);
                    } else {
                        s.onRebind(data.intent);
                        ActivityManager.getService().serviceDoneExecuting(
                                data.token, SERVICE_DONE_EXECUTING_ANON, 0, 0);
                    }
                } catch (RemoteException ex) {
                    throw ex.rethrowFromSystemServer();
                }
            } catch (Exception e) {
                if (!mInstrumentation.onException(s, e)) {
                    throw new RuntimeException(
                            "Unable to bind to service " + s
                            + " with " + data.intent + ": " + e.toString(), e);
                }
            }
        }
    }
```

这里有两点需要注意：
1. IBinder binder = s.onBind(data.intent);这里是获取我们自定义服务中的那个Binder实体对象。
2. ActivityManager.getService()跨进程调用AMS的publishService()方法，携带的是自定义的Binder实体对象。那这个binder最后传递到ServiceDispatcher.java的时候还是一个binder实体对象吗？个人认为，在publishService()方法调用之前，_data.writeStrongBinder(service);已经把这个binder实体注册到了ServiceManager中，后续使用的是reply.readStrongBinder();返回的一个BinderProxy对象。

以下开始由应用进程进入System进程。
ActivityManagerService.java

```
    public void publishService(IBinder token, Intent intent, IBinder service) {
        // Refuse possible leaked file descriptors
        if (intent != null && intent.hasFileDescriptors() == true) {
            throw new IllegalArgumentException("File descriptors passed in Intent");
        }

        synchronized(this) {
            if (!(token instanceof ServiceRecord)) {
                throw new IllegalArgumentException("Invalid service token");
            }
            mServices.publishServiceLocked((ServiceRecord)token, intent, service);
        }
    }
```
ActiveServices.java
```

    void publishServiceLocked(ServiceRecord r, Intent intent, IBinder service) {
        final long origId = Binder.clearCallingIdentity();
        try {
            if (DEBUG_SERVICE) Slog.v(TAG_SERVICE, "PUBLISHING " + r
                    + " " + intent + ": " + service);
            if (r != null) {
                Intent.FilterComparison filter
                        = new Intent.FilterComparison(intent);
                IntentBindRecord b = r.bindings.get(filter);
                if (b != null && !b.received) {
                    b.binder = service;
                    b.requested = true;
                    b.received = true;
                    ArrayMap<IBinder, ArrayList<ConnectionRecord>> connections = r.getConnections();
                    for (int conni = connections.size() - 1; conni >= 0; conni--) {
                        ArrayList<ConnectionRecord> clist = connections.valueAt(conni);
                        for (int i=0; i<clist.size(); i++) {
                            ConnectionRecord c = clist.get(i);
                            if (!filter.equals(c.binding.intent.intent)) {
                                if (DEBUG_SERVICE) Slog.v(
                                        TAG_SERVICE, "Not publishing to: " + c);
                                if (DEBUG_SERVICE) Slog.v(
                                        TAG_SERVICE, "Bound intent: " + c.binding.intent.intent);
                                if (DEBUG_SERVICE) Slog.v(
                                        TAG_SERVICE, "Published intent: " + intent);
                                continue;
                            }
                            if (DEBUG_SERVICE) Slog.v(TAG_SERVICE, "Publishing to: " + c);
                            try {
                                c.conn.connected(r.name, service, false);
                            } catch (Exception e) {
                                Slog.w(TAG, "Failure sending service " + r.shortInstanceName
                                      + " to connection " + c.conn.asBinder()
                                      + " (in " + c.binding.client.processName + ")", e);
                            }
                        }
                    }
                }

                serviceDoneExecutingLocked(r, mDestroyingServices.contains(r), false);
            }
        } finally {
            Binder.restoreCallingIdentity(origId);
        }
    }
```

上面的conn实际上已经被封装成了ServiceDispatcher内部类中的InnerConnection。InnerConnection也是一个Binder对象。

以下开始由System进程进入到了应用进程。

ServiceDispatcher.java
```
        private static class InnerConnection extends IServiceConnection.Stub {
            @UnsupportedAppUsage
            final WeakReference<LoadedApk.ServiceDispatcher> mDispatcher;

            InnerConnection(LoadedApk.ServiceDispatcher sd) {
                mDispatcher = new WeakReference<LoadedApk.ServiceDispatcher>(sd);
            }

            public void connected(ComponentName name, IBinder service, boolean dead)
                    throws RemoteException {
                LoadedApk.ServiceDispatcher sd = mDispatcher.get();
                if (sd != null) {
                    sd.connected(name, service, dead);
                }
            }
        }
```
LoadedApk.java

```
        public void connected(ComponentName name, IBinder service, boolean dead) {
            if (mActivityExecutor != null) {
                mActivityExecutor.execute(new RunConnection(name, service, 0, dead));
            } else if (mActivityThread != null) {
                mActivityThread.post(new RunConnection(name, service, 0, dead));
            } else {
                doConnected(name, service, dead);
            }
        }

        public void doConnected(ComponentName name, IBinder service, boolean dead) {
            .....
            // If there is a new viable service, it is now connected.
            if (service != null) {
                mConnection.onServiceConnected(name, service);
            } else {
                // The binding machinery worked, but the remote returned null from onBind().
                mConnection.onNullBinding(name);
            }
        }
```

最终回调了我们在aidl中定义的ServiceConnection的onServiceConnected()方法。
以上的流程分析均忽略了跨进程调用方法的时候，实际上是先由C++层回调Binder.java的transact()方法，再调用Binder实现类的onTransact()方法，这个过程涉及太多的跨进程，这样写比较混乱。后面看getService()和addService()流程的时候再慢慢理解吧。

----------------------------------------------------

getService()：从ServiceManager中获取BinderProxy

通过ActivityManager.getService()拿到了AMS进程的binder代理对象。
ActivityManager.java

```
    @UnsupportedAppUsage
    public static IActivityManager getService() {
        return IActivityManagerSingleton.get();
    }

    @UnsupportedAppUsage
    private static final Singleton<IActivityManager> IActivityManagerSingleton =
            new Singleton<IActivityManager>() {
                @Override
                protected IActivityManager create() {
                    final IBinder b = ServiceManager.getService(Context.ACTIVITY_SERVICE);
                    final IActivityManager am = IActivityManager.Stub.asInterface(b);
                    return am;
                }
            };
```

ActivityManager对外设计成一个单例模式的获取方式。ServiceManager.getService(Context.ACTIVITY_SERVICE)之所以能拿到binder对象，是因为zygote孵化系统进程的时候，顺便把系统进程add到了ServiceManager中进行管理。

ServiceManager.java
```
    @UnsupportedAppUsage
    public static IBinder getService(String name) {
        try {
            IBinder service = sCache.get(name);
            if (service != null) {
                return service;
            } else {
                return Binder.allowBlocking(rawGetService(name));
            }
        } catch (RemoteException e) {
            Log.e(TAG, "error in getService", e);
        }
        return null;
    }


    private static IBinder rawGetService(String name) throws RemoteException {
        final long start = sStatLogger.getTime();

        final IBinder binder = getIServiceManager().getService(name);
        .....
        return binder;
    }

    @UnsupportedAppUsage
    private static IServiceManager getIServiceManager() {
        if (sServiceManager != null) {
            return sServiceManager;
        }

        // Find the service manager
        sServiceManager = ServiceManagerNative
                .asInterface(Binder.allowBlocking(BinderInternal.getContextObject()));
        return sServiceManager;
    }

    @UnsupportedAppUsage
    public static void addService(String name, IBinder service, boolean allowIsolated,
            int dumpPriority) {
        try {
            getIServiceManager().addService(name, service, allowIsolated, dumpPriority);
        } catch (RemoteException e) {
            Log.e(TAG, "error in addService", e);
        }
    }

```

实际上，BinderInternal.getContextObject()最后会通过JNI调用c++层，这里会返回一个IBinder代理对象，就是BinderProxy对象。这里可以把


```
        sServiceManager = ServiceManagerNative
                .asInterface(Binder.allowBlocking(BinderInternal.getContextObject()));
```
当成
```
        sServiceManager = ServiceManagerNative
                .asInterface(new BinderProxy());
```
JNI调用的详细过程可以看老罗的博客：https://blog.csdn.net/luoshengyang/article/details/6633311

ServiceManagerNative.java
```
    @UnsupportedAppUsage
    static public IServiceManager asInterface(IBinder obj)
    {
        if (obj == null) {
            return null;
        }
        IServiceManager in =
            (IServiceManager)obj.queryLocalInterface(descriptor);
        if (in != null) {
            return in;
        }

        return new ServiceManagerProxy(obj);
    }

```
BinderProxy.java
```
    public IInterface queryLocalInterface(String descriptor) {
        return null;
    }
```
obj是JNI调用c++层返回的BinderProxy代理对象，它是固定的。因此queryLocalInterface()方法返回null。
所以，以上getIServiceManager()得到的结果只有一个。那就是ServiceManagerProxy对象，它不是一个binder，而是一个binder代理类。
ServiceManagerProxy.java
```
class ServiceManagerProxy implements IServiceManager {
    public ServiceManagerProxy(IBinder remote) {
        mRemote = remote;
    }

    public IBinder asBinder() {
        return mRemote;
    }

    @UnsupportedAppUsage
    public IBinder getService(String name) throws RemoteException {
        Parcel data = Parcel.obtain();
        Parcel reply = Parcel.obtain();
        data.writeInterfaceToken(IServiceManager.descriptor);
        data.writeString(name);
        //通过Binder驱动传递数据
        mRemote.transact(GET_SERVICE_TRANSACTION, data, reply, 0);
        //c++层返回一个BinderProxy对象
        IBinder binder = reply.readStrongBinder();
        reply.recycle();
        data.recycle();
        return binder;
    }

    public void addService(String name, IBinder service, boolean allowIsolated, int dumpPriority)
            throws RemoteException {
        Parcel data = Parcel.obtain();
        Parcel reply = Parcel.obtain();
        data.writeInterfaceToken(IServiceManager.descriptor);
        data.writeString(name);
        data.writeStrongBinder(service);
        data.writeInt(allowIsolated ? 1 : 0);
        data.writeInt(dumpPriority);
        mRemote.transact(ADD_SERVICE_TRANSACTION, data, reply, 0);
        reply.recycle();
        data.recycle();
    }
    ......

}
```

BinderProxy.java
```
   public boolean transact(int code, Parcel data, Parcel reply, int flags) throws RemoteException {
        .....
        try {
            //最过调用到Binder驱动程序，Binder驱动程序唤醒对应的Server
            return transactNative(code, data, reply, flags);
        } finally {
            if (transactListener != null) {
                transactListener.onTransactEnded(session);
            }

            if (tracingEnabled) {
                Trace.traceEnd(Trace.TRACE_TAG_ALWAYS);
            }
        }
    }

```

mRemote是一个BinderProxy对象，调用的是BinderProxy中的transact()方法,最终通过JNI调用c++层，通过Parcel在java层和c++层之间传递数据。ransact()方法执行完后，reply中保存了sever端的binder引用，也是一个BinderProxy对象。（这个引用其实是c++层的BpBinder对象）
获得server端的binder引用后，我们在java层就可以通过这个binder引用调用server端的方法。当我们使用这个binder引用调用方法的时候，Parcel传递消息到c++层，c++层会去调用Server端Binder的transact()方法，最终调用Binder实体类的onTransact()方法。

ServiceManagerNative.java
```
    public boolean onTransact(int code, Parcel data, Parcel reply, int flags)
    {
        try {
            switch (code) {
                case IServiceManager.GET_SERVICE_TRANSACTION: {
                    data.enforceInterface(IServiceManager.descriptor);
                    String name = data.readString();
                    IBinder service = getService(name);
                    reply.writeStrongBinder(service);
                    return true;
                }
                case IServiceManager.ADD_SERVICE_TRANSACTION: {
                    data.enforceInterface(IServiceManager.descriptor);
                    String name = data.readString();
                    IBinder service = data.readStrongBinder();
                    boolean allowIsolated = data.readInt() != 0;
                    int dumpPriority = data.readInt();
                    addService(name, service, allowIsolated, dumpPriority);
                    return true;
                }
                ......
            }
        } catch (RemoteException e) {
        }

        return false;
    }

```

--------------------------

addService()：向ServiceManager注册服务，其实就是把已经实现了的Binder实体向ServiceManager注册。如果其他进程指定要访问这个Binder实体的时候，ServiceManager会返回一个BinderProxy给访问者。至于Binder和BinderProxy之间的映射关系，ServiceManager已经帮我们做好了，我们只需要认为通过BinderProxy可以调用这个Binder实体即可。

ZygoteInit.java

```
 @UnsupportedAppUsage
    public static void main(String argv[]) {
        .....
            if (startSystemServer) {
                Runnable r = forkSystemServer(abiList, zygoteSocketName, zygoteServer);

                // {@code r == null} in the parent (zygote) process, and {@code r != null} in the
                // child (system_server) process.
                if (r != null) {
                    r.run();
                    return;
                }
            }
        ......
    }
    //孵化system进程
    private static Runnable forkSystemServer(String abiList, String socketName,
            ZygoteServer zygoteServer) {
        ......
        /* Hardcoded command line to start the system server */
        String args[] = {
                "--setuid=1000",
                "--setgid=1000",
                "--setgroups=1001,1002,1003,1004,1005,1006,1007,1008,1009,1010,1018,1021,1023,"
                        + "1024,1032,1065,3001,3002,3003,3006,3007,3009,3010",
                "--capabilities=" + capabilities + "," + capabilities,
                "--nice-name=system_server",
                "--runtime-args",
                "--target-sdk-version=" + VMRuntime.SDK_VERSION_CUR_DEVELOPMENT,
                "com.android.server.SystemServer",
        };
        ZygoteArguments parsedArgs = null;

        int pid;

        try {
            ......
            /* Request to fork the system server process */
            pid = Zygote.forkSystemServer(
                    parsedArgs.mUid, parsedArgs.mGid,
                    parsedArgs.mGids,
                    parsedArgs.mRuntimeFlags,
                    null,
                    parsedArgs.mPermittedCapabilities,
                    parsedArgs.mEffectiveCapabilities);
        } catch (IllegalArgumentException ex) {
            throw new RuntimeException(ex);
        }
        ......
        return null;
    }
```

不使用IDE的时候，我们想编译或者运行一个java文件，一般都通过在cmd中使用javac/java的命令行方式来运行。main()函数就是运行一个java文件的入口。这样也一样，通过Zygote进程forkSystemServer()调用后，会通过命令行的形式运行SystemServer.java

SystemServer.java
```
    public static void main(String[] args) {
        new SystemServer().run();
    }

    private void run() {
        ......
        //开启system进程的Looper循环
        Looper.prepareMainLooper();
        Looper.getMainLooper().setSlowLogThresholdMs(
                    SLOW_DISPATCH_THRESHOLD_MS, SLOW_DELIVERY_THRESHOLD_MS);
        // Initialize native services. 
        System.loadLibrary("android_servers");

        // Start services.
        try {
            traceBeginAndSlog("StartServices");
            startBootstrapServices();
            startCoreServices();
            startOtherServices();
            SystemServerInitThreadPool.shutdown();
        } catch (Throwable ex) {
            Slog.e("System", "******************************************");
            Slog.e("System", "************ Failure starting system services", ex);
            throw ex;
        } finally {
            traceEnd();
        }
        ......
        // Loop forever.
        Looper.loop();
        throw new RuntimeException("Main thread loop unexpectedly exited");
    }

    private void startBootstrapServices() {
        .....
        //初始化AMS服务
        mActivityManagerService = ActivityManagerService.Lifecycle.startService(
                mSystemServiceManager, atm);
        mActivityManagerService.setSystemServiceManager(mSystemServiceManager);
        mActivityManagerService.setInstaller(installer);
        ......
        // Set up the Application instance for the system process and get started.
        //把AMS服务加入ServiceManager中
        traceBeginAndSlog("SetSystemProcess");
        mActivityManagerService.setSystemProcess();
        traceEnd();
        ......
    }
```

AMS服务启动之后，通过ServiceManager.addService()方法向ServiceManager注册服务。

ActivityManagerService.java

```
    public void setSystemProcess() {
        try {
            ServiceManager.addService(Context.ACTIVITY_SERVICE, this, /* allowIsolated= */ true,
                    DUMP_FLAG_PRIORITY_CRITICAL | DUMP_FLAG_PRIORITY_NORMAL | DUMP_FLAG_PROTO);
            ServiceManager.addService(ProcessStats.SERVICE_NAME, mProcessStats);
            ServiceManager.addService("meminfo", new MemBinder(this), /* allowIsolated= */ false,
                    DUMP_FLAG_PRIORITY_HIGH);
            ServiceManager.addService("gfxinfo", new GraphicsBinder(this));
            ServiceManager.addService("dbinfo", new DbBinder(this));
            if (MONITOR_CPU_USAGE) {
                ServiceManager.addService("cpuinfo", new CpuBinder(this),
                        /* allowIsolated= */ false, DUMP_FLAG_PRIORITY_CRITICAL);
            }
            ServiceManager.addService("permission", new PermissionController(this));
            ServiceManager.addService("processinfo", new ProcessInfoService(this));
            ......
        }
    }
```

ServiceManager.java
```
    @UnsupportedAppUsage
    public static void addService(String name, IBinder service, boolean allowIsolated,
            int dumpPriority) {
        try {
            getIServiceManager().addService(name, service, allowIsolated, dumpPriority);
        } catch (RemoteException e) {
            Log.e(TAG, "error in addService", e);
        }
    }
```
上面已经介绍过getIServiceManager()，返回的ServiceManagerProxy代理类。

ServiceManagerProxy.java
```
class ServiceManagerProxy implements IServiceManager {
    public void addService(String name, IBinder service, boolean allowIsolated, int dumpPriority)
            throws RemoteException {
        Parcel data = Parcel.obtain();
        Parcel reply = Parcel.obtain();
        data.writeInterfaceToken(IServiceManager.descriptor);
        data.writeString(name);
        data.writeStrongBinder(service);
        data.writeInt(allowIsolated ? 1 : 0);
        data.writeInt(dumpPriority);
        mRemote.transact(ADD_SERVICE_TRANSACTION, data, reply, 0);
        reply.recycle();
        data.recycle();
    }
    ......

}
```

ActivityManagerService继承自IActivityManager.Stub，它是一个Binder实体。调用writeStrongBinder方法后，AMS这个binder实体写入了Parcel，并通过Parcel把binder实体交给了c++层（BBinder对象），在c++层，会把BBinder对象注册到c++层的Service Manager中去。
mRemote是一个BinderProxy。


