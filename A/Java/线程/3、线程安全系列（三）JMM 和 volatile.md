# JMM 和 volatile



## 1、JMM（Java 内存模型）



JMM 规定了 内存只要有两种：主存 和 工作内存

主存 就是 物理内存，一般是堆栈内存空间（堆栈都存在 变量和对象），工作内存是一层抽象的概念，指代 CPU cache 和 CPU 寄存器

工作内存 中的数据是对主存中的 一部分数据的 拷贝，线程 不能直接操作主存的数据，需要先将主存的数据拷贝到 工作内存中，然后在工作内存中修改完数据，再将工作内存中的数据写回主存

这么做的原因是可以控制多个线程的 读写操作，免得多线程操作主存数据需要对主存数据加锁，这样降低了并发效率

 ![img](https://picb.zhimg.com/80/v2-f0364f6f863d5730e2b962ac6b3387e2_720w.jpg) 

上面说了，线程的工作内存  CPU cache 和 寄存器，学习过 CPU 缓存一致性协议 MESI 就会知道， CPU 多核心 已经通过该协议保证了数据的一致性了，那么为什么还会需要 volatile 呢？

因为 CPU 默认使用了 Store Buffer 和 无效队列，而 Store Buffer 只能保证 单核 CPU 的可见性，保证多核 CPU 的可见性需要由 内存屏障 来保证，而 volatile 就是 自动添加 读 和 写 内存屏障，同时 volatile 是 Java 语言层面的，它需要禁止编译器进行重排序，保证在它之前写的其他变量的可见性



## 2、volatile 实现原理

 [内存屏障及其在 JVM 内的应用（下） - 知乎 (zhihu.com)](https://zhuanlan.zhihu.com/p/137460543) 



volatile 只能保证可见性，不能保证原子性，因为 volatile 不能禁止多个 CPU 核心同时持有 volatile 变量

```java
volatile 保证可见性是通过 happens-before 规则实现
但 happens-before 规则 实际上是为了不让程序员去过多干涉内部细节而衍生出的一个方便理解 volatile 的理论而已
```

由于 CPU 的 StoreBuffer 和 无效队列 的原因 导致了内存可见性问题

使用 volatile 它会自动添加 **内存屏障**（具体看 CPU 缓存一致性就知道了）



volatile 涉及 4 种内存屏障：

- read① LoadLoad read②：read① 必须在 read② 之前执行，并且在执行 read② 前需要清空无效队列
- write① StoreStore write②：write① 必须在 write② 之前执行，并且在执行 write② 之前，将 StoreBuffer 中的数据刷新回缓存，同时发布无效消息
- read① LoadStore write②：read① 必须在 write② 之前执行
- write① StoreLoad read②：write① 必须在 read② 之前执行，该屏障集合了 LoadLoad 和 StoreStore 的功能

这四种内存屏障看看就好，目前没有什么文章深入解说这四个，都是讲得不明不白，反正理解 volatile 也不需要去理解这四个屏障，只要知道利用这些内存屏障保证可见性 和 禁止指令重排序即可

主要是需要知道 **volatile 如何通过禁止指令重排来实现其他变量的可见性**

 

volatile 保证其他变量的可见性有以下情况：

1、v 写 和 普通写

```java
public class Main(){
	int a = 0;
	volatile int b = 0;
	
	public void h(){
        //普通写
		a = 1;
        //v 写
		b = 2;
	}
}
```

对于一个 v 写，它必须禁止跟在它上面的 普通写 重排序

先让 普通写 写入到 StoreBuffer 中，然后再进行进行 v 写，然后将 StoreBuffer 中的数据刷新回 cache，通知其他 CPU 核心数据无效，这样 v 写 和 在它上面的普通写 都对其他 CPU 核心可见了

如果将 v 写 和 普通写 重排序，那么先写 v 写，那么只有 v 写的数据对其他 CPU 核心可见，而后面的普通写的数据还留在 StoreBuffer 中



2、v 读 和 普通读

```java
public class Main(){
	int a = 0;
	volatile int b = 0;
	
	public void h(){
        //v 读
        int i = b;
        //普通读
		int j = a;
	}
}
```

对于一个 v 读，它必须禁止跟它下面的 普通读 重排序

因为进行 v 读的时候，会清空无效队列，将 cache line 置为无效，从主存中读取新数据，如果 普通读 的变量已经被 CPU 其他核心给修改了，那么随着无效队列的清空，它所在的 cache line 也会被置为无效，下次读取的时候就会去主存读取最新值

如果将 v 读 和 普通读 重排序，那么先执行普通读，由于没有清空无效队列，所以 CPU 核心仍然认为它 cache 中要读取的变量是有效的，那么就会导致无法读取最新值

所以我们一般都是先进行 v 读的





## 3、双重检查 DCL 语句分析



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



