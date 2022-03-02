#  Jetpack组件之lifecycle

在lifecycle组件没出来之前，对于Activity/Fragment的生命周期的监听，我们一般用以下方式：

```
Application app = (Application) getApplicationContext();
        app.registerActivityLifecycleCallbacks(new Application.ActivityLifecycleCallbacks(){
            @Override
            public void onActivityCreated(@NonNull Activity activity, @Nullable Bundle savedInstanceState) {

            }

            @Override
            public void onActivityStarted(@NonNull Activity activity) {

            }

            @Override
            public void onActivityResumed(@NonNull Activity activity) {

            }

            @Override
            public void onActivityPaused(@NonNull Activity activity) {

            }

            @Override
            public void onActivityStopped(@NonNull Activity activity) {

            }

            @Override
            public void onActivitySaveInstanceState(@NonNull Activity activity, @NonNull Bundle outState) {

            }

            @Override
            public void onActivityDestroyed(@NonNull Activity activity) {

            }
        });
```
以上方式，还可以使用Activity计数法来判断当前应用是否是前后台应用。那么lifecycle组件的原理有什么不一样的地方吗？其实lifecycle组件也是使用以上方式实现的，只不过官方给我们定义了一个样板代码，使用起来更加方便，不用重复造轮子。

Lifecycle版本
```
    def lifecycle_version = "2.4.0"
    implementation "androidx.lifecycle:lifecycle-runtime-ktx:$lifecycle_version"
    implementation "androidx.lifecycle:lifecycle-process:$lifecycle_version"
```

Lifecycle的使用：
```
//在Activity/Fragment/Application中把观察者注册到Lifecycle中
getLifecycle().addObserver(new MyLifecycle());

//Lifecycle观察者
class MyLifecycle implements DefaultLifecycleObserver{

    @Override
    public void onCreate(@NonNull LifecycleOwner owner) {
    }

    @Override
    public void onStart(@NonNull LifecycleOwner owner) {
    }

    @Override
    public void onResume(@NonNull LifecycleOwner owner) {
    }

    @Override
    public void onPause(@NonNull LifecycleOwner owner) {
    }

    @Override
    public void onStop(@NonNull LifecycleOwner owner) {
    }

    @Override
    public void onDestroy(@NonNull LifecycleOwner owner) {
    }
}
```


onCreate方法的调用栈：
```
at com.example.myapplication.MainActivity$MyLifecycle.onCreate(MainActivity.java:78)
        at androidx.lifecycle.FullLifecycleObserverAdapter.onStateChanged(FullLifecycleObserverAdapter.java:36)
        at androidx.lifecycle.LifecycleRegistry$ObserverWithState.dispatchEvent(LifecycleRegistry.java:354)
        at androidx.lifecycle.LifecycleRegistry.forwardPass(LifecycleRegistry.java:265)
        at androidx.lifecycle.LifecycleRegistry.sync(LifecycleRegistry.java:307)
        at androidx.lifecycle.LifecycleRegistry.moveToState(LifecycleRegistry.java:148)
        at androidx.lifecycle.LifecycleRegistry.handleLifecycleEvent(LifecycleRegistry.java:134)
        at androidx.lifecycle.ReportFragment.dispatch(ReportFragment.java:68)
        at androidx.lifecycle.ReportFragment$LifecycleCallbacks.onActivityPostCreated(ReportFragment.java:178)
        at android.app.Activity.dispatchActivityPostCreated(Activity.java:1242)
        at android.app.Activity.performCreate(Activity.java:8061)
        at android.app.Activity.performCreate(Activity.java:8041)
        at android.app.Instrumentation.callActivityOnCreate(Instrumentation.java:1307)
        at android.app.ActivityThread.performLaunchActivity(ActivityThread.java:3397)
        at android.app.ActivityThread.handleLaunchActivity(ActivityThread.java:3561)
```

ReportFragment中定义了一个内部类LifecycleCallbacks实现Application.ActivityLifecycleCallbacks接口，Application.ActivityLifecycleCallbacks是android中监听Activity/Fragment生命周期的统一接口。
LifecycleCallbacks监听并调用dispatch()分发生命周期。

ReportFragment.java
```
static class LifecycleCallbacks implements Application.ActivityLifecycleCallbacks {

        static void registerIn(Activity activity) {
            activity.registerActivityLifecycleCallbacks(new LifecycleCallbacks());
        }

        @Override
        public void onActivityCreated(@NonNull Activity activity,
                @Nullable Bundle bundle) {
        }

        @Override
        public void onActivityPostCreated(@NonNull Activity activity,
                @Nullable Bundle savedInstanceState) {
            dispatch(activity, Lifecycle.Event.ON_CREATE);
        }

        @Override
        public void onActivityStarted(@NonNull Activity activity) {
        }

        @Override
        public void onActivityPostStarted(@NonNull Activity activity) {
            dispatch(activity, Lifecycle.Event.ON_START);
        }

        @Override
        public void onActivityResumed(@NonNull Activity activity) {
        }

        @Override
        public void onActivityPostResumed(@NonNull Activity activity) {
            dispatch(activity, Lifecycle.Event.ON_RESUME);
        }

        @Override
        public void onActivityPrePaused(@NonNull Activity activity) {
            dispatch(activity, Lifecycle.Event.ON_PAUSE);
        }

        @Override
        public void onActivityPaused(@NonNull Activity activity) {
        }

        @Override
        public void onActivityPreStopped(@NonNull Activity activity) {
            dispatch(activity, Lifecycle.Event.ON_STOP);
        }

        @Override
        public void onActivityStopped(@NonNull Activity activity) {
        }

        @Override
        public void onActivitySaveInstanceState(@NonNull Activity activity,
                @NonNull Bundle bundle) {
        }

        @Override
        public void onActivityPreDestroyed(@NonNull Activity activity) {
            dispatch(activity, Lifecycle.Event.ON_DESTROY);
        }

        @Override
        public void onActivityDestroyed(@NonNull Activity activity) {
        }
    }
	
	//获取当前Activity的LifecycleRegistry对象，把生命周期封装成Event，分发到LifecycleRegistry中处理
    static void dispatch(@NonNull Activity activity, @NonNull Lifecycle.Event event) {
        if (activity instanceof LifecycleRegistryOwner) {
            ((LifecycleRegistryOwner) activity).getLifecycle().handleLifecycleEvent(event);
            return;
        }

        if (activity instanceof LifecycleOwner) {
            Lifecycle lifecycle = ((LifecycleOwner) activity).getLifecycle();
            if (lifecycle instanceof LifecycleRegistry) {
                ((LifecycleRegistry) lifecycle).handleLifecycleEvent(event);
            }
        }
    }
```

ComponentActivity中创建了LifecycleRegistry对象并通过getLifecycle()对外提供访问接口。我们日常使用的Activity基本都继承自ComponentActivity，每个Activity/Fragment都有自己的LifecycleRegistry对象。
ComponentActivity.java
```
    private final LifecycleRegistry mLifecycleRegistry = new LifecycleRegistry(this);

    public Lifecycle getLifecycle() {
        return mLifecycleRegistry;
    }
```

由于Activity提供了getLifecycle()方法获取LifecycleRegistry对象，我们可以很方便的通过getLifecycle().addObserver()注册观察者，去感知生命周期的变化。

LifecycleRegistry.java
```
    //使用Map保存观察者
    private FastSafeIterableMap<LifecycleObserver, ObserverWithState> mObserverMap =
            new FastSafeIterableMap<>();
    public void addObserver(@NonNull LifecycleObserver observer) {
        enforceMainThreadIfNeeded("addObserver");
        State initialState = mState == DESTROYED ? DESTROYED : INITIALIZED;
        ObserverWithState statefulObserver = new ObserverWithState(observer, initialState);
        ObserverWithState previous = mObserverMap.putIfAbsent(observer, statefulObserver);

        if (previous != null) {
            return;
        }
        LifecycleOwner lifecycleOwner = mLifecycleOwner.get();
        if (lifecycleOwner == null) {
            // it is null we should be destroyed. Fallback quickly
            return;
        }

        boolean isReentrance = mAddingObserverCounter != 0 || mHandlingEvent;
        State targetState = calculateTargetState(observer);
        mAddingObserverCounter++;
        while ((statefulObserver.mState.compareTo(targetState) < 0
                && mObserverMap.contains(observer))) {
            pushParentState(statefulObserver.mState);
            final Event event = Event.upFrom(statefulObserver.mState);
            if (event == null) {
                throw new IllegalStateException("no event up from " + statefulObserver.mState);
            }
            statefulObserver.dispatchEvent(lifecycleOwner, event);
            popParentState();
            // mState / subling may have been changed recalculate
            targetState = calculateTargetState(observer);
        }

        if (!isReentrance) {
            // we do sync only on the top level.
            sync();
        }
        mAddingObserverCounter--;
    }
```
FullLifecycleObserverAdapter和LifecycleRegistry的关系，以及LifecycleRegistry中状态的处理，观察者的封装稍微有点复杂，后面感兴趣的时候可以再看一下。
FullLifecycleObserverAdapter.java
```
public void onStateChanged(@NonNull LifecycleOwner source, @NonNull Lifecycle.Event event) {
        switch (event) {
            case ON_CREATE:
                mFullLifecycleObserver.onCreate(source);
                break;
            case ON_START:
                mFullLifecycleObserver.onStart(source);
                break;
            case ON_RESUME:
                mFullLifecycleObserver.onResume(source);
                break;
            case ON_PAUSE:
                mFullLifecycleObserver.onPause(source);
                break;
            case ON_STOP:
                mFullLifecycleObserver.onStop(source);
                break;
            case ON_DESTROY:
                mFullLifecycleObserver.onDestroy(source);
                break;
            case ON_ANY:
                throw new IllegalArgumentException("ON_ANY must not been send by anybody");
        }
        if (mLifecycleEventObserver != null) {
            mLifecycleEventObserver.onStateChanged(source, event);
        }
    }
```

Lifecycle组件也提供了一个样板代码去判断是否是前后台应用，它的使用跟Lifecycle差不多。原理上也是使用了两个int类型的变量来记录当前应用有多少个Activity处于onStart，onResume状态，如果数量为变为0则通知观察者进入了后台模式。
如果数量大于0则通知观察者进入了前台模式。
```
ProcessLifecycleOwner.get().getLifecycle().addObserver(new MyLifecycle());
```

ProcessLifecycleOwner.java
```
void attach(Context context) {
        mHandler = new Handler();
        mRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE);
        Application app = (Application) context.getApplicationContext();
        app.registerActivityLifecycleCallbacks(new EmptyActivityLifecycleCallbacks() {
            @RequiresApi(29)
            @Override
            public void onActivityPreCreated(@NonNull Activity activity,
                    @Nullable Bundle savedInstanceState) {
                // We need the ProcessLifecycleOwner to get ON_START and ON_RESUME precisely
                // before the first activity gets its LifecycleOwner started/resumed.
                // The activity's LifecycleOwner gets started/resumed via an activity registered
                // callback added in onCreate(). By adding our own activity registered callback in
                // onActivityPreCreated(), we get our callbacks first while still having the
                // right relative order compared to the Activity's onStart()/onResume() callbacks.
                activity.registerActivityLifecycleCallbacks(new EmptyActivityLifecycleCallbacks() {
                    @Override
                    public void onActivityPostStarted(@NonNull Activity activity) {
                        activityStarted();
                    }

                    @Override
                    public void onActivityPostResumed(@NonNull Activity activity) {
                        activityResumed();
                    }
                });
            }

            @Override
            public void onActivityCreated(Activity activity, Bundle savedInstanceState) {
                // Only use ReportFragment pre API 29 - after that, we can use the
                // onActivityPostStarted and onActivityPostResumed callbacks registered in
                // onActivityPreCreated()
                if (Build.VERSION.SDK_INT < 29) {
                    ReportFragment.get(activity).setProcessListener(mInitializationListener);
                }
            }

            @Override
            public void onActivityPaused(Activity activity) {
                activityPaused();//监听onPause()生命周期，即使某个Activity执行了onPause()，如果mResumedCounter不为0，也不会通知观察者
            }

            @Override
            public void onActivityStopped(Activity activity) {
                activityStopped();//监听onStop()生命周期，即使某个Activity执行了onStop()，如果mStartedCounter不为0，也不会通知观察者
            }
        });
    }
	
	//监听onStart()，onResume()两种生命周期，当应用切换到前台时mStartedCounter或者mResumedCounter自加一
    ActivityInitializationListener mInitializationListener =
            new ActivityInitializationListener() {
                @Override
                public void onCreate() {
                }

                @Override
                public void onStart() {
                    activityStarted();
                }

                @Override
                public void onResume() {
                    activityResumed();
                }
            };
	//切换后台的时候，postDelayed一个Handler消息出来，延迟700ms通知观察者，当前已经进入后台模式
    private Runnable mDelayedPauseRunnable = new Runnable() {
        @Override
        public void run() {
            dispatchPauseIfNeeded();
            dispatchStopIfNeeded();
        }
    };

    void activityStarted() {
        mStartedCounter++;
        if (mStartedCounter == 1 && mStopSent) {当前onStart状态的Activity数量为1，回调观察者的onStart()方法
            mRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START);
            mStopSent = false;
        }
    }

    void activityResumed() {
        mResumedCounter++;
        if (mResumedCounter == 1) {当前onResume状态的Activity数量为1，回调观察者的onResume()方法
            if (mPauseSent) {
                mRegistry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME);
                mPauseSent = false;
            } else {
                mHandler.removeCallbacks(mDelayedPauseRunnable);
            }
        }
    }

    //postDelayed一个Handler消息出来，延迟700ms通知观察者，当前已经进入后台模式
    void activityPaused() {
        mResumedCounter--;
        if (mResumedCounter == 0) {
            mHandler.postDelayed(mDelayedPauseRunnable, TIMEOUT_MS);
        }
    }

    void activityStopped() {
        mStartedCounter--;
        dispatchStopIfNeeded();
    }

    void dispatchPauseIfNeeded() {
        if (mResumedCounter == 0) {//当前onResume状态的Activity数量为0，回调观察者的onPause()方法
            mPauseSent = true;
            mRegistry.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE);
        }
    }

    void dispatchStopIfNeeded() {
        if (mStartedCounter == 0 && mPauseSent) {//当前onStart状态的Activity数量为0，回调观察者的onStop()方法
            mRegistry.handleLifecycleEvent(Lifecycle.Event.ON_STOP);
            mStopSent = true;
        }
    }
```
