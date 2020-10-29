#  JVM GC

## 1、GC 回收算法



> ###  1、标记-清除



**过程：**

如字面意思，分为 标记 和 清除 两个阶段，首先一趟遍历，**标记存活的对象（可达性分析判断）**，然后再一趟遍历，清除没有标记的对象



**优点：**

。。。



**缺点：**

一个是效率问题，需要两趟遍历，效率不高

第二个是会产生内存碎片，零零散散的内存空间，如果遇到需要分配连续大内存的对象，那么如果全都是内存碎片而拿不出连续的大内存的话，就又会触发一次效率不高的 GC



> ### 2、标记-复制



**过程：**

它将原本可用的内存空间划分为大小相等的两大块，每次只使用其中一块来进行内存分配，当需要触发 GC 时，就将这一块的不需要回收的对象 复制到另一块中去，然后再将这块使用过的内存空间全部清理掉



**优点：**

对象复制的过程中只需要按照内存顺序进行复制，只需要移动指针即可，不会产生内存碎片，简单高效



**缺点：**

将可用的堆内存分为了两半，即可用的相当于原来的一半了，内存减少，那么就可能会更多的 GC，某些长时间存活的对象每次都需要进行复制，效率会降低

![img](http://mmbiz.qpic.cn/mmbiz_png/PgqYrEEtEnqLYgY6g5DgUKYUPgXXTjorcDwjZkYkrJ4fpgTibYjMEDGVK81YIQWDpW0k1S9ibjxLvRz3848v91qg/640?wx_fmt=png&tp=webp&wxfrom=5&wx_lazy=1&wx_co=1)



> ### 3、标记-整理

**过程：**

为了不想 像 复制算法那样浪费一半的内存空间，并且经常进行复制操作，那么就出现了 标记-整理 算法

标记还是更 标记-清除一样，一趟进行标记，然后后续不是对回收对象进行处理，而是将存活对象一个个按照顺序进行移动，紧靠着



**优点：**

不会产生内存碎片，并且相比 复制算法，一些长时间存活的对象如果靠着边界的话，是不会需要移动的，后续无需管它



**缺点：**

整理相比 清除 会花费更多的时间，但其实从长远来看，这是必要的，短时间的低效率是为了以后的高效率



![img](http://mmbiz.qpic.cn/mmbiz_png/PgqYrEEtEnqLYgY6g5DgUKYUPgXXTjorH0wLYIx1oZ7fGo2uNUgB6dwGLYV3h7pJtMgficMpicMOhUENpStgSCog/640?wx_fmt=png&tp=webp&wxfrom=5&wx_lazy=1&wx_co=1)



## 2、JVM 中如何判断对象生死？（GC 回收对象的判断）



### 1、判断方法

> ### 引用计数法

每个对象都有一个引用计数的属性，每当一个有一个指针变量引用它时，那么它的引用次数 +1，反之，引用次数 -1，当次数为 0 时，表示可以进行回收

优点：

- 实现简单，判定效率高，实时删除，没有延迟



缺点：

- 无法循环引用问题，当两个对象循环引用时，那么就无法回收，因为它们的引用计数都不为 1
- 每个对象都需要添加一个 计数器 字段来存储引用计数，增加了空间的开销
- 每次增加和删除引用，都需要更新 计数器，都需要伴随加减法，增加了时间的开销



> ### 可达性分析

从 GC roots 出发，如果某个对象无法到达，那么该对象没有被引用，那么可以回收

说到 GC roots，就跟 Java **四种引用有关系了**





### 2、Java 四种引用



**Java 提出的这四种引用类型，实际上是为了方便 JVM 进行垃圾回收判断的**

> ### 强引用

```java
A a = new A()
```

基本我们平时写的代码都是强引用，当 new A() 这个在堆中的对象只要有 a 这个强引用指向的时候，永远不会被回收，即使堆内存不足，JVM 宁愿抛出 OOM 也不会去回收

而当 a = null 时，表示 new A() 这个对象没有引用，那么就会被 JVM 回收



> ### 软引用

```java
SoftReference<Student>studentSoftReference=new SoftReference<Student>(new Student());
Student student = studentSoftReference.get();
System.out.println(student);
```

软引用就是使用一个 SoftReference 包装对象，需要的时候使用 get() 获取这个对象

包装对象就是用来标识这个对象就是个软引用的，当 GC 后内存还是不足，那么就会回收掉软引用对象

即只有在内存不足的时候才会回收对象

**它适合当缓存，当内存足够的时候，我们可以使用它当作缓存，如果内存不足的时候，它自然就会被干掉了**



> ### 弱引用

```java
WeakReference<byte[]> weakReference = new WeakReference<byte[]>(new byte[1024\*1024\*10]);
System.out.println(weakReference.get());
```

同样是使用一个外壳 WeakReference 来包装对象，使用 get() 来获取对象

跟软引用相比，无论内存是否足够，只要发生 GC，就会被干掉

**ThreadLocl 上就使用了弱引用，防止发生内存泄漏（具体看 ThreadLock 源码）**



> ### 虚引用

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



### 4、死亡标记 和 拯救 finalize()

在可达性算法中，**宣判对象的死亡存在两次标记过程**：

- 从 GC roots 出发，从每个 GC root 都能够引伸出多条引用链，比如 a 引用了 b ,b 引用了 c 之类的

而没有在引用链上面的对象，那么就会被进行第一次标记

- 当第一次标记后，会在标记过的将要进行回收的对象上进行筛选，判断该对象是否有必要执行 finalize() 或者是否已经执行过 finalize() 方法，如果有必要以及还未执行过，那么就将这个对象放入了 F-Queue 队列中，让**低优先级的线程去调用它**，后续 GC 会再次来查看，如果完成了自救，那么就将这个对象从 回收集合中移除



执行 finalize() 方法有两个必要条件：

- 重写了 finalize() 方法
- 在之前没有调用过 finalize() 方法，一个对象一生中只能调用一次 finalize() 方法

> ### finalize() 

finalize() 方法是 Object() 中的一个方法，它是一个空方法，子类可以进行重写

finalize 翻译为终结者，即是最终用来给当前对象收尾的，比如我们开启某个流，可以在 finalize() 里面关闭



当我们要释放某些资源的时候，不要依靠这个方法，原因如下：

- GC 时使用低优先级线程去 F-Queue 队列中调用对象并执行，调用时机不确定，资源会迟迟得不到释放，占据内存空间

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

关于 CMS 和 G1 具体看：  https://www.zhihu.com/question/37028283/answer/352550212 

​											 https://www.zhihu.com/question/37028283/answer/1409952873 



> ### CMS 出现的原因

CMS 的目的就是为了降低 串行回收器 和 并行回收器的 STW 时间

**CMS 只负责收集老年代**



> ### GC 过程

- 初始标记：扫描 GC roots，标记直达对象（STW）
- 并发标记：递归扫描标记的 GC roots 直达对象（跟用户线程一起）
- 重新标记：因为并发标记过程中用户线程会增加对象以及修改引用，因此这里会重新扫描原来的 GC roots 以及 新生代，但是遇到黑色节点会跳过（STW）
- 并发清除：清除掉回收对象（跟用户线程一起）



> ### CMS 的缺点 及 解决方法

- 存在内存碎片：由于使用的是标记-清除，因此自然会产生内存碎片
- 存在浮动垃圾
- 会出现 **并发失败（concurrent mode failure）**：并发失败，就是在并发的过程中 GC 失败了，CMS 在并发标记 和 并发清理 过程中，用户线程也是在执行的，如果用户线程产生的对象 在 E区 或者 老年代 没有足够的空间容纳，就会出现 concurrent mode failure，CMS 会退化为串行回收器，执行 full GC，停止所有的用户线程，降低效率



解决方法：

- 内存碎片可以通过参数设置 几次 full GC 后采用标记-整理算法，消除掉内存碎片
- 没法搞
- 并发失败（concurrent mode failure）：设置预留的用户内存空间，比如设置为老年代内存占用 60% 时就进行 CMS，这样就留下 40% 给用户线程存放对象到老年代  (不能设置太大，如果设置 90%，那么就只剩下 10% 给用户线程了，可能不够用)，当然如果预留空间还是不够的话，那么就只能退化成串行回收器了



### 4、G1 回收器



> ### G1 出现的原因

上面的 GC 回收器都存在以下问题：

- 新生代、老年代都是独立连续的内存空间，每次都需要提前分配好对应的空间

- 老年代收集都必须扫描整个老年代



G1 的特点：

- 尽量让 GC 时长可控
- 避免扫描整个 老年代
- 不引起太长的 STW
- 每个代的分区数量可以调整，比如原本是属于新生代的分区可以调整为老年代，反正是哪里需要的就分给哪里

使用 G1 就是简化 JVM 调优，只需要三步即可完成调优：

- 开启 G1
- 设置堆的最大内存
- 设置最大的 STW 时间



> ### 基于Region的堆内存布局

G1 将整个堆划分成了大小相等的多个区域 Region，分别为 E、S、O、H，即 伊甸区、幸存区、老年区、大对象区，**每个是连续范围的虚拟内存。**并且一个 Region 可以存放很多的对象

回收时以 Region 作为一个基本单位（跟内存管理的分页机制类似），这样就避免了内存碎片



G1 内部会自动跟踪每个 Region 中可以回收的对象大小和预估时间，在垃圾回收的时候，根据用户的期望值来判断回收 某个 Region 的价值，即回收对象是否多，并且大概需要多少回收时间，将每个 Region 根据价值进行排序，根据用户设置的期望时间来判断是否回收该 Region，以此达到每次都回收最大价值的 Region 的目的（有点类似死锁避免的银行家算法）

![img](https://picb.zhimg.com/80/v2-f18e615acfc6b36e69194bd5736fe27b_720w.jpg)



> ### G1 回收过程

G1 的内存划分形式，决定了 G1 需要同时管理 新生代 和 老年代，根据回收区域的不同， G1 分为三种回收模式：

- young GC：只回收新生代
- mixed GC：回收所有新生代 和 部分老年代
- full GC：当 mixed GC 的回收速度跟不上用户线程请求的内存分配速度时，会暂停所有的用户线程，使用单线程进行 full GC



**mixed GC 的过程：**

- 初始标记：跟 CMS 一样（STW）
- 并发标记：标记每个 Region 中存活对象的信息，(在并发标记前会生成引用关系的快照)
- 重新标记：恢复用户线程在并发标记中删除的引用（STW）
- 筛选回收：将所有的 young Region 进行回收，统计老年代的 Region 回收价值并且按照回收价值进行排序，根据用户设置的 GC 时间回收价值最高的 Region，回收是将 Region 上的存活对象复制到空闲的 Region 上，然后清空 回收的 Region（STW）



> ### G1 的优缺点

优点：

- 不需要跟其他回收器一样每次 full GC 都需要扫描整个老年代，可以根据用户期望值进行 Region 回收，一次回收不需要针对全部内存，只需要先回收垃圾最多的 Region ，可以有效控制 STW 的时间
- Region 的堆内存划分使得 G1 不容易产生内存碎片，能够方便存储大对象



缺点：

- 每个 Region  维护一个 Rset，当引用很多的时候，记录的就变得更多，这样 Rset 占的内存空间就越多，甚至可能会占用整个堆的 20%，因此 **G1 更适合在大内存的服务器上**
- 快照会把原本应该回收的对象也会当作是存活的，比 CMS 存在更多的浮动垃圾





## 4、浮动垃圾 和 对象消失

> ### 三色标记

GC 中定义了三种颜色：白色、灰色、黑色

白色：没有 GC roots 链引用的对象或者还没有扫描的对象

灰色：已经扫描过的对象，但是这个对象还存在引用没有扫描

黑色：已经扫描过的对象，并且它的所有引用都已经扫描了

在 GC 中，最终标记还是只会剩下 白色和黑色，灰色是白色和黑色 的过渡颜色

最后如果标记为白色的对象，会被回收

![img](https://user-gold-cdn.xitu.io/2020/2/23/170726139d42da78?imageView2/0/w/1280/h/960/format/webp/ignore-error/1)

> ### 并发标记的问题一：浮动垃圾

如下面的动图，当我们处理完 6，将 7 设置为 灰色，但是这时候 用户将 6 的指针不再指向 7，而 7 由于已经被染成灰色，所以 GC 认为它还存活，所以继续将后面的引用染成黑色，这样的话，7 8 4 10 11 都不会被本次 GC 回收，这就产生了浮动垃圾

![img](https://user-gold-cdn.xitu.io/2020/3/1/17093d3d3ee999f4?imageslim)



> ### 并发标记的问题二：对象消失

正常情况下是这样的

![img](https://user-gold-cdn.xitu.io/2020/2/23/1707265e1a8f8b43?imageView2/0/w/1280/h/960/format/webp/ignore-error/1)



但是，并发标记的过程中，用户可能会修改引用，使得尚未扫描到的存活对象扫描不到了，导致它被垃圾回收

比如下图：当  5 扫描完成后，接着是扫描 6 的，而本来 6 是指向 9 的，但是由于用户线程的修改了引用，导致 6 和 9 间的引用断开了，而 5 指向了 9，因为 5 已经标记为黑色，而 6 不存在与 9 的引用，这意味着 9 不会被标记，而最终只能是白色，而被回收掉，但实际上它是存活的对象

![img](https://user-gold-cdn.xitu.io/2020/2/23/1707266cd4d9d2fb?imageslim)



### 

> ### 解决 对象消失一：增量更新

它关注的是引用的增加，不关心引用的删除

它规定，黑色对象一旦插入 白色对象后，它就会重新变成灰色

在重新标记阶段，重新扫描一遍 GC roots，遇到黑色节点会直接跳过，而遇到白色和灰色的节点就会被标记，不会出现漏标的对象消失问题



由于 CMS 使用的是增量更新，只关心引用的增加，不关心引用的删除

CMS 从 GC roots 重新扫描一遍，为了防止漏标的代价比较大，但是浮动垃圾少，只有在已经扫描过的黑色节点被删除引用的时候会产生浮动垃圾



> ### 解决 对象消失二：快照

它关注引用的删除

在并发标记前，会将引用生成一个快照

简单的说就是在 GC 的时候，该对象是存活的那么它就是存活的，因此需要提前给对象的引用关系做一次快照，当并发标记的过程中，有用户线程删除了引用的时候，导致某个对象没有被标记上，那么在并发标记结束后的重新标记阶段，会通过这个快照按照原来的引用关系再扫描发生改变的对象（注意：快照只恢复并发标记过程中用户线程删除的引用）

**恢复引用会导致浮动垃圾的存活，比如用户线程修改了引用，这个对象已经没有引用了，是需要回收的，但是按照快照又给它恢复成有引用的样子，导致它在此次 GC 中不会被回收**



![img](https://user-gold-cdn.xitu.io/2020/2/23/170726d12a19361a?imageslim)

G1 使用的是快照，只关心引用的删除，不关心引用的增加

在重新标记阶段不会扫描整个 GC roots，所以 STW 比 CMS 更短，但是浮动垃圾比 CMS 多，留到下一次 GC 处理







## 5、young GC 怎么避免扫描整个老年代



**跨代引用的假设：跨代引用相对于同代引用来说仅占极少数。所以，在对新生代单独做垃圾收集时，不应该为了少量的跨代引用而遍历整个老年代（搜索引用链）**



通过 卡表（card table）实现，用来记录 老年代对象 引用 新生代对象 的记录，防止在 young GC 时为了判断某个 新生代对象是否存活而去扫描整个老年代

将老年代空间分成若干张 512B 大小的卡（card），本质是 int 数组，由于新生代和老年代是在同一个堆中，只是逻辑上将内存分割为两部分，所以这个 card table 新生代也可见

通过卡表可以很快知道哪些新生代对象被老年代对象引用，比如老年代 3 号位置就引用了新生代的某个对象，避免了全堆扫描

![CardTable](https://user-gold-cdn.xitu.io/2020/7/3/1731052eb999f1a7?imageView2/0/w/1280/h/960/format/webp/ignore-error/1)



> ### CMS 的 card table 和 G1 的 card table

CMS 为了解决跨代问题，使用的是 card table，比如 CMS 通过将老年代划分为大小相同的 card table 来记录新生代的引用的，而不维护新生代对老年代的 car table，是因为新生代对象变化太快，维护起来开销大，引用需要经过发生改变，所以不需要



而 G1 使用的是 Region 的概念，淡化了新生代和老年代的分区概念，所以没有简单的使用这个方法，而是在 card table 的基础上加上了 Rset

- 它是将 每个 Region 分为多个 card (这点跟之前的老年代划分一样，不过这里是每个 Region 都进行划分)

- 然后为每个 Region 都分配一个 Rset，而这个 Rset 就是记录着其他的 Region 中的哪个 card 引用了当前的这个 Region
- Rset 本质上是一个 HashTable<key, int[]>，这个 key 是其他 Region 的地址，int[] 是 key 对应的 Region 的 card table，对应 card 上为 0 就表示 key 对应的 Region 的该位置的 card 引用了当前的 Region



比如下面这个

在 Region2 的下方，【 1	2	3	4 】表示的是 Rset（HashTable ）的槽位，1号槽位表示 Region1，2号槽位表示 Region2

每个槽位上都是一个 int[]，记录了该槽位对应的 Region 对当前 Region 的引用情况

*![image.png](https://pic.leetcode-cn.com/1599967286-lhDVis-image.png)*



比如上面 两个 int[] 数组是 Region2 的，分别代表 Region1 和 Region3 对 Region2 的引用

比如 第一个 int[] 数组中，2 号位置 和 4 号位置为 1，表示 Region1 的第 2 个 card 和 第 4 个 card 存在对 Region2 的引用

 第二个 int[] 数组中，1 号位置 和 4 号位置为 1，表示 Region3 的第 1 个 card 和 第 4 个 card 存在对 Region2 的引用

这样 GC 的时候可以直接获取 Region 的 Rset 判断该 Region 是否还有对象存活





## 6、频繁 full GC ，调优

full GC 是同时回收 新生代 和 老年代

根据 full GC 的 触发条件 以及 回收率 来排查：

- 如果存在 永久代，那么看看是不是永久代设置的太小了，导致 永久代 进行 full GC 使得跟它绑定的老年代也一起遭殃
- 如果一次 full GC 后，老年代剩余的对象不多，即大多数对象都被回收了，表示新生代设置太小，导致短生命周期的对象都晋升到老年代
- 如果一次 full GC 后，老年代回收率不大，那么表示老年代设置太小
- 查看是否调用了 System.gc()