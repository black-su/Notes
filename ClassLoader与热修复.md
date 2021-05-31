#ClassLoader与热修复



1. openjdk源码下载：
http://jdk.java.net/java-se-ri/8-MR3

2. 使用jdk提供的dx工具包把class文件转换成dex文件：
在ubuntu系统中，jdk的安装目录下有一个Sdk/build-tools/28.0.3/dx文件。使用base命令运行此文件
```
bash '/home/sujianze/Android/Sdk/build-tools/28.0.3/dx' --dex --output=JniText.dex com/example/myapplication/JniText.class
```
使用dx时，有一点需要注意，一般我们的java文件都有一个包名package com.example.myapplication；我们需要在当前运行base命令的目录,和class的目录之间一定要隔着com.example.myapplication。否则会报以下错误：

```
UNEXPECTED TOP-LEVEL EXCEPTION:
java.lang.RuntimeException: /home/sujianze/JniText.class: file not found
    at com.android.dex.util.FileUtils.readFile(FileUtils.java:51)
    at com.android.dx.cf.direct.ClassPathOpener.processOne(ClassPathOpener.java:168)
    at com.android.dx.cf.direct.ClassPathOpener.process(ClassPathOpener.java:143)
    at com.android.dx.command.dexer.Main.processOne(Main.java:678)
    at com.android.dx.command.dexer.Main.processAllFiles(Main.java:575)
    at com.android.dx.command.dexer.Main.runMonoDex(Main.java:310)
    at com.android.dx.command.dexer.Main.runDx(Main.java:288)
    at com.android.dx.command.dexer.Main.main(Main.java:244)
    at com.android.dx.command.Main.main(Main.java:95)
1 error; aborting

或者

PARSE ERROR:
class name (com/example/java_lib/Loader) does not match path (Loader.class)
...while parsing Loader.class
1 error; aborting
```


3. ClassLoader的使用

自定义ClassLoader的处理分为class和dex的处理，两者有很大的不同。

自定义ClassLoader处理class文件：
```
    ClassLoader classLoader1 = new ClassLoader(){
        @Override
        public Class<?> loadClass(String s) throws ClassNotFoundException {
            System.out.println("URLClassLoader============loadClass:" + s);
            return super.loadClass(s);
        }

        @Override
        protected Class<?> findClass(String name) throws ClassNotFoundException {
            System.out.println("URLClassLoader============findClass:" + s);

            String path = "/home/sujianze/Android/ClassLoaderTest.class";
            try {
                InputStream ins = new FileInputStream(path);
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                int bufferSize = 4096;
                byte[] buffer = new byte[bufferSize];
                int bytesNumRead = 0;
                // 读取类文件的字节码
                while ((bytesNumRead = ins.read(buffer)) != -1) {
                    baos.write(buffer, 0, bytesNumRead);
                }
                return defineClass(name, baos.toByteArray(), 0, baos.toByteArray().length);
            } catch (IOException e) {
                e.printStackTrace();
            }
            return null;
        }
    };

    Class myclass = classLoader1.loadClass("com.example.myapplication.ClassLoaderTest");
            if(myclass != null){
        System.out.println("1111============"+myclass.toString());
    }
```
为了不破坏双亲委派模式，不建议重写loadClass()的逻辑，需要调用super.loadClass()先从父类开始查找Class，如果查找不到，会依次顺着继承关系往下寻找，如果还是没有寻找到，那就在当前自定义的ClassLoader中的findClass()中寻找。所以我们定义一个路径，把Class类放在这个路径下，通过io流读取class数据，并调用defineClass()生成一个Class类返回。defineClass()方法通过native方法从Class流中生成Class类对象。


自定义ClassLoader处理dex文件：
方式一：
定义一个java类，通过javac命令编译成class文件，再通过dx命令把class文件转换成dex文件（也可以再把多个dex打包成jar文件），生成DexClassLoader对象，把dex目录传入DexClassLoader构造函数中。
```
    String dexSourcePath = getApplicationContext().getExternalFilesDir("").getAbsolutePath()+"/JniText.dex";
    String dexPath = getApplicationContext().getExternalFilesDir("").getAbsolutePath();
    DexClassLoader dexClassLoader = new DexClassLoader(dexSourcePath,dexPath,null,getClassLoader());
    try {
        Class cl = dexClassLoader.loadClass("com.example.myapplication.JniText");
    } catch (Exception e) {
        e.printStackTrace();
    }
```
DexClassLoader构造函数中需要传入dex文件的路径，这个路径必须要在app的安装目录之下，如果放在其他目录中，比如/storage/emulated/0/Download，会报No original dex files found for dex location异常。
```
E/System: Unable to load dex file: /storage/emulated/0/Download/JniText.dex
    java.io.IOException: No original dex files found for dex location /storage/emulated/0/Download/JniText.dex
        at dalvik.system.DexFile.openDexFileNative(Native Method)
        at dalvik.system.DexFile.openDexFile(DexFile.java:365)
        at dalvik.system.DexFile.<init>(DexFile.java:150)
        at dalvik.system.DexFile.loadDex(DexFile.java:210)
        at dalvik.system.DexPathList.loadDexFile(DexPathList.java:447)
        at dalvik.system.DexPathList.makeDexElements(DexPathList.java:393)
        at dalvik.system.DexPathList.makeDexElements(DexPathList.java:370)
        at java.lang.reflect.Method.invoke(Native Method)
        at com.example.myapplication.MainActivity$1$1.findClass(MainActivity.java:105)
        at java.lang.ClassLoader.loadClass(ClassLoader.java:379)
        at java.lang.ClassLoader.loadClass(ClassLoader.java:312)
        at com.example.myapplication.MainActivity$1$1.loadClass(MainActivity.java:134)
        at com.example.myapplication.MainActivity$1.onClick(MainActivity.java:139)
        at android.view.View.performClick(View.java:7281)
        at com.google.android.material.button.MaterialButton.performClick(MaterialButton.java:967)
        at android.view.View.performClickInternal(View.java:7255)
        at android.view.View.access$3600(View.java:828)
        at android.view.View$PerformClick.run(View.java:27925)
        at android.os.Handler.handleCallback(Handler.java:900)
        at android.os.Handler.dispatchMessage(Handler.java:103)
        at android.os.Looper.loop(Looper.java:219)
        at android.app.ActivityThread.main(ActivityThread.java:8393)
        at java.lang.reflect.Method.invoke(Native Method)
        at com.android.internal.os.RuntimeInit$MethodAndArgsCaller.run(RuntimeInit.java:513)
        at com.android.internal.os.ZygoteInit.main(ZygoteInit.java:1055)
```

方式二：
DexClassLoader中对dex的处理细节都在DexPathList中，DexPathList把当前指定目录下的每一个dex或者jar文件封装成DexFile，并保存在一个数组中。DexFile中会调用native方法，把dex文件生成Class对象。当通过DexClassLoader.loadClass()加载指定Class时，会去数组中寻找。因此，可以通过反射的方式，修改DexPathList中的数组以达到目的。
```
    public void jniDex(){
        String dexSourcePath = getApplicationContext().getExternalFilesDir("").getAbsolutePath()+"/JniText.dex";
        String dexPath = getApplicationContext().getExternalFilesDir("").getAbsolutePath();
        ClassLoader classLoader1 = getClassLoader();
        try {

            //把指定目录下的dex加入到当前的DexClassLoader中，指定目录下的dex生成的DexFile肯定是在dexElements数组的末尾。
//            Method addDexPath = findMethod(classLoader1, "addDexPath",String.class);
//            addDexPath.invoke(classLoader1,dexSourcePath);

            //反射得到DexPathList中的pathList属性，并获取pathList的值
            Field pathListField = findField(classLoader1, "pathList");
            Object dexPathList = pathListField.get(classLoader1);

            //反射得到指定目录下的dex文件的File集合(dex文件的路径集合)
            Method splitDexPath = findMethod(dexPathList, "splitDexPath",String.class);
            List<File> arrayList = (ArrayList<File>)splitDexPath.invoke(dexPathList,dexSourcePath);

            //通过反射把指定目录下的dex转换成DexFile，返回DexFile的集合
            Method makeDexElements = findMethod(dexPathList, "makeDexElements", List.class, File.class,
                    List.class,ClassLoader.class);
            ArrayList<IOException> suppressedExceptions = new ArrayList<IOException>();
            Object[] dexFiles = (Object[]) makeDexElements.invoke(dexPathList,arrayList, new File(dexPath), suppressedExceptions,classLoader1);

            //反射得到当前DexPathList的DexFile集合dexElements
            Field dexElementsField = findField(dexPathList, "dexElements");
            Object []dexElements = (Object[]) dexElementsField.get(dexPathList);

            //修改DexPathList的DexFile集合dexElements，替换成指定目录下的dex文件
            dexElementsField.set(dexPathList,dexFiles);

            Field dexElementsField1 = findField(dexPathList, "dexElements");
            Object []dexElements1 = (Object[]) dexElementsField1.get(dexPathList);

            //查找指定的Class文件
            Method findClass = findMethod(dexPathList, "findClass",String.class, List.class);
            Class cllll = (Class) findClass.invoke(dexPathList,"com.example.myapplication.JniText",suppressedExceptions);
            if(cllll != null){
                Object object = cllll.newInstance();
                String toString = object.toString();
                Log.v("sujianze","====cllll:"+cllll+"   "+object+"    "+toString);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static Field findField(Object instance, String name) throws NoSuchFieldException {
        for (Class<?> clazz = instance.getClass(); clazz != null; clazz = clazz.getSuperclass()) {
            try {
                Field field = clazz.getDeclaredField(name);

                if (!field.isAccessible()) {
                    field.setAccessible(true);
                }

                return field;
            } catch (NoSuchFieldException e) {
                // ignore and search next
            }
        }

        throw new NoSuchFieldException("Field " + name + " not found in " + instance.getClass());
    }

    public static Method findMethod(Object instance, String name, Class<?>... parameterTypes)
            throws NoSuchMethodException {
        for (Class<?> clazz = instance.getClass(); clazz != null; clazz = clazz.getSuperclass()) {
            try {
                Method method = clazz.getDeclaredMethod(name, parameterTypes);

                if (!method.isAccessible()) {
                    method.setAccessible(true);
                }

                return method;
            } catch (NoSuchMethodException e) {
                // ignore and search next
            }
        }

        throw new NoSuchMethodException("Method "
                + name
                + " with parameters "
                + Arrays.asList(parameterTypes)
                + " not found in " + instance.getClass());
    }
```
DexClassLoader.loadClass()的过程就是遍历DexPathList中的DexFile数组的过程，通过反射去修改DexFile数组，调整DexFile数组中的位置，达到指定加载的目的。比如apk打包好的dex中有一个com.example.myapplication.JniText类，我们手动push到手机或者网络下载到手机中的dex也有一个com.example.myapplication.JniText类，我们可以在DexFile数组的开头插入指定的dex，达到优先加载的目的，DexClassLoader再找到对应Class文件后不再继续往下寻找。



4. ClassLoader的原理


https://blog.csdn.net/javazejian/article/details/73413292

补充一点:

在android studio中新建java library项目，新建一个包含main函数的类，测试当前的ClassLoader
```
    ClassLoader classLoader = ClassLoader.getSystemClassLoader();
    URL url  = classLoader.getResource("");
    System.out.println(classLoader+"   "+url);
    

    //输出：
    sun.misc.Launcher$AppClassLoader@4e0e2f2a   file:/home/sujianze/AndroidStudioProjects/MyApplication/java-lib/build/classes/java/main/
```
当前默认的ClassLoader就是AppClassLoader，它的默认加载路径是AndroidStudioProjects/MyApplication/java-lib/build/classes，android studio中的class文件都是放在这个目录下。如果需要查看AppClassLoader的java源码，需要下载openjdk的源码，android studio中只能查看class源码。
```
    static class AppClassLoader extends URLClassLoader {
        public static ClassLoader getAppClassLoader(final ClassLoader extcl)
            throws IOException
        {
            final String s = System.getProperty("java.class.path");
            final File[] path = (s == null) ? new File[0] : getClassPath(s);

            // Note: on bugid 4256530
            // Prior implementations of this doPrivileged() block supplied
            // a rather restrictive ACC via a call to the private method
            // AppClassLoader.getContext(). This proved overly restrictive
            // when loading  classes. Specifically it prevent
            // accessClassInPackage.sun.* grants from being honored.
            //
            return AccessController.doPrivileged(
                new PrivilegedAction<AppClassLoader>() {
                    public AppClassLoader run() {
                    URL[] urls =
                        (s == null) ? new URL[0] : pathToURLs(path);
                    return new AppClassLoader(urls, extcl);
                }
            });
        }
    }
```
AppClassLoader设置了默认的class存取目录System.getProperty("java.class.path")，同理，其他的ClassLoader都设置了各自的class目录。
```
AppClassLoader
System.getProperty("java.class.path")
AndroidStudioProjects/MyApplication/java-lib/build/classes/java/main:
AndroidStudioProjects/MyApplication/java-lib/build/resources/main


System.getProperty("java.library.path")
/usr/java/packages/lib/amd64:/usr/lib64:/lib64:/lib:/usr/lib

BootClassPathHolder
System.getProperty("sun.boot.class.path")
android-studio/jre/jre/lib/resources.jar:
android-studio/jre/jre/lib/rt.jar:
android-studio/jre/jre/lib/sunrsasign.jar:
android-studio/jre/jre/lib/jsse.jar:
android-studio/jre/jre/lib/jce.jar:
android-studio/jre/jre/lib/charsets.jar:
android-studio/jre/jre/lib/jfr.jar:
android-studio/jre/jre/classes

ExtClassLoader
System.getProperty("java.ext.dirs")
android-studio/jre/jre/lib/ext:/usr/java/packages/lib/ext
```
有兴趣可以各个ClassLoader深入了解一下。由于采用了双亲委派模式，当我们需要加载一些java提供的基础类或者jvm运行所需的类，使用的是Bootstrap ClassLoader或者ExtClassLoader。当需要加载我们自定义的类时，使用的是AppClassLoader来从指定的目录中加载。当我们需要特别定制时，创建自定义的ClassLoader，从指定目录下加载class文件即可。


android的ART虚拟机跟java的虚拟机略微有点不同，java的虚拟机加载的是class文件，androidART虚拟机加载的是dex格式的文件。为了支持dex格式文件，android中重写了ClassLoader.java，跟jdk中的ClassLoader.java对比，略微有点不同。

```
    ClassLoader classLoader = ClassLoader.getSystemClassLoader();
    URL url = classLoader.getResource("");
    Log.v("sujianze", classLoader+"   "+classLoader.getParent()+"====" + url);


    //输出
    dalvik.system.PathClassLoader[DexPathList[[directory "."],nativeLibraryDirectories=[/system/lib64, /hw_product/lib64, /system/product/lib64, /prets/lib64, /system/lib64, /hw_product/lib64, /system/product/lib64, /prets/lib64]]]   java.lang.BootClassLoader@5449e35====file:/
```
android中默认的ClassLoader是PathClassLoader，直接父类是BootClassLoader。


```
ClassLoader.getSystemClassLoader();
getClassLoader();
this.getClass().getClassLoader()
```



https://segmentfault.com/a/1190000020254261

5. 热修复Tinker

https://www.cnblogs.com/xgjblog/p/9406332.html


6. 插件化


https://www.jianshu.com/p/71585d744076

https://zhuanlan.zhihu.com/p/33017826