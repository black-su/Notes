# HashMap 与 LinkedHashMap 源码分析


HashMap的Node节点类：

```
    static class Node<K,V> implements Map.Entry<K,V> {
        final int hash;
        final K key;
        V value;
        Node<K,V> next;
    }
```

Node节点作为HashMap存储数据的载体，里面定义了Node<K,V> next，以便把Node串在一起，形成一个链表结构。

HashMap中定义了一个Node数组，用来保存数据，Node数组的创建如下：

```
    transient Node<K,V>[] table;

    final Node<K,V>[] resize() {
        Node<K,V>[] oldTab = table;
        int oldCap = (oldTab == null) ? 0 : oldTab.length;
        int oldThr = threshold;
        int newCap, newThr = 0;
        if (oldCap > 0) {
            if (oldCap >= MAXIMUM_CAPACITY) {
                threshold = Integer.MAX_VALUE;
                return oldTab;
            }
            else if ((newCap = oldCap << 1) < MAXIMUM_CAPACITY &&
                     oldCap >= DEFAULT_INITIAL_CAPACITY)
                newThr = oldThr << 1; // double threshold
        }
        else if (oldThr > 0) // initial capacity was placed in threshold
            newCap = oldThr;
        else {               // zero initial threshold signifies using defaults
            newCap = DEFAULT_INITIAL_CAPACITY;
            newThr = (int)(DEFAULT_LOAD_FACTOR * DEFAULT_INITIAL_CAPACITY);
        }
        if (newThr == 0) {
            float ft = (float)newCap * loadFactor;
            newThr = (newCap < MAXIMUM_CAPACITY && ft < (float)MAXIMUM_CAPACITY ?
                      (int)ft : Integer.MAX_VALUE);
        }
        threshold = newThr;
        //根据HashMap设置的大小创建一个Node数组
        @SuppressWarnings({"rawtypes","unchecked"})
            Node<K,V>[] newTab = (Node<K,V>[])new Node[newCap];
        table = newTab;
        ......
        return newTab;
```


我们在new HashMap<>()的还没有创建Node数组，只是定义了数组的大小。当我们往HashMap中插入数据的时候，如果Node数组还没有创建，则调用resize()创建。之后通过hash散列函数计算出当前插入的数据应该放在Node数组的哪个下标下面，如果当前下标已经保存有数据，则插入的数据要与当前下标的Node节点形成一个链表结构。视情况是否要把节点转换成树节点，形成红黑树以优化性能。

```
    final V putVal(int hash, K key, V value, boolean onlyIfAbsent,
                   boolean evict) {
        Node<K,V>[] tab; Node<K,V> p; int n, i;
        //如果Node数组还没有创建，则调用resize()创建
        if ((tab = table) == null || (n = tab.length) == 0)
            n = (tab = resize()).length;
        //计算当前插入数据应该放在Node数组的哪个下标下面，如果当前下标还没有存放数据，则存放
        if ((p = tab[i = (n - 1) & hash]) == null)
            tab[i] = newNode(hash, key, value, null);
        //如果当前下标已经存放了数据，则插入的数据要与当前下标下的Node节点形成一个链表
        else {
            Node<K,V> e; K k;
            if (p.hash == hash &&
                ((k = p.key) == key || (key != null && key.equals(k))))
                e = p;
            //当前节点是否为树节点，如果为树节点，则Node节点形成一个红黑树
            else if (p instanceof TreeNode)
                e = ((TreeNode<K,V>)p).putTreeVal(this, tab, hash, key, value);
            else {
                for (int binCount = 0; ; ++binCount) {
                    if ((e = p.next) == null) {
                        p.next = newNode(hash, key, value, null);
                        if (binCount >= TREEIFY_THRESHOLD - 1) // -1 for 1st
                            treeifyBin(tab, hash);//是否要转换成树节点
                        break;
                    }
                    if (e.hash == hash &&
                        ((k = e.key) == key || (key != null && key.equals(k))))
                        break;
                    p = e;
                }
            }
            if (e != null) { // existing mapping for key
                V oldValue = e.value;
                if (!onlyIfAbsent || oldValue == null)
                    e.value = value;
                afterNodeAccess(e);
                return oldValue;
            }
        }
        ++modCount;
        if (++size > threshold)
            resize();
        afterNodeInsertion(evict);
        return null;
    }
```

从HashMap中取数据

```

    //hash(key)通过hash散列函数计算出Node数组的下标
    public V get(Object key) {
        Node<K,V> e;
        return (e = getNode(hash(key), key)) == null ? null : e.value;
    }

    final Node<K,V> getNode(int hash, Object key) {
        Node<K,V>[] tab; Node<K,V> first, e; int n; K k;
        if ((tab = table) != null && (n = tab.length) > 0 &&
            (first = tab[(n - 1) & hash]) != null) {
            //如果要查找的数据刚好是Node数组中的Node，则返回。
            if (first.hash == hash && // always check first node
                ((k = first.key) == key || (key != null && key.equals(k))))
                return first;
            //否则遍历Node链表，查询是否存在要查找的Node
            if ((e = first.next) != null) {
                if (first instanceof TreeNode)
                    return ((TreeNode<K,V>)first).getTreeNode(hash, key);
                do {
                    if (e.hash == hash &&
                        ((k = e.key) == key || (key != null && key.equals(k))))
                        return e;
                } while ((e = e.next) != null);
            }
        }
        return null;
    }
```

LinkedHashMap继承自HashMap，重写了get()方法，但是没有重写put()方法，所以LinkedHashMap仍然使用了HashMap的put()方法。

LinkedHashMap存储数据的Entry继承自HashMap.Node，用before，after替代Node中的next，形成双向链表结构。
```
    static class LinkedHashMapEntry<K,V> extends HashMap.Node<K,V> {
        LinkedHashMapEntry<K,V> before, after;
        LinkedHashMapEntry(int hash, K key, V value, Node<K,V> next) {
            super(hash, key, value, next);
        }
    }

    /**
     * The head (eldest) of the doubly linked list.
     */
    transient LinkedHashMapEntry<K,V> head;

    /**
     * The tail (youngest) of the doubly linked list.
     */
    transient LinkedHashMapEntry<K,V> tail;

    //新建Node节点之前先用head，tail标记链表头尾，然后用before, after构成双向链表
    Node<K,V> newNode(int hash, K key, V value, Node<K,V> e) {
        LinkedHashMapEntry<K,V> p =
            new LinkedHashMapEntry<K,V>(hash, key, value, e);
        linkNodeLast(p);
        return p;
    }

    // link at the end of list
    private void linkNodeLast(LinkedHashMapEntry<K,V> p) {
        LinkedHashMapEntry<K,V> last = tail;
        tail = p;
        if (last == null)
            head = p;
        else {
            p.before = last;
            last.after = p;
        }
    }
```

因为LinkedHashMap没有重写put()方法，其Node节点的存储仍然根Hashap完全一样，只是Node这个节点变成了LinkedHashMapEntry这个子类。因此，在HashMap的putVal()方法中调用的newNode()方法就调用了LinkedHashMap的newNode()。newNode()不仅标记了链表的头尾，还用用before, after构成一个双向链表。

LinkedHashMap重写了get()方法，每次get()方法成功从LinkedHashMap获取一个数据的时候，就把该Node节点移到双向链表的末尾，也就是tail之后，此时该Node就成了tail。（双向链表必须要有head，tail标记链表头尾，否则分不出哪里是头尾）

```
    public V get(Object key) {
        Node<K,V> e;
        if ((e = getNode(hash(key), key)) == null)
            return null;
        if (accessOrder)
            afterNodeAccess(e);
        return e.value;
    }

    void afterNodeAccess(Node<K,V> e) { // move node to last
        LinkedHashMapEntry<K,V> last;
        if (accessOrder && (last = tail) != e) {
            LinkedHashMapEntry<K,V> p =
                (LinkedHashMapEntry<K,V>)e, b = p.before, a = p.after;
            p.after = null;
            if (b == null)
                head = a;
            else
                b.after = a;
            if (a != null)
                a.before = b;
            else
                last = b;
            if (last == null)
                head = p;
            else {
                p.before = last;
                last.after = p;
            }
            tail = p;
            ++modCount;
        }
    }
```


综上，LinkedHashMap和HashMap都是用一个Node数组存储数据，如果多个插入数据计算得到的hash值相同(下标相同)，那这多个数据要形成一个链表结构。

LinkedHashMap与HashMap不同：
1. HashMap的链表结构是单向链表，而LinkedHashMap的链表结构是双向链表，这个双向链表是额外的用before, after把所有Node节点都弄成双向链表。但是本质上还是跟HashMap一样，由Node数组和单链表构成。
2. HashMap的链表结构无筛选功能，而LinkedHashMap的链表结构可以实现"最近使用原则“，即把最近使用的数据都挪到链表尾部，链表头部都是不常用的，在内存空间限制的情况下，要有限删除不常用的数据。LinkedHashMap链表结构的这个特点可以使它可以作为LruCache的数据结构。

单向链表，
LruCache的使用：

```
        LruCache<String, Bitmap> mLruCache;
        //获取手机最大内存 单位 kb
        int maxMemory = (int) (Runtime.getRuntime().maxMemory() / 1024);
        //一般都将1/8设为LruCache的最大缓存
        int cacheSize = maxMemory / 8;
        mLruCache = new LruCache<String, Bitmap>(maxMemory / 8) {

            /**
             * 这个方法从源码中看出来是设置已用缓存的计算方式的。
             * 默认返回的值是1，也就是没缓存一张图片就将已用缓存大小加1.
             * 缓存图片看的是占用的内存的大小，每张图片的占用内存也是不一样的，一次不能这样算。
             * 因此要重写这个方法，手动将这里改为本次缓存的图片的大小。
             */
            @Override
            protected int sizeOf(String key, Bitmap value) {
                return value.getByteCount() / 1024;
            }
        };
        mLruCache.put("key", BitmapFactory.decodeResource(getResources(), R.mipmap.ic_launcher));
        Bitmap bitmap = mLruCache.get("key");
```


LruCache的源码分析：

```
    public LruCache(int maxSize) {
        if (maxSize <= 0) {
            throw new IllegalArgumentException("maxSize <= 0");
        }
        this.maxSize = maxSize;//设置LruCache的最大存储空间
        this.map = new LinkedHashMap<K, V>(0, 0.75f, true);
    }

   public final V get(K key) {
        if (key == null) {
            throw new NullPointerException("key == null");
        }

        V mapValue;
        synchronized (this) {
            mapValue = map.get(key);//从LinkedHashMap中取出数据
            if (mapValue != null) {
                hitCount++;
                return mapValue;
            }
            missCount++;
        }
        //********************暂时搞不清楚下面这个的实用场景******************
        /*
         * Attempt to create a value. This may take a long time, and the map
         * may be different when create() returns. If a conflicting value was
         * added to the map while create() was working, we leave that value in
         * the map and release the created value.
         */

        V createdValue = create(key);
        if (createdValue == null) {
            return null;
        }

        synchronized (this) {
            createCount++;
            mapValue = map.put(key, createdValue);

            if (mapValue != null) {
                // There was a conflict so undo that last put
                map.put(key, mapValue);
            } else {
                size += safeSizeOf(key, createdValue);
            }
        }

        if (mapValue != null) {
            entryRemoved(false, key, createdValue, mapValue);
            return mapValue;
        } else {
            trimToSize(maxSize);
            return createdValue;
        }
    }

    public final V put(K key, V value) {
        if (key == null || value == null) {
            throw new NullPointerException("key == null || value == null");
        }

        V previous;
        synchronized (this) {
            putCount++;
            size += safeSizeOf(key, value);//修改当前实用空间size
            previous = map.put(key, value);//往LinkedHashMap插入数据
            //返回null表示插入数据失败，刚才对size做了增加，现在要减少
            if (previous != null) {
                size -= safeSizeOf(key, previous);
            }
        }

        //如有需要，可以重写entryRemoved()来处理添加数据成功的情况
        if (previous != null) {
            entryRemoved(false, key, previous, value);
        }

        //插入数据的时候，需要检查LruCache剩余的空间是否还够用，不够用需要删除旧数据
        trimToSize(maxSize);
        return previous;
    }

    public void trimToSize(int maxSize) {
        //死循环
        while (true) {
            K key;
            V value;
            synchronized (this) {
                if (size < 0 || (map.isEmpty() && size != 0)) {
                    throw new IllegalStateException(getClass().getName()
                            + ".sizeOf() is reporting inconsistent results!");
                }
                //当前LruCache的缓存空间还剩余，则跳过之后的代码。否则就要清理旧数据腾出空间
                if (size <= maxSize) {
                    break;
                }
                //返回LinkedHashMap链表头部第一个数据
                Map.Entry<K, V> toEvict = map.eldest();
                if (toEvict == null) {
                    break;
                }

                //删除LinkedHashMap链表头部第一个数据，并修改当前实用空间size
                key = toEvict.getKey();
                value = toEvict.getValue();
                map.remove(key);
                size -= safeSizeOf(key, value);
                evictionCount++;
            }

            entryRemoved(true, key, value, null);
        }
    }
```