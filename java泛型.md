#java泛型

参考：

https://blog.csdn.net/jeffleo/article/details/52250948


1. 泛型上界< ? extends Class>


```
class Fruit{}
class Apple extends Fruit{}
class Apple1 extends Apple{}
class Orange extends Fruit{}


//使用add()方法编译器报错，可以使用get()方法
List<? extends Fruit> list = new ArrayList<>();
//list.add(new Apple());//编译器报错
//list.add(new Orange());
//list.add(new Fruit());
Fruit fruit = list.get(0);//向上转型
```
List是使用数组来保存数据的，如果保存的是对象，也只是保存一个对象的引用，真正的对象保存在java堆中。List<? extends Fruit> list表示这个list中保存的对象引用的类型是Fruit或者是它的子类，可以把list数组中的数据类型当成一个盲盒来看待，有可能是包括Fruit在内的所有子类的类型，所以假设其数据类型如下：
| Fruit | Orange| Apple | Fruit | Apple1 | Orange |
如果List<? extends Fruit> list开放add()方法给我们使用，除非我们每次add()进去的Orange对象，否则，add()进去必然会发生类型转换异常。如顺序执行一下方法：
add(new Fruit())
add(new Fruit())
add(new Orange())
add(new Apple1())
add(new Fruit())
add(new Fruit())
基于以上缘由，为了减少运行时检测出问题的几率，编译器就不会让我们使用add()方法。但是get()方法是可以使用的，既然知道list中的数据类型都是Fruit及其子类，那我们可以使用Fruit fruit引用来接收list中的数据，子类对象赋值给父类引用，向上转型。

List<? extends Fruit>不能正常使用add()方法，就需要我们在初始化的直接赋值：
```
List<? extends Fruit> list1 = new ArrayList<>(Arrays.asList(new Apple() , new Apple1()));
```


2. 泛型下界< ? super Class>

```
class Fruit{}
class Apple extends Fruit{}
class Apple1 extends Apple{}
class Orange extends Fruit{}

//使用get()方法赋值引用时编译器报错，可以使用add()方法
List<? super Apple> list = new ArrayList<>();
list.add(new Apple());
list.add(new Apple1());
//list.add(new Fruit());//编译器报错
//即使你知道它是Apple类型，但你就是拿不到，只能让它转型为Object类型
Object object = list.get(0);
Apple apple = list.get(0);//编译器报错
```
List<? super Apple> list表示这个list中保存的对象引用类型是Apple及其父类。假设其数据类型如下：
| Fruit | Apple| Apple | Fruit | Apple | Apple |
这个list中我们可以使用add()添加Apple及其子类对象，这样在list中这些添加的对象可以正常向上转型。当然我们不能add()添加Apple的父类，这样会向上转型失败。
list.get(0)拿到的有可能是Fruit对象，也有可能是Apple对象，除非我们每次都记得使用最上层的父类引用去接受list.get(0)的值，否则会导致运行时类型转换异常。基于此，编译器直接不让我们使用get()方法，以减少运行时失败的几率。




3. 什么时候使用extends，super，具体的场景？