#android 内存泄漏




1. MAT的下载与安装：

https://www.pianshen.com/article/48051195949/

https://www.eclipse.org/mat/downloads.php

2. LeakCanary源码分析

使用
```
dependencies {
    debugCompile 'com.squareup.leakcanary:leakcanary-android:1.3'
    releaseCompile 'com.squareup.leakcanary:leakcanary-android-no-op:1.3'
}

```

```
        if (LeakCanary.isInAnalyzerProcess(this)) {
            // This process is dedicated to LeakCanary for heap analysis.
            // You should not init your app in this process.
            return;
        }
        mRefWatcher = LeakCanary.install(this);
```

kotlin新版本：

```
dependencies {
  // debugImplementation because LeakCanary should only run in debug builds.
  debugImplementation 'com.squareup.leakcanary:leakcanary-android:2.7'
}
```


https://blog.csdn.net/qq_20798591/article/details/104473357

http://blog.itpub.net/69912579/viewspace-2765601/


3.ss 