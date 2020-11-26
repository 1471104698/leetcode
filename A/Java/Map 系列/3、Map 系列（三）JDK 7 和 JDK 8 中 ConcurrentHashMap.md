# JDK 7 和 JDK 8 中 ConcurrentHashMap 的变化

## 1、内部的数据结构

### JDK 7



JDK 7 使用 分段 Segment + ReentrantLock + 链表 实现

ConcurrentHashMap 将原本维护的一个 大 table 分割为多个 小 table

比如在 HashMap 中 table 大小为 16，那么在 ConcurrentHashMap 将它分割为 4 个大小为 4 的 table

然后每个 table 由一个 Segment 进行管理，加锁也只需要对对应的 Segment 进行加锁，这样能够支持 Segment 数的并发量



不过这样就增加了 hash 次数，原本只需要一个定位到 槽位的，现在需要两次 hash：

- 第一次  hash 到对应的 Segment
- 第二次 hash 到 中 Segment 的 table 对应的槽位



Segment 和 HashEntry 数据结构

```java
static final class Segment<K,V> extends ReentrantLock {
    //使用 volatile 修饰，保证扩容可见性
    volatile HashEntry<K,V>[] table;
    int count;
    int modCount;
    int threshold;
    final float loadFactor;
}

/*
HashEntry 数据结构（注意：在 JDK 6 中的 next 才是 final 修饰的，JDK 7 和 JDK 8 都修改为 volatile 了）

value 和 next 使用 voaltile 修饰，保证可见性
*/
static final class HashEntry<K,V> {
    int hash;
    K key;
    volatile V value;
    volatile HashEntry<K,V> next;

    HashEntry(int hash, K key, V value, HashEntry<K,V> next) {
        this.hash = hash;
        this.key = key;
        this.value = value;
        this.next = next;
    }
}
```



### JDK 8



JDK 8 回归了 一个 table 的时代，并且相比 JDK 7 缩小了锁的粒度

使用 sync 锁 + CAS + 链表+ 红黑树 实现

table 跟 JDK 7 一样使用 volatile，保证扩容可见性

value 和 next 使用 volatile 修饰，保证修改可见性

```java
volatile Node<K,V>[] table;

static class Node<K,V> implements Map.Entry<K,V> {
    int hash;
    K key;
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

### JDK 7



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



### JDK 8



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
        /*
            这里当 eh < 0 时，表示是 TreeBin 或者 扩容节点 ForwardingNode ，
            利用多态直接调用 find() 来获取，屏蔽调用细节
        */
        else if (eh < 0){
            return (p = e.find(h, key)) != null ? p.val : null;
        }
        //链表
        while ((e = e.next) != null) {
            if (e.hash == h &&
                ((ek = e.key) == key || (ek != null && key.equals(ek)))){
                
            }
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

### JDK 7



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

/*
分段锁 Segment 的 put() 方法
*/
final V put(K key, int hash, V value, boolean onlyIfAbsent) {
    /*
    分段 Segment 调用 tryLock() CAS 加锁
    如果第一次调用 tryLock() CAS 失败，那么调用 scanAndLockForPut() 进行 自旋 CAS 加锁
    */
    HashEntry<K,V> node = tryLock() ? null : scanAndLockForPut(key, hash, value);
    V oldValue;
    try {
        HashEntry<K,V>[] tab = table;
        int index = (tab.length - 1) & hash;
        //链表头节点 first
        HashEntry<K,V> first = entryAt(tab, index);
        //遍历该槽位上的链表
        for (HashEntry<K,V> e = first;;) {
            //1、遍历链表
            if (e != null) {
                K k;
                //该槽位的链表上已经存在 key、hash 相同的节点，那么替换旧值后 break
                if ((k = e.key) == key ||
                    (e.hash == hash && key.equals(k))) {
                    oldValue = e.value;
                    if (!onlyIfAbsent) {
                        e.value = value;
                        ++modCount;
                    }
                    break;
                }
                e = e.next;
            }
            //2、链表遍历完毕，e == null，待插入节点 node 不在链表上
            else {
                //node 不为空，那么 node.next = first
                if (node != null){
                    node.setNext(first);
                }
                else{
                    //node 为空，那么新建节点，并将 node.next = first
                    node = new HashEntry<K,V>(hash, key, value, first);
                }
				/*
				需要注意的是，上面 if-else 做的都是完成 node.next = first
				使用的是头插法，但没有将 tab[index] = node，即槽位链表头 tab[index] 还是指向 first
				即现在是
					tab[index]
								->>>	first
					node
					
				我们还需要一步将 tab[index] 指向 node ，使得 node 作为链表头
				
					tab[index] -> node -> first
					
				*/     
                
                //进行扩容判断，如果需要扩容顺便将 node 传入，因为它还没有跟 tab[index] 形成引用关系，扩容后再进行关联
                int c = count + 1;
                if (c > threshold && tab.length < MAXIMUM_CAPACITY){
                    rehash(node);
                }
                else{
                    //不需要扩容，直接将 tab[index] 指向 node，使得 node 作为链表头
                    setEntryAt(tab, index, node);
                }
                ++modCount;
                count = c;
                oldValue = null;
                break;
            }
        }
    } finally {
        //解锁
        unlock();
    }
    return oldValue;
}
```



### JDK 8



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
        5. tab[i] 为空，那么 进行一次 CAS 将 node 作为头节点
        	CAS 成功返回
        	CAS 失败进入下次循环
        */
        else if ((f = tabAt(tab, i = (n - 1) & hash)) == null) {
            if (casTabAt(tab, i, null, new Node<K,V>(hash, key, value, null)))
                break;                   
        }
        //6、tab[i] 不为空，但是 table 正在扩容，那么调用 helpTransfer() 协助扩容，并且得到扩容后的 table
        else if ((fh = f.hash) == MOVED){
            tab = helpTransfer(tab, f);
        }
        //7. tab[i] 不为空，并且不在扩容状态
        else {	
            
            /*
            8、将 槽位头节点 f 加上同步锁，防止并发出现的问题
            如果线程 put、remove 操作的也是这个 table[i] 的时候会进行阻塞
            */
            synchronized (f) {
            	V oldVal = null;
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
    //20.元素个数 +1，在上面已经释放锁了，即这里的 addCount() 调用是无锁状态了
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





## 4、size()

### JDK 7



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



### JDK 8



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



baseCount 和 CounterCell 数组都是用来解决并发情况下 计算 node 节点个数的

CounterCell 内部只有一个 long 型变量：value

```java
private volatile long baseCount;
private volatile CounterCell[] counterCells;

@sun.misc.Contended static final class CounterCell {
    volatile long value;
    CounterCell(long x) { value = x; }
}
```



**它们的值是在哪里改变的？**

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
        //计算节点个数 s，用于下面的扩容判断
        s = sumCount();
    }
    //扩容判断
    if (check >= 0) {
        //上面计算的 s >=sizeCtl 即容量达到扩容阈值，需要扩容
        while (s >= (long)(sc = sizeCtl) && (tab = table) != null &&
               (n = tab.length) < MAXIMUM_CAPACITY) {
            //扩容
            transfer(tab, null);
        }
    }
}
```



上面当对 baseCount 和 0 号位置的 CounterCell 进行 CAS 都失败了的时候，那么就会调用 fullAddCount()

```java
/*
	不断修改探针哈希值 h，对不同位置的 CounterCell 进行 CAS，直到成功为止
*/
private final void fullAddCount(long x, boolean wasUncontended) {
    int h;
    //线程探针哈希值默认为 0，进入方法体，初始化
    if ((h = ThreadLocalRandom.getProbe()) == 0) {
        //初始化修改 探针哈希值
        ThreadLocalRandom.localInit();
        h = ThreadLocalRandom.getProbe();
    }

    for (;;) {
        CounterCell[] as; CounterCell a; int n; long v;
        if ((as = counterCells) != null && (n = as.length) > 0) {
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
                                break;
                            }
                        }
                        continue;           // Slot is now non-empty
                    }
                }
            }else{
                //xxxx
            }
            //重新计算 探针哈希值 h
            h = ThreadLocalRandom.advanceProbe(h);
        }
    }
}
```



这样我们可以对 JDK 8 的这个 size() **总结**一波：

- size() 内部调用 sumCount()，而 sumCount() 是统计 baseCount 和 所有的 CounterCell数组 的值，而这个 baseCount 和 CounterCell 数组是每次在 put() 和 remove() 方法的结尾调用 addCount() 进行修改的

- 在 addCount() 中，会先对 baseCount 尝试进行一次 CAS +1，如果成功了那么直接返回，如果失败了那么会获取一个跟当前线程的探针哈希值 h = ThreadLocalRandom.getProbe()，默认为 0，使用 h & (len - 1) 的 hash 方式 定位到 CounterCell 数组上的槽位，由于默认是 0，所以必定是对 0 号 CounterCell 进行 CAS，如果成功那么直接返回；如果失败了，那么调用 fullAddCount()

- 在 fullAddCount() 中，不断改变 探针哈希值 h，使用不同的 CounterCell 进行 CAS，直到成功为止



**CounterCell 数组的作用是什么？**

由于 ConcurrentHashMap 是并发情况下使用的，当高并发的时候，如果仅仅只有一个 baseCount 来记录元素个数，那么多个线程每次 addCount() 都会不断对 baseCount 进行自旋 CAS，而一次只有一个线程会成功，并发效率低；

(可能会有人说，那么直接在 sync 锁里面执行不就行了？emmm，开发者应该是考虑早点释放锁，让其他等待锁的线程早点获取锁去执行吧，毕竟计数这个不太重要)

基于此，所以出现了 CounterCell 数组，它是用作辅助的，当对 baseCount CAS 失败的时候，表示存在竞争，**避免在一棵树上吊死**，那么这时候就使用到 CounterCell 了，不断改变 探针哈希值 h 来 hash 到不同的 CounterCell 数组位置，对其进行 CAS，这样的话，在不冲突的情况下，一次就可以存在  as.length 个线程可以进行 CAS +1 计数了，比起原本的多个线程都争夺一个 baseCount 效率提高太多了



> ### 改变

JDK 7 由于是分段的，所以需要统计各个 Segment 的节点个数，使用的方式是 三次不加锁遍历，两次比较，遍历过程中会统计修改次数 和 节点个数，如果存在相邻两次遍历的修改次数相同，那么就将统计的节点个数作为最终结果返回，否则就对所有的 Segment 加锁统计一次

这其实是 乐观 到 悲观的思想体现，最开始乐观：不加锁，后来悲观：加锁



JDK 8 是使用一个全局的 baseCount 和 全局的 CounterCell 辅助数组，在 put() 和 remove() 方法结尾都会调用 addCount() 对 baseCount 和 CounterCell 进行修改。最开始会对 baseCount 进行一次 CAS，如果失败了表示存在竞争，那么找到跟线程 hash 后对应的 CounterCell  进行一次 CAS ，失败了就后续对 这个 CounterCell 进行自旋 CAS 直到成功；使用 CounterCell 数组的目的是用来提高并发效率



## 5、扩容：rehash()、transfer()

### JDK 7



```java
/*
	rehash() 是在 put() 过程中调用的，Segment 已经加锁了，因此是线程安全的
*/
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
    //下面是将 put() 的新节点 node 添加到新桶中，因为只有 put() 时才会触发扩容
    int nodeIndex = node.hash & sizeMask; // add the new node
    node.setNext(newTable[nodeIndex]);
    newTable[nodeIndex] = node;
    table = newTable;
}
```



JDK 7 的这个扩容操作看起来有点玄，**table 中的每个槽位都会进行两次扫描**

第一次扫描：找到 **重新 hash 后槽位一致** 的最后几个节点，将 lastRun 以及后面的节点进行复用，这些节点在第二次扫描中会忽略，并且不需要新建

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

第二次扫描：遍历 [e, lastRun) 范围的节点，计算它们在新桶中的槽位，为它们新建一个节点，使用头插法插入 newTab 中

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



一般看来，JDK 7 的这波操作有点迷，为什么要新建 Node 节点而不是复用它们？

HashEntry 的 next 没有用 final 修饰，所以是可以改变指向的，那么就可以复用啊，为什么没有进行复用？

网上没说明，按照我个人的理解，**是为了 get() 中读线程考虑的**

```java
个人理解：
    因为 get() 线程没有加锁，如果读的过程中在进行元素迁移，那么读的链表顺序会发生改变
    比如 get() 定位的槽位上的链表是 1 -> 2 -> 3 -> 4 -> 5，目标 key = 3，当扫描到 2 的时候，扩容 将链表分为两段， 
    table 变成 1 -> 3 -> 5 和 2 -> 4 两段链表
    那么对于 get() 线程来说，接下来读到的就是 4 了，然后到达链表尾，发现不存在 3，返回 null，所以就导致错误发生
```



大概是为了防止这个问题的出现，**所以才不选择修改 next 指向来复用节点，而是选择新建节点，让原来的节点保持原来的顺序**

而第一次扫描就是为了尽量减少创建的节点，选择可以进行复用的节点

```java
比如：
0 - 1 - 2 - 3 - 0 - 0 - 0
				↑
			hash 后 0 - 0 - 0 都在新桶中的一个槽位，而 3 hash 后跟 0 在不同的槽位
			这样的话，就可以直接将 0 - 0 - 0 迁移到 新桶中进行复用，因为它们的连接顺序在迁移后不会发生改变
			所以不需要新建，对读线程来说还是原来的那个顺序，不会出现问题
```





### JDK 8



[扩容源码解析](https://blog.csdn.net/ZOKEKAI/article/details/90051567)



> #### 前言

**JDK 8 中何时会进行扩容？**

- 当调用 addCount() CAS 完成后 会调用 sumCount() 计算一次节点个数 s ，如果超过了阈值，那么调用 transfer() 进行扩容
- 当每次 put() 过程中，发现槽位上链表的节点个数到达 8 个，但是 table 的长度不到 64，那么不会转换为红黑树，而是会调用 transfer() 进行扩容
- 当 put() 过程中，发现 table 正在扩容， 那么调用 helpTransfer() 帮助扩容



在讲解扩容之前，先介绍 JDK 8 中 ConcurrentHashMap 中 Node 新增的两种子节点：

- TreeBin：封装了 红黑树节点 TreeNode（HashMap 中只有 TreeNode）
- ForwardingNode ：table 扩容时的临时节点

**它们都继承了 Node，目的是为了在 get() 时多态调用方法**

在 Node 中有 find() 方法，表示用来查找 node，该方法在 Node 中没有什么实际作用，目的是为了让 TreeNode 和 ForwardingNode 都重写，然后利用多态进行调用

在 get() 时，无论是 红黑树节点，还是正在扩容时的临时节点，利用多态性质，统一作为 Node 调用 find() 来获取目标 node



**Node 节点：**

相比 HashMap 多了 find()

```java
static class Node<K,V> implements Map.Entry<K,V> {
    //省略代码
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
    //该方法在这里实际上没有什么作用，主要是为了让子类实现
    Node<K,V> find(int h, Object k) {
        Node<K,V> e = this;
        if (k != null) {
            do {
                K ek;
                if (e.hash == h &&
                    ((ek = e.key) == k || (ek != null && k.equals(ek))))
                    return e;
            } while ((e = e.next) != null);
        }
        return null;
    }
}
```



**TreeBin 红黑树节点：**

```java
static final class TreeBin<K,V> extends Node<K,V> {
    //维护真正的红黑树根节点
    TreeNode<K,V> root;
    volatile TreeNode<K,V> first;

    /*
        这里是调用 Node 的构造方法 Node(int hash, K key, V val, Node<K,V> next)，
        将 hash 值设置为 TREEBIN， TREEBIN = -2
        在 get() 的时候，发现 eh < 0，那么就会调用下面的 find() 方法
        */ 
    TreeBin(TreeNode<K,V> b) {
        super(TREEBIN, null, null, null);
    }
    //重写的 find()
    final Node<K,V> find(int h, Object k) {
        if (k != null) {
            for (Node<K,V> e = first; e != null; ) {
                int s; K ek;
                if (U.compareAndSwapInt(this, LOCKSTATE, s,
                                        s + READER)) {
                    TreeNode<K,V> r, p;
                    p = ((r = root) == null ? null :
                         //调用 TreeNode 的 findTreeNode()
                         r.findTreeNode(h, k, null));
                    return p;
                }
            }
        }
        return null;
    }
}
```



**扩容节点 ForwardingNode：**

ForwardingNode 作为一个封装的 Node 节点，真正的 Node 数据存储在内部的 nextTable 中

```java
static final class ForwardingNode<K,V> extends Node<K,V> {
    //封装了 newTable
    final Node<K,V>[] nextTable;
    
    ForwardingNode(Node<K,V>[] tab) {
        /*
           作用跟 TreeBin 一致
           MOVED = -1
        */ 
        super(MOVED, null, null, null);
        this.nextTable = tab;
    }

    //重写的 find()
    Node<K,V> find(int h, Object k) {
        outer: for (Node<K,V>[] tab = nextTable;;) {
            Node<K,V> e; int n;
            if (k == null || tab == null || (n = tab.length) == 0 ||
                (e = tabAt(tab, (n - 1) & h)) == null)
                return null;
            for (;;) {
                int eh; K ek;
                if ((eh = e.hash) == h &&
                    ((ek = e.key) == k || (ek != null && k.equals(ek))))
                    return e;
                if (eh < 0) {
                    if (e instanceof ForwardingNode) {
                        tab = ((ForwardingNode<K,V>)e).nextTable;
                        continue outer;
                    }
                    else
                        //红黑树节点，调用 TreeBin 的 find()
                        return e.find(h, k);
                }
                if ((e = e.next) == null)
                    return null;
            }
        }
    }
}
```



> #### 扩容逻辑 transfer()



```java
/*
stride：当前线程需要处理的扩容的槽位个数，最少必须是 16 个
transferIndex：扩容之前的初始值为 tab.length，即在 table 数组的最右边，表示还剩下多少个槽位没有分配给线程去扩容

当前线程分配到了 stride 个槽位，那么就剩下 transferIndex - stride 槽位还没有给线程去扩容
*/
private final void transfer(Node<K,V>[] tab, Node<K,V>[] nextTab) {
    int n = tab.length, stride;
    //计算每个线程处理 hash 槽位的个数 stride，最少需要 16 个
    if ((stride = (NCPU > 1) ? (n >>> 3) / NCPU : n) < MIN_TRANSFER_STRIDE)
        stride = MIN_TRANSFER_STRIDE; // subdivide range

    //初始化 nextTab 数组，长度为 tab 的两倍
    if (nextTab == null) {            // initiating
        Node<K,V>[] nt = (Node<K,V>[])new Node<?,?>[n << 1];
        nextTab = nt;
        /*
        这里的 nextTable 是全局变量，所有线程可见的，并且是 volatile 修饰
        这里没有加锁，因此可能同时存在两个线程来初始化 nextTab，然后将 nextTab 赋值给 nextTable
        该槽位是 volatile 写，一次只能有一个线程执行成功，因此先写的线程后续使用的时候，会发现 cache line 失效
        因此会去主存重新读取新的 nextTable，保证了可见性
        */
        nextTable = nextTab;
        //transferIndex 赋值为旧数组的长度，表示旧数组整个需要迁移
        transferIndex = n;
    }
    int nextn = nextTab.length;

    /*
    	创建 ForwardingNode 节点时会将 nextTab 作为参数传入，即它内部会维护 新数组
    	ForwardingNode 主要是用来代替 table 某个位置上的 Node 的
    	ForwardingNode 节点有两个作用：
    	1、代替 table 某个位置上的 Node，表示该槽位已经迁移完毕，但是整个 table 仍然处于扩容状态
    	2、当其他线程执行 get() 的时候，可以通过这个 ForwardingNode 定位到它内部的 nextTab，然后进行数据查找
    */
    ForwardingNode<K,V> fwd = new ForwardingNode<K,V>(nextTab);

    //table 的某个槽位 是否迁移完毕，默认为 true，当为 true 时，线程需要获取新的槽位进行迁移
    boolean advance = true;
    //是否扩容完成
    boolean finishing = false;
    for (int i = 0, bound = 0;;) {
        Node<K,V> f; int fh;

        //advance = true，线程获取新的槽位进行迁移
        while (advance) {
            int nextIndex, nextBound;
            //全部迁移完成
            if (--i >= bound || finishing){
                //advance 设置为 false，退出循环
                advance = false;
            }
            //还没有迁移完成，但是槽位已经全部分配给其他的线程处理了
            else if ((nextIndex = transferIndex) <= 0) {
                i = -1;
                //advance 设置为 false，退出循环
                advance = false;
            }
            //分配 stride 个槽位给当前线程处理， 槽位索引区间为 [bountd, i]
            else if (U.compareAndSwapInt
                     (this, TRANSFERINDEX, nextIndex,
                      nextBound = (nextIndex > stride ?
                                   nextIndex - stride : 0))) {
                bound = nextBound;
                i = nextIndex - 1;
                //advance 设置为 false，退出循环
                advance = false;
            }
        }
        if (i < 0 || i >= n || i + n >= nextn) {
            int sc;
            //全部迁移完成
            if (finishing) {
                nextTable = null;
                table = nextTab;
                sizeCtl = (n << 1) - (n >>> 1);
                return;
            }
            /*
            	sizeCtl 记录当前正在扩容的线程数
            	当前线程迁移完成，没有槽位需要该线程处理了，因此需要退出了，使用 CAS 将 sizeCtl - 1
            */
            if (U.compareAndSwapInt(this, SIZECTL, sc = sizeCtl, sc - 1)) {
                if ((sc - 2) != resizeStamp(n) << RESIZE_STAMP_SHIFT)
                    return;
                finishing = advance = true;
                i = n; // recheck before commit
            }
        }
        //table[i] == null，那么直接将 ForwardingNode 放上去
        else if ((f = tabAt(tab, i)) == null){
            advance = casTabAt(tab, i, null, fwd);
        }
        //table[i] 槽位上为 ForwardingNode 节点，表示该槽位已经迁移完成
        else if ((fh = f.hash) == MOVED){
            advance = true; // already processed
        }
        else {

            //对头节点加锁，注意，来这里扩容的时候是已经 put() 完成了或者在 put() 前，还没有加锁
            synchronized (f) {
                Node<K,V> ln, hn;
                //判断头节点是否发生改变
                if (tabAt(tab, i) == f) {
                    /*
                	这里的做法思路 跟 JDK 7 的扩容一样，对每个槽位上的链表
                	同样是获取最后几个 hash 后处于同一个槽位的 Node 进行复用
                	而不需要去新建它们
                	同样是为了 get() 考虑，避免出现数据查找不到的情况
                */
                    //这里的 n 是旧数组的长度，这里用 hash & n 判断 hash 后是在旧位置还是在新位置
                    int runBit = fh & n;
                    Node<K,V> lastRun = f;
                    //第一次扫描，找后面可以复用的节点
                    for (Node<K,V> p = f.next; p != null; p = p.next) {
                        int b = p.hash & n;
                        //如果后一个节点跟 lastRun 不一致，那么更新 lastRun 和 runBit
                        if (b != runBit) {
                            runBit = b;
                            lastRun = p;
                        }
                    }
                    //判断 lastRun 是属于 高链表 还是 属于 低链表 的
                    if (runBit == 0) {
                        ln = lastRun;
                        hn = null;
                    }
                    else {
                        hn = lastRun;
                        ln = null;
                    }
                    //第二次扫描，新建节点，lastRun 以及后面的节点不需要扫描
                    for (Node<K,V> p = f; p != lastRun; p = p.next) {
                        int ph = p.hash; K pk = p.key; V pv = p.val;
                        //使用高低链表存储 新旧两个位置的节点，这里使用的是头插法，因此链表会反转
                        if ((ph & n) == 0)
                            ln = new Node<K,V>(ph, pk, pv, ln);
                        else
                            hn = new Node<K,V>(ph, pk, pv, hn);
                    }
                    //在 nextTab 的旧位置处设置 低链表
                    setTabAt(nextTab, i, ln);
                    //在 nextTab 的新位置处设置 高链表
                    setTabAt(nextTab, i + n, hn);
                    /*
                	将旧数组 tab[i] 的 Node 替换为 ForwardingNode，
                	其他线程 get() 时可以通过这个 fwd 找到迁移的数据
                	这里不会影响到正在 get() 的线程，因为正在 get() 的线程仍然是在扫描 tab，链表的位置没有发生改变
                */
                    setTabAt(tab, i, fwd);
                }
                //红黑树
                else if (f instanceof TreeBin){
                    //代码省略
                }
            }
        }
    }
}
```



整个扩容的过程：

- 1）计算线程每次分配的 table 槽位数，最小为 16，即每个线程最少处理 16 个槽位
- 2）判断 nextTab 是否已经初始化，如果没有，那么初始化，同时赋值给 全局的 volatile 变量 nextTable，这是 volatile 写，它可以不加锁解决多个线程同时对 nextTab 初始化的问题
- 3）创建一个 ForwardingNode 节点，将 nextTable 作为参数传入，即 ForwardingNode 内部会维护 新的 table -- nextTable 
- 4）领取分配到的槽位区间，一些细节就忽略了，当没有槽位可以分配时，那么该线程退出
- 5）一个个处理分配到的槽位，跟 put() 时一样，对槽位上的头节点 f 进行加锁，当加锁完成后，再判断 f 是否发生改变
- 6）然后就跟 JDK 7 的 rehash() 一样，对每个槽位上的链表进行两次扫描，一次扫描找到可复用节点，一次扫描新建（复制）节点，对节点的处理 跟 JDK 8 的 HashMap 扩容一样，将一个槽位上的链表 根据 hash 后位置的不同 分为 高、低 两条链表，这里对第二次扫描新建的节点插入到 高低链表 使用的是 **头插法**。
- 7）将高低链表插入到 nextTab 上，然后将 table 上该槽位的 Node 替换为 ForwardingNode ，这样后续别的线程 get() 时就会访问到这个 ForwardingNode ，然后再通过这个 ForwardingNode  访问到迁移了数据的 nextTab



 涉及到以下几个变量：

- transferIndex：表示当前还需要分配的槽位个数，初始值为旧数组的长度 table.length，假设旧数组长度为 64，transferIndex  = 64，如果线程 A 分配了 16 个槽位，那么 transferIndex = 48，最终到达 0 时表示所有的槽位分配完毕
- stride：表示每次分配给线程处理的槽位个数，最少为 16
- ForwardingNode ：内部会维护 新数组 nextTable，已经迁移完成的旧数组节点会被替换为该节点
- sizeCtl：正在扩容的线程数量，当到达一定阈值时，那么不能再添加线程进行扩容

![img](https://img-blog.csdnimg.cn/20190510093435247.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L1pPS0VLQUk=,size_16,color_FFFFFF,t_70)  





> #### 协助扩容 helpTransfer()

```java
final Node<K,V>[] helpTransfer(Node<K,V>[] tab, Node<K,V> f) {
    Node<K,V>[] nextTab; int sc;
    if (tab != null && (f instanceof ForwardingNode) &&
        (nextTab = ((ForwardingNode<K,V>)f).nextTable) != null) {
        int rs = resizeStamp(tab.length);
        while (nextTab == nextTable && table == tab &&
               (sc = sizeCtl) < 0) {
            if ((sc >>> RESIZE_STAMP_SHIFT) != rs || sc == rs + 1 ||
                //正在协助扩容的线程数到达阈值 或者 transferIndex <= 0 所有槽位已经分配完毕，那么该线程直接退出
                sc == rs + MAX_RESIZERS || transferIndex <= 0)
                break;
            //将 sizeCtl + 1，即正在协助扩容的线程数 + 1，当 CAS 成功时，那么当前线程调用 transfer() 进行协助
            if (U.compareAndSwapInt(this, SIZECTL, sc, sc + 1)) {
                transfer(tab, nextTab);
                break;
            }
        }
        return nextTab;
    }
    return table;
}
```

