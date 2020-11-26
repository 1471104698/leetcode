# CPU 缓存一致性



## 1、CPU 和 cache 的关系

随着时间的推移，CPU 和 内存的速度之间差异越来越大，CPU 每次都 读写数据都直接跟 内存打交道，那么效率将会很低

因此为了解决这个问题，在 CPU 和 内存之间引入了 高速缓冲区 cache

每个 CPU 和 内存之间都存在 3 个高速缓冲区，L1 cache、L2 cache、L3 cache

在 多核 CPU 中，**L1 cache 和 L2 cache 每个 CPU 核心 私有，L3 cache 同个 CPU 多个核心 共享**

​	L1 cache 包含 dCache（数据缓存）和 iCache（指令缓存）

（如果是单核的，那么相当于一个 CPU 一个 L1 cache、一个 L2 cache、一个 L3 cache）

L1 cache 与 CPU 核心 直接交互，读写效率最高，同时内部容量也是最小的

<img src="https://mmbiz.qpic.cn/mmbiz_png/J0g14CUwaZf0RnQxwibdcyFOTw0NvInPPKJan1icpeMMyiawV2UvVwcCayaDLWJ00D3rh78LYZqBwOv9tSTYCvRog/640?wx_fmt=png&tp=webp&wxfrom=5&wx_lazy=1&wx_co=1" style="zoom:60%;" />





## 2、CPU 数据写回内存的方式

CPU 修改完数据将数据写回内存的方式有两种：直写 和 回写

> #### 直写

在这个方法中，当 CPU 修改了变量 a，修改完后会判断 cache 中是否存在 CPU cache 中

- 如果 CPU cache 中缓存了该变量，那么更新 CPU cache 的数据，然后再写回内存

可以看出，该方法只要进行过数据修改，都会立即将数据写回到内存中，这样的话每当 CPU 修改数据，都需要跟内存打交道，显然效率很低



> #### 写回

每个 cache line 都存在 1bit 来记录该 cache line 是否被修改过，如果没有被修改过，是干净的，那么为 0，如果被修改过，那么就是脏的，置为 1。	这个 1bit 称作 dirty bit

在这个方法中，CPU 修改了变量 a

- 将数据更新到 cache line 中，并且将 dirty bit 置为 1，然后不会立马将数据写回到内存中，只会在后面才写回内存

- 如果 已经被置为 1 的 cache line 需要别替换为别的 cache line 数据，那么需要将 脏的 cache line 数据写回内存中，然后再读取新的 cache line 

可以看出，该方法如果 cache line 之前没有修改过数据的话，是无需立马写回内存，会在后面的时间再写回内存，**存在 cache 和 内存的数据不一致问题**



## 3、CPU 和 cache 缓存一致性问题

```java
这里讲的 缓存一致性 只能是发生在一个 CPU 中，因为一个 CPU 同一时间只能调用一个进程，而一个 CPU 有多个 CPU 核心
一个 CPU 核心可以调用一个线程，而一个进程中的多个线程是数据共享的，并且共享的是当前进程的数据

对于不同 CPU 来说，它们调用的是不同的进程，一般情况下进程之间的数据是隔离的，是不可能互相访问的，因此不存在什么数据一致性问题
```



每个 cache 划分为多个 缓存行 (cache line)，每个缓存行 64B，之前也讲过了，这里就不讲了

问题是，内存中的每个 cache line 数据不一定只存在于 一个 CPU 核心的 cache line 中，同时也可能存在于 同一个 CPU 的其他核心 ，如果一个 CPU 核心 修改了某个 cache line 的数据，那么如果没有通知其他 CPU 核心数据失效，那么就会导致数据不一致

例子：

假设 CPU A 和 CPU B 中的 cache 中都存在 变量 i，而 CPU A 将 i = 0 修改为 i = 1，由于使用的是 写回机制，因此不会立马写回到内存中，同样的也没有通知 CPU B 它的 cache 失效

因此 CPU B 会继续使用自己 cache 中的 i = 0，导致使用的仍然是旧值

<img src="https://mmbiz.qpic.cn/mmbiz_png/J0g14CUwaZf0RnQxwibdcyFOTw0NvInPPYibKuToa682yhIE7RiaUq0KLxRNtib9EBGUe1L8ZNCBMYtVxL5EgHIfMg/640?wx_fmt=png&tp=webp&wxfrom=5&wx_lazy=1&wx_co=1" style="zoom:60%;" />





**要解决这个问题，需要引入一种机制，这个机制必须保证以下两点：**

- 1、某个 CPU 核心进行数据更新的时候，需要通知其他的 CPU 核心，让它们知道数据更新了，称作 **写传播**
- 2、多个 CPU 核心 修改同一个数据，在其他 CPU 核心中看到的数据修改顺序消息必须是一致的，即 **事务串行化**

第一条很容易理解，第二条是什么意思呢？

举例：

假设 CPU 存在 4 个 核心，它们共同操作 变量 i

假设 CPU A 将 i = 0 修改为 i = 100，CPU B 将 i = 0 修改为 i = 200，然后它们通知 CPU C 和 CPU D 知道此事

CPU C 和 CPU D 都会对数据进行同步，那么此时就存在一个问题，如果 CPU C  和 CPU D 收到数据更新的消息顺序不是同步的

比如  CPU C 先收到 i = 100 的修改消息，再收到 i = 200 的修改消息，那么最终 CPU C 的 i = 200

CPU D 先收到 i = 200 的修改消息，再收到 i = 100 的修改消息，那么最终 CPU D 的 i = 100

因此导致数据不一致

<img src="https://mmbiz.qpic.cn/mmbiz_png/J0g14CUwaZf0RnQxwibdcyFOTw0NvInPPAvJH3fcHDgr9GcU9icCCDM8mHKnQYyQ9p0JicUqEicjV4IMbfVhBETp8w/640?wx_fmt=png&tp=webp&wxfrom=5&wx_lazy=1&wx_co=1" style="zoom:60%;" />



因此，为了防止这种事的发生，就必须保证 CPU C 和 CPU D 收到数据变化的消息顺序是一致的

事务串行化的实现必须保证以下两点：

- 实现 写传播（即 写传播 是 事务串行化的必要条件）
- 引入类似 锁机制 的机制，如果存在多个 CPU 核心存储相同的数据，那么只有获取到锁的 CPU 核心才能够修改数据，这样可以保证广播消息的顺序



## 4、总线嗅探（写传播实现）

为了解决 CPU B 无法读取到 CPU A 的后面就引用了一种机制，叫做总线嗅探



总线：总线不是一条线，英文名为 bus，意为公交车， 总线就是公共汽车线路，连接的设备就是公交站。传输的数据包就是乘客。每个乘客都要知道自己从哪站上，到哪站下，然后等到站的时候就下去进入另一个设备进行处理。公交车需要个调度室，所以总线需要有个控制器。 



总线嗅探的工作机制：

- CPU A 修改了 i，通过总线将这个修改事件 和 修改的变量和值 广播出去，通知其他所有 CPU 核心
- 每个 CPU 核心都会监听总线上的广播事件，并且检查目标变量是否存在于自己的 cache 内
- 如果 CPU B 的 cache 中存在该变量，那么 CPU B 会认为重新从主存中读取



总线嗅探机制 有一个很明显的问题，所有 CPU 核心都需要无时无刻的监听总线上的广播事件，而所有 CPU 核心只要修改过数据，都会发送一个广播事件，这样的话，就会导致 CPU 资源的浪费，因为 如果 CPU 核心修改的数据只有自己独占，而别的 CPU 核心并没有 cache 的话，那么这次广播事件显然是一次无意义的事件，浪费 CPU 资源 并且 加重了总线的负载

同时还有一个问题，**总线嗅探并不能保证事务的串行化，它仅仅只是实现了写传播而已**



因此，出现了一个 **基于总线嗅探机制，用于解决事务串行化的 CPU 缓存一致性协议 MESI**



## 5、CPU 缓存一致性协议 MESI（事务串行化实现）



MESI 分别代表 缓存行 的 4 种状态，可以使用 2bit 来表示

- *Modified*，已修改
- *Exclusive*，独占
- *Shared*，共享
- *Invalidated*，已失效

 「已修改」 指代 cache line 被修改过，但是还没有写回到内存中，即我们上面说的 dirty bit = 1 的情况

 「已失效」 表示该 cache line 的数据已经失效，不可再使用，如果需要的话需要重新读取内存获取新值

 「独占」 表示该 cache line 中的数据只有当前 CPU 核心才具有，其他 CPU 核心没有该 cache line

 「共享」 表示该 cache line 在 多个 CPU 核心中都存在



 「独占」和「共享」 差别在于 由于独占状态时只有 CPU A 存在 i 变量，所以当 CPU A 修改这个变量时，**不需要发布广播事件，直接修改即可，也不需要设置为  「已修改」状态**

CPU A 独占 变量 i 所在的缓存行，当 CPU B 在**读取 i 变量前**，会将这个事件发布到总线上，总线进行广播， CPU A 发现自己持有这个变量，并且是  「独占」状态，此时 CPU A 中的 i 有两种情况：CPU A 修改过了 和 CPU A 没有修改过

CPU A 会将 cache 中的数据写回到内存中，CPU B 从主存中读取最新值，然后 CPU A 和 CPU B 该缓存行数据都设置为 「共享」

当 CPU A 修改处于 「共享」的 cache line 时，需要发布事件让 总线广播出去通知其他持有该 cache line 的 CPU 核心，这里会让 CPU B 将该 cache line 设置为「已失效」，等到 CPU A 修改完后，会将 cache line 设置为  「已修改」,同时会修改缓存行的 dirty bit = 1，表示该 cache line 中跟 内存数据不一致，是脏数据

当 CPU A 再次修改 i 的值时，由于该 cache line 已经设置为 「已修改」了，所以表示之前已经让其他 CPU 核心的 cache line 无效了，所以不需要再发布消息了，直接修改即可

当 CPU B 要读取 i 变量时，发现缓存行的状态为  「已失效」，因此会到主存中读取，此时会将这个事件发布到总线上，CPU A 监听到后将 cache 数据写回到主存，然后 CPU B 读取到后，两者再设置为 「共享」

当 CPU A 要替换掉某个 cache line 的数据时，需要先判断该缓存行的状态，如果 处于 「已修改」，需要先将 该 cache line 写回内存

 即当 CPU A 的缓存行处于 独占、已修改 状态时，CPU B 要去内存中读取这个缓存行的数据，那么 CPU A 会将该缓存行写回到内存中，然后 CPU B 再读取，然后两者都将缓存行的状态设置为 「共享」



这里我们可以看出，在使用了 MESI 协议后，**「已修改」和  「独占」两种状态下的修改不会去总线发布广播事件，避免了 CPU 资源的浪费 和 减少了总线的负载**





| 状态                     | 描述                                                         | 监听任务                                                     |
| :----------------------- | :----------------------------------------------------------- | :----------------------------------------------------------- |
| M 修改 (Modified)        | 该Cache line有效，数据被修改了，和内存中的数据不一致，数据只存在于本 CPU Cache中。 | 缓存行必须时刻监听所有试图读该缓存行相对就主存的操作，这种操作必须在缓存将该缓存行写回主存并将状态变成S（共享）状态之前被延迟执行。 |
| E 独占、互斥 (Exclusive) | 该Cache line有效，数据和内存中的数据一致，数据只存在于本 CPU Cache中。 | 缓存行也必须监听其它缓存读主存中该缓存行的操作，一旦有这种操作，该缓存行需要变成S（共享）状态。 |
| S 共享 (Shared)          | 该Cache line有效，数据和内存中的数据一致，数据存在于多个 CPU Cache中。 | 缓存行也必须监听其它缓存使该缓存行无效或者独享该缓存行的请求，并将该缓存行变成无效（Invalid）。 |
| I 无效 (Invalid)         | 该Cache line无效。                                           | 无                                                           |





## 6、MESI 引入的问题 以及 解法方法

[MESI 的问题 以及 解决方法 - 博客园](https://www.cnblogs.com/xmzJava/p/11417943.html)

### 6.1、Store Buffers

上面说了，CPU 在将数据写回缓存时，如果 cache line 是 「共享」，那么会向总线发布广播消息，让其他 CPU 核心中的 cache line 无效，而这个过程不是跟看着的这么简单：

- CPU A 修改 i，向总线发布广播消息，CPU A 进入等待状态，不执行指令
- CPU B 和 CPU C 都监听到无效消息，将 cache line 标记为无效，然后给 CPU A 返回 ACK 反馈，表示处理完毕
- CPU A 收到 其他 CPU 的 ACK 反馈，停止等待，将数据写入到 cache 中

很显然，问题很大，当 CPU A 持有相当一部分 cache line 都处于 「共享」时，那么修改写回缓存后需要 频繁进入等待状态，这显然对于高效的 CPU 来说是不能接受的

因此出现了 Store Buffers，它在 CPU 和 cache 之间又加了一层小小的 cache，CPU 修改数据时，不会直接写回 cache，而是会写入到 Store Buffers 中，这样就不会向总线发布失效消息，这样 CPU 就可以继续去做别的事情



### 6.2、Store Forwarding

加入 Store Buffers 后，带来了一个新的问题，CPU 从 cache 中读取数据，而有的数据在 Store Buffers 中没有写入 cache，即前面写入的逻辑对于后面的操作来说不可见，造成了数据可见性问题



解决方法是 加入 Store Forwarding

每次 CPU 读取数据的时候，如果 Store Buffers 中有数据，那么从 Store Buffers 中尝试读取，没有的话再读取 cache

这样的话就保证了 单核 CPU 的数据可见性



### 6.3、内存屏障（Memory Barriers）

Store Buffers 的引入会存在 数据可见性问题，后续再引入 Store Forwarding 解决了单核 CPU 的数据可见性问题

但是它并不能解决多核 CPU 的可见性问题，因为 CPU A 写入 Store Buffers 中的数据通过 Store Forwarding 后 CPU A 自己可见，但是它没有写入  cache 中，没有发布失效消息，这样别的 CPU 核心就不知道该数据失效了，这样 CPU A 前面执行的写逻辑 对于 其他 CPU 核心来说是不可见的



例子：

```C++
int a = 0;
int b = 0;
void foo(void)
{
 a = 1;
 b = 1;
}

void bar(void)
{
 while (b == 0) continue;
 assert(a == 1);
}
```

CPU A 访问 foo()，CPU B 访问 bar()，假设 a 被 CPU A 和 CPU B 共享，b 被 CPU A 独占

- CPU A 修改了 a，将 a 存储进 Store Buffers 中，没有刷入 cache

- CPU A 修改了 b，将 b 写入到 cache 中，发布失效消息
- CPU B 监听到失效消息，将 b 失效，然后重新访问，同步到 CPU A 的新的 b = 1，退出循环
- CPU B  cache 中 a = 0，所以  assert failed

为了解决这种情况，因此出现了 内存屏障 

```C++
int a = 0;
int b = 0;
void foo(void)
{
 a = 1;
 smp_mb();	//内存屏障
 b = 1;
}

void bar(void)
{
 while (b == 0) continue;
 assert(a == 1);
}
```

在 a 和 b 之间添加 内存屏障 smp_mb()，它的语义是：在执行 内存屏障的下一条指令之前，需要先将 Store Buffers 中的数据写回到 cache





### 6.4、无效队列（Invalidate Queues）

引入 Store Buffers 后并不能解决 MESI 全部的问题，当在写操作前添加内存屏障后，会将 Store Buffers 中的数据都写回 cache，那么表示会发布多条失效消息，这样 CPU A 还是需要等待其他 CPU 核心处理完 失效 cache line 然后接收它们返回的 ACK

因此，为了解决这个问题，引入了 Invalidate Queues，每个 CPU 核心都维护一个 Invalidate Queues，用来接收 无效消息

当 CPU A 发布无效消息后，CPU B 收到后不会立马去执行，而是将无效消息放入到 Invalidate Queues 中，然后立马返回一个 ACK，这样 CPU A 就大大缩短了等待的时间，而 CPU B 的 Invalidate Queues 内的无效消息会在后续进行处理

但这样也带来了一个问题，**如果 CPU B 没有及时将 cache line 设置为无效的话，那么就又会导致 数据可见性 问题**

例子：

```java
int a = 0;
int b = 0;
void foo(void)
{
 a = 1;
 smp_mb();	//内存屏障
 b = 1;
}

void bar(void)
{
 while (b == 0) continue;
 assert(a == 1);
}
```

a 是 CPU A 和 CPU B 共享，b 是 CPU A 独占

- CPU A 修改 a 的值，将修改数据放入到 Store Buffers
- CPU A 执行内存屏障，将 Store Buffers 中的数据刷新回 cache，发布失效消息
- CPU B 监听到失效消息，将失效消息放到 Invalidate Queues，返回一个 ACK
- CPU A 修改 b 的值，刷新回 cache
- CPU B 获取 b 的值退出循环，但是由于没有执行失效消息， cache 中 a = 0 并没有失效，导致 assert failed

解决方法是在读操作前面添加一个 内存屏障

```C++
int a = 0;
int b = 0;
void foo(void)
{
 a = 1;
 smp_mb();	//内存屏障
 b = 1;
}

void bar(void)
{
 while (b == 0) continue;
 smp_mb();	//内存屏障
 assert(a == 1);
}
```

这里的内存屏障的语义是 在执行下面的读操作前，先处理 Invalidate Queues 所有的失效消息，这样就会让 cache 中 a = 0 失效了，然后去到内存中同步新的值，解决数据可见性问题



### 6.5、读内存屏障 和 写内存屏障

可以发现，smp_mb() 这个内存屏障很重，具有两个操作：在执行下一条指令前，将 Store Buffers 中的数据刷新回 cache 和 将 Invalidate Queues 中的失效消息全部处理

但是有时候我们只需要其中一个操作而已，因此根据这个 内存屏障 的功能划分出了两个内存屏障：读内存屏障 和 写内存屏障

- 写内存屏障（ **StoreStore** ）：在执行下一条指令之前，将 Store Buffers 中的数据刷新回 cache，保证前面执行的写逻辑对于其他的 CPU 核心可见

- 读内存屏障（ **LoadLoad** ）：在执行下一条指令之前，将 Invalidate Queues 中的失效消息全部处理，保证其他 CPU 核心执行的写操作对于当前 CPU 核心可见



我们可以看出，**实际上 Java 中的 volatile 就是根据这个 读内存屏障 和 写内存屏障 来保证可见性的**



### 6.6、总结

1、CPU 和 cache：由于 CPU 和 内存之间访问速度差异太大，为了提高 CPU 的效率，所以在 CPU 和 内存之间引入了 cache，一个 CPU 有多个 核心，每个 CPU 核心都有自己的 L1 cache 和 L2 cache，CPU 核心操作的数据都是内存的数据副本，当修改完后需要写回内存，但是如果每次写操作都直接写回内存的话，那么 CPU 效率太低了，前面也说了 CPU 和 内存速度差异大才引入 cache 的，所以 CPU 使用了 回写 的方式

2、回写：CPU 操作的基本数据单位是 cache line，当对某个 cache line 的数据进行修改时，会将该 cache line 中的 dirty bit 设置为 1，表示脏数据，跟内存数据不一致。**但是这样又出现了新的问题：**由于是多核 CPU，一个 cache line 可能由多个 CPU 核心持有，如果 CPU A 修改了 变量 i，而没有通知 CPU B 已经修改了的话，那么 CPU B 并不知道 i 已经被修改了，仍然 使用的是自己 cache 中的旧数据，这就导致了数据不可见，即 CPU A 的写操作 对 CPU B 不可见，因此出现了 总线嗅探

3、总线嗅探：CPU 核心会把自己修改的数据发送到 总线上，而所有的 CPU 都会监听总线上的消息，如果 CPU A 修改了 i，CPU B 监听到这个消息，如果发现自己 cache 有 i，那么就会将 更新的 i 数据更新到自己的 cache 中。**但是这样就又有了一个新的问题**：CPU 的修改的所有数据都会发送到总线上，其中有的数据可能只有自己持有，而所有的 CPU 会监听这条数据，但实际上这条数据对于其他 CPU 来说没有任何意义，这显然是 浪费 CPU 资源 和 增加总量的负载，因此出现了 CPU 缓存一致性协议 MESI

4、CPU 缓存一致性协议 MESI：MESI 是基于总线嗅探的协议，它给 cache line 定义了四种状态：已修改、独占、共享、无效，当 CPU A 和 CPU B 的 cache line 中都有变量 i 的时候，它们会把 cache line 设置为 共享 状态，当 CPU A 修改了 i 变量时，会给总线发布 无效消息，CPU B 监听到该消息后，会将 cache line 设置为无效状态，然后 CPU A 会将 cache line 设置为 已修改，再将 dirty bit 置为 1。**在 cache line 为 已修改 和 独占 状态下修改数据不会往总线上发布消息，有效降低了总线的负载。**

5、本来没有什么问题的，**但是存在这么个问题：**当 CPU A 持有相当一部分 【共享】状态的数据时，那么 CPU A 修改数据会频繁发送 失效消息，并且还需要 等待 其他 CPU 处理完 失效消息再接收它们 返回的 ACK ，这段时间 CPU 不能干其他事，这显然对 CPU 来说是不友好的，因此引入了 Store Buffers

6、Store Buffers：CPU A 修改完数据后不会直接写入 cache，而是会先存储到 Store Buffers 中，在后面再一起刷回 cache 中，**但是这样又出现了个问题了**：如果是这么做的话，只能对 单核 CPU 来说是数据可见的，但是对于其他的 CPU 来说，由于 CPU A 修改的数据存储在 Store Buffers 中，没有通知其他 CPU 数据失效了，所以对于 其他 CPU 核心来说，它们使用的仍然是旧数据，因此 多核 CPU 又出现了 数据可见性问题，因此引入了内存屏障

7、内存屏障：在内存屏障的下一条指令执行前，需要将 Store Buffers 中的数据都 刷回 cache。**但是这样又出现了问题：**执行内存屏障的时候会有多个数据刷回 cache，从而发布失效消息，这样就会导致 CPU A 进入等待状态，效率又降低了，又回到了最初的问题。因此出现了 无效队列。

8、无效队列：每个 CPU 核心都维护一个 无效队列，用来存储无效消息，当它们收到无效消息的时候，不会立马去处理，将 cache line 设置无效状态，而是会将 无效消息放入到 无效队列中，然后立马返回一个 ACK，这样 CPU A 等待的时间就缩短了，而 无效队列 中的无效消息会在后面进行处理。**但是这样就又出现了一个问题：**CPU A 修改了 i，将它写回 cache，但是 CPU B 将无效消息放入了无效队列中，这样 CPU B 在读取 i 的时候，仍然是使用的旧值，导致数据不可见。因此需要再使用内存屏障。

9、由于内存屏障有两个语义，有时候往往只需要一个语义，因此将内存屏障按照功能划分出了新的两个内存屏障：写内存屏障 和 读内存屏障



上面我们可以看出：

单纯的 MESI 协议 已经保证了 多核 CPU 的数据可见性 了，但是 **为了避免 CPU 因为等待 其他 CPU 核心处理无效消息成功后返回的 ACK 而降低了 CPU 效率的问题**  增加了 Store Buffers ；但是又重新出现了 多核 CPU 的可见性问题，因此出现了内存屏障，用来解决 Store Buffers 出现的 多核 CPU 缓存可见性问题；但是最终 CPU 还是会等待 其他 CPU 核心处理完 无效消息 的过程，同时由于 Store Buffers 会堆积修改的数据，因此当 CPU 将数据刷新回缓存时，需要等待更多的无效 ACK，因此出现了 无效队列，其他 CPU 核心收到无效消息后先不处理，尽快返回 ACK，减少 CPU 等待的时间

这也就是在 Java 中，多线程情况下数据不可见就是因为既存在 Store Buffers 但 又没有添加内存屏障

**因此多线程环境下，添加 volatile 是通过添加内存屏障来保证内存可见性**

但是 volatile 并不能保证原子性，因为 volatile 修饰的变量在同一时间可以被多个 CPU 核心锁持有