# BlockingQueue



线程池中所有的队列都是阻塞队列，都实现了 BlockingQueue 接口

具体实现有：

- ArrayBlockingQueue：基于循环数组的阻塞队列，有边界
- LinkedBlockingQueue：基于单链表的阻塞队列，有边界
- SynchronousQueue：同步队列，没有存储空间，当向线程池提交一个任务的时候，如果线程池没有空闲线程，那么重新开一个线程来允许该任务。
- PriorityBlockingQueue：基于元素优先级排序的阻塞队列，无边界

```java
public interface BlockingQueue<E> extends Queue<E> {
    // 尝试往队列尾部添加元素，添加成功则返回true，添加失败则抛出IllegalStateException异常
    boolean add(E e);

    // 尝试往队列尾部添加元素，添加成功则返回true，添加失败则返回false
    boolean offer(E e);

    // 尝试往队列尾部添加元素，如果队列满了，则阻塞当前线程，直到其能够添加成功为止
    void put(E e) throws InterruptedException;

    
    // 尝试往队列尾部添加元素，如果队列满了，则最多等待指定时间，
    // 如果在等待过程中还是未添加成功，则返回false，如果在等待
    // 过程中被中断，则抛出InterruptedException异常
    boolean offer(E e, long timeout, TimeUnit unit) throws InterruptedException;

    // 尝试从队列头部取出元素，如果队列为空，则一直等待队列中有元素
    E take() throws InterruptedException;

    // 尝试从队列头部拉取元素，如果队列为空，则最多等待指定时间，
    // 如果等待过程中拉取到了新元素，则返回该元素，
    // 如果等待过程被中断，则抛出InterruptedException异常
    E poll(long timeout, TimeUnit unit) throws InterruptedException;

    // 获取当前队列剩余可存储元素的数量
    int remainingCapacity();

    // 从队列中移除指定对象
    boolean remove(Object o);
}
```



关于 BlockingQueue 的 add()、offer()、put() 的区别：

- add() 如果队列已满，那么抛出异常

  ```java
  public boolean add(E e) {
      if (offer(e))
          return true;
      else
          throw new IllegalStateException("Queue full");
  }
  ```

- offer() 就是直接添加，如果队列满了就直接返回 false,如果没满就添加

- put() 如果队列满了会调用 condition 的 await() 进行等待，直到队列中有元素被消费后被唤醒



## 1、ArrayBlockingQueue 实现原理

基于 数组实现的阻塞队列，跟 lc 用数组实现的队列一样，使用两个指针，循环写和读

不过不同的是，当队列 为空或者满 的时候，两个指针都重合，而 lc 是预留一个位置的，因此这里还需要一个 count 变量来记录元素个数以此来判断是否为空

阻塞 则是通过 ReentrantLock + Condition 的生产者消费者模式 实现的

```java
public class ArrayBlockingQueue<E> extends AbstractQueue<E> implements BlockingQueue<E> {
    //存储数据的 数组
    final Object[] items;
    //两个指针
    int takeIndex;
    int putIndex;
    //记录元素个数
    int count;
    //阻塞实现
    final ReentrantLock lock;
    //消费者线程，notEmpty 表示现在数据非空，可以进行消费
    private final Condition notEmpty;
    //生产者线程，notFull 表示现在数据未满，可以进行生产
    private final Condition notFull;


    //入队主逻辑
    private void enqueue(E x) {
        final Object[] items = this.items;
        items[putIndex] = x;
        if (++putIndex == items.length)
            putIndex = 0;
        count++;
        //生产了一个，可以唤醒消费者线程进行消费，即唤醒要获取任务的线程
        notEmpty.signal();
    }
    //出队主逻辑
    private E dequeue() {
        final Object[] items = this.items;
        @SuppressWarnings("unchecked")
        E x = (E) items[takeIndex];
        items[takeIndex] = null;
        if (++takeIndex == items.length)
            takeIndex = 0;
        count--;
        if (itrs != null)
            itrs.elementDequeued();
        //出队了一个，可以唤醒生产者线程进行生产，即唤醒要插入任务的线程
        notFull.signal();
        return x;
    }
	
    //对外开放插入任务的接口
    public void put(E e) throws InterruptedException {
        checkNotNull(e);
        final ReentrantLock lock = this.lock;
        lock.lockInterruptibly();
        try {
            //如果队列已满，那么当前线程进入等待状态
            while (count == items.length)
                notFull.await();
            enqueue(e);
        } finally {
            lock.unlock();
        }
    }
	//对外开放获取任务的接口
    public E poll(long timeout, TimeUnit unit) throws InterruptedException {
        long nanos = unit.toNanos(timeout);
        final ReentrantLock lock = this.lock;
        lock.lockInterruptibly();
        try {
            //队列为空，那么当前线程等待指定时间
            while (count == 0) {
                if (nanos <= 0)
                    return null;
                nanos = notEmpty.awaitNanos(nanos);
            }
            return dequeue();
        } finally {
            lock.unlock();
        }
    }
```



## 2、LinkedBlockingQueue 实现原理

使用一个单链表，有 head 和 tail 两个 dummy 节点，用于 队头和队尾的 插入 和 删除

使用两个 ReentrantLock，一个用于队头，一个用于队尾

当需要获取任务的时候，在队头加锁，然后获取任务

当需要插入任务的时候，在队尾加锁，然后插入任务



这意味着队头和队尾能够并发执行，吞吐量量高

```java
public class LinkedBlockingQueue<E> extends AbstractQueue<E> implements BlockingQueue<E> {
    //存储容量
    private int capacity;
    //元素个数，使用原子类保证线程安全
    private AtomicInteger count = new AtomicInteger();
    //链表头节点
    private Node<E> head;
    //链表尾节点
    private Node<E> last;

    //第一个 ReentrantLock，用于锁住链表头节点
    private ReentrantLock takeLock = new ReentrantLock();
    //消费者线程
    private Condition notEmpty = takeLock.newCondition();
    //第二个 ReentrantLock，用于锁住链表尾节点
    private ReentrantLock putLock = new ReentrantLock();
    //生产者线程
    private Condition notFull = putLock.newCondition();

    //单链表节点
    static class Node<E> {
        E item;	//数据
        Node<E> next;
        Node(E x) { item = x; }
    }

    public void put(E e) throws InterruptedException {
        if (e == null) throw new NullPointerException();
        int c = -1;
        Node<E> node = new Node<E>(e);
        //获取队头锁
        final ReentrantLock putLock = this.putLock;
        final AtomicInteger count = this.count;
        putLock.lockInterruptibly();
        try {
            //如果队列已满，那么进入等待状态
            while (count.get() == capacity) {
                notFull.await();
            }
            //入队
            enqueue(node);
            //元素个数 +1
            c = count.getAndIncrement();
            //当队列未满时，其他线程插入任务
            if (c + 1 < capacity)
                notFull.signal();
        } finally {
            //释放锁
            putLock.unlock();
        }
        //唤醒获取任务的线程
        signalNotEmpty();
    }

    public E poll(long timeout, TimeUnit unit) throws InterruptedException {
        E x = null;
        int c = -1;
        long nanos = unit.toNanos(timeout);
        final AtomicInteger count = this.count;
        final ReentrantLock takeLock = this.takeLock;
        takeLock.lockInterruptibly();
        try {
            while (count.get() == 0) {
                if (nanos <= 0)
                    return null;
                nanos = notEmpty.awaitNanos(nanos);
            }
            x = dequeue();
            c = count.getAndDecrement();
            if (c > 1)
                notEmpty.signal();
        } finally {
            takeLock.unlock();
        }
        //唤醒插入任务的线程
        signalNotFull();
        return x;
    }
```



注意，newFixedThreadPool() 使用的就是 LinkedBlockingQueue，但是它的默认容量为 Integer.MAX_VALUE

因此相当于无界队列，因此使用的时候注意自己指定 max 大小



> ### ArrayBlockingQueue 和 LinkedBlockingQueue  的区别

- ArrayBlockingQueue 底层是一个循环数组，LinkedBlockingQueue  底层是一个单链表

- ArrayBlockingQueue 只使用了一个 ReentrantLock，插入和删除同步执行，一次只能操作一个线程，而 LinkedBlockingQueue  使用了两个 ReentrantLock，锁住头和锁住尾，插入和删除可以并发执行
- 由于 ArrayBlockingQueue 插入和删除是同步的，所以 int count 已经保证了原子性，而 LinkedBlockingQueue  存在并发，因此 需要使用一个 AtomicInteger 来保证元素个数的原子性
- ArrayBlockingQueue 不使用两个锁（跟 ConcurrentHashMap 那样锁住槽位）的原因是 按照作者的意思就是 设计简洁，如果想要高吞吐量就使用 LinkedBlockingQueue

具体看： <https://blog.csdn.net/qq_27007251/article/details/75207050>





## 3、SynchronousQueue 实现原理

具体看 <https://www.jianshu.com/p/af6f83c78506>



这个不具体讲了，有点难

SynchronousQueue 没有 AQS，而是使用大量的 CAS

它不存在任何的容量，而是使用阻塞线程的方式

当 插入任务的线程来时，如果没有线程等待获取任务，那么进入到链表中等待匹配

当获取任务的线程来时，如果没有线程等待插入任务，那么进入到链表中等待匹配

即线程无论是获取任务还是插入任务，如果没有相应的线程与之匹配，那么就会封装成一个节点进入对应的链表中等待匹配的线程

```java
public class SynchronousQueue<E> extends AbstractQueue<E>
    implements BlockingQueue<E>, java.io.Serializable
    
     public SynchronousQueue() {
        this(false);
    }
    /*
    抽象类，只有一个 transfer()，用来转移数据
    如果 e == null，表示是获取数据，如果 e != null，表示是插入数据
    它有两种实现，一个是 TransferStack，一个是 TransferQueue
    TransferStack 是用来实现非公平的，即如果不遵循先来后到准则，每次匹配都弹出栈顶
	TransferQueue 是用来实现公平的，每次匹配都是从队头开始匹配
    */
    abstract static class Transferer<E> {
        abstract E transfer(E e, boolean timed, long nanos);
    }
    public SynchronousQueue(boolean fair) {
        // 通过 fair 值来决定公平性和非公平性
        // 公平性使用TransferQueue，非公平性采用TransferStack
        transferer = fair ? new TransferQueue<E>() : new TransferStack<E>();
    }
```



在线程池中是 newCachedThreadPool() 使用的，它默认是没有边界的，只要有提交任务，如果没有空闲线程，那么创建一个新的线程去执行任务，默认 max = Integer.MAX_VALUE

**如果大量创建线程，会导致cpu和内存飙升，甚至服务器挂掉。**



## 4、PriorityBlockingQueue 实现原理

这个也不讲了，有点难

