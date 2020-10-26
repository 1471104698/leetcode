# ThreadLocal



## 1、ThreadLocal 底层架构



> ###  ThreadLocal 类

```java
public class ThreadLocal<T> {
    
    static class ThreadLocalMap {
        //Entry 继承了弱引用
        static class Entry extends WeakReference<ThreadLocal<?>> {
            Object value;
            Entry(ThreadLocal<?> k, Object v) {
                //将 ThreadLocal 传入到 WeakReference 中，使用弱引用封装起来
                super(k);
                value = v;
            }
        }
        private Entry[] table;
    }
    //...
}
```



> ### ThreadLocalMap 和 Entry

线程类 Thread 内部维护了一个 ThreadLocalMap，即每个线程都有各自的一个 ThreadLocalMap

在 ThreadLocalMap 内部维护了一个 table 数组，元素类型为 Entry

Entry 继承了弱引用，并且内部再维护了一个 value 变量

```java
static class ThreadLocalMap {

    static class Entry extends WeakReference<ThreadLocal<?>> {
        Object value;
        Entry(ThreadLocal<?> k, Object v) {
            //将 ThreadLocal 封装为 弱引用 内部值，调用 get() 获取该值
            super(k);
            value = v;
        }
    }
    private Entry[] table;
    
}
```

即当使用 entry.get() 时使用的是 弱引用的方法，返回的是内部维护的 ThreadLocal



> ### 哈希冲突

ThreadLocalMap 对于 Entry 的维护使用的不是链表，这样的话发生哈希冲突就不是使用的链地址法，而是使用的开放地址法，直接找下一个空闲的位置

```java
private static int nextIndex(int i, int len) {
    //获取下一个位置，如果 i + 1 == len 那么就回到 索引为 0 的位置
    return ((i + 1 < len) ? i + 1 : 0);
}
```



## 2、ThreadLocal 四大方法

> ### get()

```java
public T get() {
    //获取当前线程
    Thread t = Thread.currentThread();
    //获取线程的 ThreadLocalMap
    ThreadLocalMap map = getMap(t);
    if (map != null) {
        //以当前 theadLocal 作为 key，获取对应的 Entry
        ThreadLocalMap.Entry e = map.getEntry(this);
        if (e != null) {
            //entry 不为空，那么返回内部的值
            T result = (T)e.value;
            return result;
        }
    }
    //到这里表示 this 没有对应任何 value，那么调用 setInitialValue() 创建 value 进行对应
    return setInitialValue();
}
private Entry getEntry(ThreadLocal<?> key) {
    int i = key.threadLocalHashCode & (table.length - 1);
    //遍历 Map，调用每个 entry 的 get() 获取对应的 ThreadLocal，判断是否是目标 key
    Entry e = table[i];
    if (e != null && e.get() == key)
        return e;
    else
        /*
        如果 想要查找的 key 不存在，那么意味着可能是被 GC 回收掉了，
        那么需要进行后置处理，将 value 置空 以及 对后面的 Entry 重新进行 hash 定位
        */
        return getEntryAfterMiss(key, i, e);
}

private Entry getEntryAfterMiss(ThreadLocal<?> key, int i, Entry e) {
    Entry[] tab = table;
    int len = tab.length;

    while (e != null) {
        ThreadLocal<?> k = e.get();
        if (k == key)
            return e;
        //对应位置上的 key 为空，那么可能是 GC 回收，那么进行 value 置空 以及 对后面的 Entry 重新进行 hash 定位
        if (k == null)
            expungeStaleEntry(i);
        else
            i = nextIndex(i, len);
        e = tab[i];
    }
    return null;
}
```



> ### set()

```java
public void set(T value) {
    //获取当前线程
    Thread t = Thread.currentThread();
    //获取线程的 ThreadLocalMap
    ThreadLocalMap map = getMap(t);
    if (map != null)
        //将 当前 threadLocal 作为 key，将传入的 value 作为 值
        map.set(this, value);
    else
        createMap(t, value);
}
private void set(ThreadLocal<?> key, Object value) {

    Entry[] tab = table;
    int len = tab.length;
    int i = key.threadLocalHashCode & (len-1);
	/*
		遍历 map 中的所有 Entry，调用每个 Entry 的 get() 获取对应的 ThreadLocal，判断是否是目标 key
		如果是则进行 value 替换
		如果 map 中不存在此 Key，那么插入到 map 中，此时如果存在 hash 冲突，那么使用开放地址法，往后找一个为空的位置插入
	*/
    for (Entry e = tab[i];
         e != null;
         e = tab[i = nextIndex(i, len)]) {
        ThreadLocal<?> k = e.get();

        if (k == key) {
            e.value = value;
            return;
        }

        if (k == null) {
            
            replaceStaleEntry(key, value, i);
            return;
        }
    }

    tab[i] = new Entry(key, value);
    int sz = ++size;
    if (!cleanSomeSlots(i, sz) && sz >= threshold)
        //扩容
        rehash();
}
```



> ### remove()

```java
public void remove() {
    //获取线程对应的 ThreadLocalMap
    ThreadLocalMap m = getMap(Thread.currentThread());
    if (m != null)
        //调用 ThreadLocalMap 的 remove()
        m.remove(this);
}
private void remove(ThreadLocal<?> key) {
    Entry[] tab = table;
    int len = tab.length;
    int i = key.threadLocalHashCode & (len-1);
    for (Entry e = tab[i];
         e != null;
         e = tab[i = nextIndex(i, len)]) {
        if (e.get() == key) {
            e.clear();
            /*
            找到目标 key，进行移除
            并且需要进行后置处理，将 value 置空 以及 对后面的 Entry 重新进行 hash 定位
            */
            expungeStaleEntry(i);
            return;
        }
    }
}
```





> ### expungeStaleEntry()

expungeStaleEntry() 在 get()、set()、remove() 中的方法末尾都会进行调用

expungeStaleEntry() 有两个作用：

- 将某个 Entry 的 value 进行置空，解决内存泄漏

- 对该 Entry 后面的连续不为空的 Entry 重新进行 hash 定位 顺便 value 置空，避免由于开放地址法产生了 hash 冲突的 Entry 在后续查找不到了

  - ```java
    
    如果该位置上的 Entry 还存活着，那么重新进行 hash，因为该位置上的值可能是因为开放地址法到这里来的
    所以需要重新计算 hash
        比如 存在数组 1	2	3	4
        A hash 计算后到达的是 1 号位置，没有 hash 冲突，直接放入
        1 	2 	3 	4
        A
        当 B hash 后到达的是 1 号位置，但是发生 hash 冲突，所以需要放到 2 号位置
        1 	2 	3 	4
        A	B
        当 C hash 后到达的是 1 号位置，但是发生 hash 冲突，查看 2 号位置也发生 hash 冲突，所以需要放到 3 号位置
        1 	2 	3 	4
        A	B	C
        当 D hash 后到达的是 1 号位置，后续 2、3 号位置都发生 hash 冲突，所以放到 4 号位置
        1 	2 	3 	4
        A	B	C	D
    
        这样查找 D 的话，到达 1 号位置，发生冲突，到达 2、3 号位置都发生冲突，因此会到达 4 号位置，查找完毕
        如果 A remove() 掉了，那么这意味着 1 号位置为空
        1	2	3	4
        	B	C	D
        如果不进行处理的话，那么 B、C、D 最开始都会 hash 到 1 号位置，这样的话发生为空就默认 对应的 Entry 不存在
        因此出现了错误查询
    
    ```

    

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
        //如果对应槽位上的 key 为空，即 ThreadLocal 已经被回收了，那么将 value 置空，解决内存泄漏
        if (k == null) {
            e.value = null;
            tab[i] = null;
            size--;
        } else {
            
            int h = k.threadLocalHashCode & (len - 1);
            if (h != i) {
                tab[i] = null;

                //重新查找一个位置，while() 直到找到为止
                while (tab[h] != null)
                    h = nextIndex(h, len);
                //插入
                tab[h] = e;
            }
        }
    }
    return i;
}
```



## 3、如何做到线程间不共享

最开始我认为是因为每个线程维护一个 ThreadLocalMap，这个 ThreadLocalMap 是线程私有的，所以才不会共享

实际上想得有点浅了



因为当我看到说 业务上一般 创建 ThreadLocal 实例都是使用 static 修饰，即一个类只有一个 ThreadLocal 实例，然后多线程共享，我就感觉不太对劲了

```java
public class A{
	//static 修饰
    private static ThreadLocal<Integer> threadLocal = new ThreadLocal();
    
    public void h(){
        //...
    }
}
```

如果按照我自己的理解，应该是使用一次就进行创建一个新的 ThreadLocal 实例，如果这样进行复用，那么怎么不是会导致一个 ThreadLocal 对应很多 value 么？？？或者说 一个 ThreadLocal  对应的 value 不是会被其他线程覆盖么？？？

从这里就可以看出来，我其实没有搞懂 ThreadLocal 的存储方式



我们需要先知道，ThreadLocal 它仅仅是作为 key 存在的，value 并不是存储在它里面，而是存储在 Entry 中，ThreadLocal 仅仅是用来与 value 进行映射的

多个线程复用一个 ThreadLocal 意味着什么？意味着它们都在使用同一个 ThreadLocal 对象作为 key

由于我们说了，每个线程都有自己的 一个 ThreadLocalMap，而 ThreadLocal 仅仅是作为 key 来映射 value 的

因此，对于多个线程来说，在它们的 ThreadLocalMap 中，即使 作为 key 的 ThreadLocal 是同一个对象，但是在内部映射出来的 value 是不同的，是相互独立的，因为作为载体的 Entry 是不同的对象，它们只是使用了同一个 ThreadLocal 作为 key 罢了



当然，上面使用 static 修饰 ThreadLocal 主要了为了复用 ThreadLocal ，避免 创建 够多的 ThreadLocal 对象，因为按照这个 ThreadLocal 的这个设计，在只需要存储一个数据的情况下，多个线程只需要使用 一个 ThreadLocal  对象就可以了



## 4、ThreadLocalMap 中 Entry 为什么继承弱引用

> ### 如果使用的是强引用

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



> ### 弱引用 解决 内存泄漏



**1、使用弱引用的作用在哪？**

当程序员仅仅断开了 t 对 堆中 ThreadLocal 对象的引用，而没有去调用 remove() 的时候，Entry 对 ThreadLocal 对象是弱引用，对 value 是强引用，弱引用是每次 GC 只要不存在强引用就会被回收的，因此只要程序员的 t 引用断开了对 ThreadLocal 对象的强引用，这就默认是使用完了，不会再去使用了，这时候如果忘了调用 remove()，那么 GC 的时候也会回收掉这个 ThreadLocal 对象



**2、问题来了，value 呢？Entry 对它的是强引用啊，怎么回收？**

我们可以看出来，在 get()、set()、remove() 执行结尾，都会调用一个 expungeStaleEntry(int slot)，进入到该方法内，还附带一个 slot 参数，即是 slot 指定位置上的 value 进行置空，然后对后面连续不为空的 Entry 重新进行 hash 定位，顺便清空 key 为空的 Entry 的 value

即如果某个位置上的 Entry 的 ThreadLocal 在用完没有调用 remove() 进行清空 value，那么在被 GC 回收后，value 是可能会被其他地方操作其他的 ThreadLocal 时 给清空的，但也只是仅仅减少了内存泄漏的大小和概率，如果后续调用的 这三个方法 没有涉及到这些槽位，或者 后续很长时间不再调用 这三个方法，那么这些槽位上的 value 将会导致很长一段时间的内存泄漏

```java
个人感觉 ThreadLocal 使用弱引用是在考虑程序员有时候可能会忘记调用 remove()，使得内存泄漏
但是 弱引用 只能用来减少内存泄漏发生的概率而已，并不能完全的避免 ThreadLocal 造成的内存泄漏
```



**3、value 如果也设置为 弱引用，让它能够自己被 GC 回收可以吗？**

这显然是不可以的， value 没有外界的强引用，它只有 Entry 对它的强引用，如果将 value 设置为弱引用，那么后续只要发生 GC 就会将它回收了，那么对于程序来说，就是数据平白无故消失了





## 5、ThreadLocal 的应用场景



ThteadLocal 为多个线程提供了变量的副本，数据隔离，互不干扰



1、在读写锁中，由于读锁是共享的，同一时间段可以多个线程持有，所以需要记录各个线程读锁的重入次数，就使用这个 ThreadLocal 来记录的

2、每个线程保存全局变量，避免传参的麻烦，同时可以跨层传参

- 比如 session 的获取，对于一般我们是使用 HttpServletRequest 来获取请求头内部的 session 的，这样每个需要 session 的接口都需要传输 HttpServletRequest 参数，这种问题可以使用 ThreadLocal 解决，在拦截器拦截了请求的时候，获取内部的 session 然后再存储到 ThreadLocal 中，这样在任何一个接口都可以直接获取，注意使用完要 remove()

3、在数据库连接池中，将 开启事务的线程 和 Connection 连接进行绑定，保证多次 sql 获取的都是同一个 Connection