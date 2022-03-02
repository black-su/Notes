#  Rxjava源码分析

参考： https://zhuanlan.zhihu.com/p/374178842
https://www.jianshu.com/p/121bb6342423

例子：
```
        Observable
                .create(new ObservableOnSubscribe<Integer>() {
                    @Override
                    public void subscribe(ObservableEmitter<Integer> e) throws Exception {
                        e.onNext(1);
                        e.onNext(2);
                        e.onNext(3);
                        e.onComplete();
                    }
                })
                .map(new Function<Integer, Integer>() {
                    @Override
                    public Integer apply(Integer integer) throws Throwable {
                        return 333;
                    }
                })
                .flatMap(new Function<Integer, ObservableSource<String>>() {
                    @Override
                    public ObservableSource<String> apply(Integer integer) throws Exception {
                        return Observable.just(
                                "item " + integer + " sub-item " + 1 + " Observable Thread: " + Thread.currentThread().getName()
                                , "item " + integer + " sub-item " + 2 + " Observable Thread: " + Thread.currentThread().getName()
                                , "item " + integer + " sub-item " + 3 + " Observable Thread: " + Thread.currentThread().getName());
                    }
                })
                .subscribe(new Observer<String>() {
                    @Override
                    public void onSubscribe(Disposable d) {

                    }

                    @Override
                    public void onNext(String s) {
                    }

                    @Override
                    public void onError(Throwable e) {
                    }

                    @Override
                    public void onComplete() {
                    }
                });
```

以上Observable.create()生成一个自定义的被观察者Observable对象，自定义的Observable被封装成ObservableCreate对象，并把这个ObservableCreate类型的对象返回，Observable.create()之后的链式调用，使用的是ObservableCreate对象。
同理，map()方法把ObservableCreate对象封装成ObservableMap类型的对象，并返回ObservableMap对象，之后的链式调用使用的是ObservableMap。flatMap()方法把ObservableMap对象封装成ObservableFlatMap类型的对象，并返回ObservableFlatMap对象。
自定义Observable  -> ObservableCreate -> ObservableMap -> ObservableFlatMap，前者封装在了后者中，后者持有前者的对象引用。
```
    public static <@NonNull T> Observable<T> create(@NonNull ObservableOnSubscribe<T> source) {
        Objects.requireNonNull(source, "source is null");
        return RxJavaPlugins.onAssembly(new ObservableCreate<>(source)); 
    }
    public final <@NonNull R> Observable<R> map(@NonNull Function<? super T, ? extends R> mapper) {
        Objects.requireNonNull(mapper, "mapper is null");
        return RxJavaPlugins.onAssembly(new ObservableMap<>(this, mapper));
    }
    public final <@NonNull R> Observable<R> flatMap(@NonNull Function<? super T, ? extends ObservableSource<? extends R>> mapper) {
        return flatMap(mapper, false);
    }
    public final <@NonNull R> Observable<R> flatMap(@NonNull Function<? super T, ? extends ObservableSource<? extends R>> mapper, boolean delayErrors) {
        return flatMap(mapper, delayErrors, Integer.MAX_VALUE);
    }
    public final <@NonNull R> Observable<R> flatMap(@NonNull Function<? super T, ? extends ObservableSource<? extends R>> mapper,
            boolean delayErrors, int maxConcurrency, int bufferSize) {
        Objects.requireNonNull(mapper, "mapper is null");
        ObjectHelper.verifyPositive(maxConcurrency, "maxConcurrency");
        ObjectHelper.verifyPositive(bufferSize, "bufferSize");
        if (this instanceof ScalarSupplier) {
            @SuppressWarnings("unchecked")
            T v = ((ScalarSupplier<T>)this).get();
            if (v == null) {
                return empty();
            }
            return ObservableScalarXMap.scalarXMap(v, mapper);
        }
        return RxJavaPlugins.onAssembly(new ObservableFlatMap<>(this, mapper, delayErrors, maxConcurrency, bufferSize));
    }
    public static <@NonNull T> Observable<T> onAssembly(@NonNull Observable<T> source) {
        Function<? super Observable, ? extends Observable> f = onObservableAssembly;
        if (f != null) {
            return apply(f, source);
        }
        return source;
    }
```

最后调用subscribe()把被观察者Observable和观察者Observer绑定。通过以上的链式调用，链式的最后一个方法返回的对象执行subscribe()方法，也就是执行ObservableFlatMap.subscribe()。ObservableFlatMap对传进来的自定义观察者的处理是把它封装
成MergeObserver，source也就是链式调用产生的倒数第二个被观察者ObservableMap，调用的ObservableMap.subscribe()。同理，ObservableMap也是跟ObservableFlatMap差不多一样的处理。

```
    public final void subscribe(@NonNull Observer<? super T> observer) {
        Objects.requireNonNull(observer, "observer is null");
        try {
            observer = RxJavaPlugins.onSubscribe(this, observer);

            Objects.requireNonNull(observer, "The RxJavaPlugins.onSubscribe hook returned a null Observer. Please change the handler provided to RxJavaPlugins.setOnObservableSubscribe for invalid null returns. Further reading: https://github.com/ReactiveX/RxJava/wiki/Plugins");

            subscribeActual(observer);
        } catch (NullPointerException e) { // NOPMD
            throw e;
        } catch (Throwable e) {
            Exceptions.throwIfFatal(e);
            // can't call onError because no way to know if a Disposable has been set or not
            // can't call onSubscribe because the call might have set a Subscription already
            RxJavaPlugins.onError(e);

            NullPointerException npe = new NullPointerException("Actually not, but can't throw other exceptions due to RS");
            npe.initCause(e);
            throw npe;
        }
    }
	
    public void subscribeActual(Observer<? super U> t) {

        if (ObservableScalarXMap.tryScalarXMapSubscribe(source, t, mapper)) {
            return;
        }

        source.subscribe(new MergeObserver<>(t, mapper, delayErrors, maxConcurrency, bufferSize));
    }
```

Rxjava中的链式调用中对Observable和Observer的处理：
自定义Observable  -> ObservableCreate -> ObservableMap -> ObservableFlatMap
自定义Observable.subscribe() <- ObservableCreate.subscribe() <- ObservableMap.subscribe() <- ObservableFlatMap.subscribe()
CreateEmitter <- MapObserver <- MergeObserver <- 自定义Observer

按照例子中的写法，自定义Observable.subscribe()被回调时，从传参中拿到的ObservableEmitter就是CreateEmitter，CreateEmitter中持有一个Observer观察者对象，这个Observer中套了一层又一层的对象。如果我们使用它来调用onNext(),onError()等方法时，也是按照
是按照上面Observer的关系反正来的：MapObserver -> MergeObserver -> 自定义Observer

```
 static final class CreateEmitter<T>
    extends AtomicReference<Disposable>
    implements ObservableEmitter<T>, Disposable {

        private static final long serialVersionUID = -3434801548987643227L;

        final Observer<? super T> observer;

        CreateEmitter(Observer<? super T> observer) {
            this.observer = observer;
        }

        @Override
        public void onNext(T t) {
            if (t == null) {
                onError(ExceptionHelper.createNullPointerException("onNext called with a null value."));
                return;
            }
            if (!isDisposed()) {
                observer.onNext(t);
            }
        }

        @Override
        public void onError(Throwable t) {
            if (!tryOnError(t)) {
                RxJavaPlugins.onError(t);
            }
        }
		.......
}
```

mapper.apply(t)对应map()方法中的数据转换。downstream.onNext(v)把转换后的数据继续交给MergeObserver.onNext()处理。

```
static final class MapObserver<T, U> extends BasicFuseableObserver<T, U> {
        final Function<? super T, ? extends U> mapper;

        MapObserver(Observer<? super U> actual, Function<? super T, ? extends U> mapper) {
            super(actual);
            this.mapper = mapper;
        }

        @Override
        public void onNext(T t) {
            if (done) {
                return;
            }

            if (sourceMode != NONE) {
                downstream.onNext(null);
                return;
            }

            U v;

            try {
                v = Objects.requireNonNull(mapper.apply(t), "The mapper function returned a null value.");
            } catch (Throwable ex) {
                fail(ex);
                return;
            }
            downstream.onNext(v);
        }
		......
```

如果我们需要切换线程，Rxjava中也是把线程切换这个操作封装成Observable和Observer的关系，也是一层一层的处理。比如要为观察者设置线程时，其被封装成被观察者ObservableObserveOn
```
    public final Observable<T> observeOn(@NonNull Scheduler scheduler, boolean delayError, int bufferSize) {
        Objects.requireNonNull(scheduler, "scheduler is null");
        ObjectHelper.verifyPositive(bufferSize, "bufferSize");
        return RxJavaPlugins.onAssembly(new ObservableObserveOn<>(this, scheduler, delayError, bufferSize));
    }
```
ObservableObserveOn中对onNext()处理时先把线程开启
```
        @Override
        public void onNext(T t) {
            if (done) {
                return;
            }

            if (sourceMode != QueueDisposable.ASYNC) {
                queue.offer(t);//把每个要开启线程的操作放入queue队列中，比如我在自定义Observable.subscribe()中调用了两次onNext()操作，有两个观察者指定了线程，那就加入四个操作入队列
            }
            schedule();
        }
        void schedule() {
            if (getAndIncrement() == 0) {
                worker.schedule(this);//开启线程
            }
        }
		
	    public void run() {//执行线程中的run方法
            if (outputFused) {
                drainFused();
            } else {
                drainNormal();
            }
        }
		
		//线程中循环SimpleQueue队列，拿到观察者的对象，调用观察者onNext()，onError()进行通知。
        void drainNormal() {
            int missed = 1;

            final SimpleQueue<T> q = queue;
            final Observer<? super T> a = downstream;

            for (;;) {
                if (checkTerminated(done, q.isEmpty(), a)) {
                    return;
                }

                for (;;) {
                    boolean d = done;
                    T v;

                    try {
                        v = q.poll();
                    } catch (Throwable ex) {
                        Exceptions.throwIfFatal(ex);
                        disposed = true;
                        upstream.dispose();
                        q.clear();
                        a.onError(ex);
                        worker.dispose();
                        return;
                    }
                    boolean empty = v == null;

                    if (checkTerminated(d, empty, a)) {
                        return;
                    }

                    if (empty) {
                        break;
                    }

                    a.onNext(v);
                }

                missed = addAndGet(-missed);
                if (missed == 0) {
                    break;
                }
            }
        }
```
TODO: flatMap 切换事件流、zip 合并流、buffer 做缓存、sample 采样、filter 过滤