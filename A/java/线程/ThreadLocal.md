# ThreadLocal



## 1、ThreadLocal 底层原理

> ### Thread 类

```java
public class Thread implements Runnable {
    //维护了一个 ThreadLocal 里的 ThreadLocalMap 类
	ThreadLocal.ThreadLocalMap threadLocals = null;
    //....
}
```



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



可以看出，ThreadLocal 内部维护了一个 ThreadLocalMap

而 ThreadLocalMap 内部维护了一个 Entry 类，并且创建了一个 Entry[] table （跟 hashMap 实现一样的）



Entry 继承了弱引用，通过将 ThreadLocal 传给 WeakReference，将 ThreadLocal 使用弱引用封装起来，而 value 仍然是强引用

通过 Reference 内部的 get() 方法获取到 ThreadLocal



> ### 哈希冲突

ThreadLocalMap 对于 Entry 的维护使用的不是链表，这样的话发生哈希冲突就不是使用的链地址法，而是使用的开放地址法，直接找下一个空闲的位置

```java
private static int nextIndex(int i, int len) {
    //获取下一个位置，如果 i + 1 == len 那么就回到 索引为 0 的位置
    return ((i + 1 < len) ? i + 1 : 0);
}
```



> ### get()、set()、remove()

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
            expungeStaleEntry(i);
            return;
        }
    }
}

ThreadLocalMap getMap(Thread t) {
    return t.threadLocals;
}

```



## 2、如何做到线程间不共享

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



## 3、ThreadLocalMap 中 Entry 为什么继承弱引用

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

这其实就有点搞笑，但是面试问到还是要讨论，因为如果 ThreadLocal 没有这么设置，别人也不会去想到这个问题我觉得，因为强制必须去记得使用 remove()，很多 api 都有强制使用某些方法，才不会导致出现问题，不知道这个为什么这么特殊，还要这么设计



> ### 内存泄漏的发生

```java
ThreadLocal t = new ThreadLocal();
```

当我们使用的是强引用的话，我们上面创建了一个 ThreadLocal 对象，如果我们使用完后，仅仅只是调用 t = null ，这只是清除了 t 对 堆中 ThreadLocal  对象的引用，而线程内部还有 ThreadLocalMap 中 Entry 对 这个 ThreadLocal 对象的引用，这就导致了 我们使用完 ThreadLocal 后，由于存在这个强引用，而 GC 无法回收，需要等到 线程销毁才会进行回收，由于现在都是使用的线程池，线程都是进行复用的，一个线程可能很长时间都不会被销毁，这样就发生了内存泄漏

但是上面说了，如果使用的是强引用，可以直接在 remove() 中去除掉对 ThreadLocal 对象 和 value 的引用，让它们被 GC 回收



**但是，它实际使用的是弱引用，这就意味着是在考虑程序员有时候可能会忘记调用 remove()，使得内存泄漏，仅仅是因为这个原因而已，我感觉如此**



**那么使用弱引用的原理是什么呢？**

当程序员仅仅断开了 t 对 堆中 ThreadLocal 对象的引用，而没有去调用 remove() 的时候，Entry 对 ThreadLocal 对象是弱引用，对 value 是强引用，弱引用是每次 GC 只要不存在强引用就会被回收的，因此只要程序员的 t 引用断开了对 ThreadLocal 对象的强引用，这就默认是使用完了，不会再去使用了，这时候如果忘了调用 remove()，那么 GC 的时候也会回收掉这个 ThreadLocal 对象

**但问题来了，value 呢？它也已经使用完了，但 Entry 对它的是强引用啊，怎么回收？**

我们可以看出来，在 get()、set()、remove() 执行结尾，都会调用一个 expungeStaleEntry()，它的任务就是找出 key 为 null 的 Entry，然后将它的 value 置空，结束内存泄漏，并且它还有一个功能，由于使用的是线性探测法，因此本来放到 i = 1 位置的 Entry 由于哈希冲突放到了 i =  3 位置，而这时候 i = 1 位置被回收了，tab[1] = null 了，如果不进行处理，那么当我们查找 i = 3 位置的 Entry 的时候，定位到的 i = 1 为 null，就会导致有值，但查找不到，因此它会重新将后面的值进行 hash，计算位置

综上，我们可以发现，弱引用就是为了防止程序忘了调用 remove() 而产生内存泄漏，而因此，由于改成了弱引用，所以对应的 remove() 方法也发生了修改，是将对应的 弱引用 关系断开，方便后续 expungeStaleEntry() 进行 value 置空，加速内存回收

而实际上 remove() 调用和不调用都不会产生太大影响了，只是影响了内存回收的速率





## 4、ThreadLocal 的应用场景



ThteadLocal 为多个线程提供了变量的副本，数据隔离，互不干扰



1、在读写锁中，由于读锁是共享的，同一时间段可以多个线程持有，所以需要记录各个线程读锁的重入次数，就使用这个 ThreadLocal 来记录的

2、每个线程保存全局变量，避免传参的麻烦，同时可以跨层传参

- 比如在一个拦截器这里放入了 用户的 session，另一个拦截器直接 get() 获取 session 进行用户身份验证，这样两个毫无关联的拦截器就可以进行交流了，而且多个连接的时候，互不干扰，**注意用完后需要 remove()**
- 而且如果在 controller 层要传参到一个 dao 层的话，普通的传参的就是 controller -> service -> dao，复杂的业务场景跨的层数会更多，使用 ThreadLocal 可以解决这个问题



> ###  ThreadLocal 能用来管理 分布式 session 吗？

不能，因为

- 一是每次用完一个 session 后都会 remove()
- 二是就算不 remove()，在线程 A 中存储了 用户 1 的 session，由于使用多线程，是线程复用的，那么当这个线程再去处理 用户 2 的时候，存储了用户 2 的 session，又会覆盖 用户 1 的 session