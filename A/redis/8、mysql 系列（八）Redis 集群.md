# Redis 集群



## 1、CAP 和 BASE 理论

C：一致性，所有集群节点在任意时刻的数据都是一致的，相当于强一致性

A：可用性，保证高可用，主节点宕机后从节点可以顶替上

P ：分区容忍性，这里的【分区】是指分布式节点在不同的机器上，网络分隔，这样的话就形成了【网络分区】，而分布在不同的网络，那么**节点间的通信不可避免的就会出现通信失败**（是节点间的），但是此时它们还是可以对外提供服务，这个叫做 【分区容忍性】



对于分布式系统来说，C 和 P 是不可能同时满足的

而 P 是一定要的，而 A 也是，那么 就必须舍弃掉 C ，退而求其次，要求数据的最终一致性



BASE 理论是基于 CAP 理论演化过来的，是对 CAP 理论的一种权衡

BA（Basically Available）：基本可用，假如系统出现了故障，但还是能用，相对于正常的系统，可能存在下面两方面的损失：

- 响应时间增大：比如查询原本只需要 0.5s，结果现在需要 2s
- 功能损失：比如一般情况下用户能够正常访问一个页面，但是在秒杀场景下，为了保证系统稳定性，会使部分不重要的功能降级，以及让部分用户引导到降级页面



## 2、redis 主从复制

redis 主从复制 即 一个 master，多个 slave，主要是用来保证高可用的

slave 负责读，master 负责写，这样的话读写分离，并发度增加

并且 master 宕机后可以使用 slave 再顶替上去，保证了高可用



那么 master 和 slave 如何保证数据一致性？

- 全量复制
- 增量复制



> ### 全量复制

顾名思义，是将 master 全部的数据都复制到 slave 中，全量复制涉及 RDB 文件 和 复制缓冲区

具体的实现过程如下：

- master 收到 slave 全量复制的命令
- master 调用 fork() 生成一份 RDB 文件
- 在生成 RDB 文件的过程中，master 会继续处理用户命令，因此会增加、修改一些数据，因此它会将 这段时间新的**写命令**写入到 复制缓冲区中
- 等到 RDB 文件创建完成后，将 RDB 文件 和 复制缓冲区 发送给 slave
- slave 收到后，会清空自己的全部数据，然后加载 RDB 文件 和 复制缓冲区



很简单的可以看出，全量复制有以下几个问题：

- 使用 RDB 文件，数据量过于庞大，不论是生成 RDB 文件 还是 传输 RDB 文件 效率都不高
- slave 会清空数据然后重新加载数据，这段时间它跟 master 的数据是不一致的，因此它不会分摊读操作，停止服务
- 当 RDB 生成 和 传输过程中，如果写操作太多，占满了 复制缓冲区，那么新的写命令会将旧的写命令给挤出去，导致命令丢失，这样的话后续会导致 slave 和 master 数据不一致，因此**当 复制缓冲区溢出时，当前轮次的 全量复制失败**，会进行新一轮的 全量复制，如果后续还是因为 复制缓冲区被占满了，这样的话就一直失败，一直重新开始，，，效率极低





> ### 增量复制

由于全量复制 效率太低，因此一般情况下只有在最开始新建 slave 的时候才使用全量复制，后续的数据同步都是使用 增量复制



**增量复制是一直存在的，只要 master 执行了写命令，那么就会给 slave 发送这个命令，让 salve 跟 master 保持数据同步，保证数据一致性**



增量复制涉及三个部分：

- 复制偏移量
- 复制缓冲区
- 服务器运行 ID（runid）



**复制偏移量：**master 和 slave 都会存储复制偏移量 offset，master 的 offset 是所有 slave 的基准

根据复制偏移量来判断 slave 和 master 哪些数据不一致，比如 master 的 offset 为 1000，如果存在一个 slave 的 offset 为 500，那么就意味着 slave 缺少 501 - 1000 的数据，因此 master 需要将这些数据同步给 slave

而每个 offset 对应的数据，就记录在 复制缓冲区 中



**复制缓冲区**：由 复制偏移量 和 字节值 构成

![img](https://img2020.cnblogs.com/blog/1440828/202006/1440828-20200613013215314-1831547129.png)

复制缓冲区由 master 进行维护，当 master 的 offset = 45，slave 的 offset = 35，那么就会将上面 offset 范围为 [36, 45] 的字节数据发送给 slave 让它同步

而复制缓冲区是一个类似数组的数据结构，它具有一定的大小限制，在上面全量更新也讲了，如果复制缓冲区满了，然后出现新的写命令，那么就会将旧的写命令给挤出去，这样的话，**如果 slave 欠缺的是已经被挤出去的写命令，那么就不能使用 增量更新了，只能进行 全量更新**



**服务器运行ID（runid）**

每个 redis 服务器无论是 master 还是 slave 在启动的时候都会**随机分配**一个 runid，在 master 和 slave 第一次进行复制的时候，master 会将自己的 runid 顺便发送给 slave，而 slave 会将 master 的 runid 持久化，当 slave 宕机重启后，会通过保存的 runid 来判断当前的 master 是否是之前的 master：

- 如果保存的 runid 和 当前的 master 的 runid 一致，那么表示 master 没有发生更换，可以继续 增量更新
- 如果保存的 runid 和 当前的 master 的 runid 不一致，那么意味着 master 发生过更换，那么 master 的偏移量跟自己的对不上了，那么只能进行一次 全量更新 来同步数据了



> ### 总结

在讲 全量复制 的时候也涉及到复制缓冲区了，当生成和传输 RDB 文件的过程中，会将新的写命令放入到复制缓冲区中，后续再将复制缓冲区发送给 slave，但是在传输 复制缓冲区的时候又有新的写命令，那 slave 不是接收不到吗？

这其实我们可以看作，全量复制 只涉及到 RDB 文件的生成和传输，而后续的复制缓冲区的传输是增量复制，它会伴随着一个 offset

master 的 offset 和 复制缓冲区只有一份，所有的 slave 都需要以 master 的 offset 和 复制缓冲区 作为基准，一旦缺失的内容是不存在于复制缓冲区的，那么就只能使用 全量复制 传输 RDB 文件了



优点：读写分离，可以支持更多的读操作

缺点：

- master 宕机需要手动设置新的 master
- master 宕机前如果跟 slave 的数据没有同步完，即 slave 跟 master 数据不一致，那么切换 slave 作为 master 存在数据丢失



## 3、redis 哨兵模式

哨兵模式实际上是建立在 redis 主从复制的基础上的

使用的模式仍然是主从复制，只不过是加了几个 哨兵节点，用来监控 master 和 slave 是否存活（通过心跳检测 ping）

如果 master 宕机了，那么会自动 从 slave 中选择一个新的 master，选择的依据如下：

- 过滤掉不健康（断线）的节点

- slave 中优先级最高的（如果有设置的话）
- slave 中 offset 最大的，即复制的数据更加完整的（如果存在的话）
- runid 最小的



优点：

- 读写分离
- master 宕机能够自动选举新的 master

缺点：

- 跟 主从复制的一个缺点一样，master 宕机前数据没有同步完成，那么没有同步的数据会丢失



> redis 哨兵不讲太多，基本都是用 redis 主从复制 合并起来的 redis 集群



## 4、redis cluster

redis cluster 采用了**去中心化**的方式，即没有任何一个 redis 节点是集群的中心

redis cluster 使用了**多个 主从复制 联合**的方式，即存在多个 master，而每个 master 又有 [1, N] 个 slave

redis cluster **去除了哨兵节点**，但是为每个 redis 节点增加了故障判断、故障转移能力，即将 哨兵节点的功能整合到了 redis 节点

集群中的每个 redis 节点都在跟其他的节点进行通信，它们各自存在两个端口号：一个是对外提供服务的端口号 port_1，一个是节点间通信的端口号 port_2；

一般 port_2 = port_1 + 10000，比如某个节点 port_1 =  6379，那么 port_2 = 16379



redis cluster 每个 master 存储的数据是不一致的，它们各自管理部分数据，这样的话，就需要存在一定的**规则**，让 key 固定映射到某个 master 上，这样后续才能够按照相同的规则进行查找，这个规则就是 **一致性 hash**



> ### 1、一致性 hash

一致性 hash 是将 hash 范围设置为 [0, 2^32 - 1]，同时 **围成一个圈**

计算每个 redis 节点的 hash 值，然后插到对应的 hash 位置上

比如下面的 Node A、B、C、D，就是通过对应的 hash 算法得到在 圈上的位置

而后续无论是 插入、删除、查询 key，通过相同的 hash 算法计算 key 的 hash 值，然后映射到圈上，按照顺时针走，第一个遇到的 redis 节点就是存储这个 key 的节点

<img src="https://ss.csdn.net/p?https://mmbiz.qpic.cn/mmbiz_jpg/UtWdDgynLdbhiae1AfNYAibdp7ib2wTZTrp5USkWgd1OanGzb20HvdhrBgPHutBZjsb6WvGLE7MdTMXFzRRm0cxgw/640" style="zoom:60%;" />

通过上图可以看出，假设 Node C 宕机了，那么影响的就是 Node B 和 Node C 之间的数据，不会影响到其他位置的数据

如果在 Node B 和 Node C 之间添加一个 Node E 节点，那么需要迁移的就是 Node B 和 Node E 之间的数据而已



但是这样的一致性 hash 存在一个缺点：**数据倾斜**

当只有少数的几个 节点，并且它们通过 hash 计算后的位置在圈上很靠近，这样的话对于造成了大部分的数据都达到了 Node A 上，而 Node B 只需要承担 Node A 和 Node B 之间的一点数据而已

<img src="https://ss.csdn.net/p?https://mmbiz.qpic.cn/mmbiz_png/UtWdDgynLdbhiae1AfNYAibdp7ib2wTZTrp9PHNl5KgHfujn5WQzr3sXwccleFsbhRfUdKuc3JHiafHB25SJ6ng5aQ/640" style="zoom:70%;" />



因此，出现了虚拟节点，为每个节点多计算几个 hash 值，对应 hash 值的位置就是该节点的虚拟节点，落到这个虚拟节点上的数据也是落到 真正节点上的数据

比如下面的 Node A#1、Node A#2、Node A#3 就是 Node A 服务器的虚拟节点，落在它们上的数据实际上就是映射到 Node A 服务器上，这样就能够解决数据倾斜问题，当然，前提是 要有一个好的 hash 算法来让 虚拟节点 分布均匀

<img src="https://ss.csdn.net/p?https://mmbiz.qpic.cn/mmbiz_png/UtWdDgynLdbhiae1AfNYAibdp7ib2wTZTrpnNGzjcWYy3ylL7s1Bq2UKicU5mYG8SHsuIFTOf2PMe2FstpM2gMeQbw/640" style="zoom:70%;" />



> ### 2、槽位 hash

具体看 <https://blog.csdn.net/lihongfei110/article/details/106733267>

<https://www.jianshu.com/p/04dd90ea08f5>



redis cluster 不是直接使用的上面的 一致性 hash 算法，而是在这个算法上进行改进

redis cluster 将数据映射为 16384 个槽（slot），每个 master 节点管理一部分的 slot



**槽道原理：**

槽道的组成由两个部分：

- 一个 2048 大小的 byte 数组，转换为 bit 就是 16384，这个部分称为 **位序列**，每个 redis 节点都有的
- 一个 16384 大小的 Node 类数组，每个索引位置指向的是管理该槽位的 redis 节点的信息的 Node 对象，这个部分叫做 **共享数组 / 索引数组**，同样是每个 redis 节点都有的



**位序列：**

master 将管理的槽位序号跟 位序列进行映射，对于自己管理的槽位的序号，在对应位序列上为 1，没有管理的则为 0

slave 节点没有管理任何槽位，因此它的 位序列都为 0

比如下面的 8000 号的 master，它管理 [0, 5460]，那么它的位序列中 [0, 5460] 位上都为 1，其他的位置都为 0

![在这里插入图片描述](https://img-blog.csdnimg.cn/20200613154407924.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L2xpaG9uZ2ZlaTExMA==,size_16,color_FFFFFF,t_70)



**共享数组/索引数组：**

集群创建的时候，手动给每个 master 节点分配好各自管理的槽位，当分配好后，**每个 master 节点会两两进行通信**，将自己的槽位管理信息告知其他的节点，当全部通信完毕后，每个 master 节点都知道了哪个 master 节点管理着哪些 slots

master 节点怎么存储其他 master 节点管理的 slot？

通过共享数组/索引数组，实际上并不是直接存储 master 和 slot 列表的映射，而是存储某个 slot 映射到哪个 master 上

如下，在每个 master 中，都存在以下这么一个 数组，它存储的是对应 slot 上管理着这个 slot 的 Node 对象

Node 对象内容包括：

```java
class Node{
    int id;
    int ip;	//节点的 ip 地址
    String role;
    int port;	//节点通信的端口
}
```

通过这个 Node 对象可以直接获取到管理这个 slot 的 redis 节点，然后进行通信

可以看出多个槽位复用了同一个 Node 对象

![img](https://upload-images.jianshu.io/upload_images/6152718-1938e4ed002f37aa.png?imageMogr2/auto-orient/strip|imageView2/2/w/1200/format/webp)



**操作某个 key 的过程：**

- 客户端连接了 8002，，调用 set name
- 通过 hash(name) 得到对应的 hash 值 = 5798，8002 查找自己的位序列，发现对应位为 0，那么不是自己管理的
- 到索引数组中查找 5798 位置的 Node 对象信息，获取到 8001 的详细信息，包括 ip 和 port
- 告知客户端，让客户端重定向到 8001

同时为了防止这样多次跳转，客户端一般会缓存 各个 redis 节点 和 key 之间的映射关系表，这样的话就可以直接请求到对应的 node，不过由于 key 一直在变，同时可能发生数据迁移，因此这个映射表需要经常更新



**slot 在内存中的形式：**

同时，我们可以猜测，为了 方便槽位迁移，每个 slot 就是一个 dict，即一个 redis 节点管理多少个 slot 就维护多少个 dict

并且还存在一个大的 dict 来映射这些 slot

因此 key 查找顺序为：

- 通过 slot 的 hash 值在大的 dict 中定位到对应的 slot，它也是一个 dict
- 在 slot 中通过 key 的 hash 值找到对应的 key



> ### 使用 16384 而不是使用 65536 个 slot 的原因

作者本人自己说了：

- redis 节点每秒都会发送 ping 消息作为心跳包去检测其他节点的存活情况，其中消息头有字段 `myslots[CLUSTER_SLOTS/8]`，它是使用 槽位 / 8 bit 求得字节数组大小，使用 16384 个slot，那么就是 2048B，即 2KB，如果是 65536 个 slot，那么就是 8KB，对于每秒的 心跳包来说占的网络带宽太大了
- redis 集群不太可能超过 1000 个，最多 1000 个 redis 节点平均下来每个 redis 节点管理 1600 个 slot 也足够了，不需要 65536