# AQS + ReentrantLock + 信号量 + CycleBarrier

## 1、乐观锁和悲观锁



> ### 乐观锁 和 悲观锁 的概念



**悲观锁：**

将事物都想得很悲观，每个线程拿数据的时候都认为自己使用这个数据的期间别的线程都会进行修改，因此对这个数据加上锁，对别的线程进行排斥，比如 mysql 的写锁、表锁、行锁， 以及 java 中的  synchronized 和 ReentrantLock 等独占锁都是悲观锁的实现

悲观锁适用于多写的环境，因为这样才保证每次只有一个线程在修改，确保了数据的准确性



**乐观锁：**

将事物都想得很乐观，每个线程获取数据的时候都认为别人不会修改，因此不会对数据加锁，只会在写数据的时候判断数据是否被修改过，像 数据库中的 加个字段的版本控制 和 另一种 CAS 算法 都是乐观锁的实现，而 java 中 `AtomicIntger` 等原子类就是 volatile 修饰的 int 变量 + CAS

乐观锁不会对数据加锁，因此可以存在多个线程同时修改的情况，如果一直存在冲突，比如存在 n 个线程同时修改同个数据，那么最终只有一个线程会修改成功，其他的线程都需要进行自旋，获取最新的版本号进行重试，每次只有一个线程成功，效率过低

因此乐观锁适用于多读的情况



> ### 乐观锁的两种实现



这里需要说下，乐观锁在冲突的情况下，都需要进行自旋重试（重新获取版本号之类的，然后重新提交自己更新的数据）



**版本号控制：**

像 mysql 中同个给表添加一个 `version` 字段来代替 行锁 之类的悲观锁

当事务 A 和 事务 B 同时修改某个数据，而这个数据的初始版本号是 1 ，当事务 A 修改完率先提交，将版本号修改为 2，那么当事务 B 提交的时候，发现该数据的版本号跟自己持有的数据版本号不一样，那么就意味着被其他事务修改过了，那么就需要重新获取版本号来进行修改



**CAS 算法：**

CAS 即 compare And Swap（比较和交换）

它需要 3 个值：内存值 V，线程保存的旧值 A，需要写入的新值 B

其实跟 版本号控制差不多，感觉版本号控制就是 CAS 的一种实现，不过别人分出来了就不管了

**过程：**

假设存在一个变量 x，当线程 1 获取这个 x 的值保存为 A ，然后经过一系列的处理，需要将这个 变量 x 该为 值 B

这时候不能直接写入内存，而是需要比较 内存中 变量 x 的值 V 与之前获取它的时候的值 A 是否一致，如果一致，表示没有被其他线程修改过（其实不一定，这是 ABA 问题），可以直接将 x 更新为 B，如果不一致，那么就表示被其他线程修改过了，那么通过自旋不断重试



**CAS 的缺点：**

- 存在 ABA 问题
- 在存在冲突的情况下会自旋重试，而自旋的时候是不会主动放弃 CPU 的，那么当存在多个线程冲突的时候，CPU 效率低得一批



> ### 什么是 ABA 问题？

比如 线程 1  和 线程 2 同时要修改某个变量 x，当然，这是并发的，有先后顺序的，线程1 先被 CPU 调用，将 x 的值从 A 变成了 B，然后这时插进来 线程 3，CPU 没去调用线程 2，而是调用了 线程 3，而线程 3 将 x 的值 重新修改为 A，这时候 CPU 调用线程 2，线程 2 发现 x 在内存中的值 V 跟自己保存的旧值一样都是 A，那么感觉没人修改过，那么直接将 自己的新值 C 写入内存中

但实际上，在 线程 2 修改之前，就已经存在两个线程对变量 x 进行了修改，只是最终改回了原来的值，让线程 2 无法感知到它的变化

这就是 ABA 问题，表示从 A 修改为了 B，再从 B 修改回 A，这个中间过程不被感知



举个生活例子：

桌上有一杯水，它是满的，杯子的主人不在，当 人类 A 路过时，实在渴得不得了了，过来把它喝光了，然后就把空杯子放下走了

然后过来一个人类 B，发现杯子空了，很不爽，就把自己随身携带的水给杯子满上，然后就走了

当杯子的主人来的时候，看见杯子是满的，那么他不会想到有人喝了它的水再将它满上了，所以他放心地喝了

当然，他在他不知情的情况下，喝下了别人炙热的口水



> ### 如何 解决 ABA 问题？

通过添加一个版本号，（这就更像版本号控制那个了），之后的比较就不再是比较什么数据的内存值和旧值了，而是比较版本号了

上面的例子中，线程 1 修改了变量 x，将版本号 +1，之后线程  2 发现版本号不对，自然就感知到了 x 被修改过了，那么自旋重新修改



> ### ABA 问题有什么危害？

我们这么看老来，线程 1 修改了 x 值 从 A 变成 B，线程 3 又将 x 值从 B 变成 A，对于线程 2 来说，是需要将 x 值从 A 变成 C 的，那么这个 x 值最终是要变成 C 的，好像也没什么多大问题啊

这么看来是的，但是有的时候，这个中间态就可能产生不一样的影响，然而没有具体的例子，，，啧啧，这个 ABA 问题难点就在于没有一个实际的业务场景上的问题，有点理想化了





## 2、AQS

AQS 全称为 AbstractQueuedSynchronizer，它是一个抽象类，意味着自己某些方法有实现，有些方法需要给子类实现。

它内部维护了一个 同步双向队列，存储阻塞的线程的 Node 节点



主要 api 有 acquire()、tryAcquire()、acquireQueued()、addWaiter()、enq()、tryRelease()

其中 tryAcquire()、tryRelease() 是一个抽象方法，需要子类去实现

```java
注意，AQS 内部并不存在 lock() 方法，因为 lock() 是用来获取锁的，而它的子类实现并不一定都是锁，比如 信号量，它只需要操作 state 变量即可，因此不需要用到 锁，而 ReentrantLock 则需要用到锁，所以 lock() 是在 ReentrantLock 内部自己定义的

因此，我们需要知道，不要因为 ReentrantLock 用多了 就 认为 AQS 是为 ReentrantLock 服务的，其实不然，AQS 只是提供了一套维护阻塞线程 和 对某个状态值 state 进行 CAS 操作的模板而已

由于在 AQS 中并没有锁的概念，因此定义的 tryAcquire() 并不是为了获取锁的，而是用来满足避免进入 同步队列 或者 满足 Node 节点出队的逻辑，比如 ReentrantLock 自己实现的 tryAcquire()，就是修改 state 成功，如果第一次成功了，那么就可以不用进入同步队列，如果是在同步队列中成功了，表示获取到锁，那么就可以出队了，因此这个 tryAcquire() 代表的仅仅是一个逻辑而已，具体代表的含义是看子类是用来干什么的
    
而 ReentrantLock 的锁的实现就是通过 操作 state 的方法 和 维护阻塞线程的队列，因此锁的逻辑需要自己写，然后再利用 AQS 操作 state 和 维护同步队列
而 Semaphore 并不需要锁，它需要的仅仅是操作 state 的方法 以及 维护阻塞线程的队列 而已，刚好 AQS 就满足，因此不需要重写什么大方法，直接使用 AQS 即可，显得较为方便
```

简单说，AQS 就是定义了一套操作 state 状态值 和 维护阻塞线程的同步队列的 模板，需要对应功能而不想重复造轮子的可以直接使用 AQS



> ### acquire()

```java
public final void acquire(int arg) {
    //调用子类的 tryAcquire() 实现逻辑,大部分主要是尝试修改 state，如果失败那么就将线程入队，并且尝试出队
    if (!tryAcquire(arg) &&
        acquireQueued(addWaiter(Node.EXCLUSIVE), arg))
        selfInterrupt();
}
```

调用子类的 tryAcquire() 的实现逻辑，如果失败那么调用 acquireQueued(addWaiter(Node.EXCLUSIVE)))，addWaiter 内部有一个 enq()

方法逻辑为 addWaiter() -> enq() -> acquireQueued()

addWaiter() 是将线程封装为 Node 节点，然后内部调用 enq() 入队，入队完成后将 node 节点返回，并传给 acquireQueued() 在队列中 自旋 调用 tryAcquire() 尝试 出队 的逻辑（对于 ReentrantLock 这个类就是获取锁的逻辑）



> ### Node 入队 enq 代码



```java
private Node addWaiter(Node mode) {
    Node node = new Node(Thread.currentThread(), mode);
    enq(node);
    return node;
}

private Node enq(final Node node) {
    //开始自旋
    for (;;) {
        Node t = tail;
        if (t == null) { // Must initialize
            // 如果tail为空,则新建一个head节点,并且tail指向head
            if (compareAndSetHead(new Node()))
                tail = head;
        } else {
            node.prev = t;
            // tail不为空,将新节点入队
            if (compareAndSetTail(t, node)) {
                t.next = node;
                return t;
            }
        }
    }
}
```



入队的逻辑就是使用 CAS 自旋，由于一次可能有两个线程 node 节点需要入队，因此需要 CAS 自旋进行尾部插入，一次只能有一个节点插入成功，当前轮次插入失败的，会进入下一轮循环，获取最新的 tail，然后继续 CAS 插入，直到成功为止



> ### Node 出队 acquireQueued 代码

```java
final boolean acquireQueued(final Node node, int arg) {
    boolean failed = true;
    try {
        boolean interrupted = false;
        for (;;) {
            /*
            当前线程将 node 添加到 AQS 队列中，并调用的这个方法竞争资源（锁）
            它只关心 node 节点，因此它一直在获取 node 的前驱节点，判断是否轮得到它获取资源（锁）
            如果它的前驱节点是 head，那么表示它可以获取资源（锁）了
            调用 tryAcquire() 执行获取资源（锁）的逻辑
            如果获取成功了，那么就出队
            */
            final Node p = node.predecessor();
            if (p == head && tryAcquire(arg)) {
                setHead(node);
                p.next = null; // help GC
                failed = false;
                return interrupted;
            }
            if (shouldParkAfterFailedAcquire(p, node) &&
                parkAndCheckInterrupt())
                interrupted = true;
        }
    } finally {
        if (failed)
            cancelAcquire(node);
    }
}
```



AQS 有一个头节点 head，它单纯的作为一个 dummy 指针

在队列中的节点通过 自旋来尝试 出队 逻辑，自旋过程如下：

- 判断自己的前驱节点是否是 head，如果是，表示轮到自己进行尝试出队了（获取锁），那么调用 tryAcquire() 尝试出队
  - 这里为什么 node 节点是队首节点却不能保证出队成功呢？因为有可能是非公平实现，那么可能其他线程插队率先操作成功
- 当出队逻辑方法返回 true 后，调用 `setHead(node)` 方法，将 node 作为一个新的 dummy 节点



**我们可以看出，AQS 的非公平是仅仅表示队列外 和 队列内的竞争，而队列内部的节点竞争是公平的，按照先来后到的顺序执行出队逻辑**



> ### 用于公平锁中的 hasQueuedPredecessors()

```java
public final boolean hasQueuedPredecessors() {
    //获取尾节点
    Node t = tail; 
    //获取头节点
    Node h = head;
    Node s;
    /*
    如果 h != t 即队列中还有元素，并且 h.next 线程不是当前线程，那么返回 true
    意味着如果某个线程调用 tryAcquire() 获取资源，如果队列中有元素并且 head.next 不是当前线程，那么这里返回 true
    实现公平锁，使得该线程获取资源失败
    */
    return h != t &&
        ((s = h.next) == null || s.thread != Thread.currentThread());
}
```

​	 该方法一般用于在 tryAcquire() 使用 CAS 获取资源前 判断队列中是否有元素存在
​     如果有的话，判断 头节点 是否是当前线程，因为可能是队列中的节点调用的 tryAcquire() ，所以需要这个判断
​     如果不是 则 获取失败，防止 某个线程 插队 在第一次尝试 CAS 获取的时候就获取到资源





## 3、ReentrantLock

**ReentrantLock = AQS + tryAcquire() + tryRelease()**

ReentrantLock 的一个内部类 Sync继承了 AQS，同时重写了 AQS 的 tryAcquire() 和 tryRelease() 方法，通过重写这两个方法，制定了这两个方法所代表的含义：获取锁 和 释放锁

并且 ReentrantLock 内部自己定义了两个方法 lock() 和 unlock() ，其实这两个方法内部调用的是 tryAcquire() 和 tryRelease() ，只是通过 lock() 和 unlock() 将它们封装起来，使得看起来更像是一把锁而已



> ### 公平锁 和 非公平锁 实现类

```java
public class ReentrantLock implements Lock {
    //公平锁还是非公平锁主要是看 sync 指向的对象是什么，主要操作的就是这个 sync
    private final Sync sync;

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
```



> ### lock()

```java
final void lock() {
    //先尝试一次 CAS 获取锁，如果获取成功，那么设置锁的 ID 为当前线程，如果失败，那么
    if (compareAndSetState(0, 1)) 
        setExclusiveOwnerThread(Thread.currentThread());
    else
        //上面直接 CAS 失败
        acquire(1);
}

public final void acquire(int arg) {
    if (!tryAcquire(arg) &&
        //前面获取锁失败，这里将线程放入 AQS 队列，addWaiter方法里调用 enq() 方法
        acquireQueued(addWaiter(Node.EXCLUSIVE), arg))
        selfInterrupt();
}
```

当我们调用 lock() 的时候，它会先进行一次 CAS，然后判断是否能够获取锁，如果不能的话，那么就调用 AQS 的 acquire() 方法，进入同步队列中，在队列中调用 ReentrantLock 实现的获取锁的逻辑 tryAcquire() 来尝试出队



> ### tryAcquire()

```java
final boolean tryAcquire(int acquires) {
    //获取当前线程
    final Thread current = Thread.currentThread();
    //获取state变量值
    int c = getState();
    if (c == 0) { //没有线程占用锁
        if (compareAndSetState(0, acquires)) {
            //占用锁成功,设置独占线程为当前线程
            setExclusiveOwnerThread(current);
            return true;
        }
    } else if (current == getExclusiveOwnerThread()) { //当前线程已经占用该锁
        int nextc = c + acquires;
        if (nextc < 0) // overflow
            throw new Error("Maximum lock count exceeded");
        // 更新state值为新的重入次数
        setState(nextc);
        return true;
    }
    //获取锁失败
    return false;
}
```

ReentrantLock 实现锁的逻辑就是通过赋予 tryAcquire() 不同的语义，只要 CAS 修改了 state 成功了，那么就表示该线程获取了锁，那么就将锁的主人 ID 设置为当前线程

同时可重入的实现就是如果已经有线程获取了锁，那么判断锁的主人 ID 是否是当前线程，如果是那么将 state +1 表示重入度 +1



> ### tryRelease()

```java
protected final boolean tryRelease(int releases) {
    int c = getState() - releases;
    if (Thread.currentThread() != getExclusiveOwnerThread())
        throw new IllegalMonitorStateException();
    boolean free = false;
    if (c == 0) {
        free = true;
        setExclusiveOwnerThread(null);
    }
    setState(c);
    return free;
}
```

释放锁的逻辑就是直接将 state - 1，因为已经获取锁了，其他线程是无法修改 state 的，因此可以直接 -1，无需 CAS

同时判断 state 是否为 0，如果是，那么直接释放锁，如果不是，那么表示锁还有重入，不能释放



> ### 公平锁获取锁 tryAcquire() 的逻辑

```java
protected final boolean tryAcquire(int acquires) {
    final Thread current = Thread.currentThread();
    int c = getState();
    if (c == 0) {
        /*
         在调用时 tryAcquire() 进行 CAS 获取资源之前 的时候，判断队列中是否有元素存在
         如果有的话，判断 头节点 是否是当前线程，因为可能是队列中的 头节点在获取资源，所以需要这个判断
         如果不是 则 获取失败，防止某个线程插队在第一次尝试 CAS 获取的时候就获取到资源
        */
        if (!hasQueuedPredecessors() &&
            compareAndSetState(0, acquires)) {
            setExclusiveOwnerThread(current);
            return true;
        }
    }
    else if (current == getExclusiveOwnerThread()) {
        int nextc = c + acquires;
        if (nextc < 0)
            throw new Error("Maximum lock count exceeded");
        setState(nextc);
        return true;
    }
    return false;
}
```





## 4、Condition 机制

> ### Condition 的介绍

Condition 是一个接口， AQS 一个内部类 ConditionObject 实现了 Condition 接口，`lock.newCondition()` 实际上是创建了一个 ConditionObject 对象。

ConditionObject 类内部复用了 AQS 的 Node 类，内部维护了一个 条件队列

```java
public class ConditionObject implements Condition {
    //条件队列头尾节点
    private transient Node firstWaiter;
    private transient Node lastWaiter;
}
```

一个 Condition 对象对应一个 条件队列

在条件队列中的节点都是 lock() 获取到锁后调用 await() 就会将线程封装为一个 Node，然后添加到条件队列中

在条件队列中的节点需要使用 signal() 唤醒，然后将节点调用 enq() 添加到同步队列中



总的来说，**AQS 存在两种队列：同步队列 和 条件队列**

同步队列存储的是竞争获取锁的节点

条件队列存储的是由于某种原因释放锁，然后进入阻塞状态，等待别的线程唤醒的节点，它被别的线程唤醒后，需要放入到同步队列中，重新跟其他线程一起竞争锁



> ### await()

```java
public final void await() throws InterruptedException {
    
    //将线程添加到 条件队列
    Node node = addConditionWaiter();
    //释放锁，state - 1
    int savedState = fullyRelease(node);
    
    //节点不在 AQS 的同步队列中
    while (!isOnSyncQueue(node)) {
        //挂起线程
        LockSupport.park(this);
        //如果是 signal() 唤醒的，那么退出循环
        if ((interruptMode = checkInterruptWhileWaiting(node)) != 0)
            break;
    }
    //上面退出循环，表示 node 已经在 AQS 队列中了，那么调用 acquireQueued() 竞争锁
    acquireQueued(node, savedState);
}
```



> ### signal()

```java
public final void signal() {
    Node first = firstWaiter;
    if (first != null)
        doSignal(first);
}

private void doSignal(Node first) {
    //这里就只让一个节点入队到 同步队列中，然后唤醒对应的线程让它去竞争锁
    do {
        if ( (firstWaiter = first.nextWaiter) == null)
            lastWaiter = null;
        first.nextWaiter = null;
    } while (!transferForSignal(first) &&	////唤醒线程
             (first = firstWaiter) != null);
}
private void doSignalAll(Node first) {
    //这里让所有的节点入队到 同步队列 中，然后唤醒所有的线程，让它们去竞争锁
    lastWaiter = firstWaiter = null;
    do {
        Node next = first.nextWaiter;
        first.nextWaiter = null;
        //唤醒线程
        transferForSignal(first);
        first = next;
    } while (first != null);
}

final boolean transferForSignal(Node node) {

    if (!compareAndSetWaitStatus(node, Node.CONDITION, 0))
        return false;
    //将节点放入到同步队列中
    Node p = enq(node);
    int ws = p.waitStatus;
    if (ws > 0 || !compareAndSetWaitStatus(p, ws, Node.SIGNAL))
        /*
        唤醒线程，这样它在 await() 中就会醒来，发现在同步队列中了就会开始竞争锁
        具体看 await() 的方法尾
        */
        LockSupport.unpark(node.thread);
    return true;
}
```



## 5、Semaphore

> ### 信号量的作用

Semaphore 可以类比为停车场的位置，即可以停车的车辆，每个停车场首先都会设置好各个车位的位置，车位数就是可用资源数，来一辆车就获取一个车位，即可用资源数 -1



首先定义 可用资源

```java
Semaphore semaphore = new Semaphore(10);
```

上述代码表示定义了一个可用资源数为  10 的信号量



当调用 semaphore.acquire() 时，表示申请一个可用资源数，如果可用资源数不为 0 时，那么可以直接获取，如果可用资源数为 0，那么线程会阻塞住，直到别的线程归还资源

当线程调用 semaphore.release() 时，表示归还可用资源，这时其他申请资源的阻塞线程就可以停止阻塞状态，获取资源，



**注意：**Semaphore 存在多个线程同时获取执行权，这意味着存在多个线程会操作同个共享内存，而 Semaphore 中的 volatile state 只能保证可见性，保证不了原子性，因此需要针对某些情况进行处理



> ### 信号量实现原理

Semaphore 是基于 AQS 实现的，即使用 同步队列 + CAS

看到这里，很容易想到我们 new Semaphore(x) 的时候传参传进去的 x 是 Semaphore 内部维护的一个值 available，表示当前可用资源数，类似 ReentrantLock 内部的 state，只不过 state 是表示锁的状态

同时，Semaphore 由于是基于 AQS，因此同时存在 公平锁 和 非公平锁两种，上面讲过公平锁和非公平锁了，这里就不具体讲了，具体的实现也差不多



尝试获取资源：acquire() ，如果失败就封装成 Node 进入同步队列自旋

```java
public final void acquireSharedInterruptibly(int arg)
            throws InterruptedException {
    //尝试进行 CAS 获取资源，如果获取失败，那么入队
    if (tryAcquireShared(arg) < 0)
        doAcquireSharedInterruptibly(arg);
}
private void doAcquireSharedInterruptibly(int arg)
    throws InterruptedException {
    //调用 addWaiter() 进入同步队列
    final Node node = addWaiter(Node.SHARED);
    try {
        for (;;) {
            //获取 node 前置节点
            final Node p = node.predecessor();
            //该节点为头节点
            if (p == head) {
                //尝试获取资源
                int r = tryAcquireShared(arg);
                if (r >= 0) {
                    //资源获取成功，那么解放线程，将该节点作为头节点
                    setHeadAndPropagate(node, r);
                    p.next = null; // help GC
                    failed = false;
                    return;
                }
            }
        }
    } 
    
}

protected int tryAcquireShared(int acquires) {
    return nonfairTryAcquireShared(acquires);
}

final int nonfairTryAcquireShared(int acquires) {
    for (;;) {
        //获取剩余资源数
        int available = getState();
        //减去需要的资源数
        int remaining = available - acquires;
        //CAS 设置剩余资源数
        if (remaining < 0 ||
            compareAndSetState(available, remaining))
            return remaining;
    }
}
```





> ### Semaphore 需要注意的点

acquire() 和  release() 应该是配套使用的，因为可以无限制的调用 release()，即会归还不存在的资源，导致错误

比如 

```java
Semaphore semaphore = new Semaphore(0);
semaphore.realease();
```

上述代码我们定义可用资源数为 0，而执行 release() 后可用资源数变成了 1，就像是停车场没有地方停车了，强行多出来一张停车票，但是又不存在可用车位，所以就出现问题了

**因此必须规定先执行 acquire() 后才能执行 release()**



一般我们都是先定义可用资源，然后再进行获取，这样可以限制资源的数量

比如 lc 上的 H20，我们可以定义 H 的可用数量为 2，O 的可用数量为 1，当进来一个 H 的时候，就调用 acquire()，减少一个 H 的可用资源，后续自己脑部，思路就在这





## 6、CountdownLatch

CountdownLatch 的作用就是让某个线程等待其他线程干完事后，自己才去干事

等待的线程调用 latch.await();

其他线程干完事调用 latch.countdown();

CountdownLatch 内部肯定也是 Syn 继承 AQS 实现，主要是来什么思路



> ### await()

```java
public void await() throws InterruptedException {
    sync.acquireSharedInterruptibly(1);
}

public final void acquireSharedInterruptibly(int arg)
    throws InterruptedException {
    //if() return -1 的话就进入方法体，即仍然还需要等待线程
    if (tryAcquireShared(arg) < 0)
        doAcquireSharedInterruptibly(arg);
}
//判断已经完成的线程数是不是满足需要等待的线程数，即 state == 0；return 1 相当于 return true
protected int tryAcquireShared(int acquires) {
    return (getState() == 0) ? 1 : -1;
}
//将线程封装为一个 Node 节点，放入 AQS 队列中，然后自旋判断是否可以出队
private void doAcquireSharedInterruptibly(int arg)
    throws InterruptedException {
    final Node node = addWaiter(Node.SHARED);
    boolean failed = true;
    try {
        for (;;) {
            /*
            只看 node 节点的前驱节点是否为 head
            如果是那么表示可以参与竞争了，那么调用 tryAcquireShared() 执行出队逻辑，看上面的方法
            这里是判断 state == 0，如果是的话，表示等待的线程数已经达到目标数，那么可以出队了
            */
            final Node p = node.predecessor();
            if (p == head) {
                int r = tryAcquireShared(arg);
                if (r >= 0) {
                    setHeadAndPropagate(node, r);
                    p.next = null; // help GC
                    failed = false;
                    return;
                }
            }
            if (shouldParkAfterFailedAcquire(p, node) &&
                parkAndCheckInterrupt())
                throw new InterruptedException();
        }
    } finally {
        if (failed)
            cancelAcquire(node);
    }
}
```



> ### countdown()

```java
public void countDown() {
    //表示释放 1 个资源
    sync.releaseShared(1);
}
public final boolean releaseShared(int arg) {
    //if() 内 CAS 释放资源
    if (tryReleaseShared(arg)) {
        //只有上面的 if() 为 true 进入这个方法体，这个方法目前没有什么实质意义
        doReleaseShared();
        return true;
    }
    return false;
}
/*
CAS 释放资源，这里使用 for() 的原因是可能存在线程竞争，一次只能有一个释放成功
当发现 c == 0，表示需要等待的线程数已经达到了，当前线程无需等待了，直接 return false
后面 return next == 0，只有为 0 的时候才 return true。其他情况都 return false;
*/
protected boolean tryReleaseShared(int releases) {
    for (;;) {
        int c = getState();
        if (c == 0)
            return false;
        int nextc = c-1;
        if (compareAndSetState(c, nextc))
            return nextc == 0;
    }
}
```



## 7、CyclicBarrier

CyclicBarrier 是用来多个线程一起在某个点进行等待其他线程的，比如赛跑，假设 5 人场，那么先到达的人需要等后来的人，直到 5 个人全部都到齐，5 个线程一起开始跑

CyclicBarrier 是可以进行复用的，即内部自动会将等待线程数归位，就跟赛跑等到 5 个人跑，5 个人跑出去了，就继续等待下一组 5 个人到齐跑

CyclicBarrier 使用 ReentrantLock + Condition 实现，内部维护了一个 ReentrantLock 和 Condition 和 Generation

```java
public class CyclicBarrier {
    private static class Generation {
        boolean broken = false;
    }

    private final ReentrantLock lock = new ReentrantLock();
    private final Condition trip = lock.newCondition();
    //代
    private Generation generation = new Generation();
    private static class Generation {
        boolean broken = false;
    }
}
```

CyclicBarrier 只有一个主要 api：await()



> ### await()

```java
public CyclicBarrier(int parties) {
    if (parties <= 0) throw new IllegalArgumentException();
    //栅栏数，是一个长期固定值
    this.parties = parties;
    //栅栏数，可变动的，当来一个线程就 -1
    this.count = parties;
}
public int await() throws InterruptedException, BrokenBarrierException {
    try {
        return dowait(false, 0L);
    } catch (TimeoutException toe) {
        throw new Error(toe); // cannot happen
    }
}
private int dowait(boolean timed, long nanos) throws InterruptedException, BrokenBarrierException,
TimeoutException {
    final ReentrantLock lock = this.lock;
    //加锁，即一次只能有一个线程执行
    lock.lock();
    try {
        //获取当前代
        final Generation g = generation;
        //如果当前代已经损坏，那么抛出异常
        if (g.broken)
            throw new BrokenBarrierException();
		
        //出现线程中断
        if (Thread.interrupted()) {
            //唤醒所有等待的线程，并且当前代不可再用，即 CyclicBarrier 废弃了
            breakBarrier();
            throw new InterruptedException();
        }
        //获取 -1 后的剩余等待数
        int index = --count;
        //等待数为 0，表示当前线程是最后一个线程
        if (index == 0) {  // tripped
            boolean ranAction = false;
            try {
                final Runnable command = barrierCommand;
                if (command != null)
                    command.run();
                ranAction = true;
                //唤醒所有的线程，并且重置为一个新的代
                nextGeneration();
                return 0;
            } finally {
                if (!ranAction)
                    breakBarrier();
            }
        }
		//没有进入上面的 index == 0，表示不是最后一个线程
        for (;;) {
            try {
                //没有设置阻塞时间，那么直接进入 Condition 的条件队列中等待
                if (!timed)
                    trip.await();
                else if (nanos > 0L)
                    nanos = trip.awaitNanos(nanos);
            } catch (InterruptedException ie) {
                if (g == generation && ! g.broken) {
                    breakBarrier();
                    throw ie;
                } else {
                    Thread.currentThread().interrupt();
                }
            }

            if (g.broken)
                throw new BrokenBarrierException();
			//重置为 新的代了，表示等待的线程数已经足够了，那么退出循环
            if (g != generation)
                return index;

            if (timed && nanos <= 0L) {
                breakBarrier();
                throw new TimeoutException();
            }
        }
    } finally {
        lock.unlock();
    }
}

```



> ### Generation 废弃 和 更新

```java
//创建一个新的代
private void nextGeneration() {
    //唤醒所有等待的线程
    trip.signalAll();
    //设置一个新的代
    count = parties;
    generation = new Generation();
}
//当前代已经损坏，不可再用，相当于销毁栅栏
private void breakBarrier() {
    //设置已废弃
    generation.broken = true;
    count = parties;
    //唤醒所有线程
    trip.signalAll();
}
```

一旦在等待过程中出现线程中断，那么当前 Generation 坏了，CyclicBarrier 无法继续使用

可以调用 reset() 方法进行重置，将坏的 Generation 进行重置，然后就可以继续使用 CyclicBarrier 了

```java
public void reset() {
        final ReentrantLock lock = this.lock;
        lock.lock();
        try {
            breakBarrier();   // break the current generation
            nextGeneration(); // start a new generation
        } finally {
            lock.unlock();
        }
    }
```

