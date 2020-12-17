# mysql 日志模块



## 1、undo log



undo log 记录写操作前的旧数据，用于 MVCC 和 回滚

​	（这里的回滚不只是事务过程中的回滚，也涉及到宕机恢复时的回滚）

undo log 类型有两种：

- **insert undo log：**insert 产生

  ```java
  对应的 undo log 内容为  <delete from xxx>，该 undo log 在事务提交后可以直接删除
  								（这里的删除是说不放到回滚指针上，而不是不刷盘）
  因为 Mysql 会自动在每条数据上添加 3 个隐藏字段，其中有 DB_TRX_ID 和 DB_ROLL_PTR
  DB_TRX_ID：用来记录最近一次对该数据行进行修改的事务id，初始化插入时 DB_TRX_ID 为 Null
  DB_ROLL_PTR：记录回滚指针，如果没有旧版本，那么回滚指针为 Null
  
  当事务 100 插入 data A，那么这条数据的 DB_TRX_ID = 100
  如果别的事务快照读，对于它们的 ReadView 版本来说，它们发现 DB_TRX_ID 为 Null 并且 DB_ROLL_PTR = Null，自然就认为这条数据不可见了，不需要 undo log
  如果别的事务当前读，那么自然可以直接从 缓存/磁盘 中读取，也不需要这条 undo log
  
  因此，insert undo log 在事务提交完可以直接删除
  ```

  

- **update undo log：**update 或者 delete 产生

  ```java
  InnoDB 默认将 delete 当作 update，delete 操作 只是将 删除标志位 delete_bit 置为 1
  不会真正的去释放空间，即 执行 delete 操作后数据还是占用空间
  
  update undo log 是由在后续由 purge（清除）线程认为当前所有的 ReadView 版本都不需要使用到该 undo log 时进行清除
  ```

  

> #### undo log 添加过程

undo log 中有一个字段` db_trx_id` 记录修改当前数据的事务 id，以及一个` db_roll_ptr`，指向 undo log 版本链



**插入数据行：**此时由于是刚插入，因此回滚指针为 Null，而事务 ID 记录的是 插入该行数据的事务

*![image.png](https://pic.leetcode-cn.com/1602484884-pnQOIh-image.png)*

**事务 1 修改数据行：**当前版本中的事务 ID 是 1，同时回滚指针指向旧版本
 ![img](http://www.linuxidc.com/upload/2011_09/110904154240162.gif) 



**事务 2 修改数据行：**

 ![img](http://www.linuxidc.com/upload/2011_09/110904154240163.gif) 





## 2、redo log



磁盘 IO 和 内存读写效率不是一个数量级的

在 mysql 中，如果每完成一次写操作都要写进磁盘：先寻道，然后数据更新；那么整个 IO 成本将会很高，同时效率也很低

为了解决这个问题，mysql 使用了 buffer pool 和 redo log 机制



跟我们平时使用 redis 缓存来提高效率一样， mysql 也有自己的缓存，buffer pool 就是 mysql 的缓存

mysql 写数据不会直接刷盘，而是会在 buffer pool 中保存脏数据，然后将数据更新持久化到 redo log，防止宕机数据丢失

然后在后面选择一个空闲的时间将大批量 脏数据 都刷新会磁盘



**buffer pool：**

```java
mysql 读写的都是 buffer pool 中的数据，当读的时候，如果 buffer pool 中没有数据，那么从磁盘加载进来，当写的时候，也是直接写到 buffer pool 中。
buffer pool 是一个链表结构，以 数据页（16KB） 作为每个链表节点

为什么使用链表的结构？
因为 buffer pool 的大小也是有限的，不可能只加载而不淘汰，因此这个链表节点就是为了 淘汰某些数据页 而设计的
个人认为是使用 LRU 算法
淘汰出去的页如果存在脏数据，那么需要进行刷盘
```



**redo log:**

```java
redo log 是 InnoDB 引擎特有的
    
redo log 涉及两个部分：redo log buffer 和 redo log file
redo log buffer 是内存的一段缓存，redo log file 是磁盘持久化文件

每次事务执行过程中，涉及到写操作时
在执行写操作前，会先使用 undo log 记录旧数据，执行写操作后，使用 redo log 记录新数据
事务过程中 redo log 记录行就是存储在 redo log buffer 中，当事务进行 commit 的时候，就会将 redo log buffer 中的数据按照顺序写的方式刷盘到 redo log file 中，完成持久化
```



> #### redo log file 的结构

**redo log 使用循环写的方式**

redo log 文件大小是固定的，假设配置用于 redo log file 文件大小为 4G，那么可以配置成为 4 个文件，分别为 0 - 3 号文件，每个文件大小为 1G，那么当 3 号文件写满时，会重新回到 0 号文件开始写



redo log file 有两个指针：

- check point 表示 buffer pool 脏数据的起始位置，当宕机重启时就从这个位置开始恢复

- write pos 表示 新的 redo log 记录行 写入的位置

相当于一个是读指针，一个是写指针，当 check point == write pos 时，表示日志满了，那么需要将 check point 后的部分数据刷盘，然后留出空间继续写

<img src="https://pic.leetcode-cn.com/1606119295-YjpSrv-image.png" style="zoom:80%;" />







> #### redo log 和 undo log、回滚 的关系

[redo log、undo log 和 回滚 -- 博客园](https://www.cnblogs.com/wyy123/p/7880077.html)

[redo log 和 undo log -- 简书](https://www.jianshu.com/p/57c510f4ec28)



**undo log 也是需要刷盘的**：因为 redo log 在宕机重启时也会恢复那些未提交的事务，然后需要再通过 undo log 回滚这些未提交的事务

同时 undo log 必须在 对应写操作的 redo log 刷盘之前落盘，所以为了降低复杂性，redo log 将 undo log 当作写数据，将 undo log 也写入到磁盘中

因此包含 undo log 的 redo log 是这样的，undo log 和 redo log 一一对应

```java
     记录1: <trx1, Undo log insert <undo_insert …>>	//undo log 记录旧值
     记录2: <trx1, insert …>							//redo log 记录新值
     记录3: <trx2, Undo log insert <undo_update …>>	
     记录4: <trx2, update …>
     记录5: <trx3, Undo log insert <undo_delete …>>
     记录6: <trx3, delete …>
```



redo log 没有事务性，一旦某个事务 rollback 回滚了，redo log buffer 也不会删除掉该事务已经记录的写数据

那么 redo log 中就保存着这些写操作，后续如果宕机恢复的话，不会是将已经回滚的事务给重新执行吗？

**redo log 的解决方法：**

```
由于 回滚 实际上是利用 undo log 进行数据的写操作，所以回滚的操作也会被当作 redo log 记录下来
所以当宕机利用 redo log 恢复的时候，按照顺序会先执行 redo log，然后再执行 回滚操作的 redo log
```

因此，包含了回滚操作的 redo log 是这样的，先是 undo log 和 redo log 一一对应，然后再记录回滚的所有操作

这样在回滚时，就是先执行 redo log，然后再执行 回滚的 redo log

```java
     记录1: <trx1, Undo log insert <undo_insert …>>
     记录2: <trx1, insert A…>
     记录3: <trx2, Undo log insert <undo_update …>>
     记录4: <trx2, update B…>
     记录5: <trx3, Undo log insert <undo_delete …>>
     记录6: <trx3, delete C…>
     记录7: <trx3, insert C>
     记录8: <trx2, update B to old value>
     记录9: <trx1, delete A>
```



## 3、bin log

[redo log 和 bin log（一）](https://www.cnblogs.com/xibuhaohao/p/10899586.html )

[redo log 和 bin log（二）](https://www.cnblogs.com/a-phper/p/10006417.html)

[binlog 二进制文件解析](https://mp.weixin.qq.com/s/j987HNc74gRYHl6au24XSg)



bin log 全称为 binary log（二进制文件），数据都是二进制形式的

![img](https://mmbiz.qpic.cn/mmbiz_png/pmczaLVFxOIc3VpuzVUaCib7FWbZNo0Z2lglZbb9KkDTZZ5gkb9z5Fia9Bp8E3ClMOhVaaJe60VQp7ibeOMT17n6Q/640?wx_fmt=png&tp=webp&wxfrom=5&wx_lazy=1&wx_co=1)

binlog 只会记录 create、alter table、insert、delete、update 这些写操作，不会记录 select、show 读操作



binlog 的数据结构：

```java
binlog 以 event 作为数据结构记录 update、delete、insert 等操作的
每个 event 包括 Binlog event header、Post-Header 和 Body 三部分
Binlog event header 中存在一个 event type 字段，用来表示该 event 的类型
event type 的类型如下：
    0x02 QUERY_EVENT
    0x04 ROTATE_EVENT
    0x0f FORMAT_DESCRIPTION_EVENT
    0x10 XID_EVENT				//事务提交事件，记录事务的 xid
    0x13 TABLE_MAP_EVENT		//操作的 table 表名
    0x1d ROWS_QUERY_EVENT
    0x1e WRITE_ROWS_EVENTv2		//insert 事件
    0x1f UPDATE_ROWS_EVENTv2	//update 事件
    0x20 DELETE_ROWS_EVENTv2	//delete 事件
    0x21 GTID_EVENT
    0x22 ANONYMOUS_GTID_EVENT
    0x23 PREVIOUS_GTIDS_EVENT


其中，insert 和 delete 两种 event 只有 event type 不同，其他的数据都相同，即只要将 event type 进行修改，替换下事件类型，就可以从 insert 变成 delete

假设执行 sql 如下：
insert into test
values(1,2.222222222,now(),now(),'abc');

该 event 各部分数据如下：
1）event type：insert
2）Post-Header 忽略
3）Body：
01 00 00 00：首列是int，插入的值为1。

c2 79 bf 71 1c c7 01 40：第二列是double，上面已经看到sizeof(double)为8。

5a  b2 2e 5a ：第三列值为1521626714，代表写入的now()的时间戳。

99 9f 6b 21 4e：第四列，此处代表'2018-03-21 18:05:14'，是datetime类型字段。

03 61 62 63：第五列，首先03代表位数，之后便是abc字符串。
```

对于 binlog，简单讲就是会以 event 数据结构的形式记录所有的写操作，通过 event type 记录事件的类型，在 body 中记录事件对应的数据，比如  insert，它会先通过一个 event_)type = Table_map 的  event，然后再通过一个 event_type = Write_rows 的 event 来记录插入的数据

这里有点像 Java 的操作数栈，操作的对象和方法是分开存储的，在操作的时候先将对象压入到操作数栈，然后再压数据



图形化界面查看 binlog 数据内容

![image.png](https://pic.leetcode-cn.com/1607436385-iHzbfy-image.png)





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



**因此需要使用两阶段提交：**

Mysql 内部会将普通的事务当作一个 分布式事务 来处理，自动为每个事务分配一个唯一的 ID（XID），普通的事务 commit 被划分为 prepare 和 commit 两个阶段

事务提交过程中发生宕机，数据库重启时，会顺序扫描 redo log 进行数据恢复，如果扫描到 redo log 标签为 prepare，那么就拿该 redo log 的 XID 去 binlog 中看 bin log 是否已经落盘，如果没有，那么该事务回滚（利用 undo log 回滚），如果有，那么表示事务成功了，不需要回滚

