# HashMap 原理



## 1、HashMap 和 HashTable 的区别

HashMap 和 HashTable 有 5 个区别：

- 线程安全问题
- key 和 value 空值问题
- 初始容量不同
- 迭代方式不同
- 对 hash 的计算不同



HashMap 线程不安全

HashTable 线程安全，方法级别上加 synchronized 锁  



HashMap 的 key 允许一个 Null，value 都允许 Null

```java
当存储 (key, value) = (null, 1) 时，key 的 hash == 0，因此它实际上是可以构成一个 Node 的，
    如 Node(key, value, hash; next) = Node(null, 1, 0, null)
key 只允许存在一个 null 的原因是下次 put(null, 2) 的时候，会发现已经存在该 Node 了，因此直接替换旧值
```

HashTable 的 key 和 value 都不允许 Null

```java
HashTable、ConcurrentHashMap 之类的都不允许 key 和 value 为 null
在它们的方法内部会进行判空，抛异常
```



HashMap 的默认容量是 16，后续扩容直接 * 2，即 cap << 1，这样就能保证是 2 的指数

HashTable 的默认容量是 11，后续扩容是 2 * old + 1

```java
int newCapacity = (oldCapacity << 1) + 1;
```



HashMap 的迭代方式是 Iterator

HashTable 的迭代方式是 Enumeration + Iterator

- Enumeration 支持修改，**属于 fail-safe**
- Iterator 不支持修改，**属于 fail-fast**



HashMap 插入时对象需要根据 hashCode 重新计算 hash 值（前 16bit 异或 后 16bit）

HashTable 直接将对象的 hashCode 作为 hash 值



> #### 为什么 HashTable / ConcurrentHashMap 的 key 和 value 不能存储空值？

 [这道面试题我真不知道面试官想要的回答是什么_个人文章 - SegmentFault 思否](https://segmentfault.com/a/1190000021105716) 



这个问题翻译过来就是，在 put() 的时候，作者为什么要设置：

```java
if(key == null || value == null){
	throw new NullPointerException();
}	
```



并发包的作者是谁？Doug Lea

 ![file](https://image-static.segmentfault.com/100/042/1000423480-5ddb63e23913c_articlex) 

所以要得到这个问题的答案，还是作者的回答才具有权威

早在 2006 年 5 月 12 日 早上 06 点 01 分 45 秒，就有人发”求助“邮件：

```java
大致意思是：为什么 ConcurrentHashMap 不支持 key 和 value 为 null
```



Doug Lea 大佬的回答是：

```java
当 value == null 时存在二义性，
因为当 map.get(key) 得到 null 时，在 map 内部它知道是否存在对应的 Node 节点，
但是在用户层面，得到的就是 null，你不知道到底是 map 内部不存在这个 key 的映射，还是说这个 key 映射的 value 就是 null，这就存在了 二义性

对于 HashMap，它默认不支持并发操作，因此在非并发的情况下，它可以使用 containsKey() 来判断是否存在 Node 节点，因此来最终确定这个状态
对于 ConcurrentHashMap，它主要用于并发操作，假设在第一个 get(key) 的时候得到 null，此时其中是不存在 Node 的，而在调用 containsKey() 来判断是否存在 Node 节点前，其他线程调用了 put(key, null)，那么对于当前线程来说，它调用的 containsKey() 返回的是 true，这跟它调用 get() 时的状态不一致
    
    因此，ConcurrentHashMap 的 value 不支持 null
```

实际上个人认为，虽然 ConcurrentHashMap 的 value == null 会存在二义性，但是最终实际上状态是确定了的，即其他线程在调用 put(key, null) 后，key 的映射的的确确是存在的，这个二义性并不会造成什么坏的后果，因此数据的确是存在的



而对于 key 为什么不能为 null，大佬的回答是自己不喜欢 null，并且它认为 HashMap 这种 map 集合支持 null 本身就是一种错误的设计



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



> #### 链表转红黑树的条件

链表转红黑树，需要两个条件：

- 某个 hash 槽上的链表节点个数到达 8
- 桶数组的容量达到 64

上面两个条件都需要满足，尤其第二条，经常被人忽略

如果桶数组的容量小于 64 并且某个槽上的节点个数到达 8，那么只会扩容



> #### 为什么使用红黑树，不使用 二叉查找树 或者 AVL 树？

因为二叉查找树可能会退化成链表

如果要维持平衡又要变成 AVL 树，维护平衡的代价很大

而红黑树并保证相对的平衡，层数差不超过 1，性能较好



> #### 为什么不一开始就使用红黑树？

红黑树 查询 0(logn)，插入删除 O(logn)， 链表 查询 O(n)，插入删除 0(1)

很显然，红黑树的时间复杂度优于链表

不直接使用复杂度的原因是 最开始节点少的时候，链表的效率跟红黑树其实差不多，而一个树节点需要更多的空间，因为需要存储左右子树指针，所以为了节省空间，就不使用红黑树了



> #### 为什么节点个数为 8 时才转红黑树?

概率问题

根据泊松分布，链表节点数量为 8 概率极低，除非使用的 hash 算法散列效果差得一批，就跟 HashTable 的 hash 计算一样，直接使用 hashCode 计算，高位没有用到，这样的话节点插入同个槽中，就需要转红黑树了

作者认为 链表长度为 8 过长了，查询效率低，如果每次查询的数据都在尾部，那么每次都需要查找 O(n)，所以利用空间换时间，转换为红黑树



> #### 为什么在节点个数为 6 时才转链表？

根据概率论选的 8 转红黑树，那么一旦转了红黑树，表示遇到了这个非常低的概率，那么它就可能会再次发生

如果在 7 的时候转换为链表，那么就有可能再插入一个节点变成 8，再转红黑树

这样频繁的转换会导致性能下降

因此，7 作为一个中间过渡的量，作为链表时，在 8 的时候转红黑树，作为红黑树时，在 6 的时候转链表，这样红黑树不会一删除一个节点就立马转链表



## 4、迭代器的 fail-fast 机制 和 fail-safe 机制



 [一文彻底弄懂 fail-fast、fail-safe 机制（带你撸源码） (juejin.cn)](https://juejin.cn/post/6879291161274482695) 



**这里说下使用迭代器的好处：**

使用 迭代器 iterator，它能够屏蔽底层的细节，无论底层是如何实现的，都是统一的 api `hasNext()、next()、remove() `不会改变·



- fail-fast 机制：为了防止**遍历**过程中，其他线程 结构性改变集合，导致迭代器遍历过程出现问题

  ​	`java.uti `下的集合类都是使用 fail-fast 的

- fail-safe 机制：为了可以在并发情况下修改数据，即在迭代器中不会抛出 并发异常 ConcurrentModificationException 

  ​	`java.util.concurrent` 集合下的类都是使用 fail-safe 的

```java
网上的说法：
1、fail-fast（快速失败） 是迭代器有 modCount 检测
2、fail-safe（安全失败） 是复制一份副本（类似 CopyOnWriteArrayList 这种使用 COW），写操作都是在副本上进行，这也就不会有 modCount 变量检测

对于 fail-safe 的说法我不敢苟同，什么叫做复制一份副本 来避免 modCount 的检测？
	在 CopyOnWriteArrayList 和 ConcurrentHashMap 的所有迭代器中根本就不存在 modCount 和 expectedModCount
	这样何来 modCount 变量检测？
实际上真正的 fail-safe 机制应该是在各种程度上允许并发修改，并且迭代器不会抛出并发异常
    比如 CopyOnWriteArrayList 是对写操作拷贝副本，迭代器迭代的还是原来的数据
    比如 ConcurrentHashMap 没有进行任何操作，它是弱一致性的迭代器，允许并发修改，可能会遍历过程中会读取不到一些数据，但是这些都是在它自己能够接收的范围内的。（它也可以 copy 一份副本，但是它没有这么做，因为它接收这个弱一致性）
```



### 1、fail-fast（快速失败，不允许并发修改）



ArrayList 内部构造如下：

```java
class ArrayList{
    protected int modCount = 0;
    public boolean add(E e) {
        modCount++;
        //添加操作
    }
    public E remove(int index) {
        modCount++;
        //移除操作
    }
    
    //ArrayList 内部维护的 Iterator 迭代器
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

可以看出，ArrayList 内部维护了一个 modCount，初始值为 0，每次只要 ArrayList 调用 add()、remove() 这种会结构性的修改 ArrayList 的方法，那么 modCount ++（结构性意味着改变了 ArrayList 的有效数组的长度）

我们每次 foreach 遍历 ArrayList 的时候，实际上就是创建了它内部的一个迭代器 Itr，在创建的时候 Itr 会初始化一个 期望值expectedModCount，初始值为 modCount，

```java
expectedModCount 是 Itr 内部的，
modCount 是整个 ArrayList 的
```

Itr 的 next() 和 remove() 在前面都会调用 checkForComodification() ，当遍历过程中其他线程调用了 ArrayList 的 add() 或者 remove()，那么 modCount ++，这时候 Itr 调用 next() 或者 remove() 发现 modCount != expectedModCount,

从而抛出 ConcurrentModificationException 并发异常

因此，如果单线程在使用迭代器遍历的过程中，如果要移除元素，那么不要调用 ArrayList 的 remove()，而是使用迭代器的 remove()，这样才不会触发 modCount++



### 2、fail-safe（安全失败，允许并发修改）



比如 CopyOnWriteArrayList ，使用 COW 技术支持并发修改（在 redis 的 RDB 持久化也有使用）

只需要在写操作（add()、remove()、set()）加锁，读操作无需加锁

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



再比如 ConcurrentHashMap，它有三种迭代器：KeyIterator、ValueIterator、EntryIterator，它们都是继承 BaseIterator，操作都是差不多的，不会加锁，不会抛出并发异常。

它们是弱一致性迭代器，因为遍历的过程中其他线程可以修改数据，导致有的数据遍历不到之类的，这也是 fail-safe 的一种

