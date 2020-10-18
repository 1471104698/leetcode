# HashMap 原理



## 1、hashmap 和 hashtable 的区别



HM 是线程不安全的，所有方法都没有锁机制

HT 是线程安全的，所有方法都加了 synchronized 锁  



HM 的 key 允许一个空值，value 允许多个空值

HT 的 key 和 value 都不允许空值



HM 的默认容量是 16，后续扩容直接 * 2，即 cap << 1，这样就能保证是 2 的指数

HT 的默认容量是 11，后续扩容是 2 * old + 1



HM 的迭代方式是 iterator

HT 的迭代方式是 Enumeration + Iterator

- 前一种支持修改，但不属于 fail-safe，因为它什么都没做
- 后一种不支持修改，是 fail-fast

HT 在遍历的时候跟 HM 一样，不是线程安全的，只有在 put()、get() 的时候才是线程安全的



HM 插入时对象需要重新计算 hash 值

HT 直接将对象的 hashCode 作为 hash 值，直接获取 key.hashCode()



> ### 为什么 hashtable 不能存储空值？

因为 hashTable 是具有线程安全性质的，它是在多线程条件下使用的

我们可以发现，当我们使用 get() 获取 key 对应的 value 的时候，如果 value 不存在，那么会返回 null

但是如果是在多线程环境下，如果可以存储 null 值，当 hashtable 使用 get() 获取 key 对应的 value，如果 value = null

这时候它不知道是因为元素中不存在 key 的原因 还是 因为 key 对应的 value 就是 null 的原因，才返回的 null，所以这时候它会调用 contains() 方法，查找 key 是否存在，如果存在，那么表示的 key 对应的 value 为 null

由于 hashtable 中 get() put() contains() 方法都是上锁的，所以没有问题，问题的关键在于，这是多线程环境下的，当 contains() 完成知道了 null 的原因后，当准备下一步利用这个值做某些事情的时候，别的线程可能就改变这个值，使它不为 null 了，**为 null 和 不为 null 对于后续我们的逻辑判断的影响是很大的**

所以 hashtable 和 concurrentHashMap 都不允许存储 null 值

而 hashMap 是单线程的，所以不会出现此种情况，因此可以存储 null 值



## 2、初始容量求 2 的指数次幂算法

该算法能够得到比 cap 大的最接近 cap 的 2 的指数

```java
static final int tableSizeFor(int cap) {
    int n = cap - 1;
    n |= n >>> 1;
    n |= n >>> 2;
    n |= n >>> 4;
    n |= n >>> 8;
    n |= n >>> 16;
    return (n < 0) ? 1 : (n >= MAXIMUM_CAPACITY) ? MAXIMUM_CAPACITY : n + 1;
}
```



假设 n 的二进制数为 01xxxxxxxxxx

n |= n >>> 1； 对 n 右移 1 位，然后进行或操作，那么 n 变成  011xxxxxxxxx

n |= n >>> 2； 对 n 右移 2 位，然后进行或操作，那么 n 变成  01111xxxxxxx

n |= n >>> 4； 对 n 右移 4 位，然后进行或操作，那么 n 变成  011111111xxx

n |= n >>> 8； 对 n 右移 8 位，然后进行或操作，那么 n 变成  011111111111.。。。

。。。

可以看出，最终所有操作执行完毕，得到的是将最高位的 1 后面的数全部变成 1，比如 0100101，那么就变成 0111111

然后最后对它进行 n + 1，就变成 1000000，刚好是 2 的指数次

上面是 1 + 2 + 4 + 8 + 16 = 31，因为 int 型是 32 位，这样能够覆盖所有二进制数



最开始是 n = cap - 1 是为了防止最开始的 cap 就是 2 的指数的问题

比如 cap = 0100，最接近 cap 的 2 的指数就是它本身，那么 cap - 1 后 n = 0011，那么经过计算 n = 0100

而如果 cap 不减 1，那么 n = 0100，经过计算后会变成 n = 1000





## 3、链表转红黑树



> ### 链表转红黑树的条件

链表转红黑树，需要两个条件：

- 某个 hash 槽上的链表节点个数到达 8
- 桶数组的容量达到 64

上面两个条件都需要满足，尤其第二条，经常被人忽略

如果桶数组的容量小于 64 并且某个槽上的节点个数到达 8，那么只会扩容



> ### 为什么使用红黑树，不使用 二叉查找树 或者 AVL 树？

因为二叉查找树可能会退化成链表

如果要维持平衡又要变成 AVL 树，维护平衡的代价很大

而红黑树并保证相对的平衡，层数差不超过 1，性能较好



> ### 为什么不一开始就使用红黑树？

红黑树 查询 0(logn)，插入删除 O(logn)， 链表 查询 O(n)，插入删除 0(1)

很显然，红黑树的时间复杂度优于链表

不直接使用复杂度的原因是 最开始节点少的时候，链表的效率跟红黑树其实差不多，而一个树节点需要更多的空间，因为需要存储左右子树指针，所以为了节省空间，就不使用红黑树了



> ### 为什么节点个数为 8 时才转红黑树?

概率问题

根据泊松分布，链表节点数量为 8 概率极低，除非使用的 hash 算法散列效果差得一批，就跟 hashtable 的 hash 计算一样，直接使用 hashCode 计算，高位没有用到，这样的话节点插入同个槽中，就需要转红黑树了

作者认为 链表长度为 8 过长了，查询效率低，如果每次查询的数据都在尾部，那么每次都需要查找 O(n)，所以利用空间换时间，转换为红黑树



> ### 为什么在节点个数为 6 时才转链表？

根据概率论选的 8 转红黑树，那么一旦转了红黑树，表示遇到了这个非常低的概率，那么它就可能会再次发生

如果在 7 的时候转换为链表，那么就有可能再插入一个节点变成 8，再转红黑树

这样频繁的转换会导致性能下降

因此，7 作为一个中间过渡的量，作为链表时，在 8 的时候转红黑树，作为红黑树时，在 6 的时候转链表，这样红黑树不会一删除一个节点就立马转链表



## 4、迭代器的 fail-fast 机制 和 fail-safe 机制



具体看： https://juejin.im/post/6879291161274482695 

### 1、fail-fast 和 fail-safe 的区别

fail-fast 意为 快速失败，fail-safe 意为 安全失败



**这里说下使用迭代器的好处：**

使用 迭代器 iterator，它能够屏蔽底层的细节，无论底层是如何实现的，都是统一的 api `hasNext()、next()、remove() `不会改变



使用 fail-fast 机制，为了防止**遍历**过程中，其他线程随意 添加、删除数据 导致遍历过程中数据发生变化，

​	`java.uti `下的集合类都是使用 fail-fast 的，防止迭代时被修改，**线程不安全的集合类都是使用 fail-fast 机制**

使用 fail-safe 机制，为了可以在并发情况下修改数据，使用的是 CopyOnWrite 技术，加锁复制副本，后续数据修改的是副本数据

​	`java.util.concurrent` 集合下的类都是使用 fail-safe 的，可以在并发情况下修改，不会抛异常



### 2、fail-fast 实现原理

ArrayList 内部构造如下：

```java
class ArrayList{
    protected int modCount = 0;
    public boolean add(E e) {
        modCount++;
        //xxx
    }
    public E remove(int index) {
        modCount++;
        //xxx
    }
    private class Itr implements Iterator<E> {
        int expectedModCount = modCount;

        public boolean hasNext() {
            return cursor != size();
        }
        public E next() {
            //检查 modCount 和 expectedModCount 是否相同
            checkForComodification();
            //xxx
        }
        public void remove() {
            //检查 modCount 和 expectedModCount 是否相同
            checkForComodification();
            //xxx
        }
        //如果 modCount 跟期望值不一致，那么抛出异常
        final void checkForComodification() {
            if (modCount != expectedModCount)
                throw new ConcurrentModificationException();
        }
    }
}
```

可以看出，ArrayList 内部维护了一个 modCount，初始值为 0，每次只要 ArrayList 调用 add()、remove() 这种会结构性的修改 ArrayList 的方法，那么 modCount ++（结构性意味着改变了 ArrayList 的结构，比如添加元素使得长度变长）

而我们每次 foreach 遍历 ArrayList 的时候，实际上就是创建了它内部的一个迭代器 Itr，在创建的时候 Itr 会初始化一个 期望值expectedModCount = modCount，这个 expectedModCount 是属于 Itr 内部的，modCount 是属于 整个 ArrayList 的

Itr 的 next() 和 remove() 在前面都会调用 checkForComodification() ，当遍历过程中其他线程调用了 ArrayList 的 add() 或者 remove()，那么 modCount ++，这时候 Itr 调用 next() 或者 remove() 就会导致 modCount != expectedModCount 从而抛出异常



### 3、fail-safe 实现原理

比如 CopyOnWriteArrayList  这个类，使用 COW 技术支持并发修改（在 redis 的 RDB 持久化也有使用）

只需要在写线程（add()、remove()、set()）加锁，读线程无需加锁

```java
public boolean add(E e) {
    final ReentrantLock lock = this.lock;
    //加锁复制副本
    lock.lock();
    try {
        Object[] elements = getArray();
        int len = elements.length;
        //copy 出一个副本
        Object[] newElements = Arrays.copyOf(elements, len + 1);
        newElements[len] = e;
        //将数组的指针指向副本
        setArray(newElements);
        return true;
    } finally {
        lock.unlock();
    }
}

private E get(Object[] a, int index) {
    return (E) a[index];
}
```

缺点就是读线程读到的数据不是实时的，并且在内存中复制一份副本，当 List 大小为 200MB 时，那么意味着内存中会存在 400MB 的这个数据，可能会频繁造成 GC



## 5、为什么HashMap常用String对象作为key？ 

因为 HashMap 比较的比较逻辑是

```java
if(e.hash == hash && (e.key == key || e.key.equals(key))){
	//xxx
}
```

hash 跟 hashCode 相关，key 跟 equals() 相关

要保证使用不同的对象，但相同的对象内容能够对应上同一个 Node 节点

那么就意味着在对象内容相同的时候， hashCode() 和 equals() 得到的结果是一样的

而 String 内部自己重写了 hashCode() 和 equals()，都是根据内部的 char[] 来进行比较的，只要 char[] 内容相同，那么最终得到的 hashCode 是相同的，并且 equals() 为 true

```java
public int hashCode() {
    int h = hash;
    if (h == 0 && value.length > 0) {
        char val[] = value;

        for (int i = 0; i < value.length; i++) {
            h = 31 * h + val[i];
        }
        hash = h;
    }
    return h;
}
public boolean equals(Object anObject) {
    if (this == anObject) {
        return true;
    }
    if (anObject instanceof String) {
        String anotherString = (String)anObject;
        int n = value.length;
        if (n == anotherString.value.length) {
            char v1[] = value;
            char v2[] = anotherString.value;
            int i = 0;
            while (n-- != 0) {
                if (v1[i] != v2[i])
                    return false;
                i++;
            }
            return true;
        }
    }
    return false;
}
```



当然，如果不使用 String 而是使用 自定义对象的话，那么我们就必须定义好在 对象内容相同 的情况下，生成的 hashCode() 相同，而不能 Object 默认的返回内存地址，同时还要重写 equals()

即如果使用的是 User 这种的对象作为 key，那么就必须重写 hashCode() 和 equals()

平时为了不这么麻烦，都是直接使用 String 来作为 key

