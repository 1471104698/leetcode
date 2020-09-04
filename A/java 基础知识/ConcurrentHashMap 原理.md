# ConcurrentHashMap 原理

## JDK 1.7  的实现

在 Jdk 1.7 中 使用 分段锁（Segment）+ 链表 实现



以下一张图是 ConcurrentHashMap 的结构分布图

**ConcurrentHashMap 维护着一个 Segment 数组，而 每个 Segment 又维护着一个 Entry 数组**

(其中这个 Entry 数组跟 HashMap 的 Node 节点一样)

**也就是要查找一个元素，需要经过两次 hash，第一次 hash 查找元素所在的 Segment，第二次 hash 查找元素在 Segment 的位置**

![img](https://pic2.zhimg.com/80/v2-f2bf15828fc75c7c6c7d84ead1b8c27c_720w.jpg)



**Segment 是 ConcurrentHashMap 的一个内部类，继承了 ReetrantLock，因此 也可以说是 lock + 链表**

```java
static final class Segment<K,V> extends ReentrantLock implements Serializable {
       private static final long serialVersionUID = 2249069246763182397L;
       
       // 和 HashMap 中的 HashEntry 作用一样，真正存放数据的桶
       transient volatile HashEntry<K,V>[] table;
       transient int count;
       transient int modCount;
       //阈值和加载因子，用于每个 Segment 上面的 HashEntry 扩容
       transient int threshold;
       final float loadFactor;
       
}
```



**Segment 内部的 HashEntry 类**

```java
static final class HashEntry<K, V>{
    final int hash;
    final K key;
    volatile V value;
    final  HashEntry<K, V> next;
    HashEntry(int hash, K key, V value, HashEntry<K, V> next){
        this.hash = hash;
        this.key = key;
        this.value = value;
        this.next = next;
    }
}
```



### 1、Segment 分段锁 是什么意思？

我们上面看到了 ConcurrentHashMap  的结构图了，元素 Entry 是被 Segment 管理的，即一个 Segment 管理多条 Entry 链表

因此，跟 hashtable 的每次 get/put 操作都进行整个数组 加锁而言，在 ConcurrentHashMap  中，一个线程 put 一个元素，只需要对 该元素所在的 Segment 加锁即可，这就是分段锁，将多条链表分给各个 Segment  进行管理

这样的话，如果有 N 个Segment ，那么同时并发操作的不同 Segment  的线程就可以达到 N 个，相比 hashtable 每次固定只能操作一个而言提供了效率



### 2、put 操作

通过 key 计算 hash，定位到对应的 Segment ，然后调用这个 Segment  的 put 方法

Segment  内部的 put 方法就是操作它内部的 hashEntry 了

- 首先调用 tryLock() 尝试获取锁，等到获取锁后，再进行操作

- 后面的就跟 hashMap 的 put 操作差不多了，通过 hash 计算元素对应的在 hashEntry 中的槽位，然后就跟进行比对

- 判断 key 是否存在，如果存在，再根据 hash 和 equals() 方法判断是否是同一个对象，如果是，那么更新 value 值，如果 不存在 或者  不是同一个对象，那么重新创建一个 Entry 对象，然后使用头插法接到链表头部

- 最后释放锁



```java
public V put(K key, V value) {
    Segment<K,V> s;
    if (value == null)
        throw new NullPointerException();
    int hash = hash(key);
    int j = (hash >>> segmentShift) & segmentMask;
    if ((s = (Segment<K,V>)UNSAFE.getObject          // nonvolatile; recheck
         (segments, (j << SSHIFT) + SBASE)) == null) //  in ensureSegment
        s = ensureSegment(j);
    return s.put(key, hash, value, false);
}

final V put(K key, int hash, V value, boolean onlyIfAbsent) {
    //tryLock()尝试获取锁,
    HashEntry<K,V> node = tryLock() ? null :
    //tryLock() 失败， 那么 自旋获取锁
        scanAndLockForPut(key, hash, value);
    V oldValue;
    try {
        HashEntry<K,V>[] tab = table;
        int index = (tab.length - 1) & hash;
        HashEntry<K,V> first = entryAt(tab, index);
        for (HashEntry<K,V> e = first;;) {
			//。。。。。
        }
    } finally {
        unlock();
    }
    return oldValue;
}
```



### 3、get 操作

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



我们看上面可以发现， HashEntry 数组 和 HashEntry 内部的 value 值都是使用 volatile 修饰的，保证了内存的可见性

直接跟 hashmap 一样获取即可，无需加锁，与 hashtable 相比保证了高效率

对  HashEntry  数组 加 volatile 是为了保证扩容时对其他线程可见

对 value 值加 volatile 是为了保证值修改时对其他线程可见



**为什么使用 volatile 保证可见性就行了？**

因为 volatile 修饰的变量，当线程 1 将修改的变量值从工作内存写回主存的时候，发现是 volatile 修饰的，那么就会让其他线程中的该变量的值无效，那么对于先前已经读的线程，后续使用的时候发现是无效的，那么就会重新去内存获取，对于后来才读的线程，就已经保证是最新值了



### 4、为什么 HashEntry 节点的 next 属性设置为 final

因为 HashEntry 的 next 是不可变的，所以只能使用头插法

这样使得链表不能从中间或者后面插入，保证了后续的连接指向 不会发生改变



### 5、remove 操作

前面说了，HashEntry 的 next 不可改变性，那么就表示 删除节点的时候 不能直接改变 next 

具体实现是：

- hash 定位到具体的槽位，然后一次遍历根据 key 找到对应的待删除节点
- 然后再次遍历，过程是创建一条新的链表，即 将待删除节点之前的节点使用头插法插入到原槽位上
- 将待删除节点后面的节点直接连接上去

![img](https://user-gold-cdn.xitu.io/2020/2/5/17014002a2e23c5e?imageView2/0/w/1280/h/960/format/webp/ignore-error/1)

代码模拟：

```java
int key; //待删除的 key
int i; //槽位
//待删除节点
Node dele;
for(Node node = tab[i]; node != null; node = node.next;){
    if(node.key == key){
    //找到待删除节点
        dele = node;
        break;
    }
}
int j = 0;
for(Node node = tab[i]; node != dele; node = node.next, j++;){
	if(node != dele){
        if(j == 0){
            //因为 next 不可变，所以需要提前连接待删除节点后面的节点
        	tab[i] = new Node(hash, key, node.value, dele.next);
	    }else{
            tab[i] = new Node(hash, key, node.value, tab[i]);
	    }
	}
}

```



### 6、size() 操作

对每个 Segment 进行不加锁的元素个数统计，最多进行三次，比较前后两次结果，如果一样，那么粗略的认为没有新增元素，那么就将这个作为结果

如果第一次和第二次，第二次和第三次的结果都不一样，那么就对 Segment 加锁，然后重新计算







## JDK 1.8 的实现

JDK 1.8 HashMap 和 ConcurrentHashMap 都做了修改，由于在链表节点数大时，链表的查询效率为 O(n)

因此将链表修改为二叉树

同时，ConcurrentHashMap 的分段锁机制也发生了改变，从 lock 修改为 CAS  + synchronized

并且节点类从 HashEntry 修改为 Node，但是其内容不变

（可以看出 JDK 1.7 和 JDK 1.8 HashMap 和 ConcurrentHashMap 做出的改变都是一样的）



### 1、put 操作



ConcurrentHashMap 的 put 代码有深度啊，它改成了 CAS + synchronized 后，锁住的对象也发生改变

这里我们需要先知道它几个变量的含义

| 变量名称 | 含义                        |
| -------- | --------------------------- |
| bitcount | 某个槽位上节点的个数        |
| f        | 某个槽位上的头节点          |
| n        | table 数组的容量            |
| i        | 插入节点在 table 的索引位置 |
| fh       | 头节点的 hash 值            |
| tab      | table 的副本引用            |



具体 put 过程如下：

- 对 key 判空（不允许插入 null 值）
- 调用 spread() 计算 hash 值
- 开始循环，获取索引位置 i，然后再获取对应槽位的头节点 f
- 如果 f 为空，表示当前位置没有节点，那么使用 CAS 将插入节点插入，然会 return
- 如果 f 不为空，并且 f 的 hash 值 == MOVED，表示 table 数组位于扩容状态，那么协助扩容完成然后指向新的数组，开始下一轮循环
- 上述步骤完成后，接下来表示 f 是存在的，表示已经有其他的节点，那么就对这个头节点加锁 synchronized (f)，然后执行插入操作（锁的是一个槽位，即锁的粒度相比分段锁降低了）
- 继续判断头节点是否是 f，防止并发出现问题，谨慎判断，如果不是，那么下一轮循环
- 如果是，那么判断是链表还是红黑树（根据 fh 来判断）
- 如果是链表，那么遍历链表，边遍历边判断 key hash 和 equals，并且过程中 bitCount++（只在链表中计算，树中不计算）
- 如果到最后都不是我们要找的 对象，那么创建一个新的 Node ，直接插入到链表尾部（因为已经加锁，所以无需 CAS）
- 如果是树，那么直接按照树的方式插入
- 退出循环后，判断 bitCoun 是否 >= 8，如果是，那么将链表转换为红黑树
- 最后判断是否需要扩容



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
        //5.如果对应key的哈希值上对应table数组下标的位置没有node，则通过cas操作创建一个node放入table中,然后putval出栈
        else if ((f = tabAt(tab, i = (n - 1) & hash)) == null) {
            if (casTabAt(tab, i, null,
                         new Node<K,V>(hash, key, value, null)))
                break;                   // no lock when adding to empty bin
        }
        //6、如果table正在扩容，则得到扩容后的table，然后再重新开始一个循环
        else if ((fh = f.hash) == MOVED)
            tab = helpTransfer(tab, f);
        else {
            //7.到这里说明找到了key hash后对应的table，并且table上有其他node的存在
            V oldVal = null;
            //8、把这个找到的node加上同步锁，防止并发出现的问题，如果其他key put进来的时候也对应这个tab则堵塞在这里
            synchronized (f) {
                //9.再次用cas确认索引i上的table为我们找到的node，如果不是的话则这个node被修改，直接释放锁进入下一个循环
                if (tabAt(tab, i) == f) {
                    //10.如果目标table的第一个node的哈希值大于等于0，则是链式结构，走链表查找，反之走红黑树查找
                    if (fh >= 0) {
                        //11.标志bincount为1，因为在该table上至少有一个node节点
                        binCount = 1;
                        //12.循环链表
                        for (Node<K,V> e = f;; ++binCount) {
                            K ek;
                            //13.如果遍历元素的哈希值与需要插入目标key的哈希值相同，并且值也相同，则插入的是重复key的元素
                            if (e.hash == hash &&
                                ((ek = e.key) == key ||
                                 (ek != null && key.equals(ek)))) {
                                oldVal = e.val;
                                //14.如果onlyIfAbsent为false的话，则替换为新value，否则不修改（一般传false）
                                if (!onlyIfAbsent)
                                    e.val = value;
                                //15.break循环
                                break;
                            }
                            //16.循环直到最后一个node节点的key都不是我们想要插入的key
                            Node<K,V> pred = e;
                            if ((e = e.next) == null) {
                                //在尾部添加一个新节点，break循环
                                pred.next = new Node<K,V>(hash, key,
                                                          value, null);
                                break;
                            }
                        }
                    }
                    //17.该节点属于红黑树的子节点，进行树操作
                    else if (f instanceof TreeBin) {
                        Node<K,V> p;
                        binCount = 2;
                        if ((p = ((TreeBin<K,V>)f).putTreeVal(hash, key,
                                                              value)) != null) {
                            oldVal = p.val;
                            if (!onlyIfAbsent)
                                p.val = value;
                        }
                    }
                }
            }
            //18.如果node节点不为0
            if (binCount != 0) {
                //19.如果node大于或者等于8，则转为红黑树
                if (binCount >= TREEIFY_THRESHOLD)
                    treeifyBin(tab, i);
                //20.返回原来key对应的旧值
                if (oldVal != null)
                    return oldVal;
                break;
            }
        }
    }
    //21.元素个数 +1
    addCount(1);
    return null;
}
```





### 2、get 操作

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

get 操作跟 JDK 1.7 差不多，无锁，都是借助 volatile 来实现可见性

主要就是

（具体看 锁 中 volatile 的 happens-before）



### 3、remove 操作

过程跟 put() 操作差不多，就是定位到某个槽，然后判断槽的头节点是否为空，为空直接返回

不为空就判断是否是在进行扩容，如果是那协助元素迁移

如果不是在扩容，那么就对头节点加锁，然后判断链表还是红黑树来进行移除元素，完事后调用 addCount(-1) 元素个数 -1



### 4、size() 操作

有两个获取 size 的方法，一个 size()，一个是 mappingCount()

两个代码内容几乎一样，不一样的是返回值

size() 最大返回 Integer.MAX_VALUE，超出部分直接舍弃

mappingCount() 直接返回 long 型的结果，是准确的数据

```java
public int size() {
    long n = sumCount();
    return ((n < 0L) ? 0 :
            (n > (long)Integer.MAX_VALUE) ? Integer.MAX_VALUE :
            (int)n);
}

public long mappingCount() {
    long n = sumCount();
    return (n < 0L) ? 0L : n; 
}
```



其中，它们都调用了 sumCount() 方法，返回的是 sumCount() 的结果

可以发现，sumCount 统计了 baseCount 变量 和 CounterCell 数组的数值总和，将它作为 size 返回

```java
 final long sumCount() {
        CounterCell[] as = counterCells; CounterCell a;
        long sum = baseCount;
        if (as != null) {
            for (int i = 0; i < as.length; ++i) {
                if ((a = as[i]) != null)
                    sum += a.value;
            }
        }
        return sum;
    }
```



这个 baseCount 和 CounterCell 是什么呢？

我们可以发现，在 put() 方法中，末尾调用了 addCount(1) ，就是这里将元素 +1

baseCount 和 CounterCell[] as 都是全局变量，即所有线程共享的

首先对 baseCount 使用 CAS +1，如果失败了，表示存在竞争，那么就放弃对它操作，转为对 CounterCell 进行操作，对 CounterCell[] 里某个 CounterCell 进行 +1，如果失败了，那么调用 fullAddCount()，在里面自旋 CAS +1 直到成功

```java
private final void addCount(long x, int check) {
    CounterCell[] as; long b, s;
    if ((as = counterCells) != null ||
        !U.compareAndSwapLong(this, BASECOUNT, b = baseCount, s = b + x)) {
        CounterCell a; long v; int m;
        boolean uncontended = true;
        if (as == null || (m = as.length - 1) < 0 ||
            (a = as[ThreadLocalRandom.getProbe() & m]) == null ||
            !(uncontended =
              U.compareAndSwapLong(a, CELLVALUE, v = a.value, v + x))) {
            fullAddCount(x, uncontended);
            return;
        }
        if (check <= 1)
            return;
        s = sumCount();
    }
}
```



关于 CounterCell，我们可以看到，就是简单的只有一个使用 volatile 修饰的 value 属性

```java
@sun.misc.Contended static final class CounterCell {
    volatile long value;
    CounterCell(long x) { value = x; }
}
```



因此，CounterCell 是用来在并发环境下，避免多线程竞争 baseCount  变量等待时间过长，所以使用额外的数据结构进行操作，用来代替 baseCount  ，因此最后计数的时候，需要 baseCount 和 CounterCell 两者的数量之和

不过注意，该方法没有加锁之类的，也没有跟 JDK1.7 一样重复确认结果，因此可能统计完 baseCount 或者 某个 CounterCell  后，其他线程又进行了 +1操作，因此该方法的最终结果是不一定准确的





## 总结

| 项目     | JDK1.7                                                       |                            JDK1.8                            |
| -------- | :----------------------------------------------------------- | :----------------------------------------------------------: |
| 概览     | ![这里写图片描述](https://img-blog.csdn.net/20180327171253589?watermark/2/text/aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L3Byb2dyYW1tZXJfYXQ=/font/5a6L5L2T/fontsize/400/fill/I0JBQkFCMA==/dissolve/70) | ![这里写图片描述](https://img-blog.csdn.net/20180327171318297?watermark/2/text/aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L3Byb2dyYW1tZXJfYXQ=/font/5a6L5L2T/fontsize/400/fill/I0JBQkFCMA==/dissolve/70) |
| 同步机制 | 分段锁，每个segment继承ReentrantLock                         |                CAS + synchronized保证并发更新                |
| 存储结构 | 数组+链表                                                    |                       数组+链表+红黑树                       |
| 键值对   | HashEntry，其中 next 是 final 类型                           |               Node，其中 next 是 volatile 类型               |
| put()    | s使用 key 计算出 hash 值，需要两次 hash 定位，第一次 hash 定位到对应的 Segment，第二次 hash 定位到对应的 HashEntry 槽位，然后通过调用 tryLock() 获取锁，获取成功的话，那么就进行节点插入，后面基本跟普通的 hashMap 一样了，遍历每个节点，判断 key、hash、equals，如果有一个相同，那么直接更新值，如果都不同，那么就创建一个新的节点，由于 next 是 final 类型，所以只能使用头插法； | 先使用 key 计算 hash，然后定位到对应的 Node 槽位，如果槽位的头节点 f 为空，表示该位置不存在节点，那么使用 CAS 插入当前节点，如果插入成功，那么直接 break，如果失败，那么进入下一次循环；如果槽位的头节点不为空，那么对头节点 f 进行 synchronized 加锁，然后通过 头节点的 hash 值 fh 进行判断，fh >= 0，那么就是链表，那么使用链表的方式遍历，中途判断就跟 普通的 hashMap 一样了，不过最后插入是 JDK 1.8 的尾插法； 否则就是 红黑树，那么使用树的方式插入；方法最后调用 addCount(1) 使得元素个数 +1 |
| size()   | 不加锁统计各个 Segment 的元素个数，共三次，比较第一、二次的结果是否相同，如果相同，那么返回，比较第二、三次的结果是否相同，如果相同，那么返回；如果都不相同，那么对 Segment 加锁再进行统计 | 统计全局变量 baseCount 和 全局数组 CounterCell[] as 的数值之和，在 put 的 addCount() 中，先是对 baseCount 进行一次 CAS +1，如果失败，那么表示存在线程竞争，此时不跟它竞争，使用辅助的元素统计数组 as ，对它某个槽位的 CounterCell 进行一次 CAS +1，如果失败了，那么直接自旋对 CounterCell  进行 CAS+1 直到成功为止 |
| remove() | 由于 next 是 final 类型，所以无法简单的通过 改变 next 的指向来删除节点，所以采用的是待删除节点 deleteNode 之前的节点重新创建，使用头插法重新存储到 tab[i] 位置，然后直接连接 deleteNode 之后的节点，越过 deleteNode 节点，跟普通的删除不同的是，deleteNode 前面的节点需要重新创建，才能重新指定 next，由于 deleteNode 前面的节点只能使用头插法，所以前面节点的顺序发生翻转 | 跟 put() 操作差不多，前面的步骤基本一样，就是中间的普通的插入操作变成普通的删除操作，即找到待删除节点的前一个节点，连接待删除节点的后一个节点，即跳过待删除节点，在最后调用 addCount(-1) 来使元素个数 -1 |
| get()    | 不加锁，通过  happens-before 的 volatile 方面的规则来保证可见性即可，步骤的话就是先通过 key 获取 hash，然后定位到某个槽，然后直接遍历链表，通过 key、hash、equals 判断是否是目标对象，如果是，那么返回，否则返回 null | 跟 JDK 1.7 差不多，没什么多大的变化，主要就是多了通过 对应槽位的头节点的 hash 值来判断是链表还是红黑树，然后使用不同的查找方法，（注意：fh >= 0 就是链表，fh < 0 就是红黑树） |