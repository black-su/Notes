# Fragment + ViewPager源码分析


参考：

https://blog.csdn.net/u011240877/article/details/78132990

https://www.jianshu.com/p/6ee2a817c28f

https://www.jianshu.com/p/043020843899



Activity的attach触发FragmentController执行attachHost()操作
```
    final void attach(Context context, ActivityThread aThread,
            Instrumentation instr, IBinder token, int ident,
            Application application, Intent intent, ActivityInfo info,
            CharSequence title, Activity parent, String id,
            NonConfigurationInstances lastNonConfigurationInstances,
            Configuration config, String referrer, IVoiceInteractor voiceInteractor,
            Window window, ActivityConfigCallback activityConfigCallback) {
        attachBaseContext(context);

        mFragments.attachHost(null /*parent*/);

        ......
    }
```

同理，Activity的其他生命周期同样会触发FragmentController执行相应的操作，比如

```
Activity.onCreate()  ->  mFragments.dispatchCreate();

Activity.performStart()  ->  mFragments.dispatchStart();

Activity.performResume()  ->  mFragments.dispatchResume();

Activity.performPause()  ->  mFragments.dispatchPause();

Activity.performStop()  ->  mFragments.dispatchStop();

Activity.performDestroy()  ->  mFragments.dispatchDestroy();

```
这里的操作基本包括了Activity的生命周期，但是Fragment会比Activity多出onCreateView(),onActivityCreate()，onDestroyView()这些生命周期，是因为Fragment需要把一些前期工作分层处理，比如在Fragment.onCreateView()中提供自己的xml布局，当把这个布局addView()到的mContainer中并设置可见后，Fragment.onViewCreated()会执行。之后会执行Fragment.onActivityCreate()代表fragment的create状态结束。以上的处理都是在切换mState状态到CREATED后，在CREATED处理时划分的，详情看下面介绍。

FragmentController在这里起到了一个中转站的作用，实际上的生命周期还是由FragmentManagerImpl来完成。

FragmentController.java
```
    public void dispatchCreate() {
        mHost.mFragmentManager.dispatchCreate();
    }
    public void dispatchActivityCreated() {
        mHost.mFragmentManager.dispatchActivityCreated();
    }
```

FragmentManagerImpl中把各个生命周期用int类型的state来表示，当生命周期转换时，调用moveToState()来改变state状态，以执行不同的操作，这个就使用到了状态模式。

FragmentManagerImpl.java
```
    //mState状态的种类
    static final int INITIALIZING = 0;     // Not yet created.
    static final int CREATED = 1;          // Created.
    static final int ACTIVITY_CREATED = 2; // Fully created, not started.
    static final int STARTED = 3;          // Created and started, not resumed.
    static final int RESUMED = 4;          // Created started and resumed.

    int mState = INITIALIZING;

    public void dispatchCreate() {
        mStateSaved = false;
        mStopped = false;
        dispatchStateChange(Fragment.CREATED);
    }

    public void dispatchActivityCreated() {
        mStateSaved = false;
        mStopped = false;
        dispatchStateChange(Fragment.ACTIVITY_CREATED);
    }
    private void dispatchStateChange(int nextState) {
        try {
            mExecutingActions = true;
            moveToState(nextState, false);
        } finally {
            mExecutingActions = false;
        }
        execPendingActions();
    }
    //只显示Fragment.CREATED状态时的处理
    void moveToState(Fragment f, int newState, int transit, int transitionStyle,
                     boolean keepActive) {
                ......
                case Fragment.CREATED:
                    // We want to unconditionally run this anytime we do a moveToState that
                    // moves the Fragment above INITIALIZING, including cases such as when
                    // we move from CREATED => CREATED as part of the case fall through above.
                    if (newState > Fragment.INITIALIZING) {
                        ensureInflatedFragmentView(f);
                    }

                    if (newState > Fragment.CREATED) {
                        if (DEBUG) Log.v(TAG, "moveto ACTIVITY_CREATED: " + f);
                        if (!f.mFromLayout) {
                            ViewGroup container = null;
                            if (f.mContainerId != 0) {
                                if (f.mContainerId == View.NO_ID) {
                                    throwException(new IllegalArgumentException(
                                            "Cannot create fragment "
                                                    + f
                                                    + " for a container view with no id"));
                                }
                                //拿到container布局
                                container = (ViewGroup) mContainer.onFindViewById(f.mContainerId);
                                if (container == null && !f.mRestored) {
                                    String resName;
                                    try {
                                        resName = f.getResources().getResourceName(f.mContainerId);
                                    } catch (Resources.NotFoundException e) {
                                        resName = "unknown";
                                    }
                                    throwException(new IllegalArgumentException(
                                            "No view found for id 0x"
                                                    + Integer.toHexString(f.mContainerId) + " ("
                                                    + resName
                                                    + ") for fragment " + f));
                                }
                            }
                            f.mContainer = container;
                            //拿到我们自定义的xml布局
                            f.performCreateView(f.performGetLayoutInflater(
                                    f.mSavedFragmentState), container, f.mSavedFragmentState);
                            if (f.mView != null) {
                                f.mInnerView = f.mView;
                                f.mView.setSaveFromParentEnabled(false);
                                if (container != null) {
                                    //把自定义xml布局View添加到container中
                                    container.addView(f.mView);
                                }
                                //设置fragment可见
                                if (f.mHidden) {
                                    f.mView.setVisibility(View.GONE);
                                }
                                //回调生命周期
                                f.onViewCreated(f.mView, f.mSavedFragmentState);
                                dispatchOnFragmentViewCreated(f, f.mView, f.mSavedFragmentState,
                                        false);
                                // Only animate the view if it is visible. This is done after
                                // dispatchOnFragmentViewCreated in case visibility is changed
                                f.mIsNewlyAdded = (f.mView.getVisibility() == View.VISIBLE)
                                        && f.mContainer != null;
                            } else {
                                f.mInnerView = null;
                            }
                        }

                        f.performActivityCreated(f.mSavedFragmentState);
                        dispatchOnFragmentActivityCreated(f, f.mSavedFragmentState, false);
                        if (f.mView != null) {
                            f.restoreViewState(f.mSavedFragmentState);
                        }
                        f.mSavedFragmentState = null;
                    }
    ......
    }
```

一个Activity中可以显示多个Fragment并且互相切换，多个Fragment的管理如果放在Activity中会使得Activity过于庞大，因此Fragment的管理有FragmentManager来完成，FragmentManager使用了FragmentTransaction以事务的机制来完成对Fragment的切换，隐藏和显示。
```
FragmentManager mFragmentManager = getSupportFragmentManager();
FragmentTransaction fragmentTransaction = mFragmentManager.beginTransaction();
fragmentTransaction.replace(R.id.fl_content, fragment);
fragmentTransaction.commitAllowingStateLoss();
```

FragmentTransaction的源码较多，有空可以去看一下。总体上是FragmentTransaction.replace()或者FragmentTransaction.remove()的时候，先通过FragmentManager改变一下fragment集合中的指定fragment。然后调用FragmentManager.moveToState()去修改状态，达到切换，删除，隐藏，显示的目的。
BackStackRecord.java
```
    void executeOps() {
        final int numOps = mOps.size();
        for (int opNum = 0; opNum < numOps; opNum++) {
            final Op op = mOps.get(opNum);
            final Fragment f = op.mFragment;
            if (f != null) {
                f.setNextTransition(mTransition, mTransitionStyle);
            }
            switch (op.mCmd) {
                case OP_ADD:
                    f.setNextAnim(op.mEnterAnim);
                    mManager.addFragment(f, false);
                    break;
                case OP_REMOVE:
                    f.setNextAnim(op.mExitAnim);
                    mManager.removeFragment(f);
                    break;
                case OP_HIDE:
                    f.setNextAnim(op.mExitAnim);
                    mManager.hideFragment(f);
                    break;
                case OP_SHOW:
                    f.setNextAnim(op.mEnterAnim);
                    mManager.showFragment(f);
                    break;
                case OP_DETACH:
                    f.setNextAnim(op.mExitAnim);
                    mManager.detachFragment(f);
                    break;
                case OP_ATTACH:
                    f.setNextAnim(op.mEnterAnim);
                    mManager.attachFragment(f);
                    break;
                case OP_SET_PRIMARY_NAV:
                    mManager.setPrimaryNavigationFragment(f);
                    break;
                case OP_UNSET_PRIMARY_NAV:
                    mManager.setPrimaryNavigationFragment(null);
                    break;
                case OP_SET_MAX_LIFECYCLE:
                    mManager.setMaxLifecycle(f, op.mCurrentMaxState);
                    break;
                default:
                    throw new IllegalArgumentException("Unknown cmd: " + op.mCmd);
            }
            if (!mReorderingAllowed && op.mCmd != OP_ADD && f != null) {
                mManager.moveFragmentToExpectedState(f);
            }
        }
        if (!mReorderingAllowed) {
            // Added fragments are added at the end to comply with prior behavior.
            mManager.moveToState(mManager.mCurState, true);
        }
    }
```


----------------------------------------


1. 自定义PagerAdapter

使用PagerAdapter可以自定义viewpager中的布局，我们只要传入数据并绑定布局，做好view的回收即可

```

    class MyPagerAdapter<T> extends PagerAdapter {

        private List<T> mDataList;
        public MyPagerAdapter(List<T> mDataList){
            this.mDataList =mDataList;
        }

        /**
         * 创建item布局。并把item布局add到viewPager中。
         * setOffscreenPageLimit(3) 可以设置缓存，表示除当前item外，前后各缓存3个item。这样随着viewpager的滑动，item缓存数量在1～6个之间。
         * 可以额外保存这个方法中inflate出来的view，复用。LruCache
         * @param container
         * @param position
         * @return
         */
        @NonNull
        @Override
        public Object instantiateItem(@NonNull ViewGroup container, int position) {
            View view = LayoutInflater.from(container.getContext()).inflate(R.layout.example_fragment,null);
            container.addView(view);
            //TODO : view绑定数据
            ImageView imageView = view.findViewById(R.id.image);
            imageView.setBackgroundResource(R.drawable.picture1);

            TextView textView = view.findViewById(R.id.test);
            textView.setText((String)mDataList.get(position));
            return view;
        }

        //从viewPager中remove掉当前position的item
        @Override
        public void destroyItem(@NonNull ViewGroup container, int position, @NonNull Object object) {
            container.removeView((View)object);
        }

        @Override
        public boolean isViewFromObject(@NonNull View view, @NonNull Object object) {
            return view == object;
        }

        @Override
        public int getCount() {
            if(mDataList != null){
                return mDataList.size();
            }
            return 0;
        }
    }
```


2. 自定义FragmentPagerAdapter


FragmentPagerAdapter继承自PagerAdapter。如果PagerAdapter面对的是自定义View的处理，那FragmentPagerAdapter就是面对Fragment的处理。PagerAdapter中要在instantiateItem()中inflate()来创建View对象并绑定到viewPager中。但是FragmentPagerAdapter中并不需要这么做，只需要重写getItem()并返回Fragment对象即可，至于是传入一个Fragment数组，还是在getItem()中直接new Fragment()，可以看情况选择。使用Fragment需要处理它的FragmentTransaction事务，系统已经帮我们把这个工作了。因此FragmentPagerAdapter的使用如下：

```
    class MyFragmentPagerAdapter<T> extends FragmentPagerAdapter {

        private List<T> mDataList;
        public MyFragmentPagerAdapter(@NonNull FragmentManager fm, List<T> mDataList) {
            super(fm,BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT);
            this.mDataList = mDataList;
        }
        
        @Override
        public int getCount() {
            if(mDataList != null ){
                return mDataList.size();
            }
            return 0;
        }

        @NonNull
        @Override
        public Fragment getItem(int position) {
            //TODO : 创建Fragment对象，绑定数据到Fragment中
            ExampleFragment fragment = new ExampleFragment((Integer) mDataList.get(position),""+position);
            return fragment;
        }
    }
```

首尾无限循环

```


```


FragmentStatePagerAdapter的用法和FragmentPagerAdapter一样，只不过使用的场景不一样。
FragmentStatePagerAdapter 适合大量页面，不断重建和销毁
FragmentPagerAdapter 适合少量页面，常驻内存。

FragmentStatePagerAdapter中每次滑动页面时都会调用getItem()生成一个新的Fragment对象，并交给FragmentManager管理，FragmentManager会把这个Fragment加入到集合中。划出页面后会调用destroyItem()，并通过mCurTransaction.remove(fragment)来删除集合中的数据，让Fragment被回收。
FragmentPagerAdapter中会调用getItem()生成新的Fragment对象，并加入到FragmentManager的集合中进行管理。划出页面后会调用destroyItem()，但是只是单纯的mCurTransaction.detach(fragment)，并不会把集合中的Fragment删除，下一次再划到此页面时，不用通过getItem()生成新的Fragment对象，只需要mCurTransaction.attach(fragment)即可复用。如果页面数量太大，这种方式就非常消耗内存。

3. 原理

