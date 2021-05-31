#LinkedBlockingQueue 与 ArrayBlockingQueue 的原理


LinkedBlockingQueue

总结一下：LinkedBlockingQueue是一个单链表结构，可以设置链表的长度。如果链表中还没有item元素，仍然要取item元素，或者链表中的元素数量已到达最大值，但是仍然要加入item元素。这两种情况下，提供了两套方案，一个是不阻塞当前线程，选择直接return。另一套方案是阻塞当前线程，一直到条件满足，可以正常取出或者加入元素。

阻塞和非阻塞操作：
put()和offer()
take()和poll()

LinkedBlockingQueue中使用了非公平锁，意味着put()和take()的操作，不一定会按照调用的顺序来执行。在已经阻塞的情况下，后一个put()可能比前一个put()先执行，后一个take()可能比前一个take()先拿到数据。

LinkedBlockingQueue的节点：

```
    static class Node<E> {
        E item;

        /**
         * One of:
         * - the real successor Node
         * - this Node, meaning the successor is head.next
         * - null, meaning there is no successor (this is the last node)
         */
        Node<E> next;

        Node(E x) { item = x; }
    }
```
很常见的Node，内部定义next，指向下一个Node，形成一个单链表结构

LinkedBlockingQueue的属性：
```
    /** The capacity bound, or Integer.MAX_VALUE if none */
    private final int capacity;

    /** Current number of elements */
    private final AtomicInteger count = new AtomicInteger();

    /**
     * Head of linked list.
     * Invariant: head.item == null
     */
    transient Node<E> head;

    /**
     * Tail of linked list.
     * Invariant: last.next == null
     */
    private transient Node<E> last;

    /** Lock held by take, poll, etc */
    private final ReentrantLock takeLock = new ReentrantLock();

    /** Wait queue for waiting takes */
    private final Condition notEmpty = takeLock.newCondition();

    /** Lock held by put, offer, etc */
    private final ReentrantLock putLock = new ReentrantLock();

    /** Wait queue for waiting puts */
    private final Condition notFull = putLock.newCondition();
```

LinkedBlockingQueue是单链表结构，理论上是没有容量限制的，但是这里定义了容量属性capacity，规定了LinkedBlockingQueue的最大容量就是Integer.MAX_VALUE，超过这个容量，item元素就添加不进去，会一直阻塞直到容量有剩余。

head，last作为头尾节点的标记，一开始是指向同一个空的Node，随着item元素的添加和删除移动。LinkedBlockingQueue定义了元素只能由尾节点插入，头节点删除。

LinkedBlockingQueue是线程安全的，节点的插入和删除分别由takeLock，putLock两个ReentrantLock锁来保证线程安全。

LinkedBlockingQueue是有容量限制的，如果当前容量已经达到最大值，仍然有item元素想要加入，或者当前item元素数量为0，但是这时有人取item元素，那怎么处理呢？这里使用了ReentrantLock锁里面的Condition来处理这种情况，分别是notEmpty，notFull。如果队列已满但是仍要插入，那么把当前put操作加入到ReentrantLock.Condition的一个单链表中，并阻塞当前的执行线程，等待被唤醒。如果队列为空但是有人要取item元素，那么也阻塞当前线程，take操作加入阻塞列表中，等待被唤醒。

这里使用的是LockSupport.unpark(),LockSupport.park()进行阻塞/唤醒操作，跟Object中提供的wait/notify是一样的功能。因此我们在使用LinkedBlockingQueue时，注意不要放在主线程下

LinkedBlockingQueue的构造方法：
```
    public LinkedBlockingQueue() {
        this(Integer.MAX_VALUE);
    }
    public LinkedBlockingQueue(int capacity) {
        if (capacity <= 0) throw new IllegalArgumentException();
        this.capacity = capacity;
        last = head = new Node<E>(null);
    }
    public LinkedBlockingQueue(Collection<? extends E> c) {
        this(Integer.MAX_VALUE);
        final ReentrantLock putLock = this.putLock;
        putLock.lock(); // Never contended, but necessary for visibility
        try {
            int n = 0;
            for (E e : c) {
                if (e == null)
                    throw new NullPointerException();
                if (n == capacity)
                    throw new IllegalStateException("Queue full");
                enqueue(new Node<E>(e));
                ++n;
            }
            count.set(n);
        } finally {
            putLock.unlock();
        }
    }
```
定义了capacity的最大容量Integer.MAX_VALUE，或者可以自定义capacity的大小，头尾节点last，head同时指向一个空节点Node。也可以初始化LinkedBlockingQueue时传入一个集合，就地构造出一个单链表。


LinkedBlockingQueue的item元素加入操作有两个，分别是put()和offer()，他两的区别是：如果当前容量已到达最大值，是否阻塞当前的添加操作？put()是阻塞当前线程，直到有位置插入数据。offer()是直接返回false。
```
    public void put(E e) throws InterruptedException {
        if (e == null) throw new NullPointerException();
        // Note: convention in all put/take/etc is to preset local var
        // holding count negative to indicate failure unless set.
        int c = -1;
        Node<E> node = new Node<E>(e);
        final ReentrantLock putLock = this.putLock;
        final AtomicInteger count = this.count;
        putLock.lockInterruptibly();
        try {
            /*
             * Note that count is used in wait guard even though it is
             * not protected by lock. This works because count can
             * only decrease at this point (all other puts are shut
             * out by lock), and we (or some other waiting put) are
             * signalled if it ever changes from capacity. Similarly
             * for all other uses of count in other wait guards.
             */
            while (count.get() == capacity) {
                notFull.await();//到达最大容量，放弃持有的锁，进入阻塞队列，等待被唤醒
            }
            enqueue(node);//元素添加
            c = count.getAndIncrement();//当前元素长度+1
            if (c + 1 < capacity)//唤醒阻塞队列
                notFull.signal();
        } finally {
            putLock.unlock();
        }
        if (c == 0)
            signalNotEmpty();//已加入了item元素，唤醒notEmpty可以取元素了
    }

    public boolean offer(E e) {
        if (e == null) throw new NullPointerException();
        final AtomicInteger count = this.count;
        if (count.get() == capacity)//到达最大容量，直接返回false
            return false;
        int c = -1;
        Node<E> node = new Node<E>(e);
        final ReentrantLock putLock = this.putLock;
        putLock.lock();
        try {
            if (count.get() < capacity) {
                enqueue(node);
                c = count.getAndIncrement();
                if (c + 1 < capacity)
                    notFull.signal();
            }
        } finally {
            putLock.unlock();
        }
        if (c == 0)
            signalNotEmpty();//已加入了item元素，唤醒notEmpty可以取元素了
        return c >= 0;
    }

    //在单链表的末尾添加新的Node节点，并移动last节点指向新加入的Node节点。
    private void enqueue(Node<E> node) {
        // assert putLock.isHeldByCurrentThread();
        // assert last.next == null;
        last = last.next = node;
    }

```

LinkedBlockingQueue的取出操作常用的有两个：take()和poll()，他们的区别：当前队列中如果没有item元素，那么取出操作是否要阻塞？take()是阻塞当前线程一直到有数据取出为止。poll()是直接返回null。
```
    public E take() throws InterruptedException {
        E x;
        int c = -1;
        final AtomicInteger count = this.count;
        final ReentrantLock takeLock = this.takeLock;
        takeLock.lockInterruptibly();
        try {
            while (count.get() == 0) {
                notEmpty.await();
            }
            x = dequeue();
            c = count.getAndDecrement();
            if (c > 1)
                notEmpty.signal();
        } finally {
            takeLock.unlock();
        }
        if (c == capacity)
            signalNotFull();//已取出item元素，唤醒notFull可以添加元素了
        return x;
    }

    public E poll() {
        final AtomicInteger count = this.count;
        if (count.get() == 0)
            return null;
        E x = null;
        int c = -1;
        final ReentrantLock takeLock = this.takeLock;
        takeLock.lock();
        try {
            if (count.get() > 0) {
                x = dequeue();
                c = count.getAndDecrement();
                if (c > 1)
                    notEmpty.signal();
            }
        } finally {
            takeLock.unlock();
        }
        if (c == capacity)
            signalNotFull();//已取出item元素，唤醒notFull可以添加元素了
        return x;
    }

    //取出head节点所指向的下一个Node节点的数据的item元素（head所在的是一个空节点）
    private E dequeue() {
        // assert takeLock.isHeldByCurrentThread();
        // assert head.item == null;
        Node<E> h = head;
        Node<E> first = h.next;
        h.next = h; // help GC
        head = first;
        E x = first.item;
        first.item = null;
        return x;
    }
```


另外，LinkedBlockingQueue的取出操作还有peek()，这个是取出head节点所指向的下一个Node节点的数据的item元素（head所在的是一个空节点）。不改变链表结构。
```
    public E peek() {
        if (count.get() == 0)
            return null;
        final ReentrantLock takeLock = this.takeLock;
        takeLock.lock();
        try {
            return (count.get() > 0) ? head.next.item : null;
        } finally {
            takeLock.unlock();
        }
    }
```


--------

ArrayBlockingQueue


总结一下：ArrayBlockingQueue是一个数组结构，添加和取出item元素都是从下标0开始，操作成功后takeIndex或者putIndex加1。取出item元素的时候顺便把当前下标的数据设置为null。通过takeIndex和putIndex下标的标记，达到了单链表头尾节点的操作效果。相对于LinkedBlockingQueue，ArrayBlockingQueue可以自由选择公平锁还是非公平锁。


ArrayBlockingQueue的属性：
```
    /** The queued items */
    final Object[] items;

    /** items index for next take, poll, peek or remove */
    int takeIndex;

    /** items index for next put, offer, or add */
    int putIndex;

    /** Number of elements in the queue */
    int count;

    /*
     * Concurrency control uses the classic two-condition algorithm
     * found in any textbook.
     */

    /** Main lock guarding all access */
    final ReentrantLock lock;

    /** Condition for waiting takes */
    private final Condition notEmpty;

    /** Condition for waiting puts */
    private final Condition notFull;

```
ArrayBlockingQueue使用了数组items来保存item元素。数组items的大小由使用者调用构造函数来设定。takeIndex和putIndex分别标记了要插入和取出的下标。

相比LinkedBlockingQueue，ArrayBlockingQueue只有一个lock，是因为LinkedBlockingQueue是链表结构，取出和添加是在链表的头尾，彼此的操作并不会影响对方。而ArrayBlockingQueue是数组结构，取出和添加都需要修改下标，会影响彼此的操作。

ArrayBlockingQueue的构造函数
```
    public ArrayBlockingQueue(int capacity) {
        this(capacity, false);
    }
    public ArrayBlockingQueue(int capacity, boolean fair) {
        if (capacity <= 0)
            throw new IllegalArgumentException();
        this.items = new Object[capacity];
        lock = new ReentrantLock(fair);
        notEmpty = lock.newCondition();
        notFull =  lock.newCondition();
    }
    public ArrayBlockingQueue(int capacity, boolean fair,
                              Collection<? extends E> c) {
        this(capacity, fair);

        final ReentrantLock lock = this.lock;
        lock.lock(); // Lock only for visibility, not mutual exclusion
        try {
            int i = 0;
            try {
                for (E e : c)
                    items[i++] = Objects.requireNonNull(e);
            } catch (ArrayIndexOutOfBoundsException ex) {
                throw new IllegalArgumentException();
            }
            count = i;
            putIndex = (i == capacity) ? 0 : i;
        } finally {
            lock.unlock();
        }
    }
```
相比LinkedBlockingQueue，ArrayBlockingQueue可以设置使用公平锁还是非公平锁。

ArrayBlockingQueue的添加操作：
```
    public void put(E e) throws InterruptedException {
        Objects.requireNonNull(e);
        final ReentrantLock lock = this.lock;
        lock.lockInterruptibly();
        try {
            while (count == items.length)
                notFull.await();//到达容量最大值，阻塞线程，直到被唤醒
            enqueue(e);
        } finally {
            lock.unlock();
        }
    }
    public boolean offer(E e) {
        Objects.requireNonNull(e);
        final ReentrantLock lock = this.lock;
        lock.lock();
        try {
            if (count == items.length)//到达容量最大值，直接返回false
                return false;
            else {
                enqueue(e);
                return true;
            }
        } finally {
            lock.unlock();
        }
    }

    //根据下标存储item元素。之后唤醒take()操作
    private void enqueue(E x) {
        // assert lock.getHoldCount() == 1;
        // assert items[putIndex] == null;
        final Object[] items = this.items;
        items[putIndex] = x;
        if (++putIndex == items.length) putIndex = 0;
        count++;
        notEmpty.signal();
    }
```

ArrayBlockingQueue的取出操作：
```
    public E poll() {
        final ReentrantLock lock = this.lock;
        lock.lock();
        try {
            return (count == 0) ? null : dequeue();
        } finally {
            lock.unlock();
        }
    }

    public E take() throws InterruptedException {
        final ReentrantLock lock = this.lock;
        lock.lockInterruptibly();
        try {
            while (count == 0)//没有item元素，阻塞线程，直到被唤醒
                notEmpty.await();
            return dequeue();
        } finally {
            lock.unlock();
        }
    }

    //取出指定下标的item元素，并替换为null。之后唤醒put()操作
    private E dequeue() {
        // assert lock.getHoldCount() == 1;
        // assert items[takeIndex] != null;
        final Object[] items = this.items;
        @SuppressWarnings("unchecked")
        E x = (E) items[takeIndex];
        items[takeIndex] = null;
        if (++takeIndex == items.length) takeIndex = 0;
        count--;
        if (itrs != null)
            itrs.elementDequeued();
        notFull.signal();
        return x;
    }
```

takeIndex和putIndex下标的变化可以参考dequeue()和enqueue()方法，一个自减，一个自加。达到数组的边界下标，则互相替换array.length和0。通过对下标的变换，达到类似链表的操作。


