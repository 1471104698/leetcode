# HashMap 原理



## 1、hashmap 和 hashtable 的区别



hashmap 是线程不安全的，所有方法都没有锁机制

hashtable 是线程安全的，所有方法都加了 synchronized 锁  



hashmap 的 key 允许一个空值，value 允许多个空值

hashtable 的 key 和 value 都不允许空值



hashmap 的默认容量是 16，后续扩容直接 * 2，即 cap << 1，这样就能保证是 2 的指数

hashtable 的默认容量是 11，后续扩容是 2 * old + 1



hashmap 的迭代方式是 iterator

hashtable 的迭代方式是 Enumeration + Iterator



hashmap 插入时对象需要重新计算 hash 值，调用的是 hash(key.hashCode())

hashtable 直接将对象的 hashCode 作为 hash 值，直接获取 key.hashCode()



hashmap 去除了 hashtable 的一个 contains(Object value) 方法

在 hashtable 中，containsValue() 方法就是直接调用的 contains() 方法，所以 hashmap 感觉这方法没什么用，就去除了

```java
public boolean containsValue(Object value) {
    return contains(value);
}
```



hashmap 中元素类叫做 Node

hashtable 中元素类叫做 Entry

它们的内容都是一样的，都实现了 Map 接口 内部的 Entry 接口，相当于是换了一个马甲

```java
static class Node<K,V> implements Map.Entry<K,V> {	
    //hash 用于比较，在 get/put 的时候进行比较，比如只有 两个元素的 hash 一样，并且 equals 为 true 才是同个对象
    final int hash;
    final K key;
    volatile V val;
    volatile Node<K,V> next;

    Node(int hash, K key, V val, Node<K,V> next) {
        this.hash = hash;
        this.key = key;
        this.val = val;
        this.next = next;
    }
    public final boolean equals(Object o) {
        Object k, v, u; Map.Entry<?,?> e;
        return ((o instanceof Map.Entry) &&
                (k = (e = (Map.Entry<?,?>)o).getKey()) != null &&
                (v = e.getValue()) != null &&
                (k == key || k.equals(key)) &&
                (v == (u = val) || v.equals(u)));
    }
}
```





> ### 为什么 hashtable 不能存储空值？

因为 hashTable 是具有线程安全性质的，它是在多线程条件下使用的

我们可以发现，当我们使用 get() 获取 key 对应的 value 的时候，如果 value 不存在，那么会返回 null

但是如果是在多线程环境下，如果可以存储 null 值，当 hashtable 使用 get() 获取 key 对应的 value，如果 value = null

这时候它不知道是因为元素中不存在 key 的原因 还是 因为 key 对应的 value 就是 null 的原因，才返回的 null，所以这时候它会调用 contains() 方法，查找 key 是否存在，如果存在，那么表示的 key 对应的 value 为 null

由于 hashtable 中 get() put() contains() 方法都是上锁的，所以没有问题，问题的关键在于，这是多线程环境下的，当 contains() 完成知道了 null 的原因后，当准备下一步利用这个值做某些事情的时候，别的线程可能就改变这个值，使它不为 null 了，**为 null 和 不为 null 对于后续我们的逻辑判断的影响是很大的**

所以 hashtable 和 concurrentHashMap 都不允许存储 null 值

而 hashMap 是单线程的，所以不会出现此种情况，因此可以存储 null 值



## 2、hashmap 扩容算法(找最接近 n 的 2 的指数)



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



## 3、hashmap 的扩容机制 resize()



> ### 什么时候会触发扩容？



当元素数量超过阈值的时候，那么就会触发扩容

阈值 = 容量 * 加载因子（加载因子一般是 0.75）



扩容是 容量变为原来的 2 倍，cap << 1，而阈值也同样变为原来的 2 倍



需要注意的是，**一般是在 put() 完元素的时候，才判断是否需要进行扩容，即添加完元素再进行扩容的**







> ### JDK 7 和 JDK 8 的扩容后进行的元素迁移机制



**JDK 7：**

所有的元素都是链表的保存形式，开辟一个新的 Entry[] 数组后，开始将旧数组上的 Entry 插入到新的数组上去

**注意点：**

- 使用的是头插法，即旧数组按序遍历的节点，每次都是插入到新数组对应链表的头部，那么就会造成原本的节点顺序发生翻转
- 多线程下，元素迁移的过程中会发生死循环





**JDK 8：**

由于 JDK 8 的容量始终是 2 的指数次幂，因此大大提高了迁移的效率，每个 Entry 无需重新 hash 来决定它在新数组中的存放位置，新的位置不是在 **原位置**，就是在 **原位置 + 原长度**

为什么？原因如下：

 ![img](https://pic2.zhimg.com/80/v2-da2df9ad67181daa328bb09515c1e1c8_720w.png) 

我们可以知道，JDK 8 扩容就是将 容量 左移一位，即 n << 1

当原容量为  1 0000 的时候，那么就是使用 n - 1 = 1111 来对 Entry  hash，那么只有尾四位进行计算，始终落在 0 - 15 的位置

而当扩容后，变成 10 0000 的时候，那么就是使用 n - 1 = 1 1111 来对 Entry hash，那么就多了高位一个 1，就是尾 五位 进行计算，这个 高位 1 就是原数组的长度，因此如果 hash 后该位置不为 0，只需要在原位置 加上原数组长度就是新的位置了

**注意点：**

- 正序插入，不会出现链表倒置
- 超过 8 个元素就将链表转换为红黑树，加快查询效率





> ### JDK 7 元素迁移 在 多线程 下死循环是如何产生的？



 JDK 7 元素迁移的代码：

```java
//newTable：新的数组，rehash：是否重新计算哈希
void transfer(Entry[] newTable, boolean rehash) {
    //获取新数组的长度
    int newCapacity = newTable.length;
    
    //遍历旧数组，需要注意的是，这里的 e 是一个链表头，而不是单单一个元素，通过 next 获取下一个节点
    for (Entry<K,V> e : table) {
        while(null != e) {
            //先记录下一个节点
            Entry<K,V> next = e.next;
            if (rehash) {
                e.hash = null == e.key ? 0 : hash(e.key);
            }
            int i = indexFor(e.hash, newCapacity);
            /*
            将节点 e 插入到 新的位置的头部，作为头节点，这就是头插法
            比如 
            e = 4
            newTable[i] = 3 -> 2 -> 1
            经过头插法
            那么就变成了 newTable[i] = 4 -> 3 -> 2 -> 1
            */
            e.next = newTable[i];
            newTable[i] = e;
            e = next;
        }
    }
```



**单线程下的元素迁移：**

 ![img](https://img-blog.csdnimg.cn/20190331174623768.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L3FxXzIyMTU4NzQz,size_16,color_FFFFFF,t_70) 



**多线程下的元素迁移：**

我们需要知道，多线程不是并行执行的，而是并发执行的，某个线程执行的过程中可能 CPU 会切换到其他线程

因此，某段代码 线程安不安全需要看 CPU 切换后执行别的线程是否对当前线程的这段代码的结果产生影响

很显然，这里就会

**过程：**

- 线程 1 和 线程 2 同时扩容，CPU 先执行线程 1

- 当线程 1 执行完 `Entry<K,V> next = e.next;` ，CPU 切换到 线程 2 ，注意，这里线程 1 的 e 指向的是 节点 3，而 next 指向 节点 7

- 当线程 2 执行完成后，由于是头插法，那么意味着最终的数组从 3 -> 7 就变成了 7 -> 3，注意，这里 7.next = 3，然后切换到线程 1

- 线程 1 将节点 3 插入到新的数组上，3.next = null，然后再处理 next = 7，一次新循环将 e 指向 7，由于 7.next = 3，所以 next = 3**（循环的初步建立）**

- 当使用头插法插入到新位置时，由于 3 和 7 是同个位置，因此 7 作为头节点时，7.next = 3，再然后处理 next = 3，一次新循环将 e 指向 3，再将 3 插入到新位置头节点，这时 3.next = 7，**死循环构成**

  

**这里需要注意的是：**Entry 节点对象至始至终都没有发生改变，改变的是新旧数组这个容器 和 Entry 对象存储的位置，并没有创建新的 Entry 对象去代替旧的 Entry 对象，它们存储在堆中，是所有线程共享的

因此线程 1 和 线程 2 操作的一直是同样的几个 Entry 对象，所以才造成了死循环



## 4、链表转红黑树的条件



链表转红黑树，需要两个条件：

- 某个 hash 槽上的链表节点个数到达 8
- 桶数组的容量大于 64

上面两个条件都需要满足，尤其第二条，经常被人忽略

如果桶数组的容量小于 64那么如果某个 hash 槽上的节点个数到达 8，那么只会通过扩容，然后重新计算 hash 来改变节点位置 这个方法解决



> ### 为什么使用红黑树，不使用二叉搜索树？

因为二叉搜索树可能会退化成链表，而如果要维持平衡又要变成 AVL 树，维护平衡的代价很大

而红黑树并不需要保证绝对的平衡，代价较小



> ### 为什么不一开始就使用红黑树？

红黑树 查询 0(logn)，插入删除 O(logn)， 链表 查询 O(n)，插入删除 0(1)

很显然，红黑树的时间复杂度优于链表

不直接使用复杂度的原因是 最开始节点少的时候，链表的效率跟红黑树其实差不多，而一个树节点需要更多的空间，因为需要存储左右子树，所以为了节省空间，就不使用红黑树了



> ### 为什么是 8 的时候才转红黑树?

根据泊松分布（所以这是有依据的），链表节点数量为 8 概率极低，除非使用的 hash 算法散列效果差得一批，就跟 hashtable 的 hash 计算一样，直接使用 hashCode 计算，高位没有用到，这样的话节点插入同个槽中，就需要转红黑树了

悲观的看，就是怕用户自己整的 hash 算法太差，使链表长度过长，查询效率低，如果每次查询的数据都在尾部，那么每次都需要查找 O(n)，所以利用空间换时间，转换为红黑树

因为根据 泊松分布，元素分布在一个槽中达到 8 个概率很低，如果均匀分布的话，超过阈值就会扩容了，所以极低概率会转换为红黑树，除非用户自己作死



**需要注意的是：节点到 6 的时候就又会转换成链表**



## 5、HasMap 在 JDK 8 相对 JDK 7 有什么改变？

- 扩容机制中的 元素迁移 和 元素新位置计算 发生了改变
- 链表会转红黑树
- 元素插入方式发生了改变，JDK 8 是尾插法，JDK 7 是头插法
- 节点 从 Entry 变成 Node，只是改个名字而已



## 6、hash()

注意，hashmap 的 节点 hash 值不是直接使用 hashCode 来代替，跟 hashtable 不一样，没那么偷工减料

而是通过 hashCode 的高 16 位 和 低 16 位 进行异或操作

```java
int hash = node.hashCode ^ (node.hashCode >>> 16);
```



> ### 为什么不直接使用 hashCode，而是要 高 16 位 异或 低 16 位？

首先，我们需要知道，hash 值在 hashMap 中是用来 **快速** 查找在桶数组中的位置的

在 get 和 put 中都需要用到，对于 get ，这个 hash 影响不大，但是对于 put ，这个 hash 就有影响了

因为如果都定位都同一个槽中，那么链表长度会很长，元素分布不均匀



比如：

对象 A 的 hashCode 为 1000010001110001000001111000000

对象 B 的 hashCode 为 0111011100111000101000010100000



而桶的容量为 16，hashMap 的查找算法是 hash & (n - 1)，那么 n - 1 = 15，转换为二进制就是  0000 1111，

 A.hasCode & 15 = 0

B.hashCode & 15 = 0

都是需要插入到 0 号槽位，这个 hash 算法也太 low 了吧，这样只能看  hashCode 低 4 位的 二进制数，高位的 28 位全部没有参与运算，hash 冲突多得一批

因此，就使用折半的方法，将 32 位 int 型的 hashCode 分为 高 16 位 和 低 16 位，进行异或操作，这样高位也能通过改变低位的值的方式来参与运算



> ### 为什么是使用 异或 操作？

因为如果使用 & 或者 | 的话，概率不均等

比如 &，那么 0 & 0、0 & 1、1 & 0 的结果都是 0，只有 1 & 1 的结果才是 1

比如 |， 那么 0 | 1、1 | 0、1 | 1 的结果都是 1，只有 0 | 0 的才是 0

概率不平均，只有 异或才能 五五开





## 7、迭代器的 fail-fast 机制 和 fail-safe 机制



> ### fail-fast 和 fail-safe 的区别

fail-fast 意为 快速失败，fail-safe 意为 安全失败

两种机制都是 迭代器使用



**这里说下使用迭代器的好处：**

使用 迭代器 iterator，它能够屏蔽底层的细节，无论底层是如何实现的，都是统一的 api `hasNext()、next()、remove() `不会改变



使用 fail-fast 机制，为了防止遍历过程中，其他线程随意 添加、删除数据 导致遍历过程中数据发生变化，

​	`java.uti `下的集合类都是使用 fail-fast 的，防止迭代时被修改，**线程不安全的集合类都是使用 fail-fast 机制**

使用 fail-safe 机制，为了可以在并发情况下修改数据

​	`java.util.concurrent` 集合下的类都是使用 fail-safe 的，可以在并发情况下修改，不会抛异常



### 

> ### fail-fast 实现原理

迭代器底层维护了一个 modCount 和 一个 expectedModCount，它们都是全局变量，初始状态 modCount == expectedModCount

对于 ArrayList 来说，它的 add()、remove() 方法都有一个 `ensureCapacityInternal()` 方法，它会将 modCount +1 ，即每执行一次 add() 或者 remove() , modCount 都会 +1，而在 ArrayList 内部实现的迭代器中，每一次的 next() 和 remove() 操作都会判断 modCount 是否等于 expectedModCount，如果不相等，那么会抛出 `ConcurrentModificationException`（并发修改异常）

因此，当一个线程 在使用迭代器遍历数据的时候，另一个线程修改了数据，那么读数据的线程会收到一个异常，中止读数据



ArrayList add() 方法

```java
public boolean add(E e) {
    ensureCapacityInternal(size + 1);  // Increments modCount!!
    elementData[size++] = e;
    return true;
}
```

ArrayList 内部迭代器 next() 和 remove() 方法

```java
public E next() {
    checkForComodification();
    int i = cursor;
    if (i >= size)
        throw new NoSuchElementException();
    Object[] elementData = ArrayList.this.elementData;
    if (i >= elementData.length)
        throw new ConcurrentModificationException();
    cursor = i + 1;
    return (E) elementData[lastRet = i];
}

public void remove() {
    if (lastRet < 0)
        throw new IllegalStateException();
    checkForComodification();

    try {
        ArrayList.this.remove(lastRet);
        cursor = lastRet;
        lastRet = -1;
        expectedModCount = modCount;
    } catch (IndexOutOfBoundsException ex) {
        throw new ConcurrentModificationException();
    }
}
```

抛异常方法

```java
 final void checkForComodification() {
     if (modCount != expectedModCount)
         throw new ConcurrentModificationException();
 }
```





> ### fail-safe 实现原理

迭代器迭代的数据是从原数据上进行拷贝的副本，即遍历的数据不是原来的数据，而是拷贝的数据

因此，当并发线程修改数据时，迭代器也检测不到，因此不会抛出 异常，达到可以并发修改的效果



缺点：

新修改的数据迭代器无法感知到，因此访问的都是旧数据



> ### 关于 hashtable 迭代器 注意点

虽然 hashtable 是线程安全类，但是它不是 `java.util.concurrent` 包下的

我们上面也说了，hashtable 使用的迭代器是 Enumerator，它实现了 Enumeration 和 Iterator 接口

Enumeration  接口代码如下：

```java
public interface Enumeration<E> {
    //判断是否存在元素，类似 iterator 的 hasNext()
    boolean hasMoreElements();
    //获取下一个元素，类似 iterator 的 next()
    E nextElement();
}
```



hashtable 有两种迭代方式：

一种是 Enumeration 的迭代，使用 keys() 创建的就是 Enumeration 迭代格式的迭代器，然后使用 上述两个方法迭代元素，**该迭代器是 fail-safe 机制的，即是拷贝副本然后遍历，不会引发 ConcurrentModificationException 异常**

一种是 Iterator 的迭代，hashtable 不能直接获取迭代器，通过 entrySet() 创建的就是 Iterator 迭代格式的迭代器，然后使用 foreach 迭代格式来使用 Iterator 迭代器的迭代，`foreach` 遍历方法底层使用的是 Iterator 迭代器，会使用到 hasNext() 和 next()，**由于该迭代器是 fail-fast 机制的，所以 next() 方法会进行 modCount 检验，所以会引发 ConcurrentModificationException 异常**



创建 Enumeration 迭代格式的迭代器代码

```java
public synchronized Enumeration<K> keys() {
    return this.<K>getEnumeration(KEYS);
}
private <T> Enumeration<T> getEnumeration(int type) {
    if (count == 0) {
        return Collections.emptyEnumeration();
    } else {
        return new Enumerator<>(type, false);
    }
}
```



创建 Iterator 迭代格式的迭代器代码

```java
public EntrySet entrySet() {
    return new EntrySet();
}

private class EntrySet extends AbstractSet<Map.Entry<K,V>> {
    public Iterator<Map.Entry<K,V>> iterator() {
        return getIterator(ENTRIES);
    }
}

private <T> Iterator<T> getIterator(int type) {
    if (count == 0) {
        return Collections.emptyIterator();
    } else {
        return new Enumerator<>(type, true);
    }
}
```



创建的都是 Enumerator 类型的迭代器，上面我们说了，hashtalbe 内部类 Enumerator 实现了 Enumeration 和 Iterator 接口，因此这里使用到了多态的性质

通过 `boolean iterator` 这个属性，来判断 iterator 类型，然后采取不用的迭代方法

```java
boolean iterator;

protected int expectedModCount = modCount;

Enumerator(int type, boolean iterator) {
    this.type = type;
    //指定迭代器类型
    this.iterator = iterator;
}
```







> ### hashtable、hashmap、concurrentHashMap 迭代机制比较



```java
//抛出 ConcurrentModificationException 异常
Hashtable<Integer, Integer> hashtable = new Hashtable<>();
hashtable.put(1, 1);
hashtable.put(2, 1);
hashtable.put(3, 1);
//Iteratro 迭代格式
for(Map.Entry<Integer, Integer> entry : hashtable.entrySet()){
    hashtable.remove(entry.getKey());
}

//无异常
Hashtable<Integer, Integer> hashtable = new Hashtable<>();
hashtable.put(1, 1);
hashtable.put(2, 1);
hashtable.put(3, 1);
//Enumeration 迭代格式
Enumeration<Integer> keys = hashtable.keys();
while(keys.hasMoreElements()){
    keys.nextElement();
    hashtable.remove(1);
}

//抛出 ConcurrentModificationException 异常
HashMap<Integer, Integer> map = new HashMap<>();
map.put(1, 1);
map.put(2, 1);
map.put(3, 1);
for(Map.Entry<Integer, Integer> entry : map.entrySet()){
    map.remove(entry.getKey());
}

//无异常
ConcurrentHashMap<Integer, Integer> map = new ConcurrentHashMap<>();
map.put(1, 1);
map.put(2, 1);
map.put(3, 1);
for(Map.Entry<Integer, Integer> entry : map.entrySet()){
    map.remove(entry.getKey());
}
```



