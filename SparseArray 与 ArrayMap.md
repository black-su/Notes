#SparseArray 与 ArrayMap


https://blog.csdn.net/xiaxl/article/details/77267201

SparseArray的扩容：

GrowingArrayUtils.java
```
    public static int growSize(int currentSize) {
        return currentSize <= 4 ? 8 : currentSize * 2;
    }
```

