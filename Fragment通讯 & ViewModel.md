#Fragment通讯 & ViewModel

https://developer.android.google.cn/guide/fragments/communicate

Fragment与Activity之间的通讯一般有三种，一种是两者之间以回调的方式通讯，另外一种通过setesule()来实现传递一次性值，最后一种是android提供的ViewModel方式。

ViewModel的使用：

Activity:
```
        ItemViewModel viewModel = new ViewModelProvider(this, new ViewModelProvider.Factory() {
            @NonNull
            @Override
            public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
                try {
                    T t = modelClass.newInstance();
                    return t;
                } catch (IllegalAccessException | java.lang.InstantiationException e) {
                    e.printStackTrace();
                }
                return null;
            }
        }).get(ItemViewModel.class);
        viewModel.selectItem("i am use viewmodel");
```

Fragment:
这里初始化ViewModelProvider时第一个参数必须是getActivity()，不能是this。
```
        ItemViewModel viewModel = new ViewModelProvider(getActivity(), new ViewModelProvider.Factory() {
            @NonNull
            @Override
            public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
                try {
                    T t = modelClass.newInstance();
                    return t;
                } catch (IllegalAccessException | java.lang.InstantiationException e) {
                    e.printStackTrace();
                }
                return null;
            }
        }).get(ItemViewModel.class);
        viewModel.getSelectedItem().observe(this, item -> {
            // Perform an action with the latest item data
        });
```

实现ViewModel，定义自己的数据模板:
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

以上获取ItemViewModel后，无论在Fragment还是在Activity中的任意一个地方调用viewModel.selectItem()更改数据，会回调viewModel.getSelectedItem().observe()。


ViewModel+ViewModelProvider的源码还是蛮简单的。

ViewModelProvider.java
```
    //拿到Activity/Fragment中的ViewModelStore对象
    public ViewModelProvider(@NonNull ViewModelStoreOwner owner, @NonNull Factory factory) {
        this(owner.getViewModelStore(), factory);
    }

    public ViewModelProvider(@NonNull ViewModelStore store, @NonNull Factory factory) {
        mFactory = factory;
        mViewModelStore = store;
    }

    public <T extends ViewModel> T get(@NonNull String key, @NonNull Class<T> modelClass) {
        ViewModel viewModel = mViewModelStore.get(key);
        //先从Activity/Fragment中的ViewModelStore查找ViewModel对象
        if (modelClass.isInstance(viewModel)) {
            //noinspection unchecked
            return (T) viewModel;
        } else {
            //noinspection StatementWithEmptyBody
            if (viewModel != null) {
                // TODO: log a warning.
            }
        }
        //以上查找失败，调用Factory.create()初始化一个ViewModel对象
        if (mFactory instanceof KeyedFactory) {
            viewModel = ((KeyedFactory) (mFactory)).create(key, modelClass);
        } else {
            viewModel = (mFactory).create(modelClass);
        }
        mViewModelStore.put(key, viewModel);
        //noinspection unchecked
        return (T) viewModel;
    }
```
在初始化ViewModelProvider时，一般第一个参数我们是传入this，也就是Activity或者Fragment的对象。通过owner.getViewModelStore()获取到的就是Activity/Fragment中的ViewModelStore对象。Fragment和ComponentActivity都实现了ViewModelStoreOwner接口类。而ViewModelStore本身就是一个以HashMap保存数据的一个封装类。也就是说Fragment和ComponentActivity中相当于初始化了类似HashMap的数据结构来保存数据，在Activity/Fragment的生命周期内这个数据不会被清除掉。

```
public class ViewModelStore {

    private final HashMap<String, ViewModel> mMap = new HashMap<>();

    final void put(String key, ViewModel viewModel) {
        ViewModel oldViewModel = mMap.put(key, viewModel);
        if (oldViewModel != null) {
            oldViewModel.onCleared();
        }
    }

    final ViewModel get(String key) {
        return mMap.get(key);
    }

    Set<String> keys() {
        return new HashSet<>(mMap.keySet());
    }

    /**
     *  Clears internal storage and notifies ViewModels that they are no longer used.
     */
    public final void clear() {
        for (ViewModel vm : mMap.values()) {
            vm.clear();
        }
        mMap.clear();
    }
}
```

而这个数据结构什么时候清除掉它保存的数据释放内存呢？以ComponentActivity为例，ComponentActivity初始化的时候就监听了Activity的生命周期，当Activity的生命周期为onDestroy时，且isChangingConfigurations()返回false的时候删除ViewModel中的数据释放内存。

```
    public ComponentActivity() {
        Lifecycle lifecycle = getLifecycle();
        //noinspection ConstantConditions
        if (lifecycle == null) {
            throw new IllegalStateException("getLifecycle() returned null in ComponentActivity's "
                    + "constructor. Please make sure you are lazily constructing your Lifecycle "
                    + "in the first call to getLifecycle() rather than relying on field "
                    + "initialization.");
        }
        if (Build.VERSION.SDK_INT >= 19) {
            getLifecycle().addObserver(new LifecycleEventObserver() {
                @Override
                public void onStateChanged(@NonNull LifecycleOwner source,
                        @NonNull Lifecycle.Event event) {
                    if (event == Lifecycle.Event.ON_STOP) {
                        Window window = getWindow();
                        final View decor = window != null ? window.peekDecorView() : null;
                        if (decor != null) {
                            decor.cancelPendingInputEvents();
                        }
                    }
                }
            });
        }
        //监听Activity的生命周期，删除ViewModel中的数据释放内存
        getLifecycle().addObserver(new LifecycleEventObserver() {
            @Override
            public void onStateChanged(@NonNull LifecycleOwner source,
                    @NonNull Lifecycle.Event event) {
                if (event == Lifecycle.Event.ON_DESTROY) {
                    if (!isChangingConfigurations()) {
                        getViewModelStore().clear();
                    }
                }
            }
        });

        if (19 <= SDK_INT && SDK_INT <= 23) {
            getLifecycle().addObserver(new ImmLeaksCleaner(this));
        }
    }
```


这里的isChangingConfigurations()跟Activity的横竖屏状态有关，暂时还搞不清楚什么情况下返回false/true。什么都不设置的情况下，横竖屏，都会执行到OnDestroy()方法，但是这里isChangingConfigurations()返回ture，也就是不清除ViewModelStore。但是如果是退出Activity，执行OnDestroy()方法时，这里的isChangingConfigurations()返回false，会清除ViewModelStore。

