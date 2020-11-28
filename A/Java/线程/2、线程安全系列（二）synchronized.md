# synchronized

 



## 1、synchronized 底层数据结构

 

sync 锁能够保证可见性和原子性：

- 可见性：线程获取 sync 锁时会清空工作内存，后续访问时会读取主存新值
- 原子性：通过对象头的 _mark（markOop）实现



sync 锁的四种状态：**无锁、偏向锁、轻量级锁、重量级锁**

**其中 偏向锁、轻量级锁 是 乐观锁（使用 CAS）， 重量级锁 是 悲观锁（使用 ObjectMonitor 对象实现互斥）**

```java
通过锁的优化，synchronized  的性能跟 ReentrantLock 差不多
```



OOP 对象头结构如下：

普通对象的 OOP 对象头内部维护了 _mark 和 元数据指针 _metadata

```C++
// hotspot/src/share/vm/oops/oop.hpp
class oopDesc {
    
 private:
  //markOop 对象，用于存储对象的运行时记录信息，如哈希值、GC分代年龄、锁状态等
  volatile markOop  _mark;	//（我们常说的 Mark Word）
    
  //元数据指针
  union _metadata {
    Klass*      _klass;	// 方法区中的 Klass 对象，未采用指针压缩技术时使用
    narrowKlass _compressed_klass;	// 方法区中的 Klass 对象，采用指针压缩技术时使用
  } _metadata;
    
 //...
}
```



_mark 数据结构如下：

```C++
#include "oops/oop.hpp"
class ObjectMonitor;	//维护了一个 ObjectMonitor 对象，仅在重量级锁状态时存在，所以这里不会直接赋值
class JavaThread;		//指向持有锁的线程，仅在偏向锁状态时存在
class markOopDesc: public oopDesc {

 public:
  // Constants
  enum { age_bits                 = 4,	//GC 年龄，表示经过多少次 GC 还存活，用于新生代晋升老年代，占 4 bit
         lock_bits                = 2,	//锁标志位，占 2 bit
         biased_lock_bits         = 1,  //偏向锁标志位，占 1 bit
         max_hash_bits            = BitsPerWord - age_bits - lock_bits - biased_lock_bits,
         hash_bits                = max_hash_bits > 31 ? 31 : max_hash_bits, //hashCode，占 25 bit
         cms_bits                 = LP64_ONLY(1) NOT_LP64(0),
         epoch_bits               = 2
  };
    //上面的 biased_lock_bits + lock_bits
  enum { locked_value             = 0,	//0 00 轻量级锁
         unlocked_value           = 1,	//0 01 无锁
         monitor_value            = 2,	//0 10 重量级锁
         marked_value             = 3,	//0 11 GC 标志，设置为该标志位，表示该对象可以进行回收
         biased_lock_pattern      = 5	//1 01 偏向锁
             //无锁 和 偏向锁 的 锁标志位都是 01，通过偏向锁标志位来进行判断是 无锁还是偏向锁
  };
}
```



上面 _mark 整了这么多变量，实际整体结构可以看作是一个 32 bit 的变量，它在不同锁状态时的结构如下：

 ![在这里插入图片描述](https://img-blog.csdnimg.cn/20200606113736103.png) 

| 锁状态   | 23 bits                                  | 2 bits                | 4 bits  | 1 bit         | 2 bits |
| :------- | :--------------------------------------- | :-------------------- | :------ | :------------ | :----- |
| 无锁状态 | **identity hash code**（首次调用）       |                       | GC 年龄 | 0（非偏向锁） | 01     |
| 偏向锁   | **JavaThread**（Thread ID）              | **epoch**（撤销次数） | GC 年龄 | 1（偏向锁）   | 01     |
| 轻量级锁 | 指向线程栈中 Lock Record 的指针（30bit） |                       |         |               | 00     |
| 重量级锁 | 指向监视器（monitor）的指针（30bit）     |                       |         |               | 10     |
| GC标记   | 空                                       |                       |         |               | 11=    |

- 当为无锁状态时，_mark 中有 23bit 是表示 hashCode 的，有 2bit 是空的，有 4bit 是 GC 年龄的，有 1bit 是偏向锁标志，有 2bit 是锁标志位
- 当为偏向锁状态时，_mark 中有 25bit 分别表示 JavaThread 和 Epoch，有 4bit 是 GC 年龄，有 1bit 是偏向锁标志，有 2bit 是锁标志位
- 当为轻量级锁时，_mark 中有 30bit 是记录持有锁的线程栈帧中的 锁记录地址的，有 2bit 是锁标志位
- 当为重量级锁是，_mark 中有 30bit 是记录 ObjectMonitor 对象的地址的，有 2bit 是锁标志位





_mark 中维护了一个 ObjectMonitor 对象，**该对象是只用于 重量级锁的，其他情况的锁不会涉及到该对象**

ObjectMonitor 对象数据结构如下：

```C++
ObjectMonitor() {
    _header       = NULL; //存储原始的 _mark
    _count        = 0; //记录当前线程获取锁的次数，不知道有什么作用
    _waiters      = 0,
    _recursions   = 0; //锁的重入次数
    _object       = NULL; //监视器锁所属的对象。
    _owner        = NULL; //指向占有锁的线程
    _cxq          = NULL ;	//单向链表
    _WaitSet      = NULL; //处于wait状态的线程，比如调用 wait()，需要调用 signal() 唤醒，类似 AQS 的等待队列；
    _WaitSetLock  = 0;
    _EntryList    = NULL; //处于阻塞block 状态的线程，，比如在 sync 锁处竞争锁而阻塞，比如 AQS 的同步队列；
    _previous_owner_tid = 0;
}
```

cxq、WaitSet、EntryList 的节点都是使用同一个数据结构 ObjectWaiter

```C++
ObjectWaiter * volatile _cxq ; 
ObjectWaiter * volatile _EntryList ; 
ObjectWaiter * volatile _WaitSet;
```

ObjectWaiter 类似 AQS 的 Node

跟 AQS 一样，当一个线程释放锁时，会去获取 EntryList 中队首的 ObjectWaiter ，唤醒其中阻塞的线程，让它去获取锁

而对于 WaitSet 中的线程则需要别的线程调用 signal() 去唤醒，然后加入到 EntryList 中等待获取锁

```C++
class ObjectWaiter : public StackObj {
    public:
    enum TStates { TS_UNDEF, TS_READY, TS_RUN, TS_WAIT, TS_ENTER, TS_CXQ } ;
    enum Sorted  { PREPEND, APPEND, SORTED } ;
    ObjectWaiter * volatile _next;	//后继节点
    ObjectWaiter * volatile _prev;	//前驱节点
    Thread*       _thread;			//封装的线程，类似 AQS 的 Node
    ParkEvent *   _event;
    volatile int  _notified ;
    volatile TStates TState ;		//线程状态
    Sorted        _Sorted ;           // List placement disposition
    bool          _active ;           // Contention monitoring is enabled
    public:
    ObjectWaiter(Thread* thread);

    void wait_reenter_begin(ObjectMonitor *mon);
    void wait_reenter_end(ObjectMonitor *mon);
};
```



ObjectMonitor 对象中有几个关键属性：

```C++
_header：存储原始的 _mark

_owner：指向获取了锁的线程

_recursions：记录锁的重入次数

_cxq：单向链表，存储在 monitorenter 阻塞的线程节点 ObjectWaiter，插入使用的是头插法
    
_WaitSet：维护的是 调用了 wait() 的线程，类似 AQS 的等待队列

_EntryList：维护的是 阻塞在 同步代码块处 的线程，类似 AQS 的同步队列
```





## 2、monitorenter 和 monitorexit（CXQ、EntryList、WaitSet 的关系）

[Java 并发——基石篇（中） - 知乎 (zhihu.com)](https://zhuanlan.zhihu.com/p/75533444)



monitorenter 指令调用的是 EnterI() 函数

该函数分为以下几步：

- 调用 Trylock() 进行 CAS 尝试将 ObjectMonitor 中的 owner 指针指向当前线程，如果 CAS 成功，那么表示当前线程获取锁
- CAS 失败，那么表示已经有线程获取锁，那么当前线程就需要 park 挂起
- 在 park 之前，会将当前线程封装为一个 ObjectWaiter（类似 AQS 的 Node），然后将 ObjectWaiter CAS 插入到 CXQ 队列的头部，即将 ObjectWaiter 的 next 指向 _cxq，可以看出使用的是 **头插法**
- 当 CAS 插入完成后，会将线程 park 挂起
- 直到 获取锁的线程 执行完同步代码块，执行 monitorexit 指令 释放锁以及唤醒 CXQ 队列中的线程

```java
这里我们可以看出，线程阻塞进入 CXQ 队列是采用头插法，那么意味着一般情况下，慢来的线程要比先来的线程更快获取锁
不过这是一般情况下，下面会讲 决策，根据不同的决策进行不同的顺序调度
```



monitorexit 指令调用的是 exit() 函数，方法逻辑如下：

```C++
void ObjectMonitor::exit(bool not_suspended, TRAPS) {
    for (;;) {
        //...
        ObjectWaiter * w = NULL;
        int QMode = Knob_QMode;

        if (QMode == 2 && _cxq != NULL) {
            //...
        }

        if (QMode == 3 && _cxq != NULL) {
            //...
        }

        if (QMode == 4 && _cxq != NULL) {
            //...
        }
        //...
        w = _EntryList;
        if (w != NULL) {
            ExitEpilog (Self, w) ;
            return ;
        }
        
        w = _cxq ;
        //...
        ExitEpilog(Self, w);
        return;
    }
}
```

我们可以看到，它内部根据 QMode 的值进行不同的决策，为什么需要进行这种决策？

这里需要讲下 EntryList 和 WaitSet 的关系：

当我们调用 wait() 时，线程会封装为 ObjectWaiter 节点进入到 WaitSet 中，而当线程被唤醒时，那么肯定是不可能直接获取锁的，它需要去竞争，此时 ObjectWaiter 就会从 WaitSet 中转移到 EntryList 中

```java
这里可以看出，AQS 实际上是按照 sync 锁的思路来实现的
同时，AQS 中只有两个队列：同步队列 和 等待队列，而 sync 中有三个队列：CXQ 队列 和 EntryList 和 WaitSet
而整体来看，
同步队列 = CXQ 队列 + EntryList，即 AQS 将 sync 锁 两个队列实现的功能简化为一个队列
等待队列 = WaitSet
```



这时候就存在一个问题了，**如果 CXQ 中 和 EntryList 中都存在等待线程，那么调用谁的呢？**

这时候就需要进行决策：

- 当 QMode == 0（默认为 0），EntryList 优于 CXQ，如果 EntryList 不为空，会先调用 EntryList 中的线程，而调用 CXQ 时，由于使用的是头插法，所以【后来的线程先被调用】
- 当 QMode == 1，EntryList 优于 CXQ，但是会反转 CXQ ，使得【先来的线程先被调用】
- 当 QMode == 2，CXQ 优于 EntryList，其他的跟 0 时一样
- 当 QMode == 3.，EntryList 优于 CXQ，但是它会将 CXQ 接到 EntryList 后面
- 当 QMode == 4，CXQ 优于 EntryList，但是它会将  EntryList 接到 CXQ 后面



## 3、wait() 和 notify() 使用注意点

wait() 和 notify() 一般是配合 sync 锁一起使用的

sync 锁锁住 A 对象，那么会创建一个 ObjectMonitor 对象，然后将 A 对象的 _mark 中的 30bit （保留 2bit 作为锁标识符）替换为指向 ObjectMonitor 对象的指针，内部维护了 EntryList 和 WaitSet

因此，ObjectMonitor 对象是在 A 对象中的，我们调用 wait() 也必须调用 A 对象的 wait()，因为只有调用了 A 对象的 wait() 才会将当前线程放入到 A 对象的 waitSet 中进行管理，否则如果调用的是 B 对象的 wait()，那么会发现 B 对象的 _mark 是无锁状态，根本不存在什么 waitSet 

```java
public class A {
    static Object o = new Object();
    public static void main(String[] args) {
        
        Thread t1 = new Thread(() -> {
            //锁的是 A.class 对象
            synchronized (A.class) {
                try {
                    //调用的是 o 对象的 wait()，报错 IllegalMonitorStateException
                    o.wait();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        });
        
    }
}
```



当线程调用 wait() 进入阻塞状态时，如果有其他线程调用了它的 interrupt()

那么它会中断退出阻塞，这时候**不会直接退出同步代码块，也不会在没有获取锁的情况下继续执行同步代码块**，而是会尝试获取锁，如果获取失败，仍然会继续阻塞等待

```java
public class A {

    static Object o = new Object();
    public static void main(String[] args) throws InterruptedException {
        Thread t1 = new Thread(() -> {
            System.out.println("start");
            synchronized (A.class) {
                try {
                    A.class.wait();
                } catch (InterruptedException e) {
                    //被 t2 中断，这里会抛出 InterruptedException，但是仍然会陷入阻塞获取锁的状态
                    e.printStackTrace();	
                }
                for(int i = 0; i < 10; i++){
                    System.out.println("t1");
                }
            }
        });
        Thread t2 = new Thread(() -> {
            System.out.println("t2");
            synchronized (A.class) {
                //中断 t1 的等待，但是由于当前线程没有释放锁，所以 t1 获取不了锁也不会继续执行
                t1.interrupt();
                for (int i = 0; i < 10; i++){
                    System.out.println("t2");
                }
                //IO 阻塞看 t1 的执行情况，发现这里没有执行完释放锁，那么 t1 获取不了锁也不会执行
                new Scanner(System.in).nextLine();
            }
            System.out.println("t2 end");
        });
        t1.start();
        try {
            Thread.sleep(10);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        t2.start();
        t1.join();
        t2.join();
    }
}
```



## 4、锁升级过程



### 4.1、偏向锁

[偏向锁](https://blog.csdn.net/sinat_41832255/article/details/89309944)



偏向锁使用到 _mark 中的 JavaThread



> #### 偏向锁存在的原因：

对象在不存在多线程竞争的情况i下，使用偏向锁减少获取锁的代价

比如 StringBuffer，我们使用 sb.append() 的时候，实际上大多数情况都是只有一个线程在执行，如果每次执行都直接深入内核态中获取一把重量级锁，这显然效率很低

所以，出现了偏向锁，在没有竞争的情况下，只需要一次 CAS 标记线程 ID，后续直接获取锁即可



偏向锁是没有竞争的情况下，表示锁一直是某个线程持有



> #### 偏向锁的获取：

线程访问同步代码块 synchronized，发现对象头，判断锁标志位，如果为 01，那么判断 偏向标志位，如果为 0，那么表示无锁，通过一次 CAS 将对象头上的 JavaThread 指向当前线程，表示获取了锁，将偏向锁标志位设置为 1，这样锁的标志为 1|01，此后在没有其他线程竞争的情况下，这把锁一直由该线程持有，不会主动释放锁。

**但一旦出现第二个线程来获取这把锁，那么无论持有偏向锁的线程存活，那么都会升级为轻量级锁**（亲测）



偏向锁 CAS：

- CAS 对象：JavaThread
- 旧值：NULL
- 新值：指向当前线程

即偏向锁的获取只有在 锁标志为 0|01 即，JavaThread 为空 的无锁状态进行 CAS，然后 JavaThread 指向当前线程

加锁就是将 _mark 前 23bit 的数据替换为 JavaThread，因此没有位置不能存储 hashCode

> #### 偏向锁的撤销：

**偏向锁是一把没有竞争就不会主动释放的锁，**一旦存在竞争，那么持有锁的线程就会释放锁，升级为轻量级锁，多个线程去竞争锁



偏向锁的撤销如下：（亲测）

- 根据 JavaThrea 将持有偏向锁的线程 A 在一个安全点（没有执行字节码指令的时间点） 暂停运行

- 将偏向锁撤销为无锁状态，然后线程 A 和 线程 B 进行竞争，竞争成功的升级为轻量级锁



```java
曾经以为的偏向锁：

偏向锁的撤销如下：

- 根据 JavaThrea 将持有偏向锁的线程 A 在一个安全点（没有执行字节码指令的时间点） 暂停运行
- 判断线程 A 是否存活
  - 如果没有存活，那么将 偏向标志设置为 0，变为无锁状态，将 JavaThread 置空，其他线程可以去重新偏向；
  - 如果存活，那么查看 线程 A 的栈帧信息，判断是否还需要这个锁，
    - 如果不需要，那么锁标志设置为无锁状态，重新偏向
    - 如果需要，那么撤销为无锁状态，升级为轻量级锁，线程 A 和 其他线程进行轻量级锁的竞争
```





### 4.2、轻量级锁



> #### 轻量级锁存在的原因：

轻量级锁是为了减少阻塞线程的情况，因为阻塞和唤醒线程需要调用 park() 和 unpark() ，它们需要从用户态转到内核态去执行

轻量级锁适合在 少量竞争的情况下，即线程执行的 同步代码块 时间不长，在 JVM 层面其他线程 CAS 几次就可以获取锁

如果同步代码块执行时间长，那么不适合使用 轻量级锁，因为其他线程  CAS 空转会占用 CPU 资源，这时候需要升级为重量级锁来让竞争的线程阻塞等待



> #### 轻量级锁的获取：

当前 obj 为轻量级锁，即 对象头的 锁标志位 为 00

**没有线程获取锁的情况下：**

- 线程 A 发现 _mark 的锁标志位为 00，那么会在自己的栈帧中分配用于存储 Lock Record（锁记录）的内存空间，然后将对象头中的 _mark 复制到 锁记录中，然后同时线程 A 尝试使用 CAS 将 _mark 中的 30bit 替换为自己 锁记录 的地址

- 如果 CAS 成功，那么 线程 A 获取锁

- 如果 CAS 失败，表示存在竞争，比如 线程 A 和 线程 B 同时复制 _mark 到 自己的锁记录上，而 线程 B 先 CAS 成功了，所以相对的，线程 A 就 CAS 失败了，所以 CAS 失败的 线程 A 会开始  CAS 自旋空转，它相信 线程 B 的同步代码块不需要很长时间



轻量级锁 CAS：

- CAS 对象：锁对象的 mark word
- 旧值：锁对象的 mark word
- 新值：线程栈帧中的锁记录指针（地址）

竞争锁的线程 将 锁对象的 mark word 复制到自己栈帧锁记录中，然后将 锁对象 mark word 中的 30bit 数据 替换为自己的栈帧锁记录指针，一旦替换成功那么获取锁成功。



> #### 轻量级锁升级为重量级锁:

如果同一时间，存在一个线程 CAS 到达 默认的 10 次 或者 在一个持有锁，一个 CAS 自旋的时候来了第三个线程 ，那么轻量级锁升级为重量级锁





### 4.3、重量级锁



```
重量级锁使用到 _mark 中的 ObjectMonitor 对象
```

加锁就是将 _mark 前 30 bit 的数据替换为 ObjectMonitor 对象的指针

由于重量级锁线程 在 同步代码块处 获取不到锁就会陷入阻塞状态，而 park() 阻塞和 unpark() 唤醒 都需要经过用户态和内核态的切换，效率低，所以在进入重量级锁之前设置了一个 偏向锁 和 轻量级锁



**特殊的 锁升级过程：**

- 当偏向锁 或者 轻量级锁调用 wait() 时，由于它们本身不存在 WaitSet 这种数据结构来存储等待的线程，所以会膨胀为轻量级锁
- 由于偏向锁没有内容存储 hashCode，所以当首次调用未重写的 hashCode() 时，偏向锁会膨胀为重量级锁（具体看下面，这里只是先讲）



## 5、偏向锁 和 hashCode



> ### 对象头 中的 hashCode

首先关于 hashCode，我们都认为，Object 的 hashCode() 返回的是 对象在内存中的地址

实际上不然，JDK 8 改变了做法

```C++
   // thread-specific hashCode stream generator state - Marsaglia shift-xor form
  _hashStateX = os::random() ;
  _hashStateY = 842502087 ;
  _hashStateZ = 0x8767 ;    // (int)(3579807591LL & 0xffff) ;
  _hashStateW = 273326509 ;
```

hashCode 计算方法是将 与当前线程相关的一个随机数 以及 三个固定的值 利用随机算法 进行运算得到一个随机数，将这个随机数作为 hashCode，这个 hashCode 叫做 identity hash code，它是存储在对象头中的

下图中无锁状态显示的 hashCode，实际上就是这个 identity hash code，它只有在首次调用 Object 原生的没有重写过的 hashCode() 的时候才会创建，如果没有调用过 hashCode()，那么对象头是不会主动去生成这个 hashCode 的

如果我们重写了 hashCode()，那么重写后计算出来的得到的 hashCode 是不会保存在对象头中的，这个很容易理解，当我们重写 hashCode()，我们并没有去让 对象头 保存这个值，而是每次需要使用的时候就是调用该方法去获取，如果不想一直调用我们就自己声明一个变量，比如 HashMap 的 Node 中的 hash，自己去保存这个 hashCode，我们重写的 hashCode() 自始至终都没有去覆盖对象头的 hashCode。

如果我们重写了 hashCode()，那么意味着就不能再调用 hashCode() 去获取 identity hash code，那么应该如何获取呢？

System 提供了这个方法：System.identityHashCode(Object o)



因此，如果要获取 identity hash code，有两个方法：

- 调用 未重写的 hashCode()
- System.identityHashCode(Object o)

 ![在这里插入图片描述](https://img-blog.csdnimg.cn/20200606113736103.png)



> ### 偏向锁 和 identity hash code

 [当Java处在偏向锁、重量级锁状态时，hashcode值存储在哪？ - 知乎 (zhihu.com)](https://www.zhihu.com/question/52116998) 



通过图示的不同状态下的 _mark 结构，我们可以发现，**偏向锁状态中没有位置存储 identity hash code，原先无锁状态用于存储 hashCode 的 23bit 被 JavaThread 占用**

而线程获取偏向锁又没有去保存被替换的数据，因此一旦计算了 hashCode，那么就无法使用偏向锁，如果处于偏向锁状态时计算了 hashCode，那么**偏向锁就会膨胀为轻量级锁**（亲测）



但是，从 _mark 中我们可以看出，轻量级锁 和 重量级锁 中也没有存储 hashCode 的位置啊，只有 无锁状态才有存储 hashCode 的位置，那么**轻量级锁和重量级锁是如何存储 identity hash code 的？**

- 轻量级锁在获取锁前，会将 _mark 复制到 自己栈帧的 锁记录 中，在解锁的时候会将 _mark 替换回去，而复制的这份 _mark 就是无锁状态的 _mark，因此可以保存 hashCode

- 重量级锁的 ObjectMonitor 对象中的字段 _header 是一个 markOop 类型的，即是用来存储 无锁状态下的 _mark





## 6、synchronized 和 lock 的区别

> ### synchronized

- 修饰方法和代码块
- 可重入，并且有 偏向锁、轻量级锁、重量级锁 三种，会根据竞争情况进行锁升级
- 发生异常时会自动释放锁（在异常结束处加了一条字节码指令 monitorexit），但是如果捕获了异常那么就不会执行 monitorexit，因此就不会提前释放锁

**但 synchronized 锁不够灵活，获取锁的时候只能一直阻塞，不能够中断，因此容易造成死锁，在某些需要中断等待的场景也不能使用**



> ### lock

- 具有灵活性，**灵活性在于可以手动上锁和释放锁，并且可以指定等待锁的时间，不会死等**，线程池中 Worker 类 就继承了 AQS，赋予了 tryAcquire() 锁的语义，当调用 shutdown() 的时候线程池可以调用 tryLock() 判断线程是否空闲
- 能够保证可见性（**由于 state 是 volatile 的，所以释放锁，即修改 state 的时候，会将前面的操作一并刷新入内存，这样其他线程看得到了**）
- 可重入
- 可以配套使用 `lock.newCondition()` 来指定不同类型的锁对象，可以方便唤醒某种类型的线程，用于生产者消费者模式
- 发生异常不会自动释放锁，所以需要记住在 finally 处调用 unlock()

lock 的灵活性使得可以避免无限期的阻塞，以及可以用来线程池判断线程的状态

**使用 tryLock(int time) 可以被设置等待超时时间，期间还可以被中断，不过使用 lock() 无法中断，因为线程在同步对象中会调用 park() 挂起，不会去识别中断标识符**



