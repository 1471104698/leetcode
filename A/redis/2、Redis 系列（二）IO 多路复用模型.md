# IO 多路复用模型



> ### 如果不使用 网络 IO 多路复用模型，那么会有什么问题呢？

由于 redis  是单线程的，所有操作都是线性操作的，因此如果使用普通的 IO，每个连接的读写等待操作都是阻塞的，这样一旦阻塞该线程就无法为别的连接进行服务，这要是这么搞那 redis 还玩个锤子，所以才使用了 IO 多路复用模型，避免了这种问题的发生

IO 多路复用模型有 select、poll、epoll 



在 linux 中一切皆是文件，socket 连接也一样，每个文件都有自己的 fd，进程拥有的是 fd，具体操作还得操作系统进行



## 1、select



> ### select() 相关的数据结构

**select 模型使用的是 fd_set 数据结构，底层是一个 long 数组，但实际上是作为 bitmap 来使用**

比如我们使用一个 int ，可以表示 32 位，而 fd_set 中每一位表示该位置作为索引的 fd 是否被监听，置为 0 表示不监听，置为 1 表示监听

**需要注意的是，这里的 fd 是 socket_fd，而不是普通计算机内部文件的 fd，只跟用来描述 socket 的**



共有 三个 fd_set，分别对应不同的事件

select() 方法如下：

```C
int select(int maxfdp,  fd_set* readset,  fd_set* writeset,  fe_set* exceptset,  struct timeval* timeout);
```

- maxfdp：需要检查的最大的 fd 的值 + 1，因为 fd 是从 0 开始算的，这样内核态只需要检查 [0, maxfdp) 范围内的 fd
- readset：检查是否存在读事件的 fd 集合
- writeset：检查是否存在写事件的 fd 集合
- exceptset：检查是否存在异常事件（断开连接等）的 fd 集合

select() 内部会进行筛选，将不存在读写等事件的 fd 给去除掉，然后统计所有有事件的 fd 的个数，并返回，即 select() 不会告知进程哪些 fd 有事件，而是告知有多少个 fd 有事件



> ### select 流程

**select 模型的流程如下：**

- 初始化我们要监听的 位图数组 fd_set，然后将每一位置为 0，然后将我们要监听的 fd 在 fd_set 中对应的位置的 bit 置为 1，调用 select()，将 fd_set 传给内核态
- 内核态只会检查 fd_set 中对应 bit 为 1 的 fd 是否存在事件（比如检查 readset 中的 fd 是否存在 读事件），如果不存在，那么将 对应的 bit 置为 0，过程中统计有事件的 fd 的个数并返回
- 获取到 select() 结果 ret，判断是否为 0，为 0 就表示没有事件发生，如果不为 0，那么遍历 所有的 fd，判断对应的 fd_set 中该为是否为 1，如果为 1，表示有事件，如果为 0，表示没事件

这也就说明了为什么每次调用 select() 之前都需要将 fd_set 置为 0，然后再重新加入 fd，就是为了防止上一次的结果影响当前次



**select 代码模拟：**

```C
int main() {

    fd_set read_fs, write_fs;
    struct timeval timeout;
    int max = 0;  // 用于记录最大的fd，在轮询中时刻更新即可  

    int ret = 0; // 记录就绪的事件，可以减少遍历的次数
    
    //hile() 死循环进行轮询，当处理完一次 select() 后会继续进行循环调用
    while (1) {
        //每次轮询到需要重新初始化 fd_set ，先将每个 bit 位置为 0
        FD_ZERO(&fdread);
        FD_ZERO(&fdwrite);
        for (i = 0; i < g_iTotalConn; i++)    
        {   
            //将要检查的 socket fd 加入队列，即将对应的位置为 1
            FD_SET(g_CliSocketArr2[i], &fdread);   
            FD_SET(g_CliSocketArr2[i], &fdwrite); 
        }
        /*
    调用 select() 函数，将 readset 和 writeset 拷贝到内核态中
    该方法会阻塞，直到 select() 完成返回结果
    在内核态中会检查对应的 fd 是否存在数据可读可写，如果不存在，那么将对应的 fd 出队
    */
        ret = select(max + 1, &read_fd, &write_fd, NULL, &timeout);
        if (ret == 0)    
        {   
            continue;   
        }
        // 每次需要遍历所有fd，判断有无读写事件发生
        for (i = 0; i < g_iTotalConn && i < ret; i++) 
            if (g_CliSocketArr1[i] == listenfd) {
                --ret;
                // 这里处理accept事件
                FD_SET(i, &read_fd);//将客户端socket加入到集合中
            }
        if (FD_ISSET(g_CliSocketArr1[i], &read_fd)) {
            --ret;
            // 这里处理read事件
        }
        if (FD_ISSET(g_CliSocketArr1[i], &write_fd)) {
            --ret;
            // 这里处理write事件
        }
    }
}
```



> ### select 的优缺点

**select 的好处：**

- 无需阻塞等待没有事件的 socket ，而是只对有事件的 socket 进行处理，对于没有事件的 socket 是不会调用 read() 之类的，这样就可以避免陷入阻塞等待状态了



**select 的缺点：**

- 调用 select() 需要将所有我们要检查状态的 fd 拷贝到 内核态中，当 fd_set 数据量大时，开销是很大的

- 在 内核态中，需要 O(n) 遍历所有的 fd，同样数据量大时，耗时

- 内核的宏定义 FD_SETSIZE 限制了最大只能为 1024，表示的是最大的 fd 的值，即只支持 fd 为 [0, 1023] 的 socket，同时，一个 fd_set 数组大小为 16，一个 long 有 64 位，那么总的 bit 为 16 * 64 = 1024，它是由 FD_SETSIZE 计算出来的，同样也表明了只能支持 [0, 1023] 的 fd

  ```C
  #define __FD_SETSIZE    1024
  ```

- 每次新的循环都需要初始化 fd_set 和 填入 fd，耗时效率低



## 2、poll

在最初 select 出现的时候，1024 个连接是够用的

随着网络高速发展，并发数越来越多，因此需要做出修改，由于 select 使用了 fd_set 这种数组，所以才会存在 fd 限制，因此 poll 不再使用 数组形式，而是使用链表，这样的话就没有个数限制了

poll 抛弃了 fd_set 数据结构，转换为链表形式，每个节点存储了 fd ，监听的事件，以及实际发生的事件（内核填充）

![img](https://pic1.zhimg.com/80/v2-2d9a77915b81bdcf7584426cf1be0fd8_720w.jpg)



但实际上 poll 还是使用轮询的方式，除了解决了 select 数量上的问题，其他缺点都没有解决



## 3、epoll



源码分析： https://www.cnblogs.com/shuqin/p/11772651.html 



> ### epoll 如何解决 select 问题

select 每次调用都需要将 要监听的 fd 集合 拷贝到内核态，即内核态对于监听的 socket 是无记忆的，它不会去存储监听的 socket，并且不会去直接告知 用户态进程哪些 socket 有事件，这样的话就导致了每次都需要进行用户态到内核态的拷贝，并且用户态还需要自己再进行遍历，时间复杂度 O(n) + O(n)

epoll 直接将要监听的 socket 拷贝到内核态，让内核态进行监听，让内核态自己维护，后续有一个新的连接，也只需要直接拷贝到内核态让它管理即可，后续用户态进程都只需要 回调函数 获取有事件的 socket 进行处理即可

![img](https://pic2.zhimg.com/80/v2-5ce040484bbe61df5b484730c4cf56cd_720w.jpg)



> ### epoll 的数据结构



数据结构具体内容

```c
struct eventpoll {
  spin_lock_t       lock; 
  struct mutex      mtx;  
  wait_queue_head_t     wq; 	//等待队列，调用 epoll_wait() 等待获取事件的 进程
  wait_queue_head_t   poll_wait; 
  struct list_head    rdlist;   //就绪链表 readylist
  struct rb_root      rbr;      //红黑树根节点 
  struct epitem      *ovflist;
};

struct epitem {
  struct rb_node  rbn;   //红黑树节点   
  struct list_head  rdllink;	//就绪队列的链表节点
  struct epitem  *next;     
  struct epoll_filefd  ffd;  //fd
  int  nwait;                 
  struct list_head  pwqlist;  
  struct eventpoll  *ep;      
  struct list_head  fllink;   
  struct epoll_event  event;  //事件类型
};

struct rb_node {
  unsigned long  __rb_parent_color;
  struct rb_node *rb_right;
  struct rb_node *rb_left;
} __attribute__((aligned(sizeof(long))));

struct rb_root {
  struct rb_node *rb_node;
};


typedef union epoll_data {
   void    *ptr;
   int      fd;
   uint32_t u32;
   uint64_t u64;
} epoll_data_t;


 struct epoll_event {
     uint32_t     events;    //事件类型，又叫触发状态：水平触发、边缘触发
     epoll_data_t data;     //存储 fd 的数据结构
 };
```



主要的三个 api

```C
//创建一个 epoll 对象，返回该对象的 fd：epfd
int epoll_create(int size); 	

/*
修改监听 fd 的状态
op：对 fd 的具体操作
1：添加新的 fd 到 epoll
2：修改已经注册的 fd 的监听事件
3：删除掉指定的 fd
*/
int epoll_ctl(int epfd, int op, int fd, struct epoll_event *event);  

//回调函数，获取有事件的 socket, 返回结果是事件个数，将有事件的 fd 和 对应的事件存储在 events 数组中
int epoll_wait(int epfd, struct epoll_event *events,
               int maxevents, int timeout);
```



总共涉及到 7 个主要的数据结构：evenpoll、epitem、rb_node、rb_root、rdlist、epoll_event

关于数据结构的关系如下：

- evenpoll 就是我们说的 epoll 对象，它内部维护了 进程的等待队列，fd 的就绪链表的头节点，红黑树的根节点
- 一个 epoll_event 封装了 一个 fd 和 感兴趣的事件
- 一个 epitem 内部维护了 一个 epoll_event，以及一个 就绪链表节点以及 红黑树节点，用于将当前 epitem 链接到 就绪链表 和 红黑树上
- 红黑树维护了所有内核监听的 fd
- 就绪链表存储所有发生事件的 fd



epoll 执行流程：

- 当调用 epoll_create() 的时候会创建一个 epoll 对象
- 当调用 epoll_ctl() 操作对应的 fd 和 事件
- 当进程调用 epoll_wait()，如果 就绪链表中存在 fd，那么会将这些 fd 发送给这个进程，如果没有，那么这个进程会进入到 等待队列中，然后陷入阻塞状态，等到某个 fd 有事件时，内核会唤醒这个进程，然后将 fd 发送给这个进程



![img](https://pic1.zhimg.com/80/v2-90632d0dc3ded7f91379b848ab53974c_720w.jpg)



> ### epoll 的两种触发方式

epoll 的触发方式跟上面的 epoll event 结构体有关，里面有个 events 

其中 events 表示 感兴趣的事件 和 被触发的事件，可能的取值为：
**EPOLLIN**：表示对应的文件描述符可以读；
**EPOLLOUT**：表示对应的文件描述符可以写；
**EPOLLET**：    ET(边缘触发) 的 epoll 工作模式；



水平触发（LT）：只要有数据可以读，都会通知用户态进程，即一次没读完也还会出现在 就绪链表中，**因此数据可以留着下次处理**，是 epoll_wait() 默认的方式

边缘触发（ET）：只有状态发生变化，才会通知用户态进程，即相当于对于 发送方写入一次数据，epoll 只会将这个 fd 放入就绪链表一次，如果被调用 epoll_wait() 返回了这个 fd，那么无论里面数据是否已经读完，都会从就绪队列中移除，等到下次发送方再次写入数据才会放入到就绪队列中，**因此数据需要一次处理完成**

```C
//水平触发
ret = read(fd, buf, sizeof(buf));

//边缘触发
while(true) {
    //循环读取，因此当前状态发生改变，只能获取到一次 fd，因此我们需要一直对这个 fd 进行操作，一旦退出了，那么后续在没有状态发生改变前都不会获取到这个 fd 了，即无法精准读取
    ret = read(fd, buf, sizeof(buf);
    if (ret == EAGAIN) break;
}
```

边缘触发如果要读取完全部数据，那么需要套层 while() 循环，但是如果 read() 是阻塞 IO，那么 while() 到最后没有数据可读了，对于 redis 这种单线程的来说就会陷入阻塞，这样就无法处理别的 socket

因此，**使用 边缘触发的时候需要将 fd 设置为非阻塞 IO**，这样的话没有数据的时候会返回一个 EAGAIN ，停止读取，因此我们可以根据 EAGAIN 来判断数据是否已经读取完毕



> ### ET 造成饥饿

**ET 造成 饥饿 的原因：**

因为 ET 读取数据时，在一般情况下需要 while 循环读取，一次读取完全部的数据，因此可能会造成 饥饿现象，即如果发送方不停的发送数据，那么就会导致其他的 socket 无法进行处理



**ET 饥饿 问题的解法：**

- 使用一个 list 存储 epoll_wait() 返回的 fd 集合
- 当 list 元素不为空时， for 循环处理
- 每次限制一个读取的数据量，如果没有读完，那么就将 fd 继续留在 list 中，如果读完了，就将 fd 从 list 中移除





> ### epoll_ctl() 源码

```C
作者：赛罗奥特曼~
    链接：https://www.nowcoder.com/discuss/26226
来源：牛客网

    //epoll_ctl 方法
SYSCALL_DEFINE4(epoll_ctl, int, epfd, int, op, int, fd,
        struct epoll_event __user *, event)
{
/*
     * 错误处理以及从用户空间将epoll_event结构copy到内核空间.
     */
    if (ep_op_has_event(op) &&
        copy_from_user(&epds, event, sizeof(struct epoll_event)))
        goto error_return;
    //根据操作符进行对应的操作
    switch (op) {
            /* 往红黑树中添加新的 fd */
        case EPOLL_CTL_ADD:
            if (!epi) {
                /* rbtree插入, 详情见ep_insert()的分析 */
                error = ep_insert(ep, &epds, tfile, fd);
            } else
                /* 找到了!? 重复添加! */
                error = -EEXIST;
            break;
            /* 从红黑树中删除对应的 fd */
        case EPOLL_CTL_DEL:
            if (epi)
                error = ep_remove(ep, epi);
            else
                error = -ENOENT;
            break;
            /* 从红黑树中修改对应的 fd */
        case EPOLL_CTL_MOD:
            if (epi) {
                epds.events |= POLLERR | POLLHUP;
                error = ep_modify(ep, epi, &epds);
            } else
                error = -ENOENT;
            break;
    }
}


/*
 * ep_insert()在epoll_ctl()中被调用, 完成往epollfd里面添加一个监听fd的工作
 * tfile是fd在内核态的struct file结构
 */
    static int ep_insert(struct eventpoll *ep, struct epoll_event *event,
                         struct file *tfile, int fd)
{
    int error, revents, pwake = 0;
    unsigned long flags;
    /*
    
    插入红黑树的就是这个 epitem
    
    
    */
    struct epitem *epi;
    struct ep_pqueue epq;
    /* 查看是否达到当前用户的最大监听数 */
    if (unlikely(atomic_read(&ep->user->epoll_watches) >=
                 max_user_watches))
        return -ENOSPC;
    /* 从著名的slab中分配一个epitem */
    if (!(epi = kmem_***_alloc(epi_***, GFP_KERNEL)))
        return -ENOMEM;
    epi->ep = ep;
    /* 这里保存了我们需要监听的文件fd和它的file结构 */
    ep_set_ffd(&epi->ffd, tfile, fd);
    epi->event = *event;
    epi->nwait = 0;
    /* 这个指针的初值不是NULL哦... */
    epi->next = EP_UNACTIVE_PTR;
    /* Initialize the poll table using the queue callback */
    /* 好, 我们终于要进入到poll的正题了 */
    epq.epi = epi;
    /* 初始化一个poll_table
     * 其实就是指定调用poll_wait(注意不是epoll_wait!!!)时的回调函数,和我们关心哪些events,
     * ep_ptable_queue_proc()就是我们的回调啦, 初值是所有event都关心 */
    init_poll_funcptr(&epq.pt, ep_ptable_queue_proc);
    /* 这一部很关键, 也比较难懂, 完全是内核的poll机制导致的...
     * 首先, f_op->poll()一般来说只是个wrapper, 它会调用真正的poll实现,
     * 拿UDP的socket来举例, 这里就是这样的调用流程: f_op->poll(), sock_poll(),
     * udp_poll(), datagram_poll(), sock_poll_wait(), 最后调用到我们上面指定的
     * ep_ptable_queue_proc()这个回调函数...(好深的调用路径...).
     * 完成这一步, 我们的epitem就跟这个socket关联起来了, 当它有状态变化时,
     * 会通过ep_poll_callback()来通知.
     * 最后, 这个函数还会查询当前的fd是不是已经有啥event已经ready了, 有的话
     * 会将event返回. */
    revents = tfile->f_op->poll(tfile, &epq.pt);
    /*
     * We have to check if something went wrong during the poll wait queue
     * install process. Namely an allocation for a wait queue failed due
     * high memory pressure.
     */
    error = -ENOMEM;
    if (epi->nwait < 0)
        goto error_unregister;
    /* Add the current item to the list of active epoll hook for this file */
    /* 这个就是每个文件会将所有监听自己的epitem链起来 */
    spin_lock(&tfile->f_lock);
    list_add_tail(&epi->fllink, &tfile->f_ep_links);
    spin_unlock(&tfile->f_lock);
    /*
     * Add the current item to the RB tree. All RB tree operations are
     * protected by "mtx", and ep_insert() is called with "mtx" held.
     */
        
    /* 
    
    
    都搞定后, 将epitem插入到对应的eventpoll中去 
    
    
    */
    ep_rbtree_insert(ep, epi);
    /* We have to drop the new item inside our item list to keep track of it */
    spin_lock_irqsave(&ep->lock, flags);
    /* If the file is already "ready" we drop it inside the ready list */
    /* 到达这里后, 如果我们监听的fd已经有事件发生, 那就要处理一下 */
    if ((revents & event->events) && !ep_is_linked(&epi->rdllink)) {
        /* 将当前的epitem加入到ready list中去 */
        list_add_tail(&epi->rdllink, &ep->rdllist);
        /* Notify waiting tasks that events are available */
        /* 谁在epoll_wait, 就唤醒它... */
        if (waitqueue_active(&ep->wq))
            wake_up_locked(&ep->wq);
        /* 谁在epoll当前的epollfd, 也唤醒它... */
        if (waitqueue_active(&ep->poll_wait))
            pwake++;
    }
    spin_unlock_irqrestore(&ep->lock, flags);
    atomic_inc(&ep->user->epoll_watches);
    /* We have to call this outside the lock */
    if (pwake)
        ep_poll_safewake(&ep->poll_wait);
    return 0;
    error_unregister:
    ep_unregister_pollwait(ep, epi);
    /*
     * We need to do this because an event could have been arrived on some
     * allocated wait queue. Note that we don't care about the ep->ovflist
     * list, since that is used/cleaned only inside a section bound by "mtx".
     * And ep_insert() is called with "mtx" held.
     */
    spin_lock_irqsave(&ep->lock, flags);
    if (ep_is_linked(&epi->rdllink))
        list_del_init(&epi->rdllink);
    spin_unlock_irqrestore(&ep->lock, flags);
    kmem_***_free(epi_***, epi);
    return error;
}
```



在 epoll_ctl() 中，会将用户态传入的 epoll_event  复制到内核态，epoll_event 中存储了 fd

然后根据 op 操作符进行判断进行 添加、删除、修改，都是对 eventpoll 里的 红黑树进行操作





> ### epoll_wait() 中的代码

```C
作者：赛罗奥特曼~
链接：https://www.nowcoder.com/discuss/26226
来源：牛客网


/* epoll_wait 方法 */
SYSCALL_DEFINE4(epoll_wait, int, epfd, struct epoll_event __user *, events,
        int, maxevents, int, timeout)
{
    int res, eavail;
    unsigned long flags;
    long jtimeout;
    wait_queue_t wait;//等待队列
    /*
     * Calculate the timeout by checking for the "infinite" value (-1)
     * and the overflow condition. The passed timeout is in milliseconds,
     * that why (t * HZ) / 1000.
     */
    /* 计算睡觉时间, 毫秒要转换为HZ */
    jtimeout = (timeout < 0 || timeout >= EP_MAX_MSTIMEO) ?
        MAX_SCHEDULE_TIMEOUT : (timeout * HZ + 999) / 1000;
retry:
    spin_lock_irqsave(&ep->lock, flags);
    res = 0;
    /* 如果ready list不为空, 就不睡了, 直接干活... */
    if (list_empty(&ep->rdllist)) {
        /*
         * We don't have any available event to return to the caller.
         * We need to sleep here, and we will be wake up by
         * ep_poll_callback() when events will become available.
         */
        /* OK, 初始化一个等待队列, 准备直接把自己挂起,
         * 注意current是一个宏, 代表当前进程 */
        init_waitqueue_entry(&wait, current);//初始化等待队列,wait表示当前进程
        __add_wait_queue_exclusive(&ep->wq, &wait);//挂载到ep结构的等待队列
        for (;;) {
            /*
             * We don't want to sleep if the ep_poll_callback() sends us
             * a wakeup in between. That's why we set the task state
             * to TASK_INTERRUPTIBLE before doing the checks.
             */
            /* 将当前进程设置位睡眠, 但是可以被信号唤醒的状态,
             * 注意这个设置是"将来时", 我们此刻还没睡! */
            set_current_state(TASK_INTERRUPTIBLE);
            /* 如果这个时候, ready list里面有成员了,
             * 或者睡眠时间已经过了, 就直接不睡了... */
            if (!list_empty(&ep->rdllist) || !jtimeout)
                break;
            /* 如果有信号产生, 也起床... */
            if (signal_pending(current)) {
                res = -EINTR;
                break;
            }
            /* 啥事都没有,解锁, 睡觉... */
            spin_unlock_irqrestore(&ep->lock, flags);
            /* jtimeout这个时间后, 会被唤醒,
             * ep_poll_callback()如果此时被调用,
             * 那么我们就会直接被唤醒, 不用等时间了...
             * 再次强调一下ep_poll_callback()的调用时机是由被监听的fd
             * 的具体实现, 比如socket或者某个设备驱动来决定的,
             * 因为等待队列头是他们持有的, epoll和当前进程
             * 只是单纯的等待...
             **/
            jtimeout = schedule_timeout(jtimeout);//睡觉
            spin_lock_irqsave(&ep->lock, flags);
        }
        __remove_wait_queue(&ep->wq, &wait);
        /* OK 我们醒来了... */
        set_current_state(TASK_RUNNING);
    }
    
    
/* 该函数作为callbakc在ep_scan_ready_list()中被调用
 * head是一个链表, 包含了已经ready的epitem,
 * 这个不是eventpoll里面的ready list, 而是上面函数中的txlist.
 */
static int ep_send_events_proc(struct eventpoll *ep, struct list_head *head,
                   void *priv)
{
    struct ep_send_events_data *esed = priv;
    int eventcnt;
    unsigned int revents;
    struct epitem *epi;
    struct epoll_event __user *uevent;
    /*
     * We can loop without lock because we are passed a task private list.
     * Items cannot vanish during the loop because ep_scan_ready_list() is
     * holding "mtx" during this call.
     */
    /* 扫描整个链表... */
    for (eventcnt = 0, uevent = esed->events;
         !list_empty(head) && eventcnt < esed->maxevents;) {
        /* 取出第一个成员 */
        epi = list_first_entry(head, struct epitem, rdllink);
        /* 然后从链表里面移除 */
        list_del_init(&epi->rdllink);
        /* 读取events,
         * 注意events我们ep_poll_callback()里面已经取过一次了, 为啥还要再取?
         * 1. 我们当然希望能拿到此刻的最新数据, events是会变的~
         * 2. 不是所有的poll实现, 都通过等待队列传递了events, 有可能某些驱动压根没传
         * 必须主动去读取. */
        revents = epi->ffd.file->f_op->poll(epi->ffd.file, NULL) &
            epi->event.events;
        if (revents) {
            /* 将当前的事件和用户传入的数据都copy给用户空间,
             * 就是epoll_wait()后应用程序能读到的那一堆数据.
             	
             	epi->event.data 转换为 java 语言就是：
             		epi.event.data
             		即将 epitem 里面的 epoll_event 中的 epoll_data 拷贝给用户态
             		uevent 是一个 epoll_event
             */
            if (__put_user(revents, &uevent->events) ||
                __put_user(epi->event.data, &uevent->data)) {
                list_add(&epi->rdllink, head);
                return eventcnt ? eventcnt : -EFAULT;
            }
            eventcnt++;
            uevent++;
            if (epi->event.events & EPOLLONESHOT)
                epi->event.events &= EP_PRIVATE_BITS;
            else if (!(epi->event.events & EPOLLET)) {
                /* 嘿嘿, EPOLLET和非ET的区别就在这一步之差呀~
                 * 如果是ET, epitem是不会再进入到readly list,
                 * 除非fd再次发生了状态改变, ep_poll_callback被调用.
                 * 如果是非ET, 不管你还有没有有效的事件或者数据,
                 * 都会被重新插入到ready list, 再下一次epoll_wait
                 * 时, 会立即返回, 并通知给用户空间. 当然如果这个
                 * 被监听的fds确实没事件也没数据了, epoll_wait会返回一个0,
                 * 空转一次.
                 */
                list_add_tail(&epi->rdllink, &ep->rdllist);
            }
        }
    }
    return eventcnt;
}
```

当进程调用 epoll_wait() 时，如果就绪队列中没有 fd，那么进入等待队列中挂起

等到就绪队列中有数据时就会被唤醒

就绪队列中存储的元素是 epitem，它跟几个字段的关系如下：

```C
struct epitem {
	//其他字段xxx
    
  struct epoll_event  event;  //事件类型
};

typedef union epoll_data {
    //其他字段xxx
    
   int      fd;
    
} epoll_data_t;


 struct epoll_event {
     uint32_t     events;    //事件类型，又叫触发状态：水平触发、边缘触发
     epoll_data_t data;     //存储 fd 的数据结构
 };
```

然后遍历就绪队列，获取每个 eplitem：

- 将每个 `eplitem.epoll_event.epoll_data` 拷贝到用户态，即将 fd 拷贝到用户态
- 然后再判断该 `eplitem.epoll_event.events` 是什么事件，如果是 LT，那么重新将这个 epitem 放回就绪队列，如果是 ET，那么跳过

