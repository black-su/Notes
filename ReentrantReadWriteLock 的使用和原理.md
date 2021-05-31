#ReentrantReadWriteLock 的使用和原理


总结一下：ReentrantReadWriteLock中有两个锁，分别是WriteLock写锁，ReadLock读锁。ReadLock读锁设计成共享锁，多个线程可以同时访问ReadLock读锁锁住的临界资源。WriteLock写锁设计成独占锁，只能有一个线程访问WriteLock写锁锁住的临界资源。读写，写写互相阻塞。


ReentrantReadWriteLock与ReentrantLock的区别：

ReentrantLock的lock()和unlock()都需要在同一个线程中成对出现，否则ReentrantLock会抛出异常。
ReentrantReadWriteLock中ReadLock和WriteLock的lock()，unlock()都需要在同一个线程中成对出现，否则会获取不到锁或者释放锁失败，直接返回false。

ReentrantLock是一个独占锁
ReentrantReadWriteLock.WriteLock是独占锁。
ReentrantReadWriteLock.ReadLock是共享锁。

1. ReentrantReadWriteLock的使用

读写锁
```
class ThreadReadWriter extends Thread{
    ReentrantReadWriteLock reentrantReadWriteLock = new ReentrantReadWriteLock();
    ReentrantReadWriteLock.ReadLock readLock = reentrantReadWriteLock.readLock();
    ReentrantReadWriteLock.WriteLock writeLock = reentrantReadWriteLock.writeLock();

    //ReentrantReadWriteLock.WriteLock是独占锁，最多只能由一个线程访问，其他线程阻塞
    public void write(){
        try {
            writeLock.lock();
            //TODO:写逻辑
        }catch (Exception e){
            e.printStackTrace();
        }finally {
            writeLock.unlock();
        }
    }

    //ReentrantReadWriteLock.ReadLock是共享锁，支持多个线程同时访问
    public void read(){
        try {
            readLock.lock();
            //TODO:读逻辑
        }catch (Exception e){
            e.printStackTrace();
        }finally {
            readLock.unlock();
        }
    }
}
```






2. ReentrantReadWriteLock的原理

https://tech.meituan.com/2018/11/15/java-lock.html

https://www.cnblogs.com/xrq730/p/4979021.html

ReadLock的获取锁过程：
```
   public void lock() {
        sync.acquireShared(1);
    }
    public final void acquireShared(int arg) {
        //调用的是ReentrantReadWriteLock.Sync内部类的方法tryAcquireShared()
        if (tryAcquireShared(arg) < 0)
            doAcquireShared(arg);//如果获取读锁失败，阻塞当前线程
    }
    protected final int tryAcquireShared(int unused) {
        /*
         * Walkthrough:
         * 1. If write lock held by another thread, fail.
         * 2. Otherwise, this thread is eligible for
         *    lock wrt state, so ask if it should block
         *    because of queue policy. If not, try
         *    to grant by CASing state and updating count.
         *    Note that step does not check for reentrant
         *    acquires, which is postponed to full version
         *    to avoid having to check hold count in
         *    the more typical non-reentrant case.
         * 3. If step 2 fails either because thread
         *    apparently not eligible or CAS fails or count
         *    saturated, chain to version with full retry loop.
         */
        Thread current = Thread.currentThread();
        int c = getState();
        //如果当前写锁的数量不为0，说明已有线程拿到写锁，直接返回-1，避免同时读写引发同步问题。或者之前拿到写锁的线程跟当前申请读锁的线程不是同一个，直接返回-1。
        if (exclusiveCount(c) != 0 &&
                getExclusiveOwnerThread() != current)
            return -1;
        int r = sharedCount(c);
        if (!readerShouldBlock() &&
                r < MAX_COUNT &&
                compareAndSetState(c, c + SHARED_UNIT)) {
            if (r == 0) {
                firstReader = current;
                firstReaderHoldCount = 1;
            } else if (firstReader == current) {
                firstReaderHoldCount++;
            } else {
                HoldCounter rh = cachedHoldCounter;
                if (rh == null || rh.tid != getThreadId(current))
                    cachedHoldCounter = rh = readHolds.get();
                else if (rh.count == 0)
                    readHolds.set(rh);
                rh.count++;
            }
            return 1;
        }
        return fullTryAcquireShared(current);
    }
    //把获取读锁失败的操作封装成Node节点，开启死循环，阻塞当前线程，直到被唤醒
    private void doAcquireShared(int arg) {
        final Node node = addWaiter(Node.SHARED);
        try {
            boolean interrupted = false;
            for (;;) {
                final Node p = node.predecessor();
                if (p == head) {
                    int r = tryAcquireShared(arg);
                    if (r >= 0) {
                        setHeadAndPropagate(node, r);
                        p.next = null; // help GC
                        if (interrupted)
                            selfInterrupt();
                        return;
                    }
                }
                if (shouldParkAfterFailedAcquire(p, node) &&
                    parkAndCheckInterrupt())
                    interrupted = true;
            }   
        } catch (Throwable t) {
            cancelAcquire(node);
            throw t;
        }
    }
```
以上方法来自ReentrantReadWriteLock，ReentrantReadWriteLock.ReadLock,AbstractQueuedSynchronizer,注意甄别。

ReadLock读锁只有在以下情况会发生阻塞的情况：
(1). 已经有线程拿到了WriteLock写锁，为避免读写同时存在引发的同步问题，这时要阻塞当前申请ReadLock读锁的线程，等待WriteLock写锁的释放并唤醒申请ReadLock读锁的线程。
(2). getExclusiveOwnerThread() != current
(3). fullTryAcquireShared(current)

排除以上三种情况，多个线程是可以同时访问ReadLock读锁锁住的临界资源，也就是它是一个共享锁。


WriteLock写锁获取锁的过程：
```
   public void lock() {
        sync.acquire(1);
    }
    public final void acquire(int arg) {
        //用的是ReentrantReadWriteLock.Sync内部类的方法tryAcquire()
        if (!tryAcquire(arg) &&
                acquireQueued(addWaiter(Node.EXCLUSIVE), arg))
            selfInterrupt();
    }
    //
    protected final boolean tryAcquire(int acquires) {
        /*
         * Walkthrough:
         * 1. If read count nonzero or write count nonzero
         *    and owner is a different thread, fail.
         * 2. If count would saturate, fail. (This can only
         *    happen if count is already nonzero.)
         * 3. Otherwise, this thread is eligible for lock if
         *    it is either a reentrant acquire or
         *    queue policy allows it. If so, update state
         *    and set owner.
         */
        Thread current = Thread.currentThread();
        int c = getState();//获取当前读锁+写锁的数量
        int w = exclusiveCount(c);//获取当前写锁的数量
        if (c != 0) {
            // (Note: if c != 0 and w == 0 then shared count != 0)
            //如果写锁数量w为0，读锁存在(c!=0)，不能申请写锁。或者持有锁的线程不是当前线程，说明其他线程有读/写锁，不能申请写锁
            if (w == 0 || current != getExclusiveOwnerThread())
                return false;
            //如果写入锁的数量大于最大数就抛出一个Error
            if (w + exclusiveCount(acquires) > MAX_COUNT)
                throw new Error("Maximum lock count exceeded");
            // Reentrant acquire
            setState(c + acquires);
            return true;
        }
        //// 如果当且写线程数为0，并且当前线程需要阻塞那么就返回失败；或者如果通过CAS增加写线程数失败也返回失败。
        if (writerShouldBlock() ||
                !compareAndSetState(c, c + acquires))
            return false;
        // 如果c=0，w=0或者c>0，w>0（重入），则设置当前线程或锁的拥有者
        setExclusiveOwnerThread(current);
        return true;
    }

    final boolean acquireQueued(final Node node, int arg) {
        try {
            boolean interrupted = false;
            for (;;) {
                final Node p = node.predecessor();
                if (p == head && tryAcquire(arg)) {
                    setHead(node);
                    p.next = null; // help GC
                    return interrupted;
                }
                if (shouldParkAfterFailedAcquire(p, node) &&
                        parkAndCheckInterrupt())
                    interrupted = true;
            }
        } catch (Throwable t) {
            cancelAcquire(node);
            throw t;
        }
    }
```