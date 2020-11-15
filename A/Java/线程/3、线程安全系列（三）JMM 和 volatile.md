# JMM 和 volatile



## 1、JMM

 

> ### JMM（Java 内存模型）



JMM 规定了 内存只要有两种：主存 和 工作内存

主存 就是 物理内存，一般是堆栈内存空间（堆栈都存在 变量和对象）， 工作内存是 CPU 缓存、寄存器

工作内存 中的数据是对主存中的 一部分数据的 拷贝，线程 不能直接操作主存的数据，需要先将主存的数据拷贝到 工作内存中，然后在工作内存中修改完数据，再将工作内存中的数据写回主存

这么做的原因是可以控制多个线程的 读写操作，免得多线程操作主存数据需要对主存数据加锁，这样降低了并发效率

 ![img](https://picb.zhimg.com/80/v2-f0364f6f863d5730e2b962ac6b3387e2_720w.jpg) 

上面说了，线程的工作内存也是 CPU cache，学习过 CPU 缓存一致性协议 MESI 就会知道， CPU 多核心 已经通过该协议保证了数据的一致性了，那么为什么还会需要 volatile 呢？

因为 CPU 默认使用了 Store Buffers，它只能保证 单核 CPU 的可见性，保证多核 CPU 的可见性需要由 内存屏障 来保证，而 volatile 就是 自动添加 读 和 写 内存屏障



## 2、volatile 实现原理



volatile 是用来保证 数据可见性的，但不能保证原子性

volatile 保证可见性是通过 happens-before 规则实现

但 happens-before 规则 实际上是为了不让程序员去过多干涉内部细节而衍生出的一个方便理解 volatile 的理论而已

volatile 保证修饰变量可见性是通过 **利用总线嗅探使 CPU 缓存行失效 + 读写内存屏障**（具体看 CPU 缓存一致性就知道了）

volatile 保证在它之前的写操作的可见性是通过 **禁止重排序**



**对于 volatile 的实现，有一个争议的点：**

```
因为 CPU 缓存一致性协议说的是 CPU 核心能够从另一个 CPU 核心读取数据，而没必要去访问主存，这样的话 volatile 变量的读取也没有去主存读取，可以去别的 CPU 核心读取，这样就没必要强制刷新到主存上，只需要强制将 storebuffer 中的数据刷新到 cache 上，然后通知其他的 CPU 核心的数据失效，然后其他 CPU 核心读取的时候从 这个 CPU 核心获取数据，就可以保证数据的可见性了

当然，如果是强制刷新到主存，那么就可以认为 CPU 核心不能从其他 CPU 核心中获取数据，而是只能从主存中获取数据，因此才需要强制刷新到主存中，因为其他 CPU 核心数据失效后不会从其他 CPU 中获取数据，而是需要从主存获取，所以需要保证主存中是最新的数据

以后讲的时候就从 CPU 核心能够从其他的 CPU 核心读取数据 和 CPU 核心不能够从其他 CPU 核心读取数据 这两个方面来讲
```



> ### 指令重排序

JVM 会将代码编译成一条条的指令，指令重排序就是 修改指令的执行顺序 来提高性能

下面是一段 C 语言代码：

```C
int a, b;
void foo(void)
{
	a = b + 11;
	b = 0;
}

```

通过工具查看编译结果：

```C
0000000000000750 <foo>:
 750:   90000080        adrp    x0, 10000 <__FRAME_END__+0xf6b8>
 754:   90000081        adrp    x1, 10000 <__FRAME_END__+0xf6b8>
 758:   f947dc00        ldr     x0, [x0, #4024] // 取b内存地址
 75c:   f947e821        ldr     x1, [x1, #4048] // 取a内存地址
 760:   b9400002        ldr     w2, [x0]        // 寄存器w2 = b(内存地址)
 764:   b900001f        str     wzr, [x0]       // b(内存地址) = 0
 768:   11002c40        add     w0, w2, #0xb    // 寄存器w0 = b + 11 
 76c:   b9000020        str     w0, [x1]        // w0寄存器的值存入a(内存)
 770:   d65f03c0        ret
 774:   d503201f        nop

```

**编译得到的汇编代码和我们原本的C语言代码不顺序并不一致**，相当于是下面的 C 语言代码：

```java
int a, b;
void foo(void)
{
    b = 0;
    a = b + 11;
}

```



在单线程时代这个没有什么问题，如果是在多线程环境下，指令重排序可能还是会造成数据不可见，因为有时候我们不只是要保证 volatile 变量的可见性，而是要利用 volatile 来保证 volatile 写前面所有的写操作的可见性

```java
class Main{
    private volatile boolean f = false;
	private int a = 2;
	private int b = 3;
    
    //线程 A 调用
    public void set(){
        a = 3;
        b = 4;
        f = true;
    }
    //线程 B 调用
    public void get() throw Exception{
        while(!f){
            Thread.sleep(100);
        }
        System.out.println(a);
        System.out.println(b);
    }
}
```

由于编译器是面向单线程的，只要对于单线程的上下文逻辑不变，就可能发生指令重排序

如果发生指令重排序使得 set() 中代码顺序改为：

```java
public void set(){
    a = 3;
    f = true;
    b = 4;
}
```

那么这就可能会导致 a 和 f 都写回主存，并且通知缓存行无效，但是 b 没有写回主存，或者 没有通知缓存行无效，这就导致 b 的数据不可见了，导致其他线程在 get() 中输出的 b 是一个旧值 3

 



可以转换为上面的例子看：

```C
class Main{
    private volatile boolean f = false;
	private int a = 2;
	private int b = 3;
    
    public void set(){
        a = 3;
        b = 4;
        barrier(); // 插入编译器内存屏障，禁止指令重排序
        f = true;
        barrier(); // 插入编译器内存屏障，禁止指令重排序
    }
    
    public void get() throw Exception{
        while(!f){
            Thread.sleep(100);
        }
        System.out.println(a);
        System.out.println(b);
    }
}
```





## 4、单例 - 双重检查



在 8 种单例模式写法中，有这么一种写法：

```java
class Singleton{
    //volatile 修饰，保证可见性和禁止指令重排
    private volatile static Singleton instance = null;
    
    //构造方法私有化
    private Singleton(){}
    
    public static Singleton getInstance(){
		//双重检查
        if(instance == null){
            //锁住 Class 对象，类锁（同时还有对象锁，注意，对象有多个，Class 类只有一个）
            synchronized(Singleton.class){
                if(instance == null){
                    //赋值方法非原子性
                    instance = new Singleton();
                }
            }
        }
        return instance;
    }
}
```



> ### **上面 volatile 有什么作用？**

很容易看出的是保证可见性，但是这个 禁止指令重排有什么作用呢？

我们需要先知道，上面那个赋值语句的执行 跟 i++ 一样不是原子性的，它分为如下操作：

- 分配内存空间
- 调用构造方法，创建实例对象
- 将对象所在地址返回给引用

如果发生指令重排的话，那么将 2 3 步调换，先分配内存空间，然后再直接将地址返回给引用，最后再创建实例对象

假设在将地址返回给引用，这时候 instance 引用指向了一个地址，不再为空

地址上的实例对象还没有创建的时候，这里也不说什么多线程和单线程，假设是单线程，那么到这一步前 CPU 调用其他的线程，发现 instance 不为空，那么直接拿去用了， 那么就自然存在空指针异常了，多线程同样问题

**注意：其他线程在判断的时候是在第一个判断 if(instance == null) 的时候发现不为空的，还没有到 synchronized，这时候跟 synchronized 保证的原子性无关**



> #### **上面 synchronized 有什么作用？**

保证赋值语句的原子性，上面也说了赋值语句 本身是非原子性的

因此需要赋值语句来保证原子性，这个不用讲



简单讲，volatile 主要是禁止指令重排序，防止 其他线程在第一个 if 判断处获取一个空对象

synchronized 是保证赋值语句的原子性，不会多个线程创建多个对象，这就不是真正意义上的单例



## 5、volatile、synchronized 和 lock 的区别

> ### volatile

- 保证可见性
- 禁止指令重排
- 不能保证原子性，并且只能修饰 共享变量

在只需要保证可见性 或者 禁止指令重排的时候可以使用，比如 CAS 中的 state 和 单例模式 进行 赋值语句指令重排



> ### synchronized

- 修饰方法和代码块
- 可见性（**线程获取锁前会清空工作内存，读取主存新值**）
- 原子性
- 可重入，并且有 偏向锁、轻量级锁、重量级锁 三种，会根据竞争情况进行锁升级
- 发生异常时会自动释放锁（在异常结束处加了一条字节码指令 monitorexit），但是如果捕获了异常就不会提前释放锁，会继续等待执行完毕才释放锁

如果仅仅需要同步线程，而不需要什么其他操作，使用 synchronized  就行了

**但 synchronized 锁不够灵活，获取锁的时候只能一直阻塞，不能够中断，因此容易造成死锁，在某些需要中断等待的场景也不能使用**



> ### lock

- 具有灵活性，**灵活性在于可以手动上锁和释放锁，并且可以指定等待锁的时间，不会死等**，线程池中 Worker 类 就继承了 AQS，赋予了 tryAcquire() 锁的语义，当调用 shutdown() 的时候线程池可以调用 teyLock() 判断线程是否空闲
- 能够保证可见性（**由于 state 是 volatile 的，所以释放锁，即修改 state 的时候，会将前面的操作一并刷新入内存，这样其他线程看得到了**）
- 可重入
- 可以配套使用 `lock.newCondition()` 来指定不同类型的锁对象，可以方便唤醒某种类型的线程，用于生产者消费者模式
- 发生异常不会自动释放锁，所以需要记住在 finally 处调用 unlock()

lock 的灵活性使得可以避免无限期的阻塞，以及可以用来线程池判断线程的状态

**使用 tryLock() 可以被中断**



