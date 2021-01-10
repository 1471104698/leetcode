#  简单总结六



## 1、线程池

```java
1、为什么需要线程池？
线程池用来统一管理线程的创建、复用 和 销毁线程
如果没有线程池，存在以下缺点：
1）频繁的创建和销毁线程，需要调用内核的方法来创建，会浪费 CPU 的资源
2）每个请求都使用一个新的线程，需要等待线程创建完成，线程创建完成前的这段时间不能执行请求（虽然以我们肉眼来看创建线程很快，但是换算到 CPU 执行速度就很慢了）
3）每个线程都需要占用一定的空间，当创建线程过多，那么可能会导致 栈 OOM（一般是达到进程所能分配的最大内存空间）
4）不合理的线程数，会频繁导致线程的上下文切换，而一次上下文切换的时候 CPU 能够执行 几百、几千、几万条指令

2、线程池的七大参数：
1）核心线程数 core
2）最大线程数 max
3）线程最大的空闲时间
4）时间单位
5）阻塞队列
6）线程工厂
7）拒绝策略

3、ThreadPoolExecute 内部制定的四大拒绝策略
①、抛异常
②、直接丢弃，什么都不做
③、如果线程池还没有关闭，那么将任务队头头部的任务丢弃，将该任务使用 execute() 提交
④、如果线程池还没有关闭，那么由提交任务的线程处理该任务
	（这个策略的好处在于，一是新提交的任务不会被丢弃，二是由于提交任务的线程来执行任务，
        而任务的执行通常需要较长的时间，这样就不会有新的任务提交，给线程池一个缓冲区，
        当然，有利有弊，弊就是不能再去处理其他请求。）
    
    public static class DiscardPolicy implements RejectedExecutionHandler {
        public void rejectedExecution(Runnable r, ThreadPoolExecutor e) {
            //什么都不做，相当于直接舍弃掉任务
        }
    }
	public static class AbortPolicy implements RejectedExecutionHandler {
        public void rejectedExecution(Runnable r, ThreadPoolExecutor e) {
            //抛异常
            throw new RejectedExecutionException("Task " + r.toString() +
                                                 " rejected from " +
                                                 e.toString());
        }
    }
    public static class DiscardOldestPolicy implements RejectedExecutionHandler {
        public void rejectedExecution(Runnable r, ThreadPoolExecutor e) {
            //如果线程池没有关闭，那么将任务队列队头的任务舍弃，然后将任务提交上去
            if (!e.isShutdown()) {
                e.getQueue().poll();
                e.execute(r);
            }
        }
    }
	public static class CallerRunsPolicy implements RejectedExecutionHandler {
        public void rejectedExecution(Runnable r, ThreadPoolExecutor e) {
            //如果线程池没有关闭，那么由当前提交任务的线程执行任务
            if (!e.isShutdown()) {
                r.run();
            }
        }
    }

4、线程池任务提交过程：
1）判断线程池是否在 RUNNING 状态
2）如果线程池处于 STOP、SHUTDOWN、TIDYING、TERMINATED，那么执行拒绝策略；如果还在运行状态，那么判断线程数是否到达 core，如果没有那么创建一个新线程来运行，如果已经到达 core，那么判断将它存储到任务队列中
3）如果存在成功，那么直接返回；如果存在失败，即任务队列已满，那么尝试创建一个 非 core 线程
4）如果创建成功，那么使用该线程运行任务，如果创建失败，那么执行拒绝策略。

5、线程池任务执行原理
1、Worker 类
线程池中维护的是 Worker 类的集合
Worker 类是线程池的一个内部类，它继承了 AQS，重写了 tryAcquire() 和 tryRelease()，并且跟 ReentrantLock 一样定义了 tryLock()、lock() 和 unlock()，不过没有实现锁的重入性
Worker 同时实现了 Runnable 接口，在 run() 方法中调用了 runWorker()，该方法是执行任务和获取任务的主要逻辑，后面讲。
同时在 Worker 中维护了一个 thread，在创建 Worker 时，即构造方法中同时会通过 ThreadFactory 线程工厂为该 Worker 创建一个线程。

2、execute()
通过 execute() 执行任务，会先进行上面的 4、任务提交过程，其中创建线程就是在创建一个 Worker，如果创建成功了，那么会将 Worker 添加一个存储 Worker 的 set 中，同时调用 worker 内部的 thread 来执行任务。

3、Worker 的 runWorker()
runWorker() 内部有一个 while()，判断体内是判断当前 Worker 是否存在初始任务，即创建 Worker 时顺便塞给它的任务，如果有那么执行，如果没有那么调用 getTask() 获取任务。
当存在初始任务 或者 获取到任务后，那么会调用 lock() 进行加锁，然后再调用 task.run() 执行任务，当任务执行完成后，会调用 unlock() 进行解锁，然后继续在 while() 方法体中调用 getTask() 获取任务

4、getTask()
getTask() 是线程用来获取任务的。
在该方法中，会先对线程池的状态进行判断：
①、如果线程池处于 SHUTDOWN 状态，那么判断任务队列队列是否为空，那么不为空，那么继续执行获取任务，如果为空，那么返回 null
②、如果线程池处于 STOP、TIDYING（tidying）、TERMINATED，那么直接返回 null
当能够继续往下执行获取任务时，涉及两个变量 timed 和 timeOut，它们都是用来进行线程的回收的
对于 timed，如果设置了 core 线程可以回收 或者 线程数已经超过了 core，那么 timed = true，表示该线程可以回收
在下面线程会在阻塞队列中获取任务，阻塞的时间为 线程池七大参数中的最大空闲时间，如果超过了这个时间，那么会将 timeOut = true，那么在下次循环中返回 null，该线程被回收
如果线程在等待过程中被中断了，那么在 catch 中 timeOut = false，即再给该线程一次获取任务的机会

相当于如果线程是 非 core，而又没有任务需要给该线程执行了，那么就回收该线程

当 getTask() 返回 null 时，那么就会退出 runWorker() 中的 while()，然后执行线程回收前的收尾工作，因为该线程可能是线程池中的最后一个线程，需要将线程池的状态进行转换 STOP/SHUTDOWN -> TIDYING -> TERMINATED。然后退出 runWorker()、run()，结束生命周期

5、submit()
submit() 返回的是一个 FutureTask，它能够接收 Runnable 和 Callable 两种类型的任务
在该方法中，无论是 Runnable 还是 Callable，都会被封装为 FutureTask，然后调用 execute() 将 FutureTask 传入执行，然后再将 FutureTask 返回给调用线程

6、FutureTask
FutureTask 实现了 Runnable 接口，所以它能够传入 execute() 中被调用
在 FutureTask 中，只维护了一个 Callable 变量，它存在两个构造方法，一个用来接收 Runnable，一个用来接收 Callable，对于 Runnable 任务，它会使用适配器（适配器模式，跟 Spring MVC 的 HandlerAdapter 一样）将 Runnable 转换为 Callable 任务，实际上就是定义一个 CallableAdapter，内部维护一个 Runnable 变量，然后在 call() 中调用 run()，这样的话 FutureTask 就不需要为 Runnable 单独设计一条调用方案，无论是 Runnable 还是 Callable 都可以直接当作 Callable 来处理。

当 submit() 调用 execute() 提交 FutureTask 后，线程会调用 FutureTask 的 run()，而在 run() 中会调用 Callable 任务的 call()。
通过返回的 FutureTask，可以调用它的 get() 来阻塞等待任务的完成，同时获取任务的返回结果。

7、FutureTask 的 get()
在 get() 中会判断任务是否已经完成，如果没有完成，那么调用 awaitNode() 将当前线程封装为 Node 存储到 FutureTask 维护的阻塞队列中调用 park() 进行阻塞等待，当 FutureTask 的 run() 中的 call() 任务完成后，会在最后从阻塞队列中获取线程节点 Node，然后调用 unpark() 唤醒线程，使得阻塞线程在 get() 中停止阻塞，返回结果。



8、线程池的 shutdown() 和 shutdownNow()
1）在 shutdown() 中，会先将线程的状态设置为 SHUTDOWN，然后遍历 Worker 集合，调用它们的 tryLock()，如果获取成功，表示线程没有在执行任务，那么调用它的 interrupt() 标识中断线程；如果获取失败，表示线程正在执行任务，那么暂时不管。
然后尝试将线程池的状态转换为 TIDYING，前提是 任务队列中不能有任务，并且线程池的工作线程数为 0，那么就可以转化为 TIDYING，调用钩子函数，再转换为 TERMINATED，最终完全的关闭线程池
2）在 shutdownNow() 中，会先将线程池的状态设置为 STOP，然后将所有的 Worker 线程不管是否是空闲状态，都调用 interrupt() 标识为中断，然后清空任务队列中的任务，不再执行，最后再尝试将线程池的状态转换为 TIDYING，前提是 工作线程数为 0，因为调用 interrupt() 并不能强制让工作线程关闭。

我们可以看出线程池不是直接从 SHUTDOWN/STOP 直接转换为 TERMINATED 的，而是需要先转换为 TIDYING，执行钩子函数，再转换为 TERMINATED，可以当作 TIDYING 是线程池关闭的收尾工作

9、线程池关闭调用 interrupt() 如何中断线程？
当线程池被关闭：
1）在 getTask() 中，最前面会判断线程池是否关闭，如果在任务队列中阻塞的线程会抛出异常，然后在下一次循环中发现线程池关闭了，那么返回 null
2）在 task.run() 中如果线程阻塞了，那么会抛出异常，停止执行；如果没有阻塞，那么该任务会正常执行。最后都是在 getTask() 中返回 null
    
    
10、线程池的 5 种状态变化：
1）RUNNING：线程池创建时就处于运行状态，正常工作。
2）SHUTDOWN：调用 shutdown() 线程池会处于该状态，同时会中断空闲线程，拒绝任务提交：在 execute() 中发现处于该线程会拒绝任务提交
3）STOP：调用 shutdownNow() 线程池会处于该状态，同时会中断所有线程，清空任务队列，拒绝任务提交
4）TIDYING：SHUTDOWN/STOP 在转换为 TERMINATED 前都需要先转换为该状态，执行钩子函数，做线程池关闭最后的收尾工作，该钩子函数是一个空方法
5）TERMINATED：线程池完全关闭
```





## 2、HashMap

```java
1、HashMap 如何保证初始容量为 2 的幂
传参初始值为 n，然后执行 n - 1，防止 n 已经是 2 的幂这种情况
然后执行 n |= n >>> 1; n |= n >>> 2; n |= n >>> 4; n |= n >>> 8; n |= n >>> 16; 
1 + 2 + 4 + 8 + 16 = 31，最多能够将低 31 bit 全部变成 1，然后最后 n + 1，晋升为 2 的幂

2、HashMap 计算 key hash 的 hash()
这里讲的 key hash 是 JDK 8 的
使用 key 的 高 16 位 异或 低 16 位
个人认为是让高 16 位也参与运算，避免只有低位参与运算，增加了 hash 冲突
同时使用 异或 是为了让概率分布均等，因为 按位与 & 和 按位或 | 它们得到 1 和 0 的概率都不均等

3、get()
JDK 7：通过 hash() 计算 key hash，定位到槽位上，然后判断槽位是否为空，如果为空那么直接返回,如果不为空，那么扫描链表。
JDK 8：跟 JDK 7 差不多，主要是多了 头节点的判断 和 红黑树的判断

4、put()
JDK 7：计算 key hash，定位到槽位上，扫描一遍链表，判断是否存在旧节点，如果存在那么进行旧值替换，如果不存在，那么调用 addEntry() 插入。在插入前，会先进行扩容判断，当不需要扩容或者扩容判断完成后，使用头插法插入。（JDK 7 的扩容在多线程下会产生死循环，虽然本来就不支持多线程）
JDK 8：计算 key hash，定位到槽位上，判断是否为空，如果为空直接插入，不为空，判断是否是红黑树节点，如果是那么调用红黑树的插入方法，如果不是，那么扫描链表，在扫描的过程中会同时计算链表节点的个数，如果存在旧值，那么直接替换，返回，如果不存在，使用尾插法插入，同时判断 链表节点个数是否达到阈值，如果达到阈值，同时还需要判断数组长度是否到达 64，如果没有到达 64，那么进行扩容，如果节点个数到达阈值并且数组长度大于等于 64，那么将链表转换为红黑树

5、resize()
JDK 7：创建一个新的 table，容量为旧数组的 2 倍，然后扫描所有的槽位，将槽位上的节点一个个使用头插法迁移到新数组上（多线程出现死循环），如果 hashSeed 不为 0，那么会重新计算 hash
JDK 8：创建一个新的 table，容量为旧数组的 2 倍，然后扫描所有的操作，如果槽位为空，那么跳过，
如果是红黑树节点，那么调用红黑树的迁移方法；
如果是链表，那么定义 4 个变量，作为高低链表，用来存储会迁移到原位置的节点 和 会迁移到新位置的节点，而由于是扩容为原来的两倍，所以新位置也是在旧位置上再加旧数组的长度，即 new_i = old_i + oldCap，而会迁移到新位置上的节点只有在 oldCap 处的 bit 为 1，因此直接使用 hash & oldCap，如果不为 0，那么表示该节点会迁移到新位置，那么尾插法插入到高链表，否则插入到低链表。当该槽位上的节点全部扫描完成后，将高低链表分布插入到 新 table 的两个位置


6、hash()
JDK 7：使用一个 hashSeed，应该是用来防止 哈希洪水攻击，比如 ACM 一些比赛专门就是用来哈希特定的生成规则来产生数据上的哈希冲突。然后经过几次异或得到结果
JDK 8：上面讲的 高 16 位 异或 低 16 位, JDK 8 没有 hashSeed，但是通过链表转红黑树的方式来提高查找效率，同时能够避免 扩容时 rehash 的情况。

HashMap 由于不是线程安全的，所以 key 可以存储一个 null 值，默认 key hash = 0，value 可以存储多个 null 值（不过现在设计者认为 HashMap 可以存储 null 是失败的设计）


7、为什么 HashMap 是 2 的幂？
理由有 2：
1）& 比 % 快，提高效率：因为如果是 2 的幂，那么可以使用 & 来代替 %,比如 n % 8 和 n & 7 最终是能够达到相同的效果的，即能够落在 [1, 7] 之间，而 & 是二进制位运算，比起 % 这种需要转换为十进制运算的要快得多
2）减少 hash 碰撞：在使用了上述的 & 提高效率的前提下，还能够减少 hash 碰撞，因为如果偏偏想要非 2 的幂，又想要使用 & 提高效率，假设 n = 9(1001)，那么 n - 1 后就变成 8(1000)，这样 n & 8 就相当于是 n & (1000)，基本低位的 3 个 0 都不会参与运算，这样的话只有高位的 1 决定位置，只能存储在 slot 0 和 slot 8 两个槽位，hash 碰撞产生的几率太大了，并且其他的槽位也用不上，因此如果是非 2 的幂，只能使用 %。
所以使用 2 的幂，在提高效率的同时，又能减少哈希碰撞。
```





## 3、ConcurrentHashMap

```
1、数据结构
JDK 7：使用分段锁 volatile + Segment + 链表，Segment 继承了 ReentrantLock，但存在线程写操作时，那么调用 lock() 加锁，因此如果 table 长度为 16，每个 Segment 管理 4 个槽位，那么最多可以支持 4 个线程并发修改
JDK 8：使用 volatile + sync 锁 + CAS + 链表 + 红黑树，锁的粒度减小，每次只锁一个槽位，因此最多可以支持 table.length 个线程并发修改

JDK 7 和 JDK 8 都存在 3 处 volatile：
1）table 数组：保证扩容可见
2）Node 的 next：保证引用修改可见
3）Node 的 value：保证数据修改可见

2、get()
JDK 7：跟 JDK 7 的 HashMap 差不多
JDK 8：跟 JDK 8 的 HashMap 差不多，不过，它除了链表节点 和 红黑树节点外，还添加了两种新的节点：TreeBin 和 ForwardingNode，它们都继承了 Node，并且在 Node 中添加了一个新的方法 find()，它们都重写了这个方法。
TreeBin 用来封装红黑树节点 TreeNode，ForwardingNode 是扩容节点，在扩容时使用的，在 get() 中对于这两种节点的查询使用多态来解决，直接调用 Node 的 find()

3、put()
JDK 7：计算 key hash，定位到槽位上的 Segment，调用该 Segment 的 lock() 获取锁，如果已经有别的线程获取了锁，那么 park 等待。获取锁后，通过 key hash 定位到 table 数组的槽位上，然后扫描槽位上的链表， 如果存在旧值，那么进行替换，返回；如果不存在，那么创建一个 Node 节点 node，使用头插法将 node.next = tab[i]，此时 tab[i] 还没有指向这个 node，它会再判断是否需要进行扩容，如果需要，那么调用扩容方法，如果不需要，那么再将 tab[i] 指向 node，然后 size++。
JDK 8：计算 key hash，定位到 table 槽位上，判断 tab[i] 是否为空，如果为空，那么使用 CAS 将 node 插入，如果插入成功，返回，插入失败，那么进入下一次循环。再判断是否正在发生扩容，如果是，那么协助扩容。上面使用 f 变量来指向 tab[i] 头节点，此时使用 sync 锁 对 f 进行加锁，当加锁成功后，再判断 f 是否等于 tab[i]，如果不等于，表示当时在等待锁的时候持有锁的线程修改了头节点，那么释放锁，进入下一个循环。判断是否是红黑树节点 TreeBin，如果是的话，那么调用红黑树的插入方法，如果不是，那么扫描一遍链表，替换旧值 或者 尾插法插入节点，然后最后判断是否需要转换为红黑树。释放锁，然后调用 addCount() 将节点个数 +1

4、size()
JDK 7：由于采用分段锁，节点的个数统计都分散在各个 Segment 的 size 中，因此需要统计各个 Segment 的 size。首先会进行三次不加锁的统计，如果第一次统计结果和第二次统计结果一致，那么返回，如果第二次统计结果和第三次统计结果一致，那么返回。如果都不一致，那么将所有的 Segment 加锁后再进行一次统计，最后释放锁
主要是不想因为统计个 size 就获取所有的锁，导致其它线程阻塞
JDK 8：它使用一个全局的 baseCount 和 一个 CounterCell 数组用来统计 size，在 put()、remove() 释放锁后都会调用一个 addCount()，这个 addCount() 就是来修改节点个数的。
由于 JDK 8 只是对一个槽位加锁，所以如果需要修改节点个数，那么有两种手段：
①、将所有的槽位加锁，然后将 size++，因为如果对单个槽位加锁，那么其他槽位的线程也可以修改 size
②、使用 CAS 修改 size
这里使用的是第二种方法，但是如果单单对一个变量进行 CAS，那么在高并发情况下，一次只能有一个线程 CAS 成功，那么显然效率有点低，因此它设计出了 CounterCell 数组，CounterCell 类内部只有一个 long 型变量，用来帮助统计 size.
在 addCount() 中，线程会先对 baseCount 进行一次 CAS，如果修改失败，那么将获取当前线程的 探针哈希值 h，初始值为 0，那么就是对 CounterCell 数组中 0 号槽位的 CounterCell 进行 CAS，如果 CAS 成功，那么返回，如果失败，那么对 h 进行初始化修改，然后再对对应槽位上的 CounterCell 进行 CAS，不断修改 h 和 不断 CAS，直到成功为止。
最后在 size() 中就是统计 baseCount 和 所有的 CounterCell 的值。
通过这种方法，减少 CAS 次数。

5、resize()
JDK 7：先创建一个新数组，容量为旧数组的 2 倍，扫描所有的槽位，对于每个槽位，都会进行两次扫描，第一次扫描是找到末尾节点在迁移后处于同一个槽位的连续节点复用起来，第二次扫描是从头开始扫描，忽略复能够用的节点，为每个节点创建新的节点，使用头插法插到迁移的槽位上。
这里有个问题：next 并没有跟 JDK 1.6 一样使用 final 修饰，它是可以改变指向的，那么全部可以进行复用，为什么还需要创建新节点呢?
个人认为这里是为了考虑 get() 线程，它创建新节点的目的很容易可以看出来是为了不破坏原有链表的结构，复用节点也是复用不需要进行改变指向的节点。因此只能是为 get() 线程考虑。比如原有链表为 1->2->3->4，线程在 get(3)，结果查询到 2 的时候，发生扩容，导致链表拆分为 1->3 和 2->4，那么对于该线程来说，它下一个查询的节点就变成了 4，导致数据明明存在但是却返回了 null。
JDK 8：这里的扩容方法是能够多个线程一起帮助扩容的，那么按照 CPU 的核心数来限制同时扩容的线程数，避免出现多余的线程上下文切换，同时每个线程负责部分槽位，比如 table 长度为 64，最低限制每个线程负责 16 个槽位，那么这里的 table 最多 4 个线程来进行扩容，第一个线程发现 newTab = null，那么进行初始化，将 newTab 设置为旧数组的两倍，然后创建一个 ForwardingNode，将 newTab 传入到该节点中，然后遍历它分配到的槽位，比如 [48, 63]，遍历这些槽位：
    1）如果该槽位 tab[i] = null，那么将 Forwarding 节点存储到 tab[i] 上
    2）扫描该槽位，如果是红黑树，那么调用红黑树的迁移方法，如果是链表，那么结合 JDK 8 的 HashMap 和 JDK 7 的 ConcurrentHashMap 的扩容方法，定义四个变量，作为高低链表，然后对进行两次扫描，第一次扫描找到末尾能够复用的几个节点，第二次扫描创建新的节点，这里使用的是头插法，所以节点会反转。
    3）当扫描完成后，将高低链表插入到 newTab 上，然后将 tab[i] 的 Node 节点替换为 ForwardingNode
上面创建新节点跟 JDK 7 一样，是为 get() 线程考虑的，而在扫描完成后，将 ForwardingNode 替换到 tab[i] 上同样是为 get() 线程拷贝：因为首先有多少个线程在扩容就对应多少个 ForwardingNode，但是这些 ForwardingNode 内部维护的都是相同的一个 newTab，不过它们迁移的槽位不同，比如 线程 1 负责将 tab 的 0 号槽位的数据迁移到 newTab 的 0 号 和 4 号槽位上，线程 2 负责将 tab 的 1 号槽位的数据迁移到 newTab 的 1 号 和 5 号槽位上。在 get() 中，如果判断 Node 为 ForwardingNode，那么会调用它的 find()，ForwardingNode 中重写的 find() 是用来在 newTab 上进行查询的，它会根据 key hash 重新计算槽位，这样的话，原本 tab 一个槽位上的数据分为 newTab 的两个槽位，即数据进行了分摊，它根据 key hash 可以定位到 newTab key 所在的槽位，减少扫描的数据量。
```





## 4、ThreadLocal

```java
ThreadLocal 用来存储每个线程的副本，使得线程之间进行数据隔离

1、Thread、ThreadLocal、ThreadLocalMap 的关系
Thread 维护了一个 ThreadLocalMap，初始值为 null，即每个 Thread 都会有各自的一个 ThreadLocalMap
在 ThreadLocal 中，无论是 get()、set()、remove() 都是通过当前 Thread 获取其内部的 ThreadLocalMap，这样的话就能够做到变量的存储跟只跟线程相关

2、ThreadLocalMap 的数据结构
ThreadLocalMap 维护了一个 table 数组，跟 HashMap 差不多，不过它的 Entry 继承了弱引用，key 是 ThreadLocal 对象，value  是要存储的值，它将 key 传入弱引用中，获取的时候需要调用 e.get() 来获取，而弱引用则是发生 GC 时，无论是否内存不足，只要对象不存在强引用，那么被 GC 扫描到了那么就会进行回收。（软引用则是内存不足时才会回收，即在抛出 OOM 前一定会回收软引用）

3、tab 解决 Hash 冲突：
ThreadLocalMap 解决哈希冲突使用的是开放地址法，当发生冲突时往后面扫描找到一个 tab[i] = null 的位置存储。
这也就导致了，如果一旦移除了某个 entry，或者 某个 entry 的 key 被 GC 回收变成 脏 Entry 了，那么就必须将它后面的 entry 重新进行 hash 计算，重新定位槽位，否则可能会出现数据存在但是却找不到的问题：
	存在 4 个槽位
	1	2	3	4
	插入 A 对象，定位到 1 号槽位，发现 tab[1] = null，直接插入
	1	2	3	4
	A
	插入 B 对象，定位到 2 号槽位，发现 tab[1] != null，往后找一个位置，发现 tab[2] = null，直接插入
	1	2	3	4
	A	B
	A 对象被 remove() 了，那么剩下 B 对象，而如果 get() B 对象的时候，它会定位到 1 号槽位上，发现为 null，那么认为数据不存在，直接返回 null，这就导致数据存在但是不可见的问题
	1	2	3	4
		B
	需要将 B 对象重新进行 hash
	1	2	3	4
	B
	
4、为什么 ThreadLocalMap 要继承弱引用？
都说继承弱引用是为了解决内存泄漏的问题，这里我们先讲一下如果不继承弱引用，而是使用强引用的情况。
如果使用的是强引用，那么在 remove() 中是直接将 key、value、tab[i] 都置空的，同时将后面 entry 重新 hash，那么不会存在什么内存泄漏问题，前提是程序员使用完都能够记得调用 remove()。
不过问题在于，如果程序员一旦忘记调用 remove()，而方法丢失了对这个 ThreadLocal 的强引用，那么就会导致 entry 对 ThreadLocal 存在强引用，但外部却无法访问到它，即这个 entry 已经没有任何价值，但是却不能被回收的问题。

而如果使用弱引用，一旦程序员忘记调用 remove()，并且外部丢失了对 ThreadLocal 的强引用，那么在 GC 的时候检测到这个弱引用对象会将它进行回收，避免了内存泄漏。但是 value 对象还存在着强引用，它不能被 GC 进行回收，所以在 ThreadLocalMap 中的 get()、set()、remove() 中都会对这些 脏 Entry 进行处理。

5、为什么 ThreadLocalMap 不将 value 设置为弱引用？
如果将 value 设置为弱引用，那么它只存在 entry 对它的弱引用，那么发生 GC 就被回收了，那么下次 get() 的时候莫名其妙值不见了，返回 null。

6、ThreadLocalMap 处理 脏 Entry 的方法：
ThreadLocalMap 存在三个方法来处理脏 Entry，其中 get()、remove() 调用了其他一个方法，set() 会涉及到这三个方法
1）在 get() 的时候，如果扫描过程中发现 e != null && e.get() == null，表示该 ThreadLocal 没有外部引用，已经被回收了，那么就会调用 expxxx() 对该 Entry 的 value 进行清除，同时还会扫描后面的 Entry，重新进行 hash 并且顺便清除 脏 Entry
2）在 remove() 中，当移除了一个 Entry 后，同样会调用 expxxx() 做同样的事
3）在 set() 中，它先根据 ThreadLocal 的 hash 定位到槽位，然后往后扫描，如果发现 tab[i] = null,那么退出 for，插入 tab[i]，再判断是否需要扩容，最后返回。如果发现 tab[i].get() == key (ThreadLocal)，那么替换旧值，返回。如果 tab[i] != null && tab[i].get() == null，即是脏 Entry，那么调用 replaceStaleEntry(i) 来清除脏 Entry，同时找到能够存储待插入 Entry 的位置。

7、replaceStaleEntry(i)
从 脏 Entry 的 i 位置开始进行第一次扫描，往前扫描，直到 entry = null，记录第一个脏 Entry 的位置 start，如果前面没有脏 Entry，那么这个 start = i（可能是认为出现脏 Entry 的位置相邻地方也很大可能性会出现脏 Entry，所以往前扫描）
然后从 i 位置开始进行第二次扫描，往后扫描，直到 entry = null，如果中途发现了 tab[j].get() == key，那么将该 tab[j] 替换到 tab[i] 的位置，同时替换旧值 （因为 i 位置为脏 Entry，而这个位置按照顺序来讲应该是 待插入 Entry 应该呆的地方）然后进行从 start 位置调用 cleanSomeSlots() 开始清除 脏 Entry.
cleanSomeSlots() 就不讲了，反正就是 log(n) （n ==  tab.length）的方式从 start 位置开始往后查找，调用 expxxx() 清理脏 Entry，一旦找到一个脏 Entry 就重置 n，在时间和效率上进行权衡

8、ThreadLocalMap 能够真正解决内存泄漏吗？
如果程序员忘记调用 remove()，那么单靠 TheadLocalMap 的清除机制显然是不可能杜绝的，因为实际上它清除的 Entry 都是跟当时调用的 get()、remove()、set() 对应的 key 相关的，并且只有在调用这些方法的时候才会触发
如果长时间没有调用这些方法，或者 扫描不到对应的位置，那么内存泄漏还是会发生。
因此，只有记得 remove() 才能够保证内存泄漏不会发生，而如果记得调用 remove() 了，那么 ThreadLocalMap 的弱引用机制实际上就没有什么作用了。
```





## 5、sync 锁

```java
首先 每个 Java 对象对应的是JVM 底层的 OOP 对象，每个 OOP 对象分为 OOP 对象头 和 OOP 对象体
OOP 对象体存储的是真实数据，OOP 对象头包含三个部分：mark、Klass*、array_length(可选)

而 sync 锁就跟这个 mark 字段有关，mark 字段可以看作是一段 32 bit 的数据
mark 包含五种状态：
1)无锁状态：25 bit 是 hashCode，4 bit 是 GC age，1 bit 是偏向锁标志，2 bit 是锁标识符，它整个锁标志为 0|01
2）偏向锁：25 bit 存储 JavaThread 结构 和 偏向撤销次数 epoch，4 bit 是 GC age，1 bit 是偏向锁标志，2 bit 是锁标识符，它整个锁标志为 1|01
3）轻量级锁：30 bit 存储锁记录的指针，2 bit 是锁标志位，它锁标志位为 00
4）重量级锁：30 bit 存储 ObjectMonitor 指针，2 bit 是锁标志位，它锁标志位为 10
5）GC 回收：30 bit 为空，剩下的 2 bit 为 11，表示可回收对象

锁的升级:
偏向锁的作用：在很多情况下，sync 锁实际上只有一个线程获取，因此如果都是使用重量级锁去创建一个 ObjectMonitor 然后再获取，那么效率会降低，这本来也是无用功
轻量级锁的作用：在很少线程竞争的情况下，避免升级为重量级锁，避免线程阻塞，因为线程阻塞需要陷入到内核态中执行
1）偏向锁：JVM 启动时会延迟 4s 开启偏向锁。当线程1 获取锁时，发现处于无锁状态，那么调用 CAS 将存储 hashCode 的 25bit 替换为指向当前线程的 JavaThread 指针，同时将偏向锁标志位置 1。当下次再来获取锁时，发现仍然是偏向锁状态，并且 JavaThread 指向的是当前线程，那么无需再次 CAS，直接使用即可
2）轻量级锁：如果一个线程到达，然后发现已经锁处于偏向状态，并且上面的 JavaThread 不是指向自己，那么将偏向锁撤销（此时如果持有锁的线程还存活，那么需要将该线程运行到安全点进行暂停，这里的安全点应该是 GC 的安全点）为无锁状态，同时再在自己的栈帧中创建一个锁记录空间，将 mark 复制到自己的锁记录上，然后 CAS 将 mark 中的 30 bit 替换为自己的锁记录指针。
3）重量级锁：当两个线程同时到达，发现对象处于无锁状态或者偏向锁状态（先进行撤销），那么各自会创建一个锁记录指针，然后将 mark 都复制到自己的锁记录指针上，然后进行 CAS，只有一个线程会 CAS 成功，那么剩下的一个线程会进行 CAS 自旋，当 CAS 到达一定的阈值或者 CAS 的线程数到达 CPU 的一半（也有说是第三个线程到达的），那么升级为重量级锁。
	不过后面好像舍弃了 CAS 自旋阈值 和 自旋到达 CPU 的一半 这种这种条件，使用自适应自旋锁：根据上一个线程获取锁的等待情况来调整当前 CAS 自旋的次数。如果上一次线程没有多久就获取了锁，那么认为这次应该也会很快获取锁，那么可以允许情况糟一点，增加一点 CAS 的次数；如果上一次线程很久获取锁，那么认为这次也没那么快获取锁，那么象征性的 CAS 几次就直接升级为重量级锁。

特殊的锁升级情况：
1）一旦线程调用了 wait()，那么无论是什么锁都会升级为重量级锁，因为其他锁没有数据结构来存储等待的线程
2）如果调用了未重写的 hashCode()，那么将无法使用偏向锁，同时锁的升级情况如下：
	①、锁处于无锁状态，那么仍然是无锁状态
	②、锁处于偏向锁，并且线程没有正在使用锁，那么将锁升级为轻量级锁（因为偏向锁没有存储 hashCode 的位置）
	③、锁处于偏向锁或者轻量级锁，同时线程正在使用锁，那么将锁升级为重量级锁

identity hashCode：
Object 的 hashCode() 是一个 native 方法，当第一次调用它的时候，会生成一个 hashCode，这个 hashCode 叫做 identity hashCode，它会存储在 mark 字段中，后续使用的时候直接从 mark 字段中获取，而偏向锁没有位置来存储这个 hashCode，所以当调用了未重写的 hashCode() 的时候，就无法再使用偏向锁。
当我们重写了 hashCode()，它是直接返回我们计算的 hashCode 的，并没有保存到 mark 上，所以不会覆盖已经生成的 identity hashCode。
当我们重写了 hashCode() 又想获取 identity hashCode 时，System 类提供了一个方法让我们来获取这个 identity hashCode。


sync 重量级锁的数据结构 ObjectMonitor
该对象维护了 一个指向持有锁的线程的字段，一个记录重入度的字段，以及 CXQ、EntryList、WaitSet 三个队列。
sync 锁获取过程：
1）当线程在 synchronized(){} 处，会执行 exitI() 函数，尝试获取锁，首先调用 tryLock() CAS 获取锁，如果获取失败了，将当前线程封装为 ObjectWaiter 节点，类似 AQS 的 Node 节点
2）CAS 自旋，在循环体中，先尝试调用 tryLock() 获取锁，如果成功了那么直接返回，如果失败了，那么 CAS 将 ObjectWaiter 使用头插法插入到 CXQ 队列的头部。一直自旋直到其中一个成功为止
3）如果插入到 CAS 成功，那么就是陷入 BLOCKED 阻塞状态
4）当获取锁的线程调用 wait() 时，那么会将线程封装为 ObjectWaiter 节点，然后存储到 WaitSet 队列中
5）当 wait() 的线程被唤醒时，会进入到 EntryList 队列中等待获取锁（所以 wait(1000) 的线程自动唤醒，会进入到 EntryList 队列中）
6）因此这时候存在一个问题，CXQ 队列中存储的是在同步代码块处阻塞的线程，EntryList 队列中存储的是在 wait() 后被唤醒的线程，那么当持有锁的线程释放锁后，唤醒的是哪个队列中的线程呢？它内部制定了几个策略：
	①、EntryList 优先（CXQ 是 Stack） -- 默认策略
	②、CXQ 优先（CXQ 是 Stack）
	③、EntryList 优先，不过会 CXQ 反转（CXQ 是 FIFO）
	④、EntryList 优先，不过会将 CXQ 插入到 EntryList 的尾部（CXQ 是 Stack）
	⑤、CXQ 优先，不过会将 EntryList 插入到 CXQ 的尾部（CXQ 是 Stack）
```





## 6、AQS

```java
1、AQS
AbstractQueuedSynchronizer：抽象队列同步器，简称 AQS
它内部定义了一套维护线程的模板，通过操作 volatile int state 和 一个同步队列 来控制资源的获取 和 线程出队入队的逻辑。
它将对 state 的操作 tryAcquire() 和 tryRelease() 定义为抽象方法，让子类去实现对 state 的操作逻辑，比如 ReentrantLock，它将 state 当作锁的资源，重写的 tryAcquire() 是 用来获取锁，同时记录重入度，一次只能有一个线程获取；比如 Semaphore，它将 state 当作多个互斥资源。
不过这些子类最终都是使用的同一个模板，即一旦没有获取资源成功，那么就会调用 AQS 的 enq() 方法，将线程封装为一个 Node 节点，然后存储到同步队列中，阻塞等待，然后由该线程进行 CAS 自旋，执行 tryAcuqire() 获取资源的逻辑，当然，如果一两次 CAS 后没有获取资源，那么会调用 park() 挂起线程，等待前驱节点的唤醒，只有前驱节点为 header（dummy 节点）的时候才会唤醒后续的线程去获取资源。
	（这里就体现了 lock() 和 lockInterruptibly() 的区别，在 lock() 中，如果被中断了，那么它只是使用一个变量来记录被中断过，但是还是会继续自旋，然后继续阻塞，而 lockInterruptibly() 一旦被 interrupt()，那么从 park() 中醒来，会直接抛出中断异常，停止等待。）

AQS 支持公平锁和非公平锁：
1）非公平锁：AQS 默认是非公平锁的，在同步队列中存在资源在等待时，新的线程刚来就抢占了资源，导致排队中的线程没有获取资源（比如我们在窗口排队，排了很久结果一个新来的人直接插队到窗口办事）。但是，这个非公平是针对同步队列内和外之间的线程来讲的，对于同步队列中的线程，获取资源的顺序是 FIFO，是公平的
2）公平锁：当同步队列中有线程等待资源时，同步队列外的线程不能抢占资源。在 AQS 内部提供了一个 hasQueuedPredecessors()，可以让子类调用，判断同步队列是否为空，比如在 ReentrantLock 公平锁重写的 tryAcquire() 中，当 state == 0 时，在 CAS 之前会先调用该方法判断同步队列是否为空，如果为空，那么它不能插队，只能进入到同步队列中等待。
	（提供该方法是子类无法直接访问同步队列，它只能将调用父类对外提供的接口）
	
	
2、Condition
在 ReentrantLock 中，可以通过 newCondition() 创建一个 Conditiob 对象（虽然是在 ReentrantLock 中调用的，不过创建的  AQS 中的内部类 ConditionObject），它维护了一个等待队列、await()、signal() 等方法
每个 Condition 对象都持有一个等待队列，所以我们调用不同 Condition 对象的 await() 时候，线程会进入到不同的等待队列中，内部会将线程封装为一个 Node 节点（跟 AQS 复用），然后将节点存储进等待队列中。

1）await()：调用该方法的线程必须持有锁，如果没有锁那么会抛出异常。正常流程是 先将线程持有的锁释放，调用 unpark() 唤醒同步队列中的线程，然后将当前线程封装封装为 Node 节点，存储到等待队列中，进入到 while，while() 的判断体为当前 Node 是否在同步队列中，如果不是，那么进入到循环体内，很显然，当前 Node 并不在同步队列中，而是在等待队列中，那么在循环体内调用 park() 阻塞线程。
2）signal()：该方法同样需要调用线程持有锁，它会将等待队列前面的 Node 节点从等待队列中移到同步队列中，同时唤醒 Node 中的线程，然后没什么事了 return。
3）线程被唤醒时，会在上面的 await() 的 while() 中醒来，再次在 while() 的判断体进行判断，发现已经在同步队列中了，那么退出 while，调用 acquireQueue() 执行出队逻辑（即排队获取锁，期间会 park 阻塞）。

可以看出，AQS 实际上是参照 sync 锁的实现逻辑，不过 AQS 的 同步对列 = CXQ + EntryList，AQS 的等待队列 = WaitSet。
同时，ReentrantLock + Condition 在生产者消费者模式中可以准确的表示要唤醒哪一种类型的线程，而 sync 锁的话，由于两种类型的线程都在 WaitSet 中，所以是随机唤醒，很大程度上会出现虚假唤醒，所以一般会使用 notifyAll()，将 WaitSet 中的线程都唤醒，放入到 EntryList 中排队，比如打算唤醒消费者线程，不过此时在 EntryList 中获取锁的线程是消费者，那么它会再次调用进入到 WaitSet 中，然后将锁交给 EntryList 中的下一个 ObjectWaiter，直到出现消费者线程为止。（这个逻辑是我自己猜测的）
```



