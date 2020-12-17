# ThreadLocal

## 1、ThreadLocal 底层架构

> #### Thread

```java
public class Thread implements Runnable {
    private Runnable target;
    /*
    初始值为 null，只有第一次使用 ThreadLocal 的时候才会调用 createMap() 初始化
    注意，这里是 ThreadLocal.ThreadLocalMap，即是 ThreadLocal 的内部类
    */
    ThreadLocal.ThreadLocalMap threadLocals = null;
    
    public void start(){
        //xxx
        start0();
    }
    pubkic native void start0();
}
```



> ####  ThreadLocal、ThreadLocalMap、Entry

ThreadLocal 中定义了一个内部类 ThreadLocalMap，而每个 Thread 内部都维护了这么一个 ThreadLocalMap

因此每次只需要获取当前线程 Thread 内部的 ThreadLocalMap 即可做到线程隔离，因为是每个 Thread 都维护一份的

而使用 ThreadLocal 作为 key，由于是 key，所以可以多个 ThreadLocalMap 进行存储，复用 key 对各个 ThreadLocalMap 的 value 没有影响

```java
public class ThreadLocal<T> {
    /*
    	ThreadLocal 内部类 ThreadLocalMap，类似 HashMap，内部维护了一个 table 数组，元素为 Entry
    	Entry 可以当作是 HashMap 的 Node，不过它继承了 WeakReference，形成一个弱引用 
    	key 是 ThreadLocal，它赋值给弱引用的 referent，如果要访问，那么调用 entry.get() 调用 WeakReference 的方法返回
    	value 是传入的值
    */
    static class ThreadLocalMap {
        //Entry 继承了弱引用
        static class Entry extends WeakReference<ThreadLocal<?>> {
            Object value;
            Entry(ThreadLocal<?> k, Object v) {
                //将 ThreadLocal 传入到 WeakReference 中，赋值给内部的 referent
                super(k);
                value = v;
            }
        }
        private Entry[] table;
    }
    //...
}
public class WeakReference<T> extends Reference<T> {
	private T referent;
    public WeakReference(T referent) {
        super(referent);
    }
    public T get(){
        return referent;
    }
}
```



> #### hash 冲突

使用的是开放地址法



## 2、ThreadLocal api

### 2.1、set()

```java
public void set(T value) {
    //获取当前线程
    Thread t = Thread.currentThread();
    //获取线程的 ThreadLocalMap
    ThreadLocalMap map = getMap(t);
    
    if (map != null)
        //map 不为空，那么调用 map.set()，生成 Entry 存储
        map.set(this, value);
    else
        //如果 ThreadLocalMap == null，表示这是第一次使用该线程的 ThreadLocal，那么初始化 ThreadLocalMap
        createMap(t, value);
}
```



### 2.2、get()

```java
public T get() {
    //获取当前线程
    Thread t = Thread.currentThread();
    //获取线程的 ThreadLocalMap
    ThreadLocalMap map = getMap(t);
    if (map != null) {
        //调用 map.getEntry(this) 获取当前 ThreadLocal 对应的 Entry
        //（类似 HashMap 的 get()，不过 HashMap 只返回 value，这里相当于返回了整个 Node）
        ThreadLocalMap.Entry e = map.getEntry(this);
        if (e != null) {
            //获取 Entry 的 value
            T result = (T)e.value;
            return result;
        }
    }
    //如果 ThreadLocalMap 为空，表示之前没有调用过 set() 去初始化，那么调用 setInitialValue() 返回一个初始值
    return setInitialValue();
}

private T setInitialValue() {
    //调用 initialValue() 获取初始值，如果没有重写，那么默认为 null
    T value = initialValue();
    //以下是将这个 初始值放入到 ThreadLocalMap 中，没有重写的话那么就是存储 value = null
    Thread t = Thread.currentThread();
    ThreadLocalMap map = getMap(t);
    if (map != null)
        map.set(this, value);
    else
        createMap(t, value);
    return value;
}
```

我们可以通过继承 ThreadLocal 重写 initialValue() 方法来指定初值，这样不可以不需要 set() ，而直接调用 get() 时也能够获取初始值然后存储进 ThreadLocalMap 中

在 ReentrantReadWriteLock 中就这么做了

```java
static final class ThreadLocalHoldCounter
    extends ThreadLocal<HoldCounter> {
    public HoldCounter initialValue() {
        return new HoldCounter();
    }
}
```





### 2.3、remove()

```java
public void remove() {
    //获取 ThreadLocalMap
    ThreadLocalMap m = getMap(Thread.currentThread());
    if (m != null)
        //调用 map.remove(this)，移除掉当前 ThreadLocal 对应的 Entry
        m.remove(this);
}
```





可以看出，上面多个方法的最终逻辑都是在 ThreadLocalMap 中执行的，ThreadLocal 中的方法只是作为外部接口调用的而已



## 3、ThreadLocalMap api

[一篇文章，从源码深入详解ThreadLocal内存泄漏问题 - 简书 (jianshu.com)](https://www.jianshu.com/p/dde92ec37bd1)



由于 Entry 中的 key 使用的是弱引用，那么当该 key 没有强引用时，GC 会回收掉该 key，那么 Entry 变成 [null, value]

由于 value 是强引用，那么 GC 不会自动回收，那么就需要手动进行处理，对于这种 Entry 我们称作 脏 Entry

同时，ThreadLocal 对于 hash 冲突的处理方法是 开放地址法，这意味着当 remove() 掉一个 Entry 后，需要将原来因 hash 冲突而存储到别的位置的 Entry 重新定位到它该到的位置



### 3.1、set()

这个方法对 脏 Entry 做了如下处理：

- 如果 table[i] != null 并且 table[i].key != key 的话，表示存在 hash 冲突，需要往后面查找新位置插入（开放地址法）
- 如果 table[i] != null 并且 table[i].key == key 的话，那么直接替换旧值
- 如果在查找的存储位置过程中，发现存在 table[i].key == null，那么调用 **replaceStaleEntry()** 进行清理

```java
private void set(ThreadLocal<?> key, Object value) {

    Entry[] tab = table;
    int len = tab.length;
    //threadLocalHashCode 是当前 ThreadLocal 的 hashCode
    //定位到 ThreadLocal 所在的槽位
    int i = key.threadLocalHashCode & (len-1);

    for (Entry e = tab[i]; 
         e != null; 
         e = tab[i = nextIndex(i, len)] ) {
        
        ThreadLocal<?> k = e.get();
        //旧值替换
        if (k == key) {
            e.value = value;
            return;
        }
		/*
		表示被 GC 回收了，属于脏 Entry，那么调用 replaceStaleEntry() 进行清理
		注意，扫描到这里表示还没有找 key-value 的插入位置，而这里是一个 脏 Entry，表示可以用来插入 key-value
		因此在 replaceStaleEntry() 中会情况脏 Entry，同时将 key-value 插入/替换
		*/
        if (k == null) {
            replaceStaleEntry(key, value, i);
            return;
        }
        //table[i] 位置不符合，那么进入下一轮循环，查找下一个位置
     }

    //插入新的 Entry
    tab[i] = new Entry(key, value);
    //个数 +1
    int sz = ++size;
    //调用 cleanSomeSlots() 清理一些槽位上的 脏 Entry，并且如果发现超过阈值，那么进行扩容
    if (!cleanSomeSlots(i, sz) && sz >= threshold)
        rehash();
}
```



### 3.2、cleanSomeSlots()

该方法用来处理脏 Entry，根据名字 cleanSomeSlots 可以看出，它是扫描一些槽位处理，而不是全部进行处理，那么如何确定扫描多少槽位呢？

它使用一个 n，初始值 n 为 sz，即 Entry 个数，然后每次从 i 位置开始扫描，如果遇到 脏 Entry，那么调用 **expungeStaleEntry()** 进行清理，同时重新让 n = table.length，增加扫描次数

而每次循环都是 n / 2，即如果在没有遇到 脏 Entry 的情况下，扫描的次数为 log2(n)，如果遇到了 脏 Entry，那么就会增加扫描次数，相当于扩大往后扫描的范围

**采用这种方法的目的是为了保证在时间和效率上的平衡**

```java
//初始值 n 为 sz，即 Entry 个数
private boolean cleanSomeSlots(int i, int n) {
    boolean removed = false;
    Entry[] tab = table;
    int len = tab.length;
    do {
        i = nextIndex(i, len);
        Entry e = tab[i];
        /*
        如果 e.get() 得到的 key = null，那么表示被 GC 回收了，该 entry 是 脏 Entry
        那么调用 expungeStaleEntry() 进行清理
        同时让 n = tab.length，增加扫描次数
        */
        if (e != null && e.get() == null) {
            n = len;
            removed = true;
            i = expungeStaleEntry(i);
        }
    } while ( (n >>>= 1) != 0);	//n / 2
    
    return removed;
}
```



### 3.3、expungeStaleEntry()

expungeStaleEntry 直译为 ”清除脏 Entry“，

该方法会往后一直扫描，直到遇到 table[i] == null 停止，即该槽位上不存在 Entry，这个过程存在两个作用：

- 如果遇到 e.get() == null，即 key 被回收了的，那么清除掉这些 脏 Entry

- 对遇到的每个非脏 Entry 重新计算槽位，避免由于 开放地址法 导致的数据不可见问题（这里的不可见不是线程之间的）

数据不可见问题如下：

```java
//问题产生过程：
比如 存在数组 1	2	3	4
    A hash 计算后到达的是 1 号位置，没有 hash 冲突，直接放入
    1 	2 	3
    A
    当 B hash 后到达的是 1 号位置，但是发生 hash 冲突，所以需要放到 2 号位置
    1 	2 	3
    A	B
    当 C hash 后到达的是 1 号位置，但是发生 hash 冲突，查看 2 号位置也发生 hash 冲突，所以需要放到 3 号位置
    1 	2 	3
    A	B	C

    这样如果需要查找 C 这个 ThreadLocal 对应的 Entry 的话，会先定位到 1 号位置，发生冲突，到达 2 号位置都发生冲突，
    最终会到达 3 号位置，发现 table[3].key == C，查找完毕
    
//问题产生：  
    如果 A remove() 掉了，那么这意味着 1 号位置为空
    1	2	3
    	B	C
    如果不进行处理的话，那么 B、C 在查找的时候都会先定位到 1 号位置，发现 table[1] == null，就认为 Entry 不存在
    因此出现了错误查询

//问题解决：    
因此当 remove() 掉某个 Entry 时，需要对它后面的 Entry 重新定位槽位
```



为什么 expungeStaleEntry() 遇到 table[i] == null 后停止了？

因为如果 table[i] == null，有两种可能：之前没有存储过 Entry 或者 已经被 remove() 了

- 如果是前者，那么显然是不可能存在脏 Entry 以及 数据不可见问题的

- 如果是后者，那么在 remove() 的时候同时会调用该方法对 remove 掉的 Entry 它后面的 Entry 进行一次处理，这里就不需要重复处理了

```java
private int expungeStaleEntry(int staleSlot) {
    Entry[] tab = table;
    int len = tab.length;

    //清空传入的指定槽位上的 key-value
    tab[staleSlot].value = null;
    tab[staleSlot] = null;
    size--;

    // Rehash until we encounter null
    Entry e;
    int i;
    for (i = nextIndex(staleSlot, len); (e = tab[i]) != null; i = nextIndex(i, len)) {
        ThreadLocal<?> k = e.get();
        //1、清除脏 Entry
        if (k == null) {
            e.value = null;
            tab[i] = null;
            size--;
        } else {
            //对非脏 Entry 重新计算槽位
            int h = k.threadLocalHashCode & (len - 1);
            
            //h != i，即表示该 Entry 现在所在的槽位不是它应该在的槽位
            if (h != i) {
                tab[i] = null;
				//while 解决 hash 冲突查找槽位
                while (tab[h] != null)
                    h = nextIndex(h, len);
                tab[h] = e;
            }
        }
    }
    return i;
}
```



### 3.4、replaceStaleEntry()

replaceStaleEntry 直译为 “替换脏 Entry”

该方法的唯一调用位置是 set()，当发现 新 Entry（待插入） 应该插入的位置的 table[i].key == null，即表示是 脏 Entry，那么调用replaceStaleEntry() 进行清理，同时清理过程中将 新 Entry 给插入

清理过程：

1. 第一次 for，先从 i 位置开始往前找，直到 table[i] == null 停止，此次目的是为了 记录最前面的脏 Entry 的位置 slotToExpunge（设计思想：认为出现 脏 Entry 的相邻位置很大几率也会存在脏 Entry，因此向前查找，一举清理）
2. 第二次 for，从 i 位置开始往后查找，直到 table[i] == null 停止，此次目的是为了 查找后面是否存在 table[i].key == key，如果存在，表示原先已经插入过了，只不过是由于 hash 冲突的原因插入到了后面的位置，因此当找到后会将 i 位置处的脏 Entry 和 它进行调换，并且从第一个脏 Entry ，即**将 slotToExpunge 作为 cleanSomeSlots() 的起点，清理脏 Entry**
3. 如果第二次 for 中没有找到 table[i].key == key 的 Entry，那么表示不存在 旧 Entry 进行替换，那么直接在 i 位置处插入 新 Entry，替换掉 脏 Entry，然后 **将 slotToExpunge 作为 cleanSomeSlots() 的起点，清理脏 Entry**



比如下图：

当我们 set() 的时候，发现 新 Entry 应该插入的位置 i（staleSlot） = 4 是脏 Entry，那么往前找，由于 table[2] == null，所以记录 slotToExpunge = 3 为第一个脏 Entry，然后再从 i 位置开始往后面扫描，查找可替换的 旧 Entry，这里查找到 table[7] == null，没有查找到，因此直接在 table[4] 的位置插入 新 Entry，并且将 slotToExpunge = 3 作为 cleanSomeSlots() 的起点清理后面的 脏 Entry

![img](https://upload-images.jianshu.io/upload_images/2615789-f26327e4bc42436a.png?imageMogr2/auto-orient/strip|imageView2/2/w/737/format/webp)

```java
private void replaceStaleEntry(ThreadLocal<?> key, Object value,
                               int staleSlot) {
    Entry[] tab = table;
    int len = tab.length;
    Entry e;

    //for 循环，从 i 位置向前找到第一个脏 entry，直到 table[i] == null
    int slotToExpunge = staleSlot;
    for (int i = prevIndex(staleSlot, len);
         		(e = tab[i]) != null;
         		i = prevIndex(i, len)){
        //向前查找脏 Entry，更新脏 Entry 的位置
        if (e.get() == null){
1.          slotToExpunge = i;
        }
    }
        

    for (int i = nextIndex(staleSlot, len);
         (e = tab[i]) != null;
         i = nextIndex(i, len)) {
        ThreadLocal<?> k = e.get();

        if (k == key) {
            
            //如果在向后环形查找过程中发现 key 相同的 entry 就覆盖并且和 脏 entry 进行交换
2.            e.value = value;
3.            tab[i] = tab[staleSlot];
4.            tab[staleSlot] = e;

            //如果在查找过程中还未发现脏 entry，那么就以当前位置作为 cleanSomeSlots() 的起点
            if (slotToExpunge == staleSlot)
5.                slotToExpunge = i;
            //搜索脏 entry 并进行清理
6.            cleanSomeSlots(expungeStaleEntry(slotToExpunge), len);
            return;
        }

        //如果向前未搜索到脏 entry，则在查找过程遇到脏 entry 的话，
        //后面就以此时这个位置作为起点调用 cleanSomeSlots()
        if (k == null && slotToExpunge == staleSlot)
7.            slotToExpunge = i;
    }

    //如果在查找过程中没有找到可以覆盖的 entry，则将新的 entry 插入在脏 entry
8.    tab[staleSlot].value = null;
9.    tab[staleSlot] = new Entry(key, value);

10.    if (slotToExpunge != staleSlot)
        //调用 cleanSomeSlots()
11.        cleanSomeSlots(expungeStaleEntry(slotToExpunge), len);
}
```





### 3.5、getEntry()

当在第一个定位到的目标槽位中找不到目标 Entry 时，会调用 getEntryAfterMiss()

在 getEntryAfterMiss() 中会继续往后查找，因为它认为可能 Entry 由于 hash 冲突存储到后面的位置了

一直往后查找，直到 e == null 停止，在查找过程中

- 如果 talbe[i].key == key，那么查找完成，返回结果

- 如果 talbe[i].key == null，那么表示出现脏 Entry，调用 expungeStaleEntry() 进行清理并且对后面的 Entry 重新定位槽位

```java
private Entry getEntry(ThreadLocal<?> key) {
    int i = key.threadLocalHashCode & (table.length - 1);
    Entry e = table[i];
    if (e != null && e.get() == key)
        return e;
    else
        return getEntryAfterMiss(key, i, e);
}

private Entry getEntryAfterMiss(ThreadLocal<?> key, int i, Entry e) {
    Entry[] tab = table;
    int len = tab.length;

    while (e != null) {
        ThreadLocal<?> k = e.get();
        if (k == key)
            return e;
        if (k == null)
            expungeStaleEntry(i);
        else
            i = nextIndex(i, len);
        e = tab[i];
    }
    return null;
}
```



### 3.6、remove()

一直往后找，找到目标 Entry，调用 expungeStaleEntry() 进行脏 Entry 处理以及重新定位槽位

```java
private void remove(ThreadLocal<?> key) {
    Entry[] tab = table;
    int len = tab.length;
    int i = key.threadLocalHashCode & (len-1);
    for (Entry e = tab[i];
         e != null;
         e = tab[i = nextIndex(i, len)]) {
        if (e.get() == key) {
            e.clear();
            expungeStaleEntry(i);
            return;
        }
    }
}
```



## 4、Entry 为什么设计为 “弱引用”

> #### 如果使用的是强引用

如果不是继承弱引用，那么 Entry 的结果是这样的

```java
static class Entry {
    Object value;
    ThreadLocal<?> key;
    Entry(ThreadLocal<?> k, Object v) {
        key = k;
        value = v;
    }
}
```

那么 remove() 方法应该是这样的

```java
private void remove(ThreadLocal<?> key) {
    Entry[] tab = table;
    int len = tab.length;
    int i = key.threadLocalHashCode & (len-1);
    for (Entry e = tab[i];
         e != null;
         e = tab[i = nextIndex(i, len)]) {
        if (e.key == key) {
            //直接将 key 和 value 置空
        	e.key = null;
        	e.value = null;
            tab[i] = null;
            return;
        }
    }
}
```



其实归根结底，就是防止程序员使用 ThreadLocal ，而忘了 调用 remove() 方法，导致内存泄漏



> #### 弱引用 解决 内存泄漏



**1、使用弱引用的作用在哪？**

当程序员仅仅断开了 t 对 堆中 ThreadLocal 对象的引用，而没有去调用 remove() 的时候，Entry 对 ThreadLocal 对象是弱引用，对 value 是强引用，弱引用是每次 GC 只要不存在强引用就会被回收的，因此只要程序员的 t 引用断开了对 ThreadLocal 对象的强引用，这就默认是使用完了，不会再去使用了，这时候如果忘了调用 remove()，那么 GC 的时候也会回收掉这个 ThreadLocal 对象



**2、问题来了，value 呢？Entry 对它的是强引用啊，怎么回收？**

我们可以看出来，在 get()、remove() 末尾会调用 expungeStaleEntry(int slot) 对脏 Entry 进行处理，以及对后面的 Entry 重新定位槽位，在 set() 中会调用 replaceStaleSlot() 和 cleanSomeSlots() 处理脏 Entry

即如果某个位置上的 Entry 的 ThreadLocal 在用完没有调用 remove() ，那么在被 GC 回收后，由于 key == null，所以 value 是可能会被其他地方操作其他的 ThreadLocal 时 给清空的，但也只是仅仅减少了内存泄漏的大小和概率，如果后续线程没有再调用这三个方法 或者 调用的这三个方法没有涉及到内存泄漏的槽位，那么这些槽位上的 value 将会导致很长一段时间的内存泄漏

```java
个人感觉 ThreadLocal 使用弱引用是在考虑程序员有时候可能会忘记调用 remove()，使得内存泄漏
但是 弱引用 只能用来减少内存泄漏发生的概率而已，并不能完全的避免 ThreadLocal 造成的内存泄漏
```



**3、value 如果也设置为 弱引用，让它能够自己被 GC 回收可以吗？**

这显然是不可以的， value 没有外界的强引用，它只有 Entry 对它的强引用，如果将 value 设置为弱引用，那么后续只要发生 GC 就会将它回收了，那么对于程序来说，就是数据平白无故消失了





## 5、ThreadLocal 的应用场景



ThteadLocal 为多个线程提供了变量的副本，数据隔离，互不干扰



1、在读写锁中，由于读锁是共享的，同一时间段可以多个线程持有，所以需要记录各个线程读锁的重入次数，就使用这个 ThreadLocal 来记录的

2、ThreadLocal 保存全局变量，避免频繁的方法传参

3、Spring 使用 ThreadLocal，元素为 Map，存储的是 数据库连接池 和 connection 的映射，即每个线程 同时处理 不同的数据库连接池的 connection 的情况，获取 con 的时候，先通过 ThreadLocal 获取 map，再通过指定数据库连接池的类型来获取 "线程 + 数据库连接池" 唯一对应的 con