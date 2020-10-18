# mysql 日志模块



## 1、undo log

undo log 记录的是数据修改前的旧数据，它不会存储整行旧数据，只会存储修改列的旧数据

undo log 用于回滚 和 MVCC

undo log 类型有两种：

- **insert undo log：**事务对 insert 操作产生的 undo log，该 log 用来当前事务回滚的，在 commit 后这个 undo log 会直接删除，因为对于其他事务而言，本来就不存在旧数据，那么自然没有什么旧版本的可见性
- **update undo log：**将修改列的旧数据 copy 一份作为 undo log，
  - InnoDB 默认将 delete 当作 update，delete 只是修改 删除标志位，数据的真正删除是 由  purge（清除）线程执行的，即执行 delete 操作后数据行不会立即删除



update undo log 存储在内存中的一个专门的 undo 表空间中，用于后续的 回滚 和 MVCC

undo log 不会无限增加，当数据库现在的 ReadView 中的事务 id 都跟 该 undo log 中记录的事务 id 搭不上边，该 undo log 会被删除

<img src="https://img-blog.csdnimg.cn/20190808004004673.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L3FxXzQxNjUyODYz,size_16,color_FFFFFF,t_70" style="zoom:70%;" />





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

redo log file  的大小是固定的，使用循环写的形式

redo log file 有两个指针，check point 表示 buffer pool 脏页的起始位置，write pos 表示 新的 redo log 行写入的位置

相当于一个是读指针，一个是写指针，当 check point == write pos 时，表示日志满了，那么需要将 check point 后的数据刷盘，这样就又有空闲的空间了



<img src="https://pic4.zhimg.com/80/v2-b2a4003fde5ed1a12cfb9522235319ff_720w.jpg" style="zoom:60%;" />



> ### undo log 和 redo log 的关系

具体看  https://www.cnblogs.com/xinysu/p/6555082.html 



在修改前，是先生成 undo log，再修改值，再生成 redo log

```java
A.begin.
B.记录A=1到undo log.	//修改前生辰 undo log 记录修改列的旧值
C.修改A=3.			//修改
D.记录A=3到redo log.	//修改后将修改的值记录到 redo log 中
E.记录B=2到undo log.
F.修改B=4.
G.记录B=4到redo log.
H.将redo log写入磁盘。
I.commit
```



redo log 文件并不会关系哪个 log 属于哪个事务的，对应 log 的事务 id 也会被当作数据记录进 redo log file 中，基本所有的事务都共用一个 redo log file，因此 redo log 的内容可能如下：

```java
记录1: <trx1, insert …>	//事务1的 log
记录2: <trx2, update …>	//事务2的 log
记录3: <trx1, delete …>	//事务1的 log
记录4: <trx3, update …>	//事务3的 log
记录5: <trx2, insert …>	//事务2的 log
```

redo log 将 undo log 作为数据，一并写入到文件中，包含 undo log 操作的 redo log，看起来是这样的：

```java
记录1: <trx1, Undo log insert <undo_insert …>>	//修改前的列的 undo log，在 redo log 中不是用来回滚，而是用来连接上数据行的 回滚指针
记录2: <trx1, insert …>							//修改
记录3: <trx2, Undo log insert <undo_update …>>
记录4: <trx2, update …>
记录5: <trx3, Undo log insert <undo_delete …>>
记录6: <trx3, delete …>
```

回滚的时候，redo log 并不会删除掉对应的 redo log 记录，即上面的 记录 2、4、6 都不会被删除

这样的话，在后续如果使用 redo log 进行宕机恢复，这些回滚的数据不是会被重新执行吗？



我们需要知道，回滚实际上也是数据修改，因此回滚的操作也会被记录到 redo log 中

这样一个**已经完成回滚**的 redo log，如下：

```java
记录1: <trx1, Undo log insert <undo_insert …>>
记录2: <trx1, insert A…>
记录3: <trx1, Undo log insert <undo_update …>>
记录4: <trx1, update B…>
记录5: <trx1, Undo log insert <undo_delete …>>
记录6: <trx1, delete C…>				
记录7: <trx1, insert C>				//回滚，将删除的 C 插入回去
记录8: <trx1, update B to old value>	//回滚，将修改后的 B 修改回旧值
记录9: <trx1, delete A>				//回滚，将插入的 A 删除
```

因此存在 记录 7、8、9，在执行 redo log 恢复数据的时候，对于已经回滚的事务，redo log 会先执行 记录 2、4、6，后续再执行 记录 7、8、9 重新进行一次回滚



> ### 为什么 undo log 需要落盘？

由于 MVCC 使用的 undo log 是为了记录同段时间活跃的事务的可见性的，一旦所有事务都完毕，那么 undo log 对于 MVCC 来说就没有作用了，因此 undo log 只需要存储在内存中，数据行的回滚指针在没有事务的时候都是指向空的，这样的话表示 undo log 落盘不是为了 MVCC，那么为什么 undo log 还需落盘呢？

```
undo log 的落盘实际上是为了宕机恢复时对未提交事务进行回滚
？？？这就有问题了，数据事务提交后才会找时间刷盘吗，为什么会在未提交前进行回滚？
未提交事务存在两个方面
1、这涉及到 redo log ，由于 redo log 文件大小是有限的，那么在 redo log 满了的时候，需要先将一些数据刷盘以此来空出空间，因此
```





## 3、bin log

具体看 https://www.cnblogs.com/xibuhaohao/p/10899586.html 



bin log 全称为 binary log，即二进制文件

这里先说下 **bin log 和 redo log 的区别**：

- bin log 记录的是事务的每一条产生修改的 sql，查询 sql 不会记录；redo log 存储的是数据修改的脏页

- bin log 是 mysql 层面的，所有存储引擎都有；redo log 是 InnoDB 独有的

- bin log 不支持循环写，同样是追加的方式，但是一个文件满了立马就开一个新的文件写，不会覆盖以前的数据；redo log 是文件循环写

bin log 跟 redo log 一样分为两部分：

- 一部分是内存中的缓存 bin，事务执行产生修改的 sql，就添加一条 bin log 到缓存中
- 一部分是磁盘中的 bin log 文件， commit 后会将缓存中的 bin log 刷盘



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

在第二阶段，执行 bin log 的刷盘，刷盘成功后，将 redo log 中该事务的状态设置为 commit，完成提交



> ### 两阶段提交解决的问题

看到这可能会疑惑：为什么一定要使用两阶段提交呢？如果不使用会发生什么事？

假如没有两阶段提交，而是 redo log 和 bin log 的刷盘没有任何关系，是分开的

1、master 执行完 update，记录 undo log，完成了 redo log 的刷盘，这时在 bin log 刷盘的时候，master 数据库宕机了

这就导致了 bin log 和 redo log 数据的不一致性，当 master 重启的时候，会将 redo log 的数据刷盘，而 slave 获取的是 bin log，而这个 bin log 没有记录新的数据，因此导致 master 和 slave 的数据不一致性

2、同理，如果是先对 bin log 刷盘，再对 redo log 刷盘，那么刷完 bin log 后，master 数据库宕机了，那么重启后使用 redo log 恢复，这时候 redo log 没有新数据，而 slave 获取的是 bin log，它有记录新数据，同样导致 master 和 slave 的数据不一致性



因此需要使用两阶段提交：

它会给每个事务分配一个事务 id（xid），redo log 和 bin log 在将该事务刷盘时，都会跟着该事务的事务 xid

( 这个事务 id 实际上就是上面讲的隐藏字段的事务 id，只不过我们上面没提分布式事务 id )



事务提交过程中，数据库宕机后重启存在以下情况：

- redo log 已经刷盘，prepare 状态，但是 bin log 没有刷盘成功，那么将处于 prepare 状态的事务删除
- redo log 已经刷盘，bin log 也刷盘，但是还没有记录此事务的 commit 状态，那么获取 bin log 的最后一个 xid 以及 redo log 中 prepare 状态的 xid，如果相同，那么意味着 bin log 也已经刷盘成功，所以才会有这个事务 id，将 redo log 的该事务修改为 commit，如果不同，那么就是上面的操作了