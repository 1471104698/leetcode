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



**而在 Thread 的内部都维护了一个 ThreadLocalMap，内部存储的是自己的 ThreadLocal，由于线程之间的数据的是私有的，因此这就导致了 每个 Thread 的 ThreadLocal 之间互不可见**



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



## 2、ThreadLocalMap 中 Entry 为什么继承弱引用

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