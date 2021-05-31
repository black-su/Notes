# HashCode相关


hashCode()函数与HashMap息息相关。HashMap中申请了一块连续的内存数组table，这个table数组的类型是Node<K,V>，k代表的是它的key值，v代表的是要加入HashMap的对象，这两个组合在一起形成了Node类型。既然是使用了数组，那数组中数据的插入就需要确定插入的下标是哪个。HashMap中这个数组下标的确定依赖于hashCode()函数返回的值，如下：
```
    public V put(K key, V value) {
        return putVal(hash(key), key, value, false, true);
    }
    static final int hash(Object key) {
        int h;
        return (key == null) ? 0 : (h = key.hashCode()) ^ (h >>> 16);
    }
    final V putVal(int hash, K key, V value, boolean onlyIfAbsent,
                   boolean evict) {
        Node<K,V>[] tab; Node<K,V> p; int n, i;
        if ((tab = table) == null || (n = tab.length) == 0)
            n = (tab = resize()).length;
        if ((p = tab[i = (n - 1) & hash]) == null)
            tab[i] = newNode(hash, key, value, null);
        ......
        return null;
    }
```
HashMap的数据插入过程，要先把当前的key值的hashCode()函数返回值拿到，然后通过一些列的位运算，确定了数组的下标。如果要插入的多个数据，多个数据通过hashCode()返回值计算拿到了相同的下标，那这就把这些数据之间形成一个Node节点。hashCode()返回值计算拿到了相同的值，就是哈希碰撞，哈希碰撞过多，那么Node节点形成的链表结构就越长，那么get()获取数据的开销就越大。

可以看到，HashMap中的数据存取完全是靠其key值的hashCode()计算得到的。可以说hashCode()的存在就是为了支持HashMap而设置的。对于不使用Map的情况下，比如使用List，数组等，hashCode()就完全没有作用。

Object基类中给定了hashCode()的实现：
```
    public int hashCode() {
        return identityHashCode(this);
    }
    /* package-private */ static int identityHashCode(Object obj) {
        int lockWord = obj.shadow$_monitor_;
        final int lockWordStateMask = 0xC0000000;  // Top 2 bits.
        final int lockWordStateHash = 0x80000000;  // Top 2 bits are value 2 (kStateHash).
        final int lockWordHashMask = 0x0FFFFFFF;  // Low 28 bits.
        if ((lockWord & lockWordStateMask) == lockWordStateHash) {
            return lockWord & lockWordHashMask;
        }
        return identityHashCodeNative(obj);
    }
    private static native int identityHashCodeNative(Object obj);
```
再具体的实现需要看Object的native方法了。看注释的解释是这里的返回值一般是通过key对象的内部地址转换成的整数。
我们通常习惯使用toString()方法来查看是否是同一个对象，其实也是通过hashCode()来判断的，只不过十进制的整数转换成了十六进制的。
```
    public String toString() {
        return getClass().getName() + "@" + Integer.toHexString(hashCode());
    }
```

如果我们自定义的类中不实现我们的hashCode()方法，那默认继承了Object的实现，返回的是key对象的内部地址。


String中实现的hashCode()：
```
    public int hashCode() {
        int h = hash;
        final int len = length();
        if (h == 0 && len > 0) {
            for (int i = 0; i < len; i++) {
                h = 31 * h + charAt(i);
            }
            hash = h;
        }
        return h;
    }
```
String中的hashCode()是通过遍历整个字符串计算得到的，n代表字符串长度，s代表字符的数值表示。计算如下：
s[0]*31^(n-1) + s[1]*31^(n-2) + ... + s[n-1]

Integer中实现的hashCode()：
```
    public static int hashCode(int value) {
        return value;
    }
```

以上的String，Integer的类型，都是我们通常在HashMap中用到的当作key的类型。
```
    Map<String,Object> map = new HashMap<>();
    Map<Integer,Object> map1 = new HashMap<>();
```
由于String，Integer都已经实现了hashCode()方法，在使用String，Integer的类型的HashMap之前我们不需要额外重写。那Object也实现了hashCode()方法，Object是所有类的基类。那是不是都不需要重写？这个也分情况，在使用Object作为HashMap的key值的时候，如果我们每次put进去的都是一个新的Object对象作为key，那也不需要重写，Object的hashCode()返回的就是对象的内部地址，每个对象的内部地址都不同。如果我们每次put进去的都是同一个Object对象作为key，但是又希望保存住每次修改这个对象的属性值时，就需要重写hashCode()，否则HashMap中的值会互相覆盖。
```
    class Customer {
        public String username;
        public int age;
    }

    //需要重写hashCode()
    HashMap<Customer, Integer> map = new HashMap<>();
    Customer customer = new Customer();
    map.put(customer,1);

    customer.age = 24;
    map.put(customer,2);

    customer.username = "张三";
    map.put(customer,3);

    customer.username = "李四";
    map.put(customer,4);

    customer.age = 8;
    map.put(customer,5);


    //不需要重写hashCode()
    HashMap<Customer, Integer> map = new HashMap<>();
    Customer customer = new Customer();
    Customer customer1 = new Customer();
    Customer customer2 = new Customer();
    map.put(customer,0);
    map.put(customer1,1);
    map.put(customer2,2);

```

如何重写hashCode()方法：
```
1. 初始化一个整形变量，为此变量赋予一个非零的常数值，比如int result =17;
2. 选取equals方法中用于比较的所有域，然后针对每个域的属性进行计算：
  (1) 如果是boolean值，则计算f ? 1:0
  (2) 如果是byte\char\short\int,则计算(int)f
  (3) 如果是long值，则计算(int)(f ^ (f >>> 32))
  (4) 如果是float值，则计算Float.floatToIntBits(f)
  (5) 如果是double值，则计算Double.doubleToLongBits(f)，然后返回的结果是long,再用规则(3)去处理long,得到int
  (6) 如果是对象应用，如果equals方法中采取递归调用的比较方式，那么hashCode中同样采取递归调用hashCode的方式。否则需要为这个域计算一个范式，比如当这个域的值为null的时候，那么hashCode 值为0
  (7) 如果是数组，没必要自己去重新遍历一遍数组，java.util.Arrays.hashCode方法包含了8种基本类型数组和引用数组的hashCode计算，算法同上，
3. 每个域的散列码合并到result当中：result = 31 * result + elementHash;
```
可以参考String中hashCode()的写法。



equals()方法：

Object
```
    public boolean equals(Object obj) {
        return (this == obj);
    }
```
判断是否是同一个对象，同一个对象返回true

String
```
    public boolean equals(Object anObject) {
        if (this == anObject) {
            return true;
        }
        if (anObject instanceof String) {
            String anotherString = (String)anObject;
            int n = length();
            if (n == anotherString.length()) {
                int i = 0;
                while (n-- != 0) {
                    if (charAt(i) != anotherString.charAt(i))
                            return false;
                    i++;
                }
                return true;
            }
        }
        return false;
    }
```
如果是同一个对象返回true，不同对象判断字符串是否相同，相同返回true。

Integer
```
    public boolean equals(Object obj) {
        if (obj instanceof Integer) {
            return value == ((Integer)obj).intValue();
        }
        return false;
    }
```
判断int的值是否相同，相同返回true。


equals()方法和hashCode()方法的设计规则：
```
     对于equals，我们必须遵循如下规则：

      对称性：如果x.equals(y)返回是“true”，那么y.equals(x)也应该返回是“true”。

      反射性：x.equals(x)必须返回是“true”。

      类推性：如果x.equals(y)返回是“true”，而且y.equals(z)返回是“true”，那么z.equals(x)也应该返回是“true”。

      一致性：如果x.equals(y)返回是“true”，只要x和y内容一直不变，不管你重复x.equals(y)多少次，返回都是“true”。

      任何情况下，x.equals(null)，永远返回是“false”；x.equals(和x不同类型的对象)永远返回是“false”。

      对于hashCode，我们应该遵循如下规则：

      1. 在一个应用程序执行期间，如果一个对象的equals方法做比较所用到的信息没有被修改的话，则对该对象调用hashCode方法多次，它必须始终如一地返回同一个整数。

      2. 如果两个对象根据equals(Object o)方法是相等的，则调用这两个对象中任一对象的hashCode方法必须产生相同的整数结果。

      3. 如果两个对象根据equals(Object o)方法是不相等的，则调用这两个对象中任一个对象的hashCode方法，不要求产生不同的整数结果。但如果能不同，则可能提高散列表的性能。
```



