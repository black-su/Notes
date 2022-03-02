#  Jetpack组件之LiveData


LiveData通常与ViewModel结合在一起使用：
```
public class ItemViewModel extends ViewModel {
    private final MutableLiveData<String> selectedItem = new MutableLiveData<String>();
    public void selectItem(String str) {
        selectedItem.setValue(str);
    }
    public LiveData<String> getSelectedItem() {
        return selectedItem;
    }
}
```

```
        ViewModelProvider.Factory factory = new ViewModelProvider.AndroidViewModelFactory(getApplication());
        viewModel = new ViewModelProvider(this).get(ItemViewModel.class);
        findViewById(R.id.button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                viewModel.selectItem("Test");//LiveData数据发生变化
            }
        });
        viewModel.getSelectedItem().observe(this, new Observer<String>() {
            @Override
            public void onChanged(String s) {//观察数据变化，更新UI
            }
        });
```


LiveData有两种观察者，一个是没有生命周期感知的观察者AlwaysActiveObserver，无论当前Activity/Fragment处于哪种生命周期，只要LiveData发生变化，都会通知观察者。
一个是LifecycleBoundObserver，使用Lifecycle组件去感知Activity/Fragment的生命周期，DESTROYED状态下，会移除观察者，不会进行通知。

```
    //没有生命周期感知的观察者，shouldBeActive()总是返回true
    private class AlwaysActiveObserver extends ObserverWrapper {

        AlwaysActiveObserver(Observer<? super T> observer) {
            super(observer);
        }

        @Override
        boolean shouldBeActive() {
            return true;
        }
    }
	
   //
   class LifecycleBoundObserver extends ObserverWrapper implements LifecycleEventObserver {
        @NonNull
        final LifecycleOwner mOwner;

        LifecycleBoundObserver(@NonNull LifecycleOwner owner, Observer<? super T> observer) {
            super(observer);
            mOwner = owner;
        }

        @Override
        boolean shouldBeActive() {
            return mOwner.getLifecycle().getCurrentState().isAtLeast(STARTED);
        }

        @Override
        public void onStateChanged(@NonNull LifecycleOwner source,
                @NonNull Lifecycle.Event event) {
            Lifecycle.State currentState = mOwner.getLifecycle().getCurrentState();
            if (currentState == DESTROYED) {//DESTROYED状态下移除观察者
                removeObserver(mObserver);
                return;
            }
            Lifecycle.State prevState = null;
            while (prevState != currentState) {
                prevState = currentState;
                activeStateChanged(shouldBeActive());
                currentState = mOwner.getLifecycle().getCurrentState();
            }
        }

        @Override
        boolean isAttachedTo(LifecycleOwner owner) {
            return mOwner == owner;
        }

        @Override
        void detachObserver() {
            mOwner.getLifecycle().removeObserver(this);
        }
    }
	
    private abstract class ObserverWrapper {
        final Observer<? super T> mObserver;
        boolean mActive;
        int mLastVersion = START_VERSION;

        ObserverWrapper(Observer<? super T> observer) {
            mObserver = observer;
        }

        abstract boolean shouldBeActive();

        boolean isAttachedTo(LifecycleOwner owner) {
            return false;
        }

        void detachObserver() {
        }

        void activeStateChanged(boolean newActive) {
            if (newActive == mActive) {
                return;
            }
            // immediately set active state, so we'd never dispatch anything to inactive
            // owner
            mActive = newActive;
            changeActiveCounter(mActive ? 1 : -1);
            if (mActive) {
                dispatchingValue(this);//通知观察者
            }
        }
    }
	
	//提供了无生命周期感知的观察者注册接口，使用AlwaysActiveObserver封装原始观察者。
	@MainThread
    public void observeForever(@NonNull Observer<? super T> observer) {
        assertMainThread("observeForever");//必须运行在主线程中,否则抛出异常
        AlwaysActiveObserver wrapper = new AlwaysActiveObserver(observer);
        ObserverWrapper existing = mObservers.putIfAbsent(observer, wrapper);//把注册的观察者都保存到Map集合中
        if (existing instanceof LiveData.LifecycleBoundObserver) {
            throw new IllegalArgumentException("Cannot add the same observer"
                    + " with different lifecycles");
        }
        if (existing != null) {
            return;
        }
        wrapper.activeStateChanged(true);//
    }
	
	//提供了有生命周期感知的观察者注册接口，使用LifecycleBoundObserver封装原始观察者。
	@MainThread
    public void observe(@NonNull LifecycleOwner owner, @NonNull Observer<? super T> observer) {
        assertMainThread("observe");//必须运行在主线程中,否则抛出异常
        if (owner.getLifecycle().getCurrentState() == DESTROYED) {//DESTROYED状态下不允许注册观察者
            // ignore
            return;
        }
        LifecycleBoundObserver wrapper = new LifecycleBoundObserver(owner, observer);
        ObserverWrapper existing = mObservers.putIfAbsent(observer, wrapper);//把注册的观察者都保存到Map集合中
        if (existing != null && !existing.isAttachedTo(owner)) {
            throw new IllegalArgumentException("Cannot add the same observer"
                    + " with different lifecycles");
        }
        if (existing != null) {
            return;
        }
        owner.getLifecycle().addObserver(wrapper);//通过Lifecycle组件感知生命周期
    }
	
	
   void dispatchingValue(@Nullable ObserverWrapper initiator) {
        if (mDispatchingValue) {
            mDispatchInvalidated = true;
            return;
        }
        mDispatchingValue = true;
        do {
            mDispatchInvalidated = false;
            if (initiator != null) {
                considerNotify(initiator);
                initiator = null;
            } else {//遍历通知所有观察者
                for (Iterator<Map.Entry<Observer<? super T>, ObserverWrapper>> iterator =
                        mObservers.iteratorWithAdditions(); iterator.hasNext(); ) {
                    considerNotify(iterator.next().getValue());
                    if (mDispatchInvalidated) {
                        break;
                    }
                }
            }
        } while (mDispatchInvalidated);
        mDispatchingValue = false;
    }
	
    private void considerNotify(ObserverWrapper observer) {
        if (!observer.mActive) {//非活跃状态(DESTROYED)的情况下直接返回，不通知
            return;
        }
        // Check latest state b4 dispatch. Maybe it changed state but we didn't get the event yet.
        //
        // we still first check observer.active to keep it as the entrance for events. So even if
        // the observer moved to an active state, if we've not received that event, we better not
        // notify for a more predictable notification order.
        if (!observer.shouldBeActive()) {//非活跃状态(DESTROYED)的情况下直接返回，不通知
            observer.activeStateChanged(false);
            return;
        }
        if (observer.mLastVersion >= mVersion) {
            return;
        }
        observer.mLastVersion = mVersion;
        observer.mObserver.onChanged((T) mData);//回调观察者实现的onChanged()方法
    }
	
    @MainThread
    protected void setValue(T value) {
        assertMainThread("setValue");//必须运行在主线程中,否则抛出异常
        mVersion++;
        mData = value;
        dispatchingValue(null);//通知观察者LiveData发生了变化
    }
	
	//提供了在非主线程下更改LiveData数据的接口
    protected void postValue(T value) {
        boolean postTask;
        synchronized (mDataLock) {
            postTask = mPendingData == NOT_SET;
            mPendingData = value;
        }
        if (!postTask) {
            return;
        }
        ArchTaskExecutor.getInstance().postToMainThread(mPostValueRunnable);
    }
	
    private final Runnable mPostValueRunnable = new Runnable() {
        @SuppressWarnings("unchecked")
        @Override
        public void run() {
            Object newValue;
            synchronized (mDataLock) {
                newValue = mPendingData;
                mPendingData = NOT_SET;
            }
            setValue((T) newValue);//非主线程下更改LiveData数据也是通过post到主线程然后执行setValue()
        }
    };
```