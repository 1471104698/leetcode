#  JVM GC

## 1、GC 回收算法



> ####  1、标记-清除



**过程：**

如字面意思，分为 标记 和 清除 两个阶段，首先一趟遍历，**标记存活的对象（可达性分析判断）**，然后再一趟遍历，清除没有标记的对象



**优点：**

。。。



**缺点：**

一个是效率问题，需要两趟遍历，效率不高

第二个是会产生内存碎片，零零散散的内存空间，如果遇到需要分配连续大内存的对象，那么如果全都是内存碎片而拿不出连续的大内存的话，就又会触发一次效率不高的 GC



> #### 2、复制算法



**过程：**

它将原本可用的内存空间划分为大小相等的两大块，每次只使用其中一块来进行内存分配，当需要触发 GC 时，就将这一块的不需要回收的对象 复制到另一块中去，然后再将这块使用过的内存空间全部清理掉



**优点：**

对象复制的过程中只需要按照内存顺序进行复制，只需要移动指针即可，不会产生内存碎片，简单高效



**缺点：**

将可用的堆内存分为了两半，即可用的相当于原来的一半了，内存减少，那么就可能会更多的 GC，某些长时间存活的对象每次都需要进行复制，效率会降低

![img](http://mmbiz.qpic.cn/mmbiz_png/PgqYrEEtEnqLYgY6g5DgUKYUPgXXTjorcDwjZkYkrJ4fpgTibYjMEDGVK81YIQWDpW0k1S9ibjxLvRz3848v91qg/640?wx_fmt=png&tp=webp&wxfrom=5&wx_lazy=1&wx_co=1)



> #### 3、标记-整理

**过程：**

为了不想 像 复制算法那样浪费一半的内存空间，并且经常进行复制操作，那么就出现了 标记-整理 算法

标记还是更 标记-清除一样，一趟进行标记，然后后续不是对回收对象进行处理，而是将存活对象一个个按照顺序进行移动，紧靠着



**优点：**

不会产生内存碎片，并且相比 复制算法，一些长时间存活的对象如果靠着边界的话，是不会需要移动的，后续无需管它



**缺点：**

整理相比 清除 会花费更多的时间，但其实从长远来看，这是必要的，短时间的低效率是为了以后的高效率



![img](http://mmbiz.qpic.cn/mmbiz_png/PgqYrEEtEnqLYgY6g5DgUKYUPgXXTjorH0wLYIx1oZ7fGo2uNUgB6dwGLYV3h7pJtMgficMpicMOhUENpStgSCog/640?wx_fmt=png&tp=webp&wxfrom=5&wx_lazy=1&wx_co=1)



## 2、GC 回收对象的判断



### 1、判断方法

> #### 引用计数法

每个对象都有一个引用计数的属性，每当一个有一个指针变量引用它时，那么它的引用次数 +1，反之，引用次数 -1，当次数为 0 时，表示可以进行回收

优点：

- 实现简单，判定效率高，实时删除，没有延迟



缺点：

- 无法循环引用问题，当两个对象循环引用时，那么就无法回收，因为它们的引用计数都不为 1
- 每个对象都需要添加一个 计数器 字段来存储引用计数，增加了空间的开销
- 每次增加和删除引用，都需要更新 计数器，都需要伴随加减法，增加了时间的开销



> #### 可达性分析

从 GC roots 出发，如果某个对象无法到达，那么该对象没有被引用，那么可以回收

说到 GC roots，就跟 Java **四种引用有关系了**





### 2、强软弱虚引用

**Java 提出的这四种引用类型，实际上是为了方便 JVM 进行垃圾回收判断的**

> #### 强引用

```java
A a = new A()
```

基本我们平时写的代码都是强引用，当 new A() 这个在堆中的对象只要有 a 这个强引用指向的时候，永远不会被回收，即使堆内存不足，JVM 宁愿抛出 OOM 也不会去回收

而当 a = null 时，表示 new A() 这个对象没有引用，那么就会被 JVM 回收



> #### 软引用

```java
SoftReference<Student>studentSoftReference=new SoftReference<Student>(new Student());
Student student = studentSoftReference.get();
System.out.println(student);
```

软引用就是使用一个 SoftReference 包装对象，需要的时候使用 get() 获取这个对象

包装对象就是用来标识这个对象就是个软引用的，当 GC 后内存还是不足，那么就会回收掉软引用对象

即只有在内存不足的时候才会回收对象

**它适合当缓存，当内存足够的时候，我们可以使用它当作缓存，如果内存不足的时候，它自然就会被干掉了**



> #### 弱引用

```java
WeakReference<byte[]> weakReference = new WeakReference<byte[]>(new byte[1024\*1024\*10]);
System.out.println(weakReference.get());
```

同样是使用一个外壳 WeakReference 来包装对象，使用 get() 来获取对象

跟软引用相比，无论内存是否足够，只要发生 GC，就会被干掉

**ThreadLocl 上就使用了弱引用，防止发生内存泄漏（具体看 ThreadLock 源码）**



> #### 虚引用

```java
ReferenceQueue queue = new ReferenceQueue();
PhantomReference<byte[]> reference = new PhantomReference<byte[]>(new byte[1], queue);
System.out.println(reference.get());

public T get() {        
 return null;
}
```

它涉及两个类：虚引用外壳 PhantomReference， 引用队列 ReferenceQueue

它的 get() 方法跟上面的软引用和弱引用都不一样，直接返回 null 的，即虚引用目的不是用来当缓存之类的，因为无法获取传入的对象

虚引用需要配合 引用队列一起使用，它的主要作用是用来通知对象回收

当发生 GC 的时候，虚引用会被回收，并且回收前会将消息放入到 引用队列中，我们到时候就可以直接从引用队列获取被回收的对象

这个 引用队列 就相当于是消息队列，通知哪些对象被回收了



### 3、GC roots

具体看：<https://blog.csdn.net/qq_31459039/article/details/104684817>



GC roots 是查找引用链，当某个对象没有被引用的时候，就会被对象回收

GC roots 引用的对象包括：

- 虚拟机栈的各个栈帧中引用的对象**（局部变量）**
- 方法区中的常量、静态变量引用的对象
- 本地方法（**native**） 中引用的对象**（局部变量）**

```java
public class GCRootDemo {
    private byte[] byteArray = new byte[100 * 1024 * 1024];
    private static GCRootDemo gc2;
    private static final GCRootDemo gc3 = new GCRootDemo();
    
    public static void m1(){
        GCRootDemo gc1 = new GCRootDemo();
        System.gc();
        System.out.println("第一次GC完成");
    }

    public static void main(String[] args) {
        m1();
    }
}　　
```

解释：

gc1 是虚拟机栈中某个栈帧引用的局部变量

gc2 是常量池中的静态变量

gc3 是常量池中的常量

![è¿éåå¾çæè¿°](https://img-blog.csdn.net/20170915085559178?watermark/2/text/aHR0cDovL2Jsb2cuY3Nkbi5uZXQvc2luYXRfMzMwODcwMDE=/font/5a6L5L2T/fontsize/400/fill/I0JBQkFCMA==/dissolve/70/gravity/SouthEast)

新生代 GC 不会去扫描老年代，因此新生代的 GC roots 包含两部分：

- 直接扫描 GC roots 直达的新生代的对象
- 卡表中老年代引用的新生代的对象

老年代 GC 会去扫描新生代，因此老年代的 GC roots 包含两部分：

- 新生代引用的老年代对象
- GC roots 直达的 老年代对象



### 4、finalize()



finalize() 方法是 Object() 中的一个方法，它是一个空方法，子类可以进行重写

finalize 翻译为终结者，即是最终用来给当前对象收尾的



GC 时，如果某个对象不可达，那么会判断该对象是否 重写了 finalize()，如果没有重写，那么直接标记为回收对象，如果重写了并且之前没有调用过，那么将该对象 加入到 F-Queue（Finalize 队列）中，让一个低优先级的线程去调用该队列中的对象的 finalize()，后面 GC 会再次判断该对象是否可达，如果不可达，那么标记为回收对象，如果可达，那么该对象 存活



常说 finalize() 不可靠，原因：GC 时使用低优先级线程去 F-Queue 队列中调用对象的 finalize()，该方法的调用时机不确定



如果要某个对象要实现自救，**重写这个方法，在这个方法中重新让某个引用指向自己，而不会被回收**

不过一般也不会在 finalize() 里自救，只是一种知识点而已



以下是自救 demo

```java
public class FinalizeDemo {
    //引用
    public static FinalizeDemo Hook = null;

    //重写 finalize() 方法，实现自救
    @Override
    protected void finalize() throws Throwable {
        super.finalize();
        System.out.println("执行finalize方法");
        //将别的对象的引用指向自己，让自己有引用，而不会被回收
        FinalizeDemo.Hook = this;
    }

    public static void main(String[] args) throws InterruptedException {
        Hook = new FinalizeDemo();
        // 第一次拯救
        Hook = null;
        //执行 finalize() 方法
        System.gc();
        Thread.sleep(500); // 等待finalize执行
        if (Hook != null) {
            System.out.println("我还活着");
        } else {
            System.out.println("我已经死了");
        }
        // 第二次，代码完全一样
        Hook = null;
        //这里不会再执行 finalize() 方法，因为上面已经执行过一次了
        System.gc();
        Thread.sleep(500); // 等待finalize执行
        if (Hook != null) {
            System.out.println("我还活着");
        } else {
            System.out.println("我已经死了");
        }
    }
} 
```

执行的结果：

> 执行finalize方法
> 我还活着
> 我已经死了





## 3、GC 回收器

总共有 4 种垃圾回收器，按照级别排序就是：串行回收器，并行回收器，CMS 回收器，G1 回收器

**新生代频繁发生 GC，应选择高吞吐量的回收器，老年代对内存空间大小比较敏感，因此需要避免使用 复制 算法的垃圾回收器**



### 1、串行回收器

串行回收器是最开始提出的垃圾回收器，基本是在单 CPU 下运行的，**使用的是复制算法**

单线程进行垃圾回收，每次 GC 都会产生 "stop the word"（砸瓦鲁do）现象，即其他工作的用户线程都会停止，等待垃圾回收线程完成后才继续工作

这样用户体验及其不好，效率极低，如果垃圾回收 10s，那么用户线程就需要停止 10s，太 low 了





### 2、并行回收器

并行：多个线程同时工作，多个 CPU 的

并发：单个 CPU 切换多条线程



并行回收器主要是用在 多核 CPU 上，开启多个垃圾回收线程，并发回收，回收算法什么的跟串行基本一样

而且同样会产生 "stop the word"（砸瓦鲁do）现象，只是多条线程可以更快完成垃圾回收，停顿的时间更短



如果是在单 CPU 下使用的话，那么由于多线程需要 CPU 频繁进行切换，那么效率可能比 串行回收器 还低





### 3、CMS 回收器

 [关于CMS、G1垃圾回收器的重新标记、最终标记疑惑?  - 知乎 (zhihu.com) 答者一](https://www.zhihu.com/question/37028283/answer/352550212) 

 [关于CMS、G1垃圾回收器的重新标记、最终标记疑惑?  - 知乎 (zhihu.com) 答者二](https://www.zhihu.com/question/37028283/answer/1409952873) 

 [jvm 优化篇-（8）-跨代引用问题(RememberSet、CardTable、ModUnionTable、DirtyCard) - 简书 (jianshu.com)](https://www.jianshu.com/p/f1ff4ab0fed7) 

[G1 中用户创建新对象的解决方法](https://www.cnblogs.com/thisiswhy/p/12388638.html)



> #### CMS 出现的原因

CMS 的目的就是为了降低 串行回收器 和 并行回收器的 STW 时间

**CMS 只负责收集老年代**



> #### GC 过程

- 初始标记：**发生 STW 停顿，**扫描 GC roots，标记所有直达的新生代 和 老年代 对象（因为新生代对象可能引用老年代对象，所以需要扫描新生代）
  - 扫描区域：新生代 + 老年代
- 并发标记：递归扫描所有 GC roots 链上所有可达对象，此时由于跟用户线程一起工作，因此用户线程大概率会发生引用更新，在更新引用前，会使用写屏障记录 dirty，同时会使用 增量更新
  - 扫描区域：新生代 + 老年代
- 预清理：进行一次 young GC，减少新生代对象，降低 remark 的 STW 停顿时间
- 重新标记：**发生 STW 停顿，**因为并发标记过程中用户线程会增加对象以及修改引用，因此这里会重新扫描 GC roots 以及 新生代，但是遇到黑色节点会跳过（大部分为黑色节点，不需要重复扫描）
  - 扫描区域：新生代 + 老年代
- 并发清除：清除掉老年代中需要回收的对象
  - 扫描区域：老年代
- 并发重置：重置 CMS 内部数据，为下一次 GC 做准备



> #### CMS 的缺点 及 解决方法

- 存在内存碎片：由于使用的是标记-清除，因此自然会产生内存碎片
- 存在浮动垃圾
- 会出现 **并发失败（concurrent mode failure）**：并发失败，就是在并发的过程中 GC 失败了，CMS 在并发标记 和 并发清理 过程中，用户线程也是在执行的，如果用户线程产生的对象 在 E区 或者 老年代 没有足够的空间容纳，就会出现 concurrent mode failure，CMS 会退化为串行回收器，执行 full GC，停止所有的用户线程，降低效率



解决方法：

- 内存碎片可以通过参数设置 几次 full GC 后采用标记-整理算法，消除掉内存碎片
- 没法搞
- 并发失败（concurrent mode failure）：设置预留的用户内存空间，比如设置为老年代内存占用 60% 时就进行 CMS，这样就留下 40% 给用户线程存放对象到老年代  (不能设置太大，如果设置 90%，那么就只剩下 10% 给用户线程了，可能不够用)，当然如果预留空间还是不够的话，那么就只能退化成串行回收器了



### 4、G1 回收器



> #### 基于Region的堆内存布局

G1 将整个堆空间划分为了一个个大小相同的小空间，每个小空间称为 Region

每个 Region 内部又划分为了一个个大小相同的更小的空间，每个更小空间称为 card（卡页），大小为 512B（解决跨代引用）



G1相比 CMS 多了一个 H 区，用来存储大对象

G1 每个 Region 在同一时间只有一个角色：Eden、Survior、Old 或者 Huge 对象，一个 Region 不能同时存储两种类型的对象，比如同一个 Region 不能同时存储 Eden 对象 和 Old 对象

![img](https://picb.zhimg.com/80/v2-f18e615acfc6b36e69194bd5736fe27b_720w.jpg)



> #### G1 相比 CMS 的优点

**G1 要求在较短的停顿时间内回收更大的空间**

在 GC 时，它会统计 Region 的回收价值（通过 能够回收的空间 和 需要回收的时间 进行计算），将回收价值作为优先级作为排序，在指定 停顿时间内 将高价值的 Region 放入 CSet（Collection Set：收集 Set），后续进行回收



**G1 变相使用了 新生代的 复制算法**

在 GC 时，会将 CSet 中每个 Region 存活的对象复制到空闲的 Region 中，然后回收掉整个 Region；新的 Region 中肯定也会存在内存碎片，但是内存碎片相比整个堆 和 CMS 来说要少得说



> #### G1 回收过程

G1 的内存划分形式，决定了 G1 需要同时管理 新生代 和 老年代，根据回收区域的不同， G1 分为三种回收模式：

- young GC：只回收新生代
- mixed GC：回收所有新生代 和 部分老年代
- full GC：当 mixed GC 的回收速度跟不上用户线程请求的内存分配速度时，会暂停所有的用户线程，使用单线程进行 full GC



**mixed GC 的过程：**

- 初始标记：跟 CMS 一样（STW）
- 并发标记：标记每个 Region 中存活对象的信息，由于用户线程也在工作，G1 使用写屏障 + STAP（快照）来解决引用删除导致的对象消失问题，将 删除的引用关系 存储到  satb_mark_queue  队列中，在 remark 时处理这个队列（注意，只关心删除的引用，因为只有删除引用才会导致对象消失）
- 重新标记：处理 satb_mark_queue  队列中的引用，将原本执行的对象进行扫描，比如 B 引用了 C，但是在 并发标记过程中 B 删除了对 C 的引用，这里会对 C 进行扫描，防止出现对象消失（STW）
- 筛选回收：将所有的 young Region 进行回收，统计老年代的 Region 回收价值并且按照回收价值进行排序，根据用户设置的 GC 时间回收价值最高的 Region，回收是将 Region 上的存活对象复制到空闲的 Region 上，然后清空旧的 Region（STW）



> #### G1 的优缺点

优点：

- 不需要跟其他回收器一样每次 full GC 都需要扫描整个老年代，可以根据用户期望值进行 Region 回收，一次回收不需要针对全部内存，只需要先回收垃圾最多的 Region ，可以有效控制 STW 的时间
- Region 的堆内存划分使得 G1 不容易产生内存碎片，能够方便存储大对象



缺点：

- 每个 Region  维护一个 Rset，当引用很多的时候，记录的就变得更多，这样 Rset 占的内存空间就越多，甚至可能会占用整个堆的 20%，因此 **G1 更适合在大内存的服务器上**
- 快照会把原本应该回收的对象也会当作是存活的，比 CMS 存在更多的浮动垃圾



## 5、频繁 minor GC 和 full GC ，调优

minor GC 只回收新生代

full GC 是同时回收 新生代 和 老年代（只有 CMS 才存在单独回收老年代的模式，其他 GC 回收器都是 只收集新生代 或者 同时收集新生代 和 老年代）



频繁发生 minor GC，**只能是 新生代设置太小**



频繁发生 full GC，根据 full GC 的 触发条件 以及 回收率 来排查：

- 如果存在 永久代，那么可能是 **永久代设置太小**，由于永久代 和 老年代 是 GC 绑定的，永久代发生 GC 也会包含老年代
- 如果一次 full GC 后，老年代剩余的对象不多，即大多数对象都被回收了，表示**新生代设置太小**，导致 由于 **动态计算晋升年龄阈值** 策略 将大于等于 x 年龄，但又实际存活时间不长的对象 都搬到老年代
- 如果一次 full GC 后，老年代回收率不大，那么表示是**老年代设置太小**，没有足够的空间存储对象
- 查看是否调用了 System.gc()（一般不太可能是这个原因）