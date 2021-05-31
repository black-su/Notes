# Fragment + ViewPager源码分析


参考：

https://blog.csdn.net/u011240877/article/details/78132990

https://www.jianshu.com/p/6ee2a817c28f

https://www.jianshu.com/p/043020843899


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


3. 自定义