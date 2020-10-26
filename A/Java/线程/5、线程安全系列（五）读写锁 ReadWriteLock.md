# 读写锁 ReadWriteLock



## 1、ReadWriteLock 的使用场景

ReadWriteLock 主要是用在读多写少的场景，因为普通的锁无论是读还是写，一次都只能一个线程执行

而读写锁能够保证 多个线程并发的读，而写还是一个线程，因此在多读的情况下最好使用读写锁



## 2、ReadWriteLock 的前置知识

首先主流的一种读写锁：ReentrantReadWriteLock 也是通过 AQS 来实现的，通过 state 变量来进行控制

state 是一个 int 型变量，32 位，高 16 位表示读锁的情况，低 16 位表示写锁的情况



比如现在要加一把读锁：

- 通过 state & 0x0000ffff 来获取写锁的状态，如果 结果为 0，表示不存在写锁
- 通过 state + 1 << 16 给 高 16 位 +1，表示加上一把读锁

比如现在要加一把写锁：

- 通过 state >>> 16 来获取读锁的状态，如果结果为 0，表示不存在读锁
- 通过 state + 1 给 低 16 位 +1，表示加上一把写锁

读锁一次可以有多把，而写锁一次只能有一把，但是**可以重入**



## 3、锁升级 和 锁降级

读写锁不支持锁升级，但支持锁降级

锁升级：一个线程在没有释放读锁的情况下，又去获取一把写锁

锁降级：一个线程在没有释放写锁的情况下，又去获取一把读锁



为什么不支持锁升级？看以下代码

```java
pubic void h(){
    ReadWriteLock lock = new ReentrantReadWriteLock();
    Lock readLock = lock.readLock();
    Lock writeLock = lock.writeLock();
	
    //获取读锁
	readLock.lock(); //代码①
    try{
        //业务逻辑
        //....
        //获取写锁
        writeLock.lock();	//代码②
		//...        
        writeLock().unlock();
    }finally{
        readLock.unlock();
    }
}
```

上面代码，假设有两个线程同时执行 h()，线程 A 先执行到 代码① 获取到读锁，然后 线程 B 接下来也执行到 代码①，由于读锁是共享的，所以 线程 B 也能够获取读锁，后面当 线程 A 执行到 代码② 的时候，发现有其他线程占用了读锁，由于不同线程间的读写锁是互斥的，那么 线程 A 就会进入阻塞状态，等待其他线程释放读锁，而同时，线程 B 也执行到 代码②，获取写锁，同理由于其他线程获取了读锁，所以会陷入阻塞，这样 线程 A 和 线程 B 都会阻塞在 代码②，导致死锁



锁降级是 线程 A 会先获取写锁，这样的话别的线程就无法获取读锁和写锁，线程 A 内部自己可以再获取读锁，不会存在阻塞问题

锁降级主要是为了保证没有脏读的情况，以下的官方文档的 锁降级代码

```java
 class CachedData {
     //真正使用的数据
   Object data;
     //用于实现可见性的变量
   volatile boolean flag;
   final ReentrantReadWriteLock rwl = new ReentrantReadWriteLock();

   void processCachedData() {
       //获取读锁
     rwl.readLock().lock();
     if (!flag) {
         //释放读锁
        rwl.readLock().unlock();
         //获取写锁
        rwl.writeLock().lock();
        try {
            //修改数据
          if (!flag) {
            data = ...
                //将修改的数据全部刷新回内存，并让其他线程的缓存失效
            flag = true;
          }
            //获取读锁
          rwl.readLock().lock();
        } finally {
            //释放写锁
          rwl.writeLock().unlock(); // Unlock write, still hold read
        }
     }
	
     try {
         //使用修改的数据
       use(data);
     } finally {
         //释放读锁
       rwl.readLock().unlock();
     }
   }
 }
```

在上面的代码中，在修改完数据 data，然后在释放写锁前加读锁是为了后续的 use(data) 过程中 data 不被其他线程修改，同时保证了并发效率，这个锁降级不是必须的，如果不想使用锁降级：

- 在调用 use(data) 时继续保持写锁，用完再释放
- 先释放写锁，然后再获取读锁 调用 use(data)

但是存在问题

- 如果继续持有写锁，那么如果 use(data) 的时间过长的话，其他线程都无法进行操作，而 use(data) 仅仅是进行读操作而已，这样导致其他读线程也无法执行，并发效率降低
- 如果先释放锁再获取读锁，那么在获取读锁前，可能先被其他线程争夺到了写锁，将 data 数据修改了，由于 data 不是 volatile 变量，所以导致后续使用的 data 与 内存中的 data 不一致出现脏读，就算给 data 加上 volatile 保证可见性，但是可能还是跟自己要的逻辑不一致，因为 data 不是自己修改的那个值，这个就看业务能不能接受了。并且释放写锁，由于后面本来也是要进行读操作的，后续还需要去跟需要写的线程竞争读锁，增加了不需要的锁竞争



## 4、ReentrantReadWriteLock 原理



**主要 api**

```java
public class ReentrantReadWriteLock implements ReadWriteLock {
    //读锁
    private final ReentrantReadWriteLock.ReadLock readerLock;
    //写锁
    private final ReentrantReadWriteLock.WriteLock writerLock;
    //读写锁操作的就是这个 Sync，公平和非公平主要看 sync 指向的对象
    final Sync sync;


    public ReentrantReadWriteLock(boolean fair) {
        sync = fair ? new FairSync() : new NonfairSync();
        //读写锁内部操作的就是 sync
        readerLock = new ReadLock(this);
        writerLock = new WriteLock(this);
    }

    /*
    Sync 继承 AQS，是一个抽象类
    当指定公平锁时，sync = new FairSync()
    当指定公平锁时，sync = new NonfairSync()
    */
    abstract static class Sync extends AbstractQueuedSynchronizer {}
    //公平锁
    static final class FairSync extends Sync {}
    //非公平锁
    static final class NonfairSync extends Sync {}


    //state >>> 16，获取读锁的值
    static int sharedCount(int c)    { return c >>> 16; }
    //state & 0x0000ffff，获取写锁的值
    static int exclusiveCount(int c) { return c & 0x0000ffff; }
```



**写锁的获取**

acquire() -> tryAcquire()

```java
public final void acquire(int arg) {
    if (!tryAcquire(arg) &&
        //上面获取失败，那么进入 AQS 队列
        acquireQueued(addWaiter(Node.EXCLUSIVE), arg))
        selfInterrupt();
}
protected final boolean tryAcquire(int acquires) {
  	//获取当前线程
    Thread current = Thread.currentThread();
    //获取 state
    int c = getState();
    //获取写锁的值
    int w = exclusiveCount(c);
    if (c != 0) {
	    /*
	    1、如果写锁为 0 而 state != 0 表示存在读线程
	    2、如果写锁不为 0，但是占有锁的线程不是自己，那么不能重入
	    */
        if (w == 0 || current != getExclusiveOwnerThread())
            return false;
        //判断写锁数据是否超标
        if (w + exclusiveCount(acquires) > MAX_COUNT)
            throw new Error("Maximum lock count exceeded");
        //上面没有返回表示当前线程已经占有写锁了，所以重入度 +1
        setState(c + acquires);
        return true;
    }
    //判断是否能够获取，writerShouldBlock() 内部调用 hasQueuedProcess()，如果可以，那么尝试一次 CAS
    if (writerShouldBlock() ||
        !compareAndSetState(c, c + acquires))
        return false;
    //获取成功，将锁占有者设置为当前线程
    setExclusiveOwnerThread(current);
    return true;
}
```





**读锁的获取** 

这里由于读锁是多个线程共享的，所以记录各个线程重入的次数，所以涉及到两个新的 api

ThreadLocalHoldCounter readHolds 和 HoldCounter holdCounter

可以看出 readHolds 是一个 ThreadLocal 变量，存储的是 HoldCounter ，即在 Entry 中 key 是 readHolds，而 value 是 HoldCounter ，HoldCounter 用于存储该线程的重入度

```java
static final class ThreadLocalHoldCounter extends ThreadLocal<HoldCounter> {
    /*
    调用链： ThreadLcaol 中 get() -> setInitialValue() -> initialValue()
    在这里生成一个 HoldCounter 对象，返回给 setInitialValue()，然后跟 ThreadLocal 组成 key-value
    */
    public HoldCounter initialValue() {
        return new HoldCounter();
    }
}
static final class HoldCounter {
    int count = 0;
    final long tid = getThreadId(Thread.currentThread());
}

private transient ThreadLocalHoldCounter readHolds;
private transient HoldCounter holdCounter;
```

在 ThreadLocalHoldCounter 中有一个 initialValue() 方法，该方法在 ThreadLocal 中被 setInitialValue() 调用

而 setInitialValue() 在 get() 中被调用

即 readHolds 调用 get() 获取对应的 value，如果为空，那么就会调用 setInitialValue()，里面再调用 ThreadLocalHoldCounter 中的 initialValue() 创建一个 HoldCounter 存储进去

```java
public class ThreadLocal<T> {
    public T get() {
        Thread t = Thread.currentThread();
        ThreadLocalMap map = getMap(t);
        if (map != null) {
            ThreadLocalMap.Entry e = map.getEntry(this);
            if (e != null) {
                @SuppressWarnings("unchecked")
                T result = (T)e.value;
                return result;
            }
        }
        //Map 中没有 对应的 key，调用 setInitialValue() 创建值
        return setInitialValue();
    }

    private T setInitialValue() {
        //调用 ThreadLocalHoldCount 中的 initialValue()，返回一个 
        T value = initialValue();
        Thread t = Thread.currentThread();
        ThreadLocalMap map = getMap(t);
        if (map != null)
            map.set(this, value);
        else
            createMap(t, value);
        return value;
    }
}
```



acquireShared() -> tryAcquireShared()

```java
public final void acquireShared(int arg) {
    if (tryAcquireShared(arg) < 0)
        //获取失败，进入 AQS 队列
        doAcquireShared(arg);
}

protected final int tryAcquireShared(int unused) {

    //获取当前线程
    Thread current = Thread.currentThread();
    //获取 state
    int c = getState();
    /*
    如果已经有线程获取了写锁，并且线程不是自己，那么返回
    这里体现了锁降级，如果有线程获取写锁，并且这个线程是自己的话，那么还会继续往下执行获取读锁的逻辑
    */
    if (exclusiveCount(c) != 0 &&
        getExclusiveOwnerThread() != current)
        return -1;
    //获取读锁个数
    int r = sharedCount(c);
    if (!readerShouldBlock() &&
        r < MAX_COUNT &&
        //CAS 获取读锁
        compareAndSetState(c, c + SHARED_UNIT)) {
        //如果当前线程是第一个获取读锁的线程，那么将 firstReader 指向当前线程
        if (r == 0) {
            firstReader = current;
            firstReaderHoldCount = 1;
        } else if (firstReader == current) {
            firstReaderHoldCount++;
        } else {
            
            HoldCounter rh = holdCounter;
            //如果 holdCounter 为空，或者 不是当前线程的 holdCounter，那么调用 get() 获取，没有的话就在内部创建一个
            if (rh == null || rh.tid != getThreadId(current))
                holdCounter = rh = readHolds.get();
            else if (rh.count == 0)
                readHolds.set(rh);
            //该线程读的重入度 +1
            rh.count++;
        }
        return 1;
    }
    //这里面在尝试获取
    return fullTryAcquireShared(current);
}
```



# 