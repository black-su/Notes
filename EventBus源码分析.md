#EventBus源码分析


官网地址：https://github.com/greenrobot/EventBus

一 . 使用方式详见官网介绍

二 . 源码分析

   EventBus的源码中，有以下几个模块：EventBus，EventBusAnnotationProcessor，EventBusPerformance，EventBusTest，EventBusTestJava，EventBusTestSubscriberInJar。
   主要的就是EventBus，EventBusAnnotationProcessor两个模块，EventBusAnnotationProcessor模块是处理编译期的注释的，剩下的模块都是有关的测试模块。

   个人觉得EventBus的难点或者知识点在于@Subscribe注释的处理方式上。这个看懂了，其他的源码就很好理解了。
   EventBus在3.0版本后使用了annotationProcessor的方式来管理注释，在编译期扫描代码搜集注释，并做后续的逻辑处理。相对于3.0版本之前在jvm运行时通过纯反射的方式来搜集注释并处理。性能上更好。
   我们可以通过EventBusPerformance来观察annotationProcessor方式的使用。
   EventBus中的annotationProcessor的使用详见官网的介绍：https://greenrobot.org/eventbus/documentation/subscriber-index/
   主要分为两个步骤：

   1. 在项目的build.gradle中定义如下：


```
    dependencies {
        def eventbus_version = '3.2.0'
        implementation "org.greenrobot:eventbus:$eventbus_version"
        annotationProcessor "org.greenrobot:eventbus-annotation-processor:$eventbus_version"
    }
    defaultConfig {
        minSdkVersion 7
        targetSdkVersion 26
        versionCode 1
        versionName "2.0.0"
        javaCompileOptions {
            annotationProcessorOptions {
                arguments = [eventBusIndex: 'org.greenrobot.eventbusperf.MyEventBusIndex']
            }
        }
    }
```

   以上定义的arguments参数表示编译期生成的类的全路径名是org.greenrobot.eventbusperf.MyEventBusIndex。
   这些参数会在编译期被回调给EventBusAnnotationProcessor.java。参见EventBusAnnotationProcessor.java中的process()方法。

   2. 手动添加SubscriberInfoIndex子类到EventBus中，让EventBus在订阅--发布过程中，在annotationProcessor方式拿到的注释集合中寻找目标，避免每次都是jvm运行时反射寻找目标造成性能消耗。
```
    //第一种方式
    EventBus eventBus = EventBus.builder().addIndex(new MyEventBusIndex()).build();
    //第二种方式
    EventBus.builder().addIndex(new MyEventBusIndex()).installDefaultEventBus();
    EventBus eventBus = EventBus.getDefault();
```
   第一种方式每次都生成一个新的EventBus对象。
   第二种方式以单例模式生成一个唯一的EventBus对象。
   

   接下来就是源码分析了，EventBusPerformance模块中build.gradle中定义如下：定义了annotationProcessor，指定了java编译器由eventbus-annotation-processor模块处理。

```
   dependencies {
    implementation project(':eventbus')
    annotationProcessor project(':eventbus-annotation-processor')
    implementation 'com.squareup:otto:1.3.8'
   }
```

   那么EventBusPerformance在编译过程中，就会执行EventBusAnnotationProcessor模块的代码。代码编译期会回调以下函数：
```
    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment env) {
        Messager messager = processingEnv.getMessager();
        try {
            //拿到build.gradle中定义的eventBusIndex参数值
            String index = processingEnv.getOptions().get(OPTION_EVENT_BUS_INDEX);
            ......
            //收集@Subscribe注释过的方法，保存在集合methodsByClass中
            collectSubscribers(annotations, env, messager);
            //跳过那些设置为private的类
            checkForSubscribersToSkip(messager, indexPackage);

            if (!methodsByClass.isEmpty()) {
                //根据index来创建一个新的java类，
                createInfoIndexFile(index);
            } else {
                messager.printMessage(Diagnostic.Kind.WARNING, "No @Subscribe annotations found");
            }
            writerRoundDone = true;
        } catch (RuntimeException e) {
            // IntelliJ does not handle exceptions nicely, so log and print a message
            e.printStackTrace();
            messager.printMessage(Diagnostic.Kind.ERROR, "Unexpected error in EventBusAnnotationProcessor: " + e);
        }
        return true;
    }
```

   我们在rebuild project后就可以在项目中找到编译期自动生成的类：
   EventBus-master\EventBusPerformance\build\generated\ap_generated_sources\debug\out\org\greenrobot\eventbusperf\MyEventBusIndex.java

```
   public class MyEventBusIndex implements SubscriberInfoIndex {
    private static final Map<Class<?>, SubscriberInfo> SUBSCRIBER_INDEX;

    //把项目中已经register了的类中标注有@Subscribe注释的地方，封装成SubscriberMethodInfo类，保存到静态集合SUBSCRIBER_INDEX中
    static {
        SUBSCRIBER_INDEX = new HashMap<Class<?>, SubscriberInfo>();

        putIndex(new SimpleSubscriberInfo(org.greenrobot.eventbusperf.testsubject.PerfTestEventBus.SubscribeClassEventBusBackground.class,
                true, new SubscriberMethodInfo[] {
            new SubscriberMethodInfo("onEventBackgroundThread", TestEvent.class, ThreadMode.BACKGROUND),
        }));

        putIndex(new SimpleSubscriberInfo(TestRunnerActivity.class, true, new SubscriberMethodInfo[] {
            new SubscriberMethodInfo("onEventMainThread", TestFinishedEvent.class, ThreadMode.MAIN),
        }));

        putIndex(new SimpleSubscriberInfo(org.greenrobot.eventbusperf.testsubject.SubscribeClassEventBusDefault.class,
                true, new SubscriberMethodInfo[] {
            new SubscriberMethodInfo("onEvent", TestEvent.class),
        }));

        putIndex(new SimpleSubscriberInfo(org.greenrobot.eventbusperf.testsubject.PerfTestEventBus.SubscribeClassEventBusMain.class,
                true, new SubscriberMethodInfo[] {
            new SubscriberMethodInfo("onEventMainThread", TestEvent.class, ThreadMode.MAIN),
        }));

        putIndex(new SimpleSubscriberInfo(org.greenrobot.eventbusperf.testsubject.PerfTestEventBus.SubscriberClassEventBusAsync.class,
                true, new SubscriberMethodInfo[] {
            new SubscriberMethodInfo("onEventAsync", TestEvent.class, ThreadMode.ASYNC),
        }));

        putIndex(new SimpleSubscriberInfo(org.greenrobot.eventbusperf.testsubject.PerfTestEventBus.SubscribeClassEventBusMainOrdered.class,
                true, new SubscriberMethodInfo[] {
            new SubscriberMethodInfo("onEvent", TestEvent.class, ThreadMode.MAIN_ORDERED),
        }));

    }

    private static void putIndex(SubscriberInfo info) {
        SUBSCRIBER_INDEX.put(info.getSubscriberClass(), info);
    }

    //通过getSubscriberInfo()方法对外暴露，查询编译期间收集到的@Subscribe注释过的方法信息。代理模式
    @Override
    public SubscriberInfo getSubscriberInfo(Class<?> subscriberClass) {
        SubscriberInfo info = SUBSCRIBER_INDEX.get(subscriberClass);
        if (info != null) {
            return info;
        } else {
            return null;
        }
    }
   }
```

至此，annotationProcessor注释的处理过程，就是在编译期生成一个新的java类，搜集到所有的@Subscribe注释后保存在一个静态集合中。之后我们只需要分析一下EventBus注册过程即可。

EventBus注册过程：

EventBus.java
```
    public void register(Object subscriber) {
        Class<?> subscriberClass = subscriber.getClass();
        //根据注册所在的类信息，寻找该类中标注有@Subscribe的方法
        List<SubscriberMethod> subscriberMethods = subscriberMethodFinder.findSubscriberMethods(subscriberClass);
        synchronized (this) {
            //把该类标注有@Subscribe的方法加入到监听集合中，合适的时机将会调用该方法进行通知
            for (SubscriberMethod subscriberMethod : subscriberMethods) {
                subscribe(subscriber, subscriberMethod);
            }
        }
    }

```

SubscriberMethodFinder.java
```
    List<SubscriberMethod> findSubscriberMethods(Class<?> subscriberClass) {
        List<SubscriberMethod> subscriberMethods = METHOD_CACHE.get(subscriberClass);
        if (subscriberMethods != null) {
            return subscriberMethods;
        }
        if (ignoreGeneratedIndex) {
            //通过jvm运行时反射来寻找目标
            subscriberMethods = findUsingReflection(subscriberClass);
        } else {
            //根据编译期生成的注释集合来寻找目标
            subscriberMethods = findUsingInfo(subscriberClass);
        }
        if (subscriberMethods.isEmpty()) {
            throw new EventBusException("Subscriber " + subscriberClass
                    + " and its super classes have no public methods with the @Subscribe annotation");
        } else {
            //通过上面寻找到了目标，加入缓存中，下次直接获取
            METHOD_CACHE.put(subscriberClass, subscriberMethods);
            return subscriberMethods;
        }
    }

    private List<SubscriberMethod> findUsingReflection(Class<?> subscriberClass) {
        //新建FindState对象，把class信息封装到FindState中
        FindState findState = prepareFindState();
        findState.initForSubscriber(subscriberClass);
        while (findState.clazz != null) {
            //jvm运行时反射获取该类的所有@Subscribe的方法
            findUsingReflectionInSingleClass(findState);
            //遍历父类
            findState.moveToSuperclass();
        }
        return getMethodsAndRelease(findState);
    }

    private List<SubscriberMethod> findUsingInfo(Class<?> subscriberClass) {
        //新建FindState对象，把class信息封装到FindState中
        FindState findState = prepareFindState();
        findState.initForSubscriber(subscriberClass);
        while (findState.clazz != null) {
            findState.subscriberInfo = getSubscriberInfo(findState);
            if (findState.subscriberInfo != null) {
                //查找到标注有@Subscribe的方法也加入到FindState中的方法集合中
                SubscriberMethod[] array = findState.subscriberInfo.getSubscriberMethods();
                for (SubscriberMethod subscriberMethod : array) {
                    if (findState.checkAdd(subscriberMethod.method, subscriberMethod.eventType)) {
                        findState.subscriberMethods.add(subscriberMethod);
                    }
                }
            } else {
                //编译期生成的集合中找不到，那就通过jvm运行时反射寻找目标，把信息保存到FindState中
                findUsingReflectionInSingleClass(findState);
            }
            //这个类的父类是否也有标注了@Subscribe的方法。有的话也需要遍历并保存。
            findState.moveToSuperclass();
        }
        return getMethodsAndRelease(findState);
    }

    private SubscriberInfo getSubscriberInfo(FindState findState) {
        if (findState.subscriberInfo != null && findState.subscriberInfo.getSuperSubscriberInfo() != null) {
            SubscriberInfo superclassInfo = findState.subscriberInfo.getSuperSubscriberInfo();
            if (findState.clazz == superclassInfo.getSubscriberClass()) {
                return superclassInfo;
            }
        }
        //如果设置了SubscriberInfoIndex，那就从SubscriberInfoIndex的静态集合中获取目标（编译期扫描得到的注释集合）
        if (subscriberInfoIndexes != null) {
            for (SubscriberInfoIndex index : subscriberInfoIndexes) {
                //MyEventBusIndexl类中的getSubscriberInfo方法，最终也是在一个静态集合中寻找目标
                SubscriberInfo info = index.getSubscriberInfo(findState.clazz);
                if (info != null) {
                    return info;
                }
            }
        }
        return null;
    }

```

   手动添加SubscriberInfoIndex子类到EventBus中，让EventBus在订阅--发布过程中，在annotationProcessor方式拿到的注释集合中寻找目标 

   EventBusBuilder.java
```
    /** Adds an index generated by EventBus' annotation preprocessor. */
    public EventBusBuilder addIndex(SubscriberInfoIndex index) {
        if (subscriberInfoIndexes == null) {
            subscriberInfoIndexes = new ArrayList<>();
        }
        subscriberInfoIndexes.add(index);
        return this;
    }
```


三 . 优缺点

优点：使用方便，解耦了发送者和接收者。线程的处理方便。数据的传递无限制
缺点：耗时（3.0之前的版本，大量使用EventBus做消息传递时，有明显的耗时，不能立即通知，需要时间查找接收方）
     接口泛滥，管理困难（每个事件都要自定义，增加了接口。不过个人认为不是问题，在公共模块定义一个统一的接口管理即可，但这个也会产生一个问题，所有的该接口类型的接收方都会收到发送方的消息，这样又需要做判断区分接收方了）
