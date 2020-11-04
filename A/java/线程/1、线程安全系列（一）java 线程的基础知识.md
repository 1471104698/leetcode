# 线程基础知识



## 1、sleep() 和 wait() 的区别

sleep() 是 Thread 的静态方法，随时可以 进行调用，`Thread.sleep()`，它跟线程是否持有锁的状态无关，它不需要锁，调用后线程会挂起，如果持有锁的话也不会释放锁，因为说了跟锁无关，只是挂起后不会跟其他的线程争夺 CPU



wait() 是Object 的方法，它是一个实例方法，任何对象都相当于是继承了父类 Object 的这个方法，调用某个对象的 wait() 就表示当前线程 进入以 某个对象为锁对象的同步队列中，它需要配合 syn 锁使用，

- 如果syn 锁 锁的对象为 obj，那么后续调用 wait() 就应该用这个 obj 来调用，这样才能将线程放入 以 obj 作为锁对象的同步队列中
- 如果 syn 锁 锁的对象为 this,那么表示锁 的是当前对象，那么直接调用当前对象的 wait() 即可，它会进入当前对象的同步队列



它们都需要 捕获 中断异常，因为它们都可以被 线程的 interrupt()  方法打断，并且抛出中断异常

```java
public void h(){
    Thread thread = new Thread(() -> {
        synchronized (this) {
            try {
                wait();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    });
    thread.start();
    thread.interrupt(); //可以打断 wait
}


public void h(){
    Thread thread = new Thread(() -> {
        try {
            Thread.sleep(1100);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    });
    thread.start();
    thread.interrupt(); //可以打断 sleep
}
```



> ### wait(1000)与sleep(1000)的区别

sleep(1000) 表示将线程挂起，在未来的 1000ms 内不参与竞争 CPU，在 1000ms 后，会取消挂起状态，参与竞争 CPU ，这时不一定 CPU 就立马调度它，因此它**等待的时间的 >= 1000ms**



wait(1000) 跟 wait() 的差别在于 **wait(1000) 不需要 notify() 来唤醒**，它等待 1000ms 后，如果此时没有线程占有锁，那么它会自动唤醒尝试获取锁





## 2、线程等待的四种方式（多个线程等待，统一放行）

- 使用 CountDownLatch
- 使用 CyclicBarrier
- 使用 thread.join() 等待线程死亡
- 使用 线程池的 submit 提交任务后获取 Future 调用 get()

各自的使用场景：

- CountDownLatch 适合指定特定个数的线程执行完毕后主线程才能执行的场景（追求任意，不针对哪个线程）
  -  new CountDownLatch(5)：指定等待 5 个线程执行完
  -  在子线程尾部结束前调用  countDown() 减值，在主线程中调用 await() 进行阻塞，当减到 0 的时候，那么 await() 停止阻塞，主线程开始执行，
- CyclicBarrier 指定特定数量的线程在其指定的某个点暂停，然后统一执行（追求任意，不针对哪个线程）
  - new CyclicBarrier(5)：指定在某个暂停点等待 5 个线程
  - 各个子线程在某个位置调用 await() 陷入阻塞，当调用 await() 的线程数量到达 5 时，那么停止阻塞，开始执行，类似赛跑，跑道有 5 个位置，那么任意来齐 5 个人后，统一开始赛跑
- join() 适合少量线程的情况，因为需要调用特定线程的 join()（线程的数量确定，针对某个特定线程）
  - 线程 A 调用 线程 B 的 join()，那么线程 A 会陷入阻塞，直到线程 B 执行完毕
- 线程池的 submit + future.get() 跟 join() 差不多，同样是针对某个线程的，由于使用的是线程池，因此使用的情况是线程的数量是不确定的时候（线程的数量不确定，针对某个特定线程）
  - 提交任务时使用 submit，返回一个 Future，调用 get() 等待线程完成任务返回结果



> ### 能使用 sleep() 或者 while() 么

如果是仅仅的进行线程等待，那么可以，但是这里要求的是统一放行，**要从 能否实现 和 效率 的方面看**

如果使用的是 sleep() ，它需要指定睡眠的时间，各个线程的执行任务的时间都是不确定的，可能一个执行 100ms，一个执行 500ms，所以无法确定睡眠时间，即无法做到统一放行

如果使用的是 while()，其实也是可以的，可以使用一个 volatile boolean 变量，各个线程 自旋读取这个变量，修改后就能统一放行，但是自旋的话会一直占用 CPU，会降低 CPU 的执行效率去做这种无意义的事，当然，可能会说使用 wait() 什么的进行阻塞，但是使用 wait() 就需要用到锁，而一次只能有一个线程获取锁，怎么做到统一放行。。。





## 3、创建线程池的两种方式

1、通过 Executors 工厂创建，比如上面的 newCachedThreadPool，这里创建的都是内部定义参数

2、通过 new ThreadPoolExecutor() 自定义参数





## 4、创建线程的 三 种方式

这里的线程 指代的是执行体，比如 run() 或者 call()，而不是 new 出来的一个对象

- 继承 Thread 并重写 run()
- 实现 Runnable 接口 重写 run()，并通过 Thread 来运行
- 实现 Callable 接口 并实现 call()（类似 run()）并使用 FutureTask 进行封装
- 线程池的 execute() 和 submit()



Runnable 和 Callable 的区别：

1. Runnable 的方法体为 run()，没有返回值，Callable 的方法体为 call()，有返回值
2. Callable 由于需要阻塞等待返回值，所以需要跟 FutureTask 配合使用，封装为一个 FutureTask
3. Callable 的 call() 方法不是线程的最高层，所以它可以抛出异常，让 Futiure 的 get() 处理异常，而 Runnable 的 run() 是贤臣的最高层，所以它不能往上抛出异常，需要在 run() 内处理异常



## 5、线程、进程、线程池的状态变化

> ### 1、进程的状态

**进程至少存在 3 个状态：【运行、就绪、阻塞、退出】**

状态变化如下：

运行状态 -> 就绪状态、阻塞状态

就绪状态 -> 运行状态

阻塞状态 -> 就绪状态

**退出状态：成为僵尸进程**

![img](https://mmbiz.qpic.cn/mmbiz_png/J0g14CUwaZcvw4t9kicec370n3cvX2JS9EfRviciaGMLREQ1nqvjWkibKlREGPI9JyfhA5XlmzFRRiaIATAEiaLbCx4w/640?wx_fmt=png&tp=webp&wxfrom=5&wx_lazy=1&wx_co=1)

**阻塞事件**：比如 java 中的 scanner，即等待输入，等待的过程中，即使给进程 CPU 的控制权也没什么意义，因为它被 I/O 事件阻塞了，无法继续执行





> ### 2、java线程的状态

在 Thread.State 中定义了 6 种状态

```java
public enum State {
    NEW,
    RUNNABLE,
    BLOCKED,
    WAITING,
    TIMED_WAITING,
    TERMINATED;
}
```



初始化（NEW）：线程刚创建



运行（Runnable）：java 线程将 就绪（ready）和 运行中（running）合并为一种状态 Runnable，当调用 start() 时，该线程位于 就绪状态（ready）等待 CPU 调度； 当就绪状态中的线程获取到 CPU 时间片后，会变成 运行中状态（running）



等待状态（Waiting）：这种状态的线程 CPU 不会调度，需要显示唤醒，比如 notify()，进入该状态的方法为 wait()、Thread.join()



超时（timed_waiting）：这种状态的线程 CPU 不会调度，在到达一定时间后会自动唤醒进入该状态的方法为 wait(1000)、Thread.sleep(1000)、Thread.join(1000)



阻塞（Blocking）：线程因为某种原因停止 CPU 调度，暂时停止运行，直到事件解决后才会重新进入 运行状态中的就绪状态 等待 CPU 调度，进入该状态的方法比如 阻塞获取锁、IO 事件



终止（TERMINATED）：线程发生异常没有 try-catch 或者 执行完成 退出 run() 



 ![img](https://greenhathg.github.io/2019/08/04/Java%E7%BA%BF%E7%A8%8B%E7%9A%84%E7%8A%B6%E6%80%81/3.jpeg) 





> ### 3、线程池的状态

```java
    private static final int RUNNING    = -1 << COUNT_BITS;
    private static final int SHUTDOWN   =  0 << COUNT_BITS;
    private static final int STOP       =  1 << COUNT_BITS;
    private static final int TIDYING    =  2 << COUNT_BITS;
    private static final int TERMINATED =  3 << COUNT_BITS;
```

RUNNING：运行中，这时候会接受新的任务，并且去执行

SHUTDOWN：调用 shutdown()，表示线程池关闭，不接收新的任务，但会等待正在执行的线程执行完毕

STOP：调用 shutdownNow()，表示立即关闭线程，不接收新的任务，同时会中断正在执行任务的线程

TIDYING：在 SHUTDOWN 状态下，如果所有线程都执行完毕了，并且任务队列中的任务都销毁了 或者 在 STOP 状态下任务队列中所有任务都销毁了，那么会转换到这个状态，并且会执行钩子函数  terminated ()，线程池中默认 为空，需要用户去实现。这个钩子函数名为 terminated 跟下面的 终止状态名字一样，可以理解为最后的收尾工作

TERMINATED：线程池的最终状态，表示线程池彻底终止了





## 6、java 线程 和 操作系统线程的关系

具体看  https://www.cnblogs.com/lusaisai/p/12729334.html 



在 linux 系统下启动一个线程，代码如下：

```C
#include <pthread.h>//头文件
#include <stdio.h>
pthread_t pid;//定义一个变量，接受创建线程后的线程id

//定义线程的主体函数，类似于 java 中的 run()
void* thread_entity(void* arg)
{   
    printf("i am new Thread!");
}

//main方法，程序入口，main和java的main一样会产生一个进程，继而产生一个main线程
int main()
{
    /*
    调用 操作系统 的 pthread_create() 函数 创建线程，注意四个参数， 
    pid 是指针，内部创建后会将让这个指针指向线程的 id 
    NULL
    thread_entity 相当于线程指向的主体，即 java 中的 run()
    NULL
    */
    pthread_create(&pid,NULL,thread_entity,NULL);
    //usleep是睡眠的意思，那么这里的睡眠是让谁睡眠呢？
    //为什么需要睡眠？如果不睡眠会出现什么情况?? 不清楚，不想知道
    usleep(100);
}
```

我们可以看出，Linux 中创建线程是调用 pthread_create() 函数来创建线程的



在 java 中，线程的创建又是如何的呢？

```java
public static void main(String[] args) {
    new Thread().start();
}

public synchronized void start() {
    start0();
}

private native void start0();
```

当我们指向 t.start() 的时候，实际上内部调用的是一个 本地方法 start0()，本地方法不是 java 语言写的，这里的 start0() 是使用 C 写的

当我们 new Thread()  的时候实际上就是跟上面的 linux demo 一样，创建出来的只是一个线程的主体，即线程要执行的内容，而不是一个真正的线程，只有在调用 start() 后，jvm 才会创建出一个线程

我们可以猜测 java 线程创建的调用链 start() -> start0() -> pthread_create()



通过查看 openJDK，可以发现以下调用链：

```C
static JNINativeMethod methods[] = {
    {"start0",           "()V",        (void *)&JVM_StartThread},
};
```

这是一个本地方法表，start0 这个本地方法对应的就是 JVM_StartThread 方法

可以在 jvm.h 头文件（类似于 java 接口）中找到这个方法

```C
/*
 * java.lang.Thread
 */
JNIEXPORT void JNICALL
JVM_StartThread(JNIEnv *env, jobject thread);
```

然后在 jvm.h 的实现文件 jvm.cpp 中找到这个方法的具体实现

```C
JVM_StartThread(JNIEnv* env, jobject jthread){
    //xxxxx，省略代码
    
    JavaThread *native_thread = NULL;
    native_thread = new JavaThread(&thread_entry, sz);//关键代码，创建一个 javaThread
    
    //xxxxx，省略代码
}
```

再看 new JavaThread() 内部的逻辑

```C
JavaThread::JavaThread(ThreadFunction entry_point, size_t stack_sz){
    //xxx. 代码省略      
    os::create_thread(this, thr_type, stack_sz);//这里创建线程

    //xxx. 代码省略      
}
```

调用了 linux 的 create_thread() 方法，再跟进这个方法查看内部逻辑

```C
bool os::create_thread(Thread* thread, ThreadType thr_type, size_t stack_size) {
	//xxx. 代码省略      
    pthread_t tid;
    int ret = pthread_create(&tid, &attr, (void* (*)(void*)) java_start, thread);//linux系统的线程调用函数
    
    //xxx. 代码省略      
}

```

我们可以发现，最终在 create_Thread() 里调用了 pthread_create() 创建了一个线程



因此，java 线程的创建底层是调用 linux 的 pthread_create() 创建的

java 线程就是 linux 系统的线程





## 7、java 程序启动时会创建几个线程

可以通过打印出 JVM 中的所有线程信息

```java
public class ThreadNumDemo {
    public static void main(String[] args) {
        ThreadMXBean threadMXBean = ManagementFactory.getThreadMXBean();
        ThreadInfo[] threadInfos = threadMXBean.dumpAllThreads(false, false);
        for (ThreadInfo threadInfo : threadInfos) {
            System.out.println(threadInfo.getThreadId() + "-" + threadInfo.getThreadName());
        }
    }
}
```

输出结果为：

```java
6-Monitor Ctrl-Break	//检测死锁
5-Attach Listener		//接收外部的 jvm 命令，比如 java -version
4-Signal Dispatcher		//Attach 线程接收命令后，会交给这个线程分发到不同的模块去处理
3-Finalizer				//执行 finalize() 方法的线程
2-Reference Handler		//垃圾回收时处理 强软弱虚 引用
1-main					//主线程
```

在 JDK1.8 中，可以发现一个 java 程序启动时，会创建 6 个线程

