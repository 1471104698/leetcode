# synchronized

 



## 1、synchronized 底层原理

 

synchronized 锁 实现是靠 OOP 对象头中的 markOop 对象 _mark，即常说的 Mark Word



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



上面 _mark 整了这么多变量，实际上它是一个 32 bit 的变量，它在不同锁状态时的结构如下：

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
    _header       = NULL;//markOop对象头
    _count        = 0; //记录当前线程获取锁的次数
    _waiters      = 0,//等待线程数
    _recursions   = 0;//锁的重入次数
    _object       = NULL;//监视器锁寄生的对象。锁不是平白出现的，而是寄托存储于对象中。
    _owner        = NULL;//指向占有锁的线程
    _WaitSet      = NULL;//处于wait状态的线程，会被加入到waitSet，比如调用 wait()；
    _WaitSetLock  = 0;
    _EntryList    = NULL;//处于阻塞block 状态的线程，会被加入到entryList，比如在 sync 锁处竞争锁而阻塞；
    _previous_owner_tid = 0;//上一个锁拥有者线程的ID
}
```

ObjectMonitor 对象中有几个关键属性：

```C++
_owner：指向获取了锁的线程

_recursions：记录锁的重入次数

_WaitSet：维护的是 调用了 wait() 的线程

_EntryList：维护的是 阻塞在 同步代码块处 的线程
```



## 2、锁升级过程



### 2.1、偏向锁

[偏向锁](https://blog.csdn.net/sinat_41832255/article/details/89309944)



偏向锁使用到 _mark 中的 JavaThread



> #### 偏向锁存在的原因：

对象在不存在多线程竞争的情况i下，使用偏向锁减少获取锁的代价

比如 StringBuffer，我们使用 sb.append() 的时候，实际上大多数情况都是只有一个线程在执行，如果每次执行都直接深入内核态中获取一把重量级锁，这显然效率很低

所以，出现了偏向锁，在没有竞争的情况下，只需要一次 CAS 标记线程 ID，后续直接获取锁即可



偏向锁是没有竞争的情况下，表示锁一直是某个线程持有



> #### 偏向锁的获取：

线程访问同步代码块 synchronized，发现对象头，判断锁标志位，如果为 01，那么判断 偏向标志位，如果为 0，那么表示无锁，通过一次 CAS 将对象头上的 JavaThread 指向当前线程，表示获取了锁，将偏向锁标志位设置为 1，这样锁的标志为 1|01，此后在没有其他线程竞争的情况下，这把锁一直由该线程持有，不会主动释放锁



偏向锁 CAS：

- CAS 对象：JavaThread
- 旧值：NULL
- 新值：指向当前线程

即偏向锁的获取只有在 锁标志为 0|01 即，JavaThread 为空 的无锁状态进行 CAS，然后 JavaThread 指向当前线程

加锁就是将 _mark 前 23bit 的数据替换为 JavaThread，因此没有位置不能存储 hashCode

> #### 偏向锁的撤销：

**偏向锁是一把没有竞争就不会主动释放的锁，**一旦存在竞争，那么持有锁的线程就会释放锁，升级为轻量级锁，几个线程一起去竞争锁

- 根据 JavaThrea 将持有偏向锁的线程 A 在一个安全点（没有执行字节码指令的时间点） 暂停运行
- 判断线程 A 是否存活
  - 如果没有存活，那么将 偏向标志设置为 0，变为无锁状态，将 JavaThread 置空，其他线程可以去重新偏向；
  - 如果存活，那么查看 线程 A 的栈帧信息，判断是否还需要这个锁，
    - 如果不需要，那么撤销为无锁状态，重新偏向
    - 如果需要，那么撤销为无锁状态，升级为轻量级锁，线程 A 和 其他线程进行轻量级锁的竞争



> #### 偏向锁 性能一定比 自旋锁高吗？

不一定

偏向锁当存在其他线程要获取锁时，都需要进行 撤销过程，而撤销过程 是 需要等待 持有锁的线程到达一个安全点，然后再将偏向锁撤销为无锁状态，然后再重新偏向 或者 升级为轻量级锁，会消耗性能。

当频繁的多个线程交互获取锁，但是没有发生锁冲突时，就会频繁的进行 偏向锁撤销，导致性能下降

```java
场景：
线程 A 获取锁，发现无锁状态，CAS 获取偏向锁，同步代码块执行完毕
线程 B 获取锁，发现偏向 A，但是线程 A 不需要锁，进行撤销，再 CAS，同步代码块执行完毕
线程 A 获取锁，发现偏向 B，但是 B 不需要，进行撤销，再 CAS，同步代码块执行完毕
//无穷无尽的撤销。。。
```

[批量偏向 和 批量撤销](https://www.it610.com/article/1296551396493041664.htm)

因此**当偏向锁的撤销次数 默认的 40 次时，该对象不可偏向，也会升级为轻量级锁**，撤销次数由 epoch 记录，每撤销一次，epoch + 1





### 2.2、轻量级锁



> #### 轻量级锁存在的原因：

轻量级锁是为了减少阻塞线程的情况，因为阻塞和唤醒线程需要调用 park() 和 unpark() ，它们需要从用户态转到内核态去执行

轻量级锁适合在 少量竞争的情况下，即线程执行的 同步代码块 时间不长，在 JVM 层面其他线程 CAS 几次就可以获取锁

如果同步代码块执行时间长，那么不适合使用 轻量级锁，因为其他线程  CAS 空转会占用 CPU 资源，这时候需要升级为重量级锁来让竞争的线程阻塞等待



> #### 轻量级锁的获取：

线程 A 获取锁前，会在自己的栈帧中分配用于存储 Lock Record（锁记录）的内存空间，然后将对象头中的 _mark 复制到 锁记录中，然后同时线程 A 尝试使用 CAS 将 _mark 替换为自己 锁记录 的地址

如果 CAS 成功，那么 线程 A 获取锁，将 锁标志为 设置为 00

如果 CAS 失败，表示存在竞争，比如 线程 A 和 线程 B 同时复制 _mark 到 自己的锁记录上，而 线程 B 先 CAS 成功了，所以相对的，线程 A 就 CAS 失败了，所以 CAS 失败的 线程 A 会开始  CAS 自旋空转，它相信 线程 B 不需要执行多长时间就会释放锁



**如果存在一个线程 CAS 到达 默认的 10 次 或者 自旋等待的 线程数 达到 CPU / 2 ，那么轻量级锁升级为重量级锁**

但是后续抛弃了这种升级重量级锁的条件，而是使用了 自适应自旋锁，它是根据上一个线程竞争这个锁 所需要的时间来决定当前线程自旋的时间：如果上个线程竞争锁需要的时间短，那么认为当前线程也应该很快就能获取锁，那么允许拖延一点点时间，所以会将自旋次数增加一点点；如果上个线程竞争锁需要的时间长，那么认为当前线程很难获取锁，那么干脆就尝试性的自旋几次 或者 干脆直接阻塞当前线程



轻量级锁 CAS：

- CAS 对象：锁对象的 mark word
- 旧值：锁对象的 mark word
- 新值：线程栈帧中的锁记录指针（地址）

竞争锁的线程 将 锁对象的 mark word 复制到自己栈帧锁记录中，然后将 锁对象 mark word 中的 30bit 数据 替换为自己的栈帧锁记录指针，一旦替换成功那么获取锁成功。

而其他后来的线程，如何获取 mark word 进行 CAS？

个人猜测是通过 锁记录的指针 找到获取锁线程的栈帧中的锁记录，再获取锁记录中的 mark word，再将 mark word 复制到自己的锁记录中，进行 CAS





### 2.3、重量级锁



```
重量级锁使用到 _mark 中的 ObjectMonitor 对象
```

加锁就是将 _mark 前 30 bit 的数据替换为 ObjectMonitor 对象的指针



由于重量级锁线程 在 同步代码块处 获取不到锁就会陷入阻塞状态，而 park() 阻塞和 unpark() 唤醒 都需要经过用户态和内核态的切换，效率低，所以在进入重量级锁之前设置了一个 偏向锁 和 轻量级锁



```
synchronized 中可以配合使用 wait() 和 notify() 来阻塞和唤醒线程 来控制锁的释放和获取

当调用 wait() 时，会释放锁，然后最终调用 park() 挂起线程，当调用 notify() 时，调用 unpark() 唤醒线程，然后竞争锁
```



## 3、偏向锁 和 hashCode



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

比如下面无锁状态中显示的 hashCode，实际上就是这个 identity hash code，它只有在首次调用 Object 原生的没有重写过的 hashCode() 的时候才会创建，如果没有调用，那么对象头是不包含这个 hashCode 的

如果我们重写了 hashCode()，那么重写后计算出来的得到的 hashCode 是不会保存在对象头中的，这个很容易理解，当我们重写 hashCode()，我们并没有去让 对象头 保存这个值，而是每次需要使用的时候就是调用该方法去获取，如果不想一直调用我们就自己声明一个变量，比如 HashMap 的 Node 中的 hash，自己去保存这个 hashCode，我们重写的 hashCode() 自始至终都没有去覆盖对象头的 hashCode。

如果我们重写了 hashCode()，那么意味着就不能再调用 hashCode() 去获取 identity hash code，那么应该如何获取呢？

System 提供了这个方法：System.identityHashCode(Object o)

 ![在这里插入图片描述](https://img-blog.csdnimg.cn/20200606113736103.png)



> ### identity hash code 和 偏向锁的关系

通过图示的不同状态下的 _mark 结构，我们可以发现，偏向锁状态中不存在 存储 identity hash code 的位置

因此，当处于偏向锁状态的对象一旦调用了 未重写的 hashCode() 或者 System.identityHashCode(Object o) 生成了 identity hash code，该对象就无法使用偏向锁，而是会强制升级为重量级锁，这样才可以在对象头中存储 identity hash code



但是，从 _mark 中我们可以看出，轻量级锁 和 重量级锁 中也没有存储 hashCode 的位置啊，只有 无锁状态才有存储 hashCode 的位置

轻量级锁在获取锁前，对象是无锁状态的，而它会将 _mark 复制到 自己栈帧的 锁记录 中，在解锁的时候会将 _mark 替换回去，而复制的这份 _mark 就是无锁状态的 _mark，即计算出来的 hashCode 只要存储在 无锁状态的 _mark 中再复制起来，后续再替换回去就可以了

重量级锁的 ObjectMonitor 对象中有字段会记录 无锁状态下的 _mark