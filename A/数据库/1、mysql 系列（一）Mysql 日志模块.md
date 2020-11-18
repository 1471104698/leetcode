# mysql 日志模块



## 1、undo log

undo log 记录的是数据修改前的旧数据，它不会存储整行旧数据，只会存储修改列的旧数据

undo log 用于回滚 和 MVCC

undo log 类型有两种：

- **insert undo log：**事务对 insert 操作产生的 undo log，该 log 用来当前事务回滚的，在 commit 后这个 undo log 会直接删除，因为对于其他事务而言，本来就不存在旧数据，那么自然没有什么旧版本的可见性
- **update undo log：**将修改列的旧数据 copy 一份作为 undo log，
  - InnoDB 默认将 delete 当作 update，delete 只是修改 删除标志位，数据的真正删除是 由  purge（清除）线程执行的，即执行 delete 操作后数据行不会立即删除

undo log 不会无限增加，当数据库现在的 ReadView 中的事务 id 都跟 该 undo log 中记录的事务 id 搭不上边，该 undo log 会被删除



> ### undo log 添加过程

undo log 中有一个字段` db_trx_id` 记录修改当前数据的事务 id，以及一个` db_roll_ptr`，指向 undo log 版本链



**初始数据行：**

*![image.png](https://pic.leetcode-cn.com/1602484884-pnQOIh-image.png)*

**事务 1 修改数据行：**
 ![img](http://www.linuxidc.com/upload/2011_09/110904154240162.gif) 



**事务 2 修改数据行：**

 ![img](http://www.linuxidc.com/upload/2011_09/110904154240163.gif) 





## 2、redo log

> ### 为什么会存在 redo log

磁盘 IO 和 内存读写效率不是一个数量级的

在 mysql 中，如果每完成一次写操作都要写进磁盘：先寻道，然后数据更新；那么整个 IO 成本将会很高，同时效率也很低

为了解决这个问题，mysql 使用了另外一种更新机制：redo log，这种机制可以让新数据在数据库空闲的时候再刷盘



1、redo log 涉及两个东西：redo log  buffer 和 redo log file，redo log  buffer 是内存中的，redo log file 是磁盘中的

2、每次执行写操作的时候，mysql 会将更新的数据先写入到 buffer pool（缓冲池），同时会生成一条 redo log 数据，写到 redo log buffer 中，等到事务 commit 时，将 redo log buffer 中的内容刷新到 redo log file 上，由于是顺序写入，按照 append() 的方式，所以比直接刷新到磁盘上的 随机写入效率高得多，因为只需要一次寻道

3、buffer pool 中的脏数据一般会在数据库空闲的时候写回磁盘

5、由于 buffer pool 的数据是存储在内存中的，所以可能会由于数据库宕机等原因导致 buffer pool 中的数据还没刷新到磁盘中就丢失了，这时候当数据库重启的时候，就会去读取 redo log file，进行数据的恢复

6、所以，redo log 就是因为 mysql 使用了 buffer pool 在内存中保存更新的数据，而没有及时刷新为硬盘，以此来提高效率，但可能会由于数据库宕机导致 buffer pool 中的所有数据都丢失了，而需要 redo log 机制 来恢复数据



**redo log 存储的是修改列的新值，而不是数据行**



> ### redo log file 的结构

**redo log 使用循环写的方式**

redo log 文件大小是固定的，假设配置用于 redo log file 文件大小为 4G，那么可以配置成为 4 个文件，分别为 0 - 3 号文件，每个文件大小为 1G，那么当 3 号文件写满时，会重新回到 0 号文件开始写



redo log file 有两个指针：

- check point 表示 buffer pool 脏数据的起始位置，当宕机重启时就从这个位置开始恢复

- write pos 表示 新的 redo log 记录行 写入的位置

相当于一个是读指针，一个是写指针，当 check point == write pos 时，表示日志满了，那么需要将 check point 后的部分数据刷盘，然后留出空间继续写



<img src="https://pic4.zhimg.com/80/v2-b2a4003fde5ed1a12cfb9522235319ff_720w.jpg" style="zoom:60%;" />



## 3、bin log



[redo log 和 bin log（一）](https://www.cnblogs.com/xibuhaohao/p/10899586.html )

[redo log bin log（二）](https://www.cnblogs.com/a-phper/p/10006417.html)



bin log 全称为 binary log（二进制文件），数据都是二进制形式的

**bin log 和 redo log 的区别**：

- bin log 存储 事务过程中 产生数据库更新的 sql 语句（insert、update、delete），select 不会记录；redo log 存储的是数据修改的脏页
  - 注意 bin log 在 InnoDB 中只有事务提交了才会刷盘，在 Mysiam 这种没有事务的那么不需要等到事务提交
- bin log 是 server 层面的，所有存储引擎都有；redo log 是 InnoDB 独有的
- bin log 使用的是“追加写” 的方式，一个文件满了换一个新的文件写，不会覆盖以前的数据（所以可以当作数据备份，当误删数据库文件时，如果存在备份，可以使用 binlog 备份文件恢复）；redo log 大小固定，使用 "循环写" 的方式



bin log 可以用来在 数据库集群 主从复制的情况下，保证 slave 和 master 数据一致性

因为一般 master 是用来写的，slave 是用来读的，因此在 master 写完后，这段时间 master 和 slave 是数据不一致的

因此需要进行数据同步，而数据同步的策略就是将 master 的 bin log 日志传输给 slave，让它们根据 bin log 上的 sql 同步数据



**redo log 影响 master 的数据，bin log 影响 slave 的数据**，如果想要 master 和 slave 的数据一致，那么在事务 commit 的时候，必须保证 redo log 和 bin log 日志记录的一致性

因此，mysql 在事务提交时，使用了**分布式事务的 两阶段提交（ Two-phase Commit ）**





## 4、两阶段提交 2PC

以下是两阶段提交的流程图

 ![img](https://oscimg.oschina.net/oscnet/up-95cd031d1ed1db36c01e6c4c12637491497.JPEG) 



mysql 将 redo log 刷回磁盘的完整状态分为两阶段

在第一阶段，只执行 redo log 的刷盘，然后在 redo log 中将该事务的状态设置为 prepare

在第二阶段，执行 bin log 的刷盘，刷盘成功后，将 redo log 中该事务的状态设置为 commit，事务成功提交



> ### 两阶段提交解决的问题

看到这可能会疑惑：为什么一定要使用两阶段提交呢？如果不使用会发生什么事？

假如不使用两阶段提交：

1、master 事务提交后，完成了 redo log 的刷盘，这时在 bin log 刷盘的时候，master 数据库宕机了

这就导致了 bin log 和 redo log 数据的不一致性，当 master 重启的时候，会将 redo log 的数据刷盘，而 slave 获取的是 bin log，而这个 bin log 没有记录 redo log 中的新数据，因此导致 master 和 slave 的数据不一致性

2、同理，如果是先对 bin log 刷盘，再对 redo log 刷盘，那么刷完 bin log 后，master 数据库宕机了，那么重启后使用 redo log 恢复，这时候 redo log 没有新数据，而 slave 获取的是 bin log，它有记录新数据，同样导致 master 和 slave 的数据不一致性



因此需要使用两阶段提交：

Mysql 内部会将普通的事务当作一个 分布式事务 来处理，自动为每个事务分配一个唯一的 ID（XID），普通的事务 commit 被划分为 prepare 和 commit 两个阶段

事务提交过程中发生宕机，数据库重启时，会顺序扫描 redo log 进行数据恢复，如果扫描到 redo log 标签为 prepare，那么就拿该 redo log 的 XID 去 binlog 中看 bin log 是否已经落盘，如果没有，那么该事务回滚（删除 redo log 和 bin log 数据），如果有，那么修改为 commit，并且将数据恢复

将 redo log 和 bin log 按照状态详细拆分过程，如下：

- redo log 已经刷盘，prepare 状态，但是 bin log 没有刷盘成功，那么将该事务已经刷盘成功的 redo log 和  未刷盘或者刷盘刷一半的 bin log 删除
- redo log 已经刷盘，bin log 也刷盘
  - redo log 事务状态已经从 prepare 修改为 commit，那么表示事务提交成功，不管了
  - redo log 事务状态还没有修改为 commit，那么获取 bin log 的最后一个 xid 以及 redo log 中 prepare 状态的 xid，如果相同，那么意味着 bin log 也已经刷盘成功，将 redo log 的该事务修改为 commit；如果不同，那么将 redo log 和 bin log 多余的数据都删除 

