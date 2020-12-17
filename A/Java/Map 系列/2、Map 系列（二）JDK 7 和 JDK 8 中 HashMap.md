# JDK 7 和 JDK 8 中 HashMap 的变化



## 1、table 初始化时机（了解即可）

```java
HashMap<Integer, Integer> map = new HashMap<>();
```

当创建一个 HashMap 的时候，table 为空，需要进行初始化，而 JDK 7 和 JDK 8 的初始化时机不同



> ### JDK 7

在 HashMap 中就定义了初始的容量 16 和 阈值 0.75f，即如果创建的时候没有指定容量，那么容量就默认为 16

```java
public class HashMap<K,V> extends AbstractMap<K,V> implements Map<K,V>, Cloneable{
    //默认初始容量
    static final int DEFAULT_INITIAL_CAPACITY = 1 << 4; // aka 16
    //默认初始阈值
    static final float DEFAULT_LOAD_FACTOR = 0.75f;
    //构造方法一
    public HashMap() {
        this(DEFAULT_INITIAL_CAPACITY, DEFAULT_LOAD_FACTOR);
    }
    //构造方法二
    public HashMap(int initialCapacity, float loadFactor) {
        this.loadFactor = loadFactor;
        threshold = initialCapacity;
        //空方法
        init();
    }
    void init() {
    }
}
```

但我们可以看出在构造方法中并没有去初始化 table 数组，只是计算好加载因子和阈值之类的

因此 JDK 7 的 table 初始化不是在 new 的时候

我们看到 put() ，

```java
public V put(K key, V value) {
    //判断 table 是否为空表，如果为空表，那么进行初始化
    if (table == EMPTY_TABLE) {
        inflateTable(threshold);
    }
    //xxxx
}
private void inflateTable(int toSize) {
    //这里得到的 size = 16
    int capacity = roundUpToPowerOf2(toSize);
    threshold = (int) Math.min(capacity * loadFactor, MAXIMUM_CAPACITY + 1);
    table = new Entry[capacity];
    initHashSeedAsNeeded(capacity);
}
```

可以发现在最开始就进行了空表判断，即 JDK 7 的 table 初始化是在第一次调用 put() 时进行的



> ### JDK 8

当我们 new 的构造方法为 空的还是，那么它最后经过种种调用，也是调用到这个构造方法中，并且 initialCapacity = 11

```java
public HashMap(int initialCapacity) {
    this(initialCapacity, DEFAULT_LOAD_FACTOR);
}
```

然后进入到 this() 构造方法

```java
public HashMap(int initialCapacity, float loadFactor) {
    if (initialCapacity < 0)
        throw new IllegalArgumentException("Illegal initial capacity: " +
                                           initialCapacity);
    if (initialCapacity > MAXIMUM_CAPACITY)
        initialCapacity = MAXIMUM_CAPACITY;
    if (loadFactor <= 0 || Float.isNaN(loadFactor))
        throw new IllegalArgumentException("Illegal load factor: " +
                                           loadFactor);
    this.loadFactor = loadFactor;
    //调用 tableSizeFor() 将初始容量调整为 2 的幂
    this.threshold = tableSizeFor(initialCapacity);
}
```

我们可以发现，在构造方法中同样没有对 table 的初始化，就是单纯计算 阈值 和 初始容量（调整为 2 的幂）

那么是在何时进行初始化的呢？

同样看 put()

```java
final V putVal(int hash, K key, V value, boolean onlyIfAbsent, boolean evict) {
    Node<K,V>[] tab; Node<K,V> p; int n, i;
    //如果 tab 为空，那么调用 resize() 进行初始化
    if ((tab = table) == null || (n = tab.length) == 0)
        n = (tab = resize()).length;
    
    if ((p = tab[i = (n - 1) & hash]) == null)
        //xxxxx
}
```

因此，JDK 8 的 table 初始化也是在第一次调用 put() 的时候进行的



> ### 改变

没啥改变，都是在第一次调用 put() 的时候进行 table 初始化



## 2、get()

get() 方法其实没什么好讲的



> #### JDK 7

hash 定位到槽位，然后遍历链表，对比 e.hash 和 e.key

```java
final Entry<K,V> getEntry(Object key) {

    int hash = (key == null) ? 0 : hash(key);
    // 在“该hash值对应的链表”上查找“键值等于key”的元素 
    for (Entry<K,V> e = table[indexFor(hash, table.length)]; e != null; e = e.next) {
        Object k;
        if (e.hash == hash &&
            ((k = e.key) == key || (key != null && key.equals(k))))
            return e;
    }
    return null;
}
```



> ### JDK 8

hash 定位到槽位，判断头节点是否是目标节点，如果是就返回，再判断节点是否是红黑树类型，如果是那么调用红黑树的查找方法，如果不是那么遍历链表，链表遍历方法跟 JDK 7 的一样

```java
final Node<K,V> getNode(int hash, Object key) {
    Node<K,V>[] tab; Node<K,V> first, e; int n; K k;
    if ((tab = table) != null && (n = tab.length) > 0 &&
        (first = tab[(n - 1) & hash]) != null) {
        //判断头节点是否是目标节点
        if (first.hash == hash && // always check first node
            ((k = first.key) == key || (key != null && key.equals(k))))
            return first;
        if ((e = first.next) != null) {
            //判断如果是红黑树类型，那么调用树的查找方法
            if (first instanceof TreeNode){
                return ((TreeNode<K,V>)first).getTreeNode(hash, key);
            }
            //链表遍历
            do {
                if (e.hash == hash &&
                    ((k = e.key) == key || (key != null && key.equals(k))))
                    return e;
            } while ((e = e.next) != null);
        }
    }
    return null;
}
```



> ### 改变

get() 在 JDK 8 相比 JDK 7 就是加了 头节点的判断 以及 红黑树的查找



## 3、put()

> ### JDK 7

先判断 table 是否已经进行初始化，如果还没有那么进行初始化

hash 定位到槽位，遍历链表，同样判断 e.hash 和 e.key, 如果相同那么进行 value 的更新

如果遍历到链表末尾，那么就表示需要插入

**在插入前会进行扩容判断**，如果节点个数打到阈值，那么就会进行扩容

扩容完成 或 无需扩容，使用头插法，将原来的链表连接到新节点 node 的后面，然后让 node 当作链表头

```java
public V put(K key, V value) {
    //如果当前是第一次调用 put()，则 table 还没有初始化，为空表，那么调用 inflateTable() 进行初始化
    if (table == EMPTY_TABLE) {
        inflateTable(threshold);
    }
    //校验key是否为空
    if (key == null)
        return putForNullKey(value);
    int hash = hash(key);	//获取key对应的hash值
    int i = indexFor(hash, table.length);	//得到该KV对应的table的index
    //这个for循环就是在校验table[i]对应的链表中要插入的K key有没有存在，如果有，那么就用put的 value替换，然后返回该key对应的老的value
    for (Entry<K,V> e = table[i]; e != null; e = e.next) {
        Object k;
        if (e.hash == hash && ((k = e.key) == key || key.equals(k))) {
            V oldValue = e.value;
            e.value = value;
            e.recordAccess(this);
            return oldValue;
        }
    }
    modCount++;//修改次数+1
    //上面判断完不能进行值更新后，这里插入(K,V)
    addEntry(hash, key, value, i); 
    return null;
}

void addEntry(int hash, K key, V value, int bucketIndex) {
    //在插入节点前，扩容判断
    if ((size >= threshold) && (null != table[bucketIndex])) {
        resize(2 * table.length);
        hash = (null != key) ? hash(key) : 0; //扩容后，重新计算当前节点的 hash
        bucketIndex = indexFor(hash, table.length);//扩容后，重新计算槽位
    }
    //插入节点
    createEntry(hash, key, value, bucketIndex);
}

void createEntry(int hash, K key, V value, int bucketIndex) {
    //采用头插法插入
    Entry<K,V> e = table[bucketIndex];
    //这里将 e 传给新节点，让新节点的 next 指向 e，然后槽位 table[i] 指向 新节点
    table[bucketIndex] = new Entry<>(hash, key, value, e);
    size++;
}
```



> ### JDK 8

hash 到槽位，判断头节点的 hash 和 key 是否相同，如果相同那么直接更新值

判断头节点是否是红黑树，如果是则调用红黑树的插入方法

否则遍历链表，在遍历前初始化一个 int 变量 bitCount  来记录链表节点个数，然后遍历链表，判断 hash 和 key 是否相同，如果相同则更新值

遍历到链表尾，那么使用尾插法在链表尾部插入节点 node，

插入完成后，判断 bitCount 是否大于等于 8，如果是的话那么将链表转换为红黑树

最终**插入后判断是否需要扩容 resize()**

```java
final V putVal(int hash, K key, V value, boolean onlyIfAbsent,
               boolean evict) {
    /**
	 * Node<K,V>[] tab  指向HashMap中table的指针
	 * Node<K,V> p		table[i]指向的对象
	 * n				table的长度
	 * i				p在table对应的index
	 */
    Node<K,V>[] tab; Node<K,V> p; int n, i;
    //如果还没初始化就初始化
    if ((tab = table) == null || (n = tab.length) == 0)
        n = (tab = resize()).length;
    //如果插入到 index 对应的table的slot还没有值，那么直接将(K,V)封装的newNode对象赋给tab[i]
    if ((p = tab[i = (n - 1) & hash]) == null)
        tab[i] = newNode(hash, key, value, null);
    else {
        Node<K,V> e; K k;
        //如果插入的key跟table[i]的key相同，那么将table[i]的slot的值（node）赋给e
        if (p.hash == hash &&
            ((k = p.key) == key || (key != null && key.equals(k))))
            e = p;
        //如果table[i]是红黑树类型，则调用红黑树的插入方法
        else if (p instanceof TreeNode)
            e = ((TreeNode<K,V>)p).putTreeVal(this, tab, hash, key, value);
        //否则，table[i]是链表
        else {
            //binCount 记录节点个数
            for (int binCount = 0; ; ++binCount) {	
                //采用尾插法插入新的node
                if ((e = p.next) == null) { 
                    p.next = newNode(hash, key, value, null);
                    //如果链表的长度大于指定的长度，默认是8，则转换成红黑树
                    if (binCount >= TREEIFY_THRESHOLD - 1) // -1 for 1st
                        treeifyBin(tab, hash);
                    break;
                }
                //如果链表中有存在跟插入的node的key和hash是一样的话，那么e指定该节点，然后退出循环
                if (e.hash == hash &&
                    ((k = e.key) == key || (key != null && key.equals(k))))
                    break;
                p = e; //下个节点
            }
        }
        //说明有key相同的node
        if (e != null) {
            V oldValue = e.value;
            if (!onlyIfAbsent || oldValue == null)
                e.value = value;
            afterNodeAccess(e);
            return oldValue;
        }
    }
    ++modCount;	//修改次数+1
    if (++size > threshold) //插入后判断是否需要扩容
        resize();
    return null;
}

```



> ### 改变

JDK 7 插入使用的是头插法，扩容判断是在插入节点前判断的，并且扩容后会重新计算所有节点的 hash 值，无用功

JDK 8 在最开始会判断头节点是否是目标节点，然后再判断是否是红黑树，最后再遍历链表，同时节点等于 8 个并且桶容量达到 64 时会转红黑树，插入使用的是尾插法，扩容判断是在插入节点后判断的



## 4、hash()

> ### JDK 7

JDK 7 的 hash 算法没什么好讲的，为了让高位也参加运算，所以进行繁琐的 ^ 操作

```java
//JDK 7 的 hash 算法比 JDK 8 的复杂很多
final int hash(Object k) {
    int h = hashSeed;
    
	//疯狂 hash , 尽量让每一位参与运算，其实 JDK 8 的简单但又比较实用，所以才进行修改
    h ^= k.hashCode();
    h ^= (h >>> 20) ^ (h >>> 12);
    return h ^ (h >>> 7) ^ (h >>> 4);
}
```

里面最开始有一个初始变量 hashSeed，它在一般情况下都是 0，不会发生改变（除非在 JVM 中配置参数），该参数的目的是通过添加一个随机数来参与 hash 运算，防止别人猜测到 hash 计算值，防止 Hash Flooding（哈希洪水攻击）[哈希洪水攻击](https://www.zhihu.com/question/286529973/answer/679818605)



> ### JDK 8

简单的高 16 位异或 低 16 位

使用 高 16 位 异或 16 位是为了让高位参加运算，好处在于能够均匀分散节点：

比如 sizeMask = 0011，而 节点 A 的 hash = 1010 0001， 节点 B 的 hash = 0011 0001

这样的话 节点 A 和 节点 B hash 定位的结果都是 0001，由于高位全部没有参加运算，只看了 跟 sizeMask 为 1 的低 2 位，hash 冲突多得一批，当存在三个节点，往后每插入一个就起冲突

因此通过这种方式来让高位参加运行

而使用 ^ 而不使用 &、| 的原因是 0^0 = 1^1 = 0，0^1 = 1^0 = 1，均匀分布，概率五五开

```java
static final int hash(Object key) {
    int h;
    return (key == null) ? 0 : (h = key.hashCode()) ^ (h >>> 16);
}
```

JDK 8 通过将转换为红黑树来提高查询效率，每个操作的时间复杂度为 O(logn)，不知道作者是不是因为感觉使用红黑树降低了时间复杂度所以不再需要 哈希种子 来防止哈希洪水攻击了



> ### 改变

JDK 7 中节点的 hash 值受到一个 int 型变量 hashSeed 的影响，但是在一般情况下 hashSeed 的值都默认为 0

JDK 8 使用高 16 位 异或 低 16 位，算法简单高效，既能让 高位 参加运算，又能均匀分布



## 5、resize()

> ### JDK 7

**流程如下：**

直接将桶长度扩大一倍，创建一个新的桶

判断是否需要对节点进行 rehash，即是判断 hashSeed 是否发生改变，一般情况下是不会发生改变的，所以默认不需要重新计算 hash

然后遍历所有槽位上节点，使用 e.hash & (len - 1) 的方式迁移到新的桶上，这里迁移的方式是使用头插法，所以当迁移到新的桶上时，链表的节点会倒置**（这也是出现多线程扩容死循环的原因）**

```java
void resize(int newCapacity) {
	Entry[] oldTable = table;
	int oldCapacity = oldTable.length;

	Entry[] newTable = new Entry[newCapacity];
    boolean oldAltHashing = useAltHashing;
	useAltHashing |= sun.misc.VM.isBooted() &&
			(newCapacity >= Holder.ALTERNATIVE_HASHING_THRESHOLD);
    //判断是否需要 rehash，一般情况下为 false，如果我们普通使用的话是不可能为 true 的
	boolean rehash = oldAltHashing ^ useAltHashing;
    
	transfer(newTable, rehash); //扩容核心方法
	table = newTable;
	threshold = (int)Math.min(newCapacity * loadFactor, MAXIMUM_CAPACITY + 1);
}
void transfer(Entry[] newTable, boolean rehash) {
	int newCapacity = newTable.length;
	for (Entry<K,V> e : table) {//直接遍历table变量
		//链表跟table[i]断裂遍历，头部往后遍历插入到newTable中
		while(null != e) {
			Entry<K,V> next = e.next;
            //只有 rehash 为 true 的时候才重新计算 hash，但一般不会为 true
			if (rehash) {
				e.hash = null == e.key ? 0 : hash(e.key);
			}
			int i = indexFor(e.hash, newCapacity);
			e.next = newTable[i];
			newTable[i] = e;
			e = next;
		}
	}
}
```



> ### JDK 8

JDK 8 的 resize() 包含了 table 初始化 和 扩容两个作用



**流程如下：**

将桶长度扩大一倍，创建一个新的桶

遍历所有的槽位，如果为空，那么直接跳过

如果是红黑树节点，那么使用红黑树的扩容方法

如果是链表，那么创建四个变量 loHead、loTail、hiHead、hiTail 作为两个链表的头节点和尾节点，一个用来存储 "原位置" 的节点，一个用来存储 "原位置 + 原长度" 的节点

首先使用 e.hash & oldCap 来判断该节点 对于新增的 hash 位是否为 1，如果是 1，那么添加到 hi 链表上去，如果不是 1，那么添加到 lo 链表上去

等到这个槽位遍历完毕时，将 lo 链表存储到 newTab[i] 上，将 hi 链表存储到 newTab[i + oldCap] 上，这样就完成了迁移



原理就是通过判断新增的 hash 位是否为 1，如果为 1，那么新位置就是 "原位置 + 原长度"，如果为 0，那么位置就是 "原位置"

通过两个链表将两种节点分开，然后将两个链表插到各自的位置上完成迁移

```java
final Node<K,V>[] resize() {
    Node<K,V>[] oldTab = table;
    //这里的 oldTab == null 表示是 table 初始化
    int oldCap = (oldTab == null) ? 0 : oldTab.length;
    int oldThr = threshold;
    int newCap, newThr = 0;
    //一系列校验
    if ((newCap = oldCap << 1) < MAXIMUM_CAPACITY && //容量翻倍
        oldCap >= DEFAULT_INITIAL_CAPACITY)
        newThr = oldThr << 1; //扩容阈值也翻倍

    threshold = newThr;
    Node<K,V>[] newTab = (Node<K,V>[])new Node[newCap];
    table = newTab;
    //这里只有扩容才会进入，因为如果是初始化那么 oldTab == null
    if (oldTab != null) {
        for (int j = 0; j < oldCap; ++j) {//开始遍历oldTable
            Node<K,V> e;
            if ((e = oldTab[j]) != null) {
                oldTab[j] = null;
                if (e.next == null) //table[i] 为空
                    newTab[e.hash & (newCap - 1)] = e;
                else if (e instanceof TreeNode) // table[i]是红黑树的结构
                    ((TreeNode<K,V>)e).split(this, newTab, j, oldCap);
                
                //链表扩容
                else { 
                    //“低链表”的头和尾，即扩容后的在new table的index和old index是一样的
                    Node<K,V> loHead = null, loTail = null; 
                    //“高链表”的头和尾，即扩容后的在new table的index是old index的2倍
                    Node<K,V> hiHead = null, hiTail = null; 
                    Node<K,V> next;
                    do {
                        next = e.next;
                        /*
                            重点①
                        */
                        if ((e.hash & oldCap) == 0) {
                            if (loTail == null)
                                loHead = e;//头只当初始时才赋值
                            else
                                loTail.next = e;//新插入的都到添加到tail中
                            loTail = e;
                        }
                        else {//newTable index ==  oldTable index * 2
                            if (hiTail == null)
                                hiHead = e;//头只当初始时才赋值
                            else
                                hiTail.next = e;//新插入的都到添加到tail中
                            hiTail = e;
                        }
                    } while ((e = next) != null);
                    /*
                    	重点②
                    */
                    if (loTail != null) { //将“低链表”添加到newTable中
                        loTail.next = null;
                        newTab[j] = loHead;
                    }
                    if (hiTail != null) {//将“高链表”添加到newTable中
                        hiTail.next = null;
                        newTab[j + oldCap] = hiHead;
                    }
                }
            }
        }
    }
    return newTab;
}

```



> ### 改变

JDK 7 的扩容就是简单粗暴，将桶长度扩大一倍，然后使用 e.hash & (len - 1) 计算节点新的位置，这里元素迁移时插入使用的是头插法，所以迁移完成后链表会倒置。而前面会判断是否需要对节点重新进行 hash，即 hashSeed 值发生改变的时候，但基本不会需要

JDK 8 先将桶长度扩大一倍，然后判断如果是红黑树，那么使用红黑树的扩容方式，如果是链表，那么就使用 4 个变量作为两个链表，将迁移后是在原来位置的节点串到一根链表上，将迁移后是在新位置上的串到一根链表上，而新旧位置的判断则是通过 e.hash & oldCap 来判断新增的 hash 位是否为 1，如果新位置表示是 原位置 + 原长度

![img](https://pic2.zhimg.com/80/v2-da2df9ad67181daa328bb09515c1e1c8_720w.png)





## 6、JDK 7 的多线程扩容 如何 会出现死循环？

JDK 7 的元素迁移代码如下：

```java
for (Entry<K,V> e : table) {
    while(null != e) {
        Entry<K,V> next = e.next;
        int i = indexFor(e.hash, newCapacity);
        e.next = newTable[i];
        newTable[i] = e;
        e = next;
    }
}
```

假设存在一个大小为 2 的桶

```
槽位 链表
0	
1	3->7->5
```

使用的是头插法

在单机下扩容完成后节点分布如下：

```
槽位 链表
0	
1	5
2
3	7->3
```



但是如果是在多线程**并发**环境下，存在线程 A 和 线程 B，它们都对桶进行扩容：

需要注意的是，扩容操作的是原来的旧节点，并不会重新创建新的节点，因此多线程操作共享资源没有加锁，导致死锁发生

**死锁的发生：**

线程 A 标记完 e 和 next 后发生 CPU 切换，线程 B 进行扩容，扩容完成后由于是头插法所以链表倒置，并且将全部变量 table 指向它的 newTable

切换到线程 A 继续执行，由于 A 使用的是 table，因此这时候指向变成了线程 B 的新桶，并且由于链表倒置，3 和 7 后续会互相的 next 会互相指向，倒置死锁，并且线程 A 由于前面遍历过 1 号槽位，所以不会再去遍历 1 号槽位，导致 key = 5 丢失

![image.png](https://pic.leetcode-cn.com/1601190635-MxebKP-image.png)



## 7、头插法和尾插法 的好处以及实现

JDK 7 使用头插法，那么节点 node 定位到某个槽位上，无需遍历一遍该槽位的链表，只需要直接将 node.next 指向该槽位的头节点即可，省去了遍历，但是在扩容的时候会出现死循环



JDK 8 使用尾插法，由于尾插法需要插在某个槽位的链表末尾，所以一般情况下需要遍历链表，这样的话效率会很低，由于 put()  的时候就已经是遍历链表了，所以对 put() 是无所谓的，但是对于 resize() 是有效率问题的

所以在 resize() 的时候使用 lo 链表 和 hi 链表来存储节点，这样的话只要每次插入都在 loTail 和 hiTail 处插入，并且更新这两个指针，就可以省去扫描链表了，等到遍历完成，直接将这两个链表插入到对应的新槽位中

同时 JDK 8 的尾插法也解决了 JDK 7 头插法扩容出现死循环的问题

