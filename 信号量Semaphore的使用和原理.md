# 信号量Semaphore的使用和原理



信号量Semaphore跟ReentrantLock一样，也是用于多线程下的并发编程。他们的源码实现都是借助了AbstractQueuedSynchronizer，简称AQS。

Semaphore与ReentrantLock的区别：
1. Semaphore是共享锁，ReentrantLock是独占锁。
2. Semaphore释放锁的时候不需要先获取锁，ReentrantLock释放锁前必须先获取锁，如果没有获取到锁就释放会抛出异常。


总结一下：Semaphore构造函数中传入一个信号量总量值permits，在AQS中根据CAS乐观锁实现permits的安全变量同步。每请求一个信号量，permits减一，每释放一个信号量，permits加1。如果当前请求的信号量过大，或者信号量总量permits为0(信号量耗尽)，那么当前请求信号量的线程阻塞/不阻塞。可以设置公平锁/非公平锁，在阻塞期间，信号量空出时等待的线程队列是顺序执行还是加塞处理。

1. Semaphore的使用：

```
    //设置信号量为2，意味着可以同时有两个线程同时访问，其他的线程阻塞等待
    Semaphore semaphore = new Semaphore(2);
            new Thread(new Runnable() {
        @Override
        public void run() {
            try {
                semaphore.acquire();
                //TODO：临界资源的使用代码逻辑
            }catch (InterruptedException e){
                e.printStackTrace();
            }finally {
                semaphore.release();
            }
        }
    }).start();
```
Semaphore的使用和ReentrantLock差不多，只不过获取锁和释放锁的方法名称不一样。
Semaphore提供的主要方法：
```
void acquire() ：从信号量获取一个许可，如果无可用许可前将一直阻塞等待，
void acquire(int permits) ：获取指定数目的许可，如果无可用许可前也将会一直阻塞等待
boolean tryAcquire()：从信号量尝试获取一个许可，如果无可用许可，直接返回false，不会阻塞
boolean tryAcquire(int permits)： 尝试获取指定数目的许可，如果无可用许可直接返回false
boolean tryAcquire(int permits, long timeout, TimeUnit unit)： 在指定的时间内尝试从信号量中获取许可，如果在指定的时间内获取成功，返回true，否则返回false
void release()： 释放一个许可，别忘了在finally中使用，注意：多次调用该方法，会使信号量的许可数增加，达到动态扩展的效果，如：初始permits为1， 调用了两次release，最大许可会改变为2
int availablePermits()： 获取当前信号量可用的许可
```

Semaphore可以解决死锁：
```
    final Semaphore semaphore = new Semaphore(1);
    final Semaphore semaphore1 = new Semaphore(1);  
    new Thread(new Runnable() {
        @Override
        public void run() {
            try {
                semaphore.acquire();//获取semaphore锁
                Thread.sleep(2000);//休眠让其他线程先获取semaphore1锁
                semaphore1.acquire();获取semaphore1锁，获取不到阻塞线程
            }catch (Exception e){
                e.printStackTrace();
            }

        }
    }).start();

    new Thread(new Runnable() {
        @Override
        public void run() {
            try {
                semaphore1.acquire();获取semaphore1锁
                Thread.sleep(2000);//休眠让其他线程先获取semaphore锁
                semaphore.acquire();//尝试获取semaphore锁，获取不到阻塞线程
            }catch (Exception e){
                e.printStackTrace();
            }
        }
    }).start();
    
    //Semaphore的acquire()和release()不需成对出现，意味着我们可以在其他线程释放锁。
    new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
        @Override
        public void run() {
            semaphore.release();//释放semaphore锁
            semaphore1.release();//释放semaphore锁
        }
    },5000);

```
如果以上的Semaphore换成ReentrantLock，会抛出java.lang.IllegalMonitorStateException异常。因为ReentrantLock的lock()和unLock()需要成对的出现，unLock()释放的时候所在的线程必须是lock()时所在的线程，否则抛出java.lang.IllegalMonitorStateException异常。setExclusiveOwnerThread()中保存的线程是持有锁的线程。
```
    public boolean tryLock() {
        return sync.nonfairTryAcquire(1);
    }
    final boolean nonfairTryAcquire(int acquires) {
        final Thread current = Thread.currentThread();
        int c = getState();
        if (c == 0) {
            if (compareAndSetState(0, acquires)) {
                setExclusiveOwnerThread(current);//设置lock时的当前线程
                return true;
            }
        }
        else if (current == getExclusiveOwnerThread()) {
            int nextc = c + acquires;
            if (nextc < 0) // overflow
                throw new Error("Maximum lock count exceeded");
            setState(nextc);
            return true;
        }
        return false;
    }
    protected final void setExclusiveOwnerThread(Thread thread) {
        exclusiveOwnerThread = thread;
    }

    public void unlock() {
        sync.release(1);
    }
    public final boolean release(int arg) {
        if (tryRelease(arg)) {
            Node h = head;
            if (h != null && h.waitStatus != 0)
                unparkSuccessor(h);
            return true;
        }
        return false;
    }
    protected final boolean tryRelease(int releases) {
        int c = getState() - releases;
        //执行unlock操作的所在线程不是lock时的线程，抛出异常
        if (Thread.currentThread() != getExclusiveOwnerThread())
            throw new IllegalMonitorStateException();
        boolean free = false;
        if (c == 0) {
            free = true;
            setExclusiveOwnerThread(null);
        }
        setState(c);
        return free;
    }
```
以上的方法有些是ReentrantLock.java中的，有些是AbstractOwnableSynchronizer.java，注意甄别。

2. Semaphore的源码


Semaphore的构造函数
```
    public Semaphore(int permits) {
        sync = new NonfairSync(permits);
    }
    public Semaphore(int permits, boolean fair) {
        sync = fair ? new FairSync(permits) : new NonfairSync(permits);
    }
```
设置信号量，并且可以选择使用公平锁还是非公平锁，信号量由公平锁/非公平锁处理。


公平锁：
```
    static final class FairSync extends Sync {
        private static final long serialVersionUID = 2014338818796000944L;

        FairSync(int permits) {
            super(permits);
        }

        protected int tryAcquireShared(int acquires) {
            for (;;) {
                //当前的等待队列中有值，不可以加塞，要公平，按照等待队列顺序执行
                if (hasQueuedPredecessors())
                    return -1;
                int available = getState();
                int remaining = available - acquires;
                if (remaining < 0 ||
                    compareAndSetState(available, remaining))
                    return remaining;
            }
        }
    }
```



非公平锁：
```
    static final class NonfairSync extends Sync {
        private static final long serialVersionUID = -2694183684443567898L;

        NonfairSync(int permits) {
            super(permits);
        }

        protected int tryAcquireShared(int acquires) {
            return nonfairTryAcquireShared(acquires);
        }
    }
```


NonfairSync/FairSync的父类Sync：
```
    abstract static class Sync extends AbstractQueuedSynchronizer {
        private static final long serialVersionUID = 1192457210091910933L;

        Sync(int permits) {
            setState(permits);
        }

        final int getPermits() {
            return getState();
        }
        
        //如果当前请求的信号量超过当前的信号量总量，则直接返回remaining(负值或者0)，。如果当前请求的信号量在当前信号量总量内，修改AQS中的STATE值，修改成功返回remaining，不成功就一直死循环直到满足条件成功修改AQS中的STATE值。
        final int nonfairTryAcquireShared(int acquires) {
            for (;;) {
                int available = getState();//当前信号量的值(Semaphore构造方法传入的)
                int remaining = available - acquires;
                if (remaining < 0 ||
                    compareAndSetState(available, remaining))
                    return remaining;
            }
        }

        //释放指定数量的信号量，修改AQS中的STATE值。
        protected final boolean tryReleaseShared(int releases) {
            for (;;) {
                int current = getState();
                int next = current + releases;
                if (next < current) // overflow
                    throw new Error("Maximum permit count exceeded");
                if (compareAndSetState(current, next))
                    return true;
            }
        }

        //修改当前的信号量总量
        final void reducePermits(int reductions) {
            for (;;) {
                int current = getState();
                int next = current - reductions;
                if (next > current) // underflow
                    throw new Error("Permit count underflow");
                if (compareAndSetState(current, next))
                    return;
            }
        }

        //清空所有的信号量，信号量总量变为0
        final int drainPermits() {
            for (;;) {
                int current = getState();
                if (current == 0 || compareAndSetState(current, 0))
                    return current;
            }
        }
    }
```
Semaphore构造方法传入的信号量，通过setState(permits)传入了AbstractQueuedSynchronizer中处理。根据AQS中逻辑，信号量permits作为AQS中的state值，设置为volatile，线程间可见。之后state值通过CAS乐观锁实现了变量同步。
```
    private volatile int state;
    protected final void setState(int newState) {
        state = newState;
    }
    protected final boolean compareAndSetState(int expect, int update) {
        return U.compareAndSwapInt(this, STATE, expect, update);//乐观锁
    }
```

Semaphore获取信号量:

```
    public void acquire() throws InterruptedException {
        sync.acquireSharedInterruptibly(1);//获取一个信号量
    }
    public final void acquireSharedInterruptibly(int arg)
            throws InterruptedException {
        if (Thread.interrupted())
            throw new InterruptedException();
        if (tryAcquireShared(arg) < 0)//这里调用的是NonfairSync/FairSync中重写的方法
            doAcquireSharedInterruptibly(arg);//获取信号量失败的处理
    }

    //把获取信号量失败的操作封装成Node节点，开启死循环，阻塞当前线程，直到被唤醒
    private void doAcquireSharedInterruptibly(int arg)
        throws InterruptedException {
        final Node node = addWaiter(Node.SHARED);
        try {
            for (;;) {
                final Node p = node.predecessor();
                if (p == head) {
                    int r = tryAcquireShared(arg);
                    if (r >= 0) {
                        setHeadAndPropagate(node, r);
                        p.next = null; // help GC
                        return;
                    }
                }
                if (shouldParkAfterFailedAcquire(p, node) &&
                    parkAndCheckInterrupt())
                    throw new InterruptedException();
            }
        } catch (Throwable t) {
            cancelAcquire(node);
            throw t;
        }
    }
```


Semaphore释放信号量

```
    public void release() {
        sync.releaseShared(1);
    }
    public final boolean releaseShared(int arg) {
        if (tryReleaseShared(arg)) {//这里调用的是NonfairSync/FairSync中重写的方法
            doReleaseShared();
            return true;
        }
        return false;
    }

    //修改AQS中的STATE值，唤醒获取信号量被阻塞的线程
    private void doReleaseShared() {
        /*
         * Ensure that a release propagates, even if there are other
         * in-progress acquires/releases.  This proceeds in the usual
         * way of trying to unparkSuccessor of head if it needs
         * signal. But if it does not, status is set to PROPAGATE to
         * ensure that upon release, propagation continues.
         * Additionally, we must loop in case a new node is added
         * while we are doing this. Also, unlike other uses of
         * unparkSuccessor, we need to know if CAS to reset status
         * fails, if so rechecking.
         */
        for (;;) {
            Node h = head;
            if (h != null && h != tail) {
                int ws = h.waitStatus;
                if (ws == Node.SIGNAL) {
                    if (!h.compareAndSetWaitStatus(Node.SIGNAL, 0))
                        continue;            // loop to recheck cases
                    unparkSuccessor(h);
                }
                else if (ws == 0 &&
                         !h.compareAndSetWaitStatus(0, Node.PROPAGATE))
                    continue;                // loop on failed CAS
            }
            if (h == head)                   // loop if head changed
                break;
        }
    }
```