# JDK 7 和 JDK 8 中 ConcurrentHashMap 的变化

## 1、内部的数据结构

> ### JDK 7

JDK 7 使用 分段 Segment + ReentrantLock + 链表 实现

ConcurrentHashMap 将原本维护的一个 大 table 分割为多个 小 table

比如在 HashMap 中 table 大小为 16，那么在 ConcurrentHashMap 将它分割为 4 个大小为 4 的 table

然后每个 table 由一个 Segment 进行管理，加锁也只需要对对应的 Segment 进行加锁，这样能够支持 Segment 数的并发量



不过这样就增加了 hash 次数，原本只需要一个定位到 槽位的，现在需要两次 hash：

- 第一次  hash 到对应的 Segment
- 第二次 hash 到 中 Segment 的 table 对应的槽位



Segment 数据结构

其中 table 使用 volatile 修饰，保证扩容可见性

```java
static final class Segment<K,V> extends ReentrantLock {
       //使用 volatile 修饰，保证扩容对其他线程可见
       volatile HashEntry<K,V>[] table;
       //元素个数
       int count;
       //
       int modCount;
       //阈值和加载因子，用于每个 Segment 上面的 HashEntry 扩容
       int threshold;
       final float loadFactor;
}
```



HashEntry 数据结构（注意：在 JDK 6 中的 next 才是 final 修饰的，JDK 7 和 JDK 8 都修改为 volatile 了）

value 和 next 使用 voaltile 修饰，保证可见性

```java
static final class HashEntry<K,V> {
    final int hash;
    final K key;
    volatile V value;
    volatile HashEntry<K,V> next;

    HashEntry(int hash, K key, V value, HashEntry<K,V> next) {
        this.hash = hash;
        this.key = key;
        this.value = value;
        this.next = next;
    }
```



> ### JDK 8

JDK 8 回归了 一个 table 的时代，并且相比 JDK 7 缩小了锁的粒度

使用 sync 锁 + CAS + 链表+ 红黑树 实现

table 跟 JDK 7 一样使用 volatile，保证扩容可见性

value 和 next 使用 volatile 修饰，保证修改可见性

```java
volatile Node<K,V>[] table;

static class Node<K,V> implements Map.Entry<K,V> {
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
```



> ### 改变 

JDK 7 使用 分段 Segment + ReentrantLock + 链表 实现，最高并发量为 Segment 数，定位到某个 key 的位置需要两次 hash

JDK 8 使用 sync + CAS + 链表 + 红黑树 实现，锁的粒度减小，hash 定位只需要一次



可见性保证：

```
比如线程 A 在读取某个槽位上的链表时，而 线程 B 在修改该槽位上的链表，如果将链表指向改变了，由于是 volatile 变量，所以线程 A 能够感知到这个变化，这样的话就会去主存获取新值，防止读取的是旧数据，使得后续在已经无效的旧数据上进行逻辑处理
```



## 2、get()

> ### JDK 7

无需加锁，hash 定位到对应 Segment 上 table 中的槽位

遍历链表进行读取即可，value 已经有 volatile 保证了可见性，因此值被修改也是已知的

```java
V get(Object key, int hash) {
    if (count != 0) { // read-volatile
        HashEntry<K,V> e = getFirst(hash);
        while (e != null) {
            if (e.hash == hash && key.equals(e.key)) {
                V v = e.value;
                if (v != null)
                    return v;
                return readValueUnderLock(e); // recheck
            }
            e = e.next;
        }
    }
    return null;
}

```



> ### JDK 8

同样无需加锁，先定位到对应的槽位

判断头节点是否是目标节点，如果是则直接返回

判断如果是 红黑树那么使用红黑树的遍历方法，如果是链表那么使用链表的遍历方法

```java
public V get(Object key) {
    Node<K,V>[] tab; Node<K,V> e, p; int n, eh; K ek;
    int h = spread(key.hashCode());   //获得Hash值
    if ((tab = table) != null && (n = tab.length) > 0 &&
        (e = tabAt(tab, (n - 1) & h)) != null) {
        if ((eh = e.hash) == h) {  // 比较 此头结点e是否是我们需要的元素
            if ((ek = e.key) == key || (ek != null && key.equals(ek)))
                return e.val;   // 如果是，就返回
        }
        else if (eh < 0)   // 如果小于零，说明此节点是红黑树 
            return (p = e.find(h, key)) != null ? p.val : null;
        while ((e = e.next) != null) {
            // 开始循环 查找
            if (e.hash == h &&
                ((ek = e.key) == key || (ek != null && key.equals(ek))))
                return e.val;
        }
    }
    return null;
}

```



> ### 改变

没有多大的变化，JDK 8 相比 JDK 7 就是增加了头节点的判断已经红黑树的判断

在读数据方面都是有 volatile 保证可见性而无需加锁，相比 HashTable 提高了并发效率



## 3、put()

> ### JDK 7

hash 定位到某个 Segment，然后再调用 Segment 的 put()

调用 tryLock() 尝试获取锁，如果获取失败，那么自旋 while(!tryLock()) 获取锁，**当达到最大自旋次数时，会进入到同步队列中**

定位到对应的槽位上

遍历链表上的节点，如果某个节点的 key 和 hash 相同，那么直接进行旧值的替换

如果链表中不存在 插入的 key，那么创建一个新的节点，使用头插法插入到 first 的前面

然后**判断是否需要扩容**，如果需要则进行调用 rehash() 扩容，不需要则将 tab[i] 指向 node，将它作为链表的新头节点

插入/更新节点完成后，会进行 c++ 和 modCount++，一个是记录当前 Segment 的节点个数，一个是记录当前 Segment 的修改次数

```java
public V put(K key, V value) {
    Segment<K,V> s;
    if (value == null)
        throw new NullPointerException();
    int hash = hash(key);

    //调用 segmentFor 进行 hash 定位到对应的 Segment，并调用其 put()
    int j = (hash >>> segmentShift) & segmentMask;
    s = ensureSegment(j);
    return s.put(key, hash, value, false);
}

final V put(K key, int hash, V value, boolean onlyIfAbsent) {
    //尝试 CAS 加锁，如果失败则自旋 while(!tryLock()) 直到获取锁成功
    HashEntry<K,V> node = tryLock() ? null : scanAndLockForPut(key, hash, value);
    V oldValue;
    try {
        HashEntry<K,V>[] tab = table;
        int index = (tab.length - 1) & hash;
        //链表头节点 first
        HashEntry<K,V> first = entryAt(tab, index);
        //遍历链表
        for (HashEntry<K,V> e = first;;) {
            if (e != null) {
                K k;
                //遇到相同的 Node
                if ((k = e.key) == key ||
                    (e.hash == hash && key.equals(k))) {
                    oldValue = e.value;
                    if (!onlyIfAbsent) {
                        //旧值替换
                        e.value = value;
                        ++modCount;
                    }
                    break;
                }
                e = e.next;
            }
            else {
                if (node != null)
                    node.setNext(first);
                else
                    //创建一个新节点，插入到链表的头部作为头节点，即头插法
                    node = new HashEntry<K,V>(hash, key, value, first);
                int c = count + 1;
                //扩容判断：判断是否超出阈值，如果超出那么在 rehash() 内进行扩容
                if (c > threshold && tab.length < MAXIMUM_CAPACITY)
                    rehash(node);
                else
                    //将 table[i] 指向 node 代替 first 作为头节点
                    setEntryAt(tab, index, node);
                ++modCount;
                count = c;
                oldValue = null;
                break;
            }
        }
    } finally {
        unlock();
    }
    return oldValue;
}
```



> ### JDK 8

一次 hash 定位到对应的槽位

判断槽位上是否为空，如果为空则使用 CAS 将当前节点作为头节点，插入失败则进入到下一轮循环，插入成功则直接返回

对该槽位上头节点 f 加 sync 锁，如果已经被其他线程加锁了，那么会进行阻塞

当加锁成功后，会再次进行 CAS 判断 tab[i] 的头节点是否就是 现在持有的 f，即判断是否被其他线程修改了头节点，如果不一样，表示 tab[i] 的头节点不是 f，那么释放锁，等待下一轮循环**（重点理解，后续讲解）**

f 和 tab[i] 一致，那么判断节点类型，如果是链表，那么初始化一个 int 类型变量 bitCount 来记录链表的长度，遍历链表，判断是否存在 hash 和 key 相同的节点，如果存在，那么直接更新旧值

遍历完成，不存在节点，那么使用尾插法将节点插到链表末尾，插入完成后判断 bitCount 值，如果 bitCount >= 8，那么调用 treeifyBin() 将链表转换为红黑树，需要注意的是，在 treeifyBin() 中会判断 桶的长度是否大于等于 64，如果没有，那么直进行扩容，而不转换为红黑树

在方法结尾进行 addCount(1)，即将总的节点个数值 +1，然后在里面进行扩容判断

```java
final V putVal(K key, V value, boolean onlyIfAbsent) {
    //1、判断key是否为空
    if (key == null || value == null) throw new NullPointerException();
    //2、计算哈希值
    int hash = spread(key.hashCode());
    int binCount = 0;
    //3、得到table的数组
    for (Node<K,V>[] tab = table;;) {

        Node<K,V> f; int n, i, fh;
        //4.如果table数组为空，则初始化table
        if (tab == null || (n = tab.length) == 0)
            tab = initTable();
        /*
        5.如果对应key的哈希值上对应table数组下标的位置没有node
        则通过 CAS 将节点插入 table[i] 中作为头节点
        如果插入成功，break
        插入失败，进入下一轮循环
        */
        else if ((f = tabAt(tab, i = (n - 1) & hash)) == null) {
            if (casTabAt(tab, i, null, new Node<K,V>(hash, key, value, null)))
                break;                   
        }
        //6、如果table正在扩容，则得到扩容后的table，然后再重新开始一个循环
        else if ((fh = f.hash) == MOVED)
            tab = helpTransfer(tab, f);
        else {
            //7.到这里说明找到了key hash后对应的table，并且table上有其他node的存在
            V oldVal = null;
            /*
            8、将 槽位头节点 f 加上同步锁，防止并发出现的问题
            如果线程 put、remove 操作的也是这个 table[i] 的时候会进行阻塞
            */
            synchronized (f) {
                /*
                9.使用 CAS 确认槽位上的节点是否是我们要的 node
                	如果不是的话则这个 table[i] 的头节点被修改，锁住的 f 是释放了的，因此直接释放锁进入下一个循环
                */
                if (tabAt(tab, i) == f) {
                    //10.如果目标table的第一个node的哈希值大于等于0，则是链式结构，走链表查找，反之走红黑树查找
                    if (fh >= 0) {
                        //11.标志bincount为1，因为在该table上至少有一个node节点
                        binCount = 1;
                        //12.循环链表, ++binCount 用来统计链表节点个数
                        for (Node<K,V> e = f;; ++binCount) {
                            K ek;
                         //13.如果遍历元素的哈希值与需要插入目标key的哈希值相同，并且值也相同，则插入的是重复key的元素
                            if (e.hash == hash && ((ek = e.key) == key ||
                                                   (ek != null && key.equals(ek)))) {
                                //14.更新值
                                e.val = value;
                                //15.break循环
                                break;
                            }
                            //16.链表中不存在 key 节点，那么直接在尾部插入
                            Node<K,V> pred = e;
                            if ((e = e.next) == null) {
                                //在尾部添加一个新节点，break循环
                                pred.next = new Node<K,V>(hash, key, value, null);
                                break;
                            }
                        }
                    }
                    //17.该节点属于红黑树的子节点，进行树操作
                    else if (f instanceof TreeBin) {
                        //红黑树插入~~~~
                    }
                }
            }
            //18.如果node节点不为0
            if (binCount != 0) {
                //19.如果node大于或者等于8，则转为红黑树
                if (binCount >= TREEIFY_THRESHOLD)
                    treeifyBin(tab, i);
                break;
            }
        }
    }
    //20.元素个数 +1
    addCount(1);
    return null;
}
```



**为什么加了 sync 锁后还需要进行 CAS 判断头节点？**

多线程环境下，存在下面这种情况：

- 线程 A 对 tab[i] 的头节点 f 进行删除操作，线程 B 对 tab[i] 进行插入操作
- 线程 A 先获取了 tab[i] 的锁，而线程 B 会阻塞在 synchronized (f) 处
- 此时等待线程 A 操作完成后，即删除了 f，这时候 tab[i] 实际上就变成了 f.next，然后 线程 A 释放锁
- 线程 B 获取锁，这时候它所持有的 f 是线程 A 已经删除了的节点，即 f 跟 tab[i] 是已经分离了的，如果还在 f 上进行插入，那么插入的节点也跟 tab[i] 是分离的，这意味着插入的节点是不可见的了，相当于消失了
- 因此线程 B 获取锁后需要再次 CAS 进行判断

模拟过程如下：

```java
tab[i] = 1 -> 2 -> 3 -> 4
		 ↑
		 f
线程 B 阻塞在  synchronized (f) 处

线程 A 删除了 f 节点，释放锁，这时候变成了
tab[i] = 2 -> 3 -> 4
f = 1

而线程 B 获取锁，由于引用仍然是 f，因此指向的是
f = 1
这时候如果进行插入操作，那么就变成
tab[i] = 2 -> 3 -> 4
f = 1 -> 5
对于线程来说，f 是不可见的，后续会被 GC 回收，而插入的节点也会被回收了，GG
```

这个对于 remove() 来说也是会造成类似的问题



> ### 改变

JDK 7 是直接对 Segment  调用 tryLock() 加锁，锁的粒度较大，最大并发量为 Segment 数，**对于元素个数的统计直接使用一个普通的 int 变量 c 进行 c++，因为已经加了锁，所以保证了原子性和可见性**

JDK 8 相比 JDK 7 锁的粒度减小了，缩小到某个槽位，使用 sync 锁 + CAS 解决并发插入问题

并且增加了红黑树 和 链表转红黑树的判断，**对于元素个数的统计是调用 addCount() 方法，不是简单的使用一个变量来统计**



**JDK 7 和 JDK 8 都是在插入后进行扩容判断的，这点跟 JDK 7 和 JDK 8 的 HashMap 不一样**

JDK 7 的扩容方法为 rehash()，JDK 8 的扩容方法为 transfer()，它是在 addCount(1) 内被调用的



## 4、remove()

> ### JDK 7

JDK 7 的方法很简答，定位到对应的 Segment，然后再获取锁，再定位到对应的槽位上

遍历链表，判断 key 和 hash，如果一致则将 pre.next = e.next，跳过删除节点

删除完节点后进行 c-- 和 modCount++

```java
final V remove(Object key, int hash, Object value) {
    //CAS 获取锁
    if (!tryLock())
        scanAndLock(key, hash);
    V oldValue = null;
    try {
        HashEntry<K,V>[] tab = table;
        //槽位
        int index = (tab.length - 1) & hash;
        //头节点
        HashEntry<K,V> e = entryAt(tab, index);
        //作为前驱节点指针
        HashEntry<K,V> pred = null;
        while (e != null) {
            K k;
            HashEntry<K,V> next = e.next;
            if ((k = e.key) == key ||
                (e.hash == hash && key.equals(k))) {
                V v = e.value;
                //找到删除节点
                if (value == null || value == v || value.equals(v)) {
                    //如果前驱节点为空，表示删除的是头节点，那么将 next 设置为头节点
                    if (pred == null)
                        setEntryAt(tab, index, next);
                    else
                        //pre.next = e.next 跳过删除节点
                        pred.setNext(next);
                    ++modCount;
                    --count;
                    oldValue = v;
                }
                break;
            }
            pred = e;
            e = next;
        }
    } finally {
        unlock();
    }
    return oldValue;
}
```



> ### JDK 8

JDK 8 的 remove() 和 put() 方法 差不多，基本没什么变化

定位到槽位，判断头节点 f 是否为空，如果为空直接返回

加锁 f，等到争夺到锁的时候，进行 CAS 判断 tab[i] 和 f 是否相同，如果不相同，那么释放锁进入下一轮循环（跟 put() 原因一样）

判断是否是红黑树，如果是则调用红黑树的删除方法

遍历链表，判断 key 和 value，如果存在，那么直接 pre.next = e.next，跳过删除节点

```java
final V replaceNode(Object key, V value, Object cv) {
    int hash = spread(key.hashCode());
    for (Node<K,V>[] tab = table;;) {
        Node<K,V> f; int n, i, fh;
        if (tab == null || (n = tab.length) == 0 ||
            //获取头节点
            (f = tabAt(tab, i = (n - 1) & hash)) == null)
            break;
        else if ((fh = f.hash) == MOVED)
            tab = helpTransfer(tab, f);
        else {
            V oldVal = null;
            boolean validated = false;
            //加锁
            synchronized (f) {
                //CAS 判断头节点是否发生改变
                if (tabAt(tab, i) == f) {
                    if (fh >= 0) {
                        validated = true;
                        //遍历链表
                        for (Node<K,V> e = f, pred = null;;) {
                            K ek;
                            //找到了删除节点
                            if (e.hash == hash &&
                                ((ek = e.key) == key ||
                                 (ek != null && key.equals(ek)))) {
                                V ev = e.val;
                                if (cv == null || cv == ev ||
                                    (ev != null && cv.equals(ev))) {
                                    oldVal = ev;
                                    if (value != null)
                                        //将值置空
                                        e.val = value;
                                    else if (pred != null)
                                        //跳过删除节点
                                        pred.next = e.next;
                                    else
                                        //没有前驱节点，即是头节点，那么将 next 节点设置为头节点
                                        setTabAt(tab, i, e.next);
                                }
                                break;
                            }
                            pred = e;
                            if ((e = e.next) == null)
                                break;
                        }
                    }
                    else if (f instanceof TreeBin) {
                        //xxxx
                    }
                }
            }
            if (validated) {
                if (oldVal != null) {
                    if (value == null)
                        //节点个数 -1
                        addCount(-1L, -1);
                    return oldVal;
                }
                break;
            }
        }
    }
    return null;
}
```



> ### 改变

JDK 7 的 remove() 平平无奇，就是直接定位，获取锁，然后遍历链表找到对应节点

找到后，如果是头节点，那么将 next 节点设置为头节点，如果不是则跳过删除节点，然后 c--



JDK 8 的 remove() 实现跟 put() 基本一致，先判断头节点 f 是否为空，不为空则加锁，再 CAS 判断 f 是否发生改变，后面的操作跟 JDK 7 遍历链表的基本一样，在方法最后调用 addCount(-1) 将节点个数 -1



## 5、size()

> ### JDK 7

JDK 7 的节点都分散在各个 Segment 中，因此所有节点的个数需要统计所有的 Segment 中的 普通 int 变量 c

c 的统计是在 put() 和 remove() 中直接 c++ 和 c-- 的，因为加了锁，在锁内操作，所以保证了原子性和可见性

具体的 size() 操作是最多尝试 3 次 不加锁的 Segment 元素个数统计，如果相邻两次的统计中任何一个 Segment 都没有存在线程修改，那么就将统计结果作为最终结果



如何知道 Segment 是否存在修改？

这里是通过 modCount 来判断的，modCount 记录的是每个 Segment 调用 put() 和 remove() 的总次数，即修改次数

通过相邻两次统计的 modCount 之和进行比对，如果相同，那么表示 Segment 没有经过任何修改，那么统计得到的 sum 就是正确的所有节点个数；如果不同，表示这期间有 Segment 发生了修改，那么无效；



最多遍历 3 次，即只有两次的比对机会：1 和 2 比对， 2 和 3 比对

如果都对不上，那么就会对所有的 Segment 加锁，再进行一次的统计，这次的结果就是最终结果了

```java
public int size() {
    final Segment<K,V>[] segments = this.segments;
    int size;
    boolean overflow; // true if size overflows 32 bits
    long sum;         // sum of modCounts
    long last = 0L;   // previous sum
    int retries = -1; // first iteration isn't retry
    try {
        for (;;) {
            //static final int RETRIES_BEFORE_LOCK = 2;, 如果比较次数达到 2，那么对所有的 Segment 进行加锁
            if (retries++ == RETRIES_BEFORE_LOCK) {
                for (int j = 0; j < segments.length; ++j)
                    //加锁
                    ensureSegment(j).lock(); // force creation
            }
            //修改次数总和
            sum = 0L;
            //元素个数总和
            size = 0;
            overflow = false;
            for (int j = 0; j < segments.length; ++j) {
                Segment<K,V> seg = segmentAt(segments, j);
                if (seg != null) {
                    //添加修改次数
                    sum += seg.modCount;
                    int c = seg.count;
                    			 //添加元素个数
                    if (c < 0 || (size += c) < 0)
                        overflow = true;
                }
            }
            //如果当前次的修改次数跟上一次的修改次数相同，那么退出循环
            if (sum == last)
                break;
            last = sum;
        }
    } finally {
        //释放锁
        if (retries > RETRIES_BEFORE_LOCK) {
            for (int j = 0; j < segments.length; ++j)
                segmentAt(segments, j).unlock();
        }
    }
    return overflow ? Integer.MAX_VALUE : size;
}
```



> ### JDK 8

JDK 8 的 size() 有点复杂，一步步分析



size() 内部调用了 sumCount()

```java
public int size() {
    //调用 sumCount()
    long n = sumCount();
    return ((n < 0L) ? 0 :
            (n > (long)Integer.MAX_VALUE) ? Integer.MAX_VALUE :
            (int)n);
}
```



sumCount() 的返回结果为 int 变量 baseCount + CounterCell 数组 as 中各个 CounterCell 的 value 值

即 JDK 8 中所有元素的个数为 baseCount + for(; ;) c.val

```java
final long sumCount() {
    //CounterCell 数组 as
    CounterCell[] as = counterCells; CounterCell a;
    //初始值为 baseCount
    long sum = baseCount;
    if (as != null) {
        for (int i = 0; i < as.length; ++i) {
            if ((a = as[i]) != null)
                //加上各个 CounterCell 的 value 值
                sum += a.value;
        }
    }
    return sum;
}
```



这个 baseCount 和 CounterCell 数组 是什么？

baseCount 和 CounterCell 数组很容易理解，而 CounterCell 这个类内部只有一个 long 型变量：value

```java
private volatile long baseCount;
private volatile CounterCell[] counterCells;

@sun.misc.Contended static final class CounterCell {
    volatile long value;
    CounterCell(long x) { value = x; }
}
```



它们的值是在哪里改变的？

这要追溯到 addCount()，这个方法就是对 baseCount 和 CounterCell 数组进行操作的

```java
private final void addCount(long x, int check) {
    CounterCell[] as; long b, s;
    /*
    这里 if 进入条件是 as != null 或者 对 baseCount 进行一次 CAS +1 失败了
    */
    if ((as = counterCells) != null ||
        !U.compareAndSwapLong(this, BASECOUNT, b = baseCount, s = b + x)) {
        CounterCell a; long v; int m;
        boolean uncontended = true;
        /*
        进到这里面的逻辑上 as != null 或者 as == null && 对 baseCount 进行一次 CAS 失败了
        这里 if 进入条件是 as == null 或者 
        	找到当前线程某个值 与 as.len - 1 进行 hash，定位到一个 CounterCell，对它 进行一次 CAS +1 失败了
        
        */
        if (as == null || (m = as.length - 1) < 0 ||
            //ThreadLocalRandom.getProbe()：探针哈希值，初始值为 0，即第一次尝试 0 号位置的 CounterCell
            (a = as[ThreadLocalRandom.getProbe() & m]) == null ||
            !(uncontended =
              U.compareAndSwapLong(a, CELLVALUE, v = a.value, v + x))) {
            //调用 fullAddCount()
            fullAddCount(x, uncontended);
            return;
        }
        if (check <= 1)
            return;
        s = sumCount();
    }
    //扩容判断
    if (check >= 0) {
		//xxxx
    }
}
```



上面当对 baseCount 和 线程的某个值 ThreadLocalRandom.getProbe() & (as.len - 1) 位置的 CounterCell 进行 CAS 都失败了的时候，那么就会调用 fullAddCount()

```java
    private final void fullAddCount(long x, boolean wasUncontended) {
        int h;
        //如果探针哈希值为 0，那么调用 localInit() 进行初始化，转变成别的值
        if ((h = ThreadLocalRandom.getProbe()) == 0) {
            ThreadLocalRandom.localInit();      // force initialization
            h = ThreadLocalRandom.getProbe();
            wasUncontended = true;
        }
        
        boolean collide = false;                // True if last slot nonempty
        for (;;) {
            CounterCell[] as; CounterCell a; int n; long v;
            if ((as = counterCells) != null && (n = as.length) > 0) {
                /*
                这里 a = as[h & (len - 1)]，跟前面进行 CAS 尝试的 CounterCell 是同一个
                如果 a == null，那么创建一个
                */
                if ((a = as[(n - 1) & h]) == null) {
                    if (cellsBusy == 0) {            // Try to attach new Cell
                        CounterCell r = new CounterCell(x); // Optimistic create
                        //后续都是对这个 a 进行 CAS +1，直到成功为止
                        if (cellsBusy == 0 &&
                            U.compareAndSwapInt(this, CELLSBUSY, 0, 1)) {
                            boolean created = false;
                            try {               // Recheck under lock
                                CounterCell[] rs; int m, j;
                                if ((rs = counterCells) != null &&
                                    (m = rs.length) > 0 &&
                                    rs[j = (m - 1) & h] == null) {
                                    rs[j] = r;
                                    created = true;
                                }
                            } finally {
                                cellsBusy = 0;
                            }
                            if (created)
                                break;
                            continue;           // Slot is now non-empty
                        }
                    }
                }
                else if (U.compareAndSwapLong(a, CELLVALUE, v = a.value, v + x))
                    break;
            }
            else if (U.compareAndSwapLong(this, BASECOUNT, v = baseCount, v + x))
                break;                          // Fall back on using base
        }
    }
```

fullAddCount() 内部也是获取 ThreadLocalRandom.getProbe() & (as.len - 1) =》 h & (len - 1) 位置的 CounterCell 进行自旋尝试 CAS + 1 直到成功为止



这样我们可以对 JDK 8 的这个 size() **总结**一波，size() 内部调用 sumCount()，而 sumCount() 是统计 baseCount 和 所有的 CounterCell数组 的值，而这个 baseCount 和 CounterCell 数组是每次在 put() 和 remove() 方法的结尾调用 addCount() 进行修改的

在 addCount() 中，会先对 baseCount 尝试进行一次 CAS +1，如果成功了那么直接返回，如果失败了那么会获取一个跟当前线程相关的值 h = ThreadLocalRandom.getProbe()，然后使用 h & (len - 1) 的 hash 方式 定位到 CounterCell 数组中的某个位置，使用该位置上的 CounterCell 再进行一次 CAS +1，如果成功那么直接返回，如果失败了，那么调用 fullAddCount()

在 fullAddCount() 中，同样是使用 上面线程的那个值，利用上面相同的 hash ，定位到相同的位置，使用 CounterCell 不断进行 CAS 自旋 + 1，直到成功为止

**CounterCell 数组的作用是什么？**

由于 ConcurrentHashMap 是并发情况下使用的，当高并发的时候，如果仅仅只有一个 baseCount 来记录元素个数，那么多个线程每次 addCount() 都会不断对 baseCount 进行自旋 CAS，而一次只有一个线程会成功，并发效率低；

(可能会有人说，那么直接在 sync 锁里面执行不就行了？emmm，开发者应该是考虑早点释放锁，让其他等待锁的线程早点获取锁去执行吧，毕竟计数这个不太重要)

基于此，所以出现了 CounterCell 数组，它是用作辅助的，当对 baseCount CAS 失败的时候，表示存在竞争，那么这时候就使用到 CounterCell 了，获取每个线程的某个值 h 来 hash 到不同的 CounterCell 数组位置，对其进行 CAS，这样的话，在不冲突的情况下，一次就可以存在  as.length 个线程可以进行 CAS +1 计数了，比起原本的多个线程都争夺一个 baseCount 效率提高太多了



> ### 改变

JDK 7 由于是分段的，所以需要统计各个 Segment 的节点个数，使用的方式是 三次不加锁遍历，两次比较，遍历过程中会统计修改次数 和 节点个数，如果存在相邻两次遍历的修改次数相同，那么就将统计的节点个数作为最终结果返回，否则就对所有的 Segment 加锁统计一次

这其实是 乐观 到 悲观的思想体现，最开始乐观：不加锁，后来悲观：加锁



JDK 8 是使用一个全局的 baseCount 和 全局的 CounterCell 辅助数组，在 put() 和 remove() 方法结尾都会调用 addCount() 对 baseCount 和 CounterCell 进行修改。最开始会对 baseCount 进行一次 CAS，如果失败了表示存在竞争，那么找到跟线程 hash 后对应的 CounterCell  进行一次 CAS ，失败了就后续对 这个 CounterCell 进行自旋 CAS 直到成功；使用 CounterCell 数组的目的是用来提高并发效率



```java

1	//v == 0 ,也就是0000 0000 , m0是size == 8时的掩码，也就是0000 0111
    
    
    
2	v |= ~m0; //~m0按位取反，为1111 1000 , 跟v做或得到v的新值为  1111 1000
	m0 = 11111111 11111111 11111111 11111000

3	v = rev(v);//将V的每一位反过来，得到 0001 1111


4	v++; //这个是关键，加1，注意其效果，得到0010 0000 , 什么意思呢？对一个数加1，其实就是将这个数的低位的连续1变为0，然后将最低的一个0变为1，其实就是将最低的一个0变为1


5	v = rev(v);//再次反过来，得到了：0000 0100  , 十进制就是4 ， 正好跟上面的吻合
```



## 6、扩容：rehash()、transfer()

尼玛复杂得一批

> ### JDK 7

这里挂上源码，具体分析看下面

```java
private void rehash(HashEntry<K,V> node) {
	//旧桶
    HashEntry<K,V>[] oldTable = table;
    //旧桶长度
    int oldCapacity = oldTable.length;
    //新桶长度 = 旧桶长度 << 1
    int newCapacity = oldCapacity << 1;
    threshold = (int)(newCapacity * loadFactor);
    //新桶
    HashEntry<K,V>[] newTable =
        (HashEntry<K,V>[]) new HashEntry[newCapacity];
    //用来 hash 定位槽位, 即 新桶长度 - 1
    int sizeMask = newCapacity - 1;
    //遍历旧桶中的所有槽位
    for (int i = 0; i < oldCapacity ; i++) {
        
        HashEntry<K,V> e = oldTable[i];
        if (e != null) {
            HashEntry<K,V> next = e.next;
            int idx = e.hash & sizeMask;
            if (next == null)   //  Single node on list
                newTable[idx] = e;
            else { // 在同一槽重用连续序列，减少节点的创建
                //想要查找的目标的最后一个节点
                HashEntry<K,V> lastRun = e;
                //想要查找的目标的最后一个节点的位置
                int lastIdx = idx;
                //遍历 链表
                for (HashEntry<K,V> last = next;
                     last != null;
                     last = last.next) {
                    //获取当前节点在新桶中的槽位
                    int k = last.hash & sizeMask;
                    //如果当前节点 hash 后跟前面节点 hash 后会在不同位置，那么舍弃前面的节点，从这个节点开始复用
                    if (k != lastIdx) {
                        lastIdx = k;
                        lastRun = last;
                    }
                }
                //复用 lastRun 及后面的节点，后续第二次遍历就不会去遍历 lastRun 以及后面的节点
                newTable[lastIdx] = lastRun;
                // Clone remaining nodes
                for (HashEntry<K,V> p = e; p != lastRun; p = p.next) {
                    V v = p.value;
                    int h = p.hash;
                    int k = h & sizeMask;
                    //获取槽位上的头节点
                    HashEntry<K,V> n = newTable[k];
                    //头插法：将新节点插入到链表头
                    newTable[k] = new HashEntry<K,V>(h, p.key, v, n);
                }
            }
        }
    }
    //下面是添加新节点到新桶中，具体看 put() 源码为什么这里需要传入 node
    int nodeIndex = node.hash & sizeMask; // add the new node
    node.setNext(newTable[nodeIndex]);
    newTable[nodeIndex] = node;
    table = newTable;
}
```



JDK 7 的这个扩容操作看起来有点玄乎，**需要知道的一点是：它是在 put() 的时候调用的，是已经加了锁的，因此线程安全**

它里面有这么一段代码，即遍历到某个槽位，会对这个槽位进行第一次遍历

这次槽位它的目的是找到在 hash 后在新桶中位置一致的最后几个节点，然后复用起来，不需要新建

```java
//想要查找的目标的最后一个节点
HashEntry<K,V> lastRun = e;
//想要查找的目标的最后一个节点的位置
int lastIdx = idx;
//遍历 链表
for (HashEntry<K,V> last = next;
     last != null;
     last = last.next) {
    //获取在新桶中的槽位
    int k = last.hash & sizeMask;
    //如果
    if (k != lastIdx) {
        lastIdx = k;
        lastRun = last;
    }
}
//复用 lastRun 及后面的节点，后续第二次遍历就不会去遍历 lastRun 以及后面的节点
newTable[lastIdx] = lastRun;
```

而后面第二次进行遍历，就是遍历 [e, lastRun) 这个范围的节点，计算它们在新桶中的槽位，然后新建一个节点，使用头插法插入新桶中

```java
for (HashEntry<K,V> p = e; p != lastRun; p = p.next) {
    V v = p.value;
    int h = p.hash;
    int k = h & sizeMask;
    //获取槽位上的头节点
    HashEntry<K,V> n = newTable[k];
    //将新节点使用头插法插入到链表头
    newTable[k] = new HashEntry<K,V>(h, p.key, v, n);
}
```



的确一般看来，JDK 7 的这波操作有点迷，为什么要第一次遍历找出最后几个能够复用的节点？为什么第二次遍历要给每个节点都新建一个节点而不是复用它们？

因为 HashEntry 的 next 没有用 final 修饰，所以是可以改变指向的，那么就可以复用啊，为什么没有进行复用？

网上没说明，按照我个人的理解，**是为了 get() 中读线程考虑的**

```
因为 get() 线程没有加锁，如果读的过程中在进行元素迁移，那么读的链表顺序会发生改变
比如 get() 定位的槽位上的链表是 1 -> 2 -> 3 -> 4 -> 5，而目标节点是 4，当读到 2 的时候，rehash 将链表分为两段， 
1 -> 3 -> 5 和 2 -> 4，那么对于 get() 线程来说，接下来读到的就是 4 了，然后到达链表尾，发现不存在自己要找的值，返回 null，但其实是存在的，所以就导致错误发生
```

大概是为了防止这个问题的出现，所以才**不选择复用节点，新建节点，让原来的节点保持原来的顺序**

而第一次遍历就是找出后面能够保证顺序的 hash 后会在同一个槽位的节点，这些节点可以进行复用，因为不需要改变它们的顺序

比如

```
0 - 1 - 2 - 3 - 0 - 0 - 0
				↑
			hash 后 0 - 0 - 0 都在新桶中的一个槽位，而 3 hash 后跟 0 在不同的槽位
			这样的话，就可以直接将 0 - 0 - 0 迁移到 新桶中进行复用，因为它们的连接顺序在迁移后不会发生改变
			所以不需要新建，对读线程来说还是原来的那个顺序，不会出现问题
```





> ### JDK 8

尼玛复杂得一批，看不太懂



