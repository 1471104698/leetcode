# subList()、asList()、singletonList() 坑点



## 1、ArrayList.subList()



在 阿里巴巴开发手册中，告知要慎用 ArrayList 的 subList() 方法，规定如下：

 ![-w1379](https://user-gold-cdn.xitu.io/2019/6/25/16b8c5809732a52d?imageView2/0/w/1280/h/960/format/webp/ignore-error/1) 

 ![img](https://user-gold-cdn.xitu.io/2019/6/25/16b8c58095d31b5e?imageView2/0/w/1280/h/960/format/webp/ignore-error/1)



> ### SubList 无法强转为 ArrayList

现在就来解析下 subList() 方法

```java
List<String> subList = list.subList(1, 3);

public List<E> subList(int fromIndex, int toIndex) {
    //创建一个 subList 对象，并且将当前 ArrayList 对象 this 传入
    return new SubList(this, 0, fromIndex, toIndex);
}
```

 当我们调用 subList() 的时候，内部实际上是创建了一个 SubList 对象



```java
private class SubList extends AbstractList<E> implements RandomAccess {	
    private final AbstractList<E> parent;
    private final int parentOffset;
    private final int offset;
    int size;
    //构造方法
    SubList(AbstractList<E> parent,
            int offset, int fromIndex, int toIndex) {
        //这里的 parent 就是 ArrayList 对象
        this.parent = parent;
        //偏移量
        this.parentOffset = fromIndex;
        this.offset = offset + fromIndex;
        this.size = toIndex - fromIndex;
        this.modCount = ArrayList.this.modCount;
    }
}
```

SubList 对象是 ArrayList 的一个内部类，它继承了 AbstractList，实际上就是一个 List 类，但是**不是一个 ArrayList 类，所以强转为 ArrayList 会失败**





> ### 并发异常问题

从上面代码可以看出，通过构造方法它并没有截取 指定索引范围的 ArrayList 中的元素，将其放到一个新的容器里，而是直接将 ArrayList 对象 和 偏移量给保存了起来，这意味着是要在 ArrayList 对象上直接操作啊。。。

那么操作 SubList 就会影响到 ArrayList，操作 ArrayList 就会影响到 SubList



```java
public E set(int index, E e) {
    rangeCheck(index);
    checkForComodification();
    E oldValue = ArrayList.this.elementData(offset + index);
    ArrayList.this.elementData[offset + index] = e;
    return oldValue;
}

public E get(int index) {
    rangeCheck(index);
    checkForComodification();
    return ArrayList.this.elementData(offset + index);
}

public void add(int index, E e) {
    rangeCheckForAdd(index);
    checkForComodification();
    parent.add(parentOffset + index, e);
    this.modCount = parent.modCount;
    this.size++;
}
```

通过查看 SubList 的 get() 、set()、add() ， 我们也可以发现，它实际上还真是直接对 ArrayList 进行操作

因此，当我们调用 SubList 插入元素的时候，实际上就是在 ArrayList 对应偏移量上进行元素插入，对 SubList 的操作全部会影响到 ArrayList 上

```java
public class A {
    public static void main(String[] args) throws InterruptedException {
        ArrayList<String> list = new ArrayList<String>() {{
            add("H");
            add("O");
            add("L");
            add("L");
            add("I");
            add("S");
        }};

        List<String> subList = list.subList(1, 3);

        subList.add("1");
        System.out.println(list);
        System.out.println(subList);
    }
}
```

输出结果为：

```java
[H, O, L, 1, L, I, S]
[O, L, 1]
```

可以看出 ArrayList 也被影响了



当我们操作 ArrayList 时

```java
public class A {
    public static void main(String[] args) throws InterruptedException {
        ArrayList<String> list = new ArrayList<String>() {{
            add("H");
            add("O");
            add("L");
            add("L");
            add("I");
            add("S");
        }};

        List<String> subList = list.subList(1, 3);
        // List<String> strings = Collections.singletonList("1");

        list.add("1");
        System.out.println(list);
        System.out.println(subList);
    }
}
```

输出结果：

```java
[H, O, L, L, I, S, 1]
Exception in thread "main" java.util.ConcurrentModificationException
	at java.util.ArrayList$SubList.checkForComodification(ArrayList.java:1231)
	at java.util.ArrayList$SubList.listIterator(ArrayList.java:1091)
	at java.util.AbstractList.listIterator(AbstractList.java:299)
	at java.util.ArrayList$SubList.iterator(ArrayList.java:1087)
	at java.util.AbstractCollection.toString(AbstractCollection.java:454)
	at java.lang.String.valueOf(String.java:2994)
	at java.io.PrintStream.println(PrintStream.java:821)
	at cur.A.main(A.java:31)
```

可以发现出现了并发异常



1、ArrayList 结构性修改 导致 Sublist 并发异常的原因：

如果结构性修改了 ArrayList，modCount 会 + 1，而 SubList 就跟 Itr 迭代器机制一样，在初始化的就使用一个**期望值**记录最开始 modCount，每次 遍历或者 调用方法 前都会进行判断是否发生过修改，如果修改就抛出并发异常

2、SubList 结构性修改 不会导致 ArrayList 并发异常的原因：
因为 ArrayList 的方法没有判断什么期望值，只有 ArrayList 内部的 Itr 和 SubList 才会存储一个期望值去判断



## 2、Arrays.asList() 和 Collections.singletonList() 



```java
public class Arrays {
    public static <T> List<T> asList(T... a) {
        return new ArrayList<>(a);
    }
    //内部类还 ArrayList 的名字。。。
    private static class ArrayList<E> extends AbstractList<E>
        implements RandomAccess, java.io.Serializable
    {
        private final E[] a;

        ArrayList(E[] array) {
            a = Objects.requireNonNull(array);
        }
        //xxxx
    }
}
```

```java
public class Collections {
    private static class SingletonList<E>
        extends AbstractList<E>
        implements RandomAccess, Serializable {

        //因为 SingletonList 只存储一个值，所以不需要数组接收 
        private final E element;

        SingletonList(E obj){
            element = obj;
        }
    }
}
```



同 ArrayList.subList() 类似的还有 Arrays.asList() 和 Collections.singletonList()

它们内部都是返回的 Arrays 自己的内部类，这些内部类都继承了 AbstractList，它是 List 的一个实现类，因此向上转型只能使用 List 来接收，又由于不是 ArrayList 类型的，所以无法强转为 ArrayList，如果强转则会抛出异常



它们不能执行 remove() 和 add() 方法

```java
List<String> asList = Arrays.asList("1", "2");
asList.remove(1);
```

输出结果：报 UnsupportedOperationException 错误

```java
Exception in thread "main" java.lang.UnsupportedOperationException
    at java.util.AbstractList.remove(AbstractList.java:161)
    at cur.A.main(A.java:17)
```



在 AbstractList 内部，add() 和 remove() 都是直接抛出异常，没有任何实现，而 asList() 和 singletonList() 也没有去重写这两个方法，这意味着这仅仅是用来 **只读而不写**

```java
public void add(int index, E element) {
    throw new UnsupportedOperationException();
}

public E remove(int index) {
    throw new UnsupportedOperationException();
}
```









