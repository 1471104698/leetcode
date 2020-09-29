# mysql 日志模块

## 1、redo log

> ### 为什么会存在 redo log

磁盘 IO 和 内存读写效率不是一个数量级的

在 mysql 中，如果每完成一次写操作都要写进磁盘：找到数据对应的位置，然后进行 更新/插入/删除，那么整个 IO 成本将会很高，同时效率也很低

为了解决这个问题，mysql 使用了另外一种更新机制：redo log，这种机制可以让更新的数据攒到一定程度再刷新回硬盘



redo log 涉及两个东西：redo log  buffer 和 redo log file，redo log  buffer 是内存中的，redo log file 是磁盘中的



每次执行写操作的时候，mysql 会将更新的数据先写入到 buffer pool（缓冲池），同时会生成一条 redo log 数据，写到 redo log buffer 中，等到事务 commit 时，将 redo log buffer 中的内容刷新到 redo log file 上，由于是顺序写入，按照 append() 的方式，所以比直接刷新到磁盘上的 随机写入效率高得多

buffer pool 中的数据会定期刷新到磁盘中（一般是满了的时候），这时候用户进行访问就直接返回内存中的数据



由于 buffer pool 的数据是存储在内存中的，所以可能会由于数据库宕机等原因导致 buffer pool 中的数据还没刷新到磁盘中就丢失了，这时候当数据库重启的时候，就会去读取 redo log file，进行数据的恢复



所以，redo log 就是因为 mysql 使用了 buffer pool 在内存中保存更新的数据，而没有及时刷新为硬盘，以此来提高效率，但可能会由于数据库宕机导致 buffer pool 中的所有数据都丢失了，而需要 redo log 来恢复数据



> ### redo log file 的结构

redo log file  的大小是固定的，从文件头开始写，写到末尾，然后又从头开始写，**循环写**

里面有两个指针，check point（检查点）表示下次数据写入磁盘的位置，write pos 表示下次数据写入 redo log 文件的位置

当 write pos == check point 时，表示文件已经满了，需要将数据写入到磁盘中，腾出空间

相当于一个是读指针，一个是写指针，当读指针和写指针重叠时，表示内存空间已满（类似数组实现队列一样）

![img](https://pic4.zhimg.com/80/v2-b2a4003fde5ed1a12cfb9522235319ff_720w.jpg)







## 2、undo log





Undo log中存储的是老版本数据，当一个事务需要读取记录行时，如果当前记录行的新版本数据对于该事务不可见，那么可以顺着undo log 链找到满足其可见性条件的记录行版本（MVCC 实现条件之一）



> ### undo log 分为两种

- insert undo log：事务对 insert 操作产生的 undo log，只有在事务回滚时才需要，当事务提交后可以立即丢弃
- update undo log：update 和 delete 都会产生的 undo log，InnoDB 默认将 delete 当作 update 的一种，所谓的 delete 只是修改了 删除标志位；该 undo log 不仅在回滚的时候需要，在 MVCC 中其他事务会需要，因此事务提交后不会立即删除，只有数据库现在持有的所有 ReadView  的事务 id 都跟 该 undo log 中记录的事务 id 搭不上边了，该 undo log 才会被删除





> ### 记录行 update 的过程模拟

undo log 中有一个字段` db_trx_id` 记录修改当前数据的事务 id，以及一个` db_roll_ptr`，指向 undo log 版本链



假设有一条记录行如下，字段有 Name 和 Honor，值分别为"curry"和"mvp"，最新修改这条记录的事务ID为1。

![img](https://img-blog.csdnimg.cn/20200701205716343.png)



（1）现在事务A（事务ID为2）对该记录的Honor做出了修改，将Honor改为"fmvp"：

​                ①事务A先对该行加排它锁
​                ②然后把该行数据拷贝到undo log中，作为旧版本
​                ③拷贝完毕后，修改该行的Honor为"fmvp"，并且修改DB_TRX_ID为2（事务A的ID）, 回滚指针指向拷贝到undo log的旧版本。
​                ④事务提交，释放排他锁

![img](https://img-blog.csdnimg.cn/20200701210046670.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L1dhdmVzX19f,size_16,color_FFFFFF,t_70)



（2） 接着事务B（事务ID为3）修改同一个记录行，将Name修改为"iguodala"：

​                ①事务B先对该行加排它锁
​                ②然后把该行数据拷贝到undo log中，作为旧版本
​                ③拷贝完毕后，修改该行Name为"iguodala"，并且修改DB_TRX_ID为3（事务B的ID）, 回滚指针指向拷贝到undo log最新的旧版本。
​                ④事务提交，释放排他锁

![img](https://img-blog.csdnimg.cn/2020070121022442.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L1dhdmVzX19f,size_16,color_FFFFFF,t_70)

从上面可以看出，不同事务或者相同事务的对同一记录行的修改，会使该记录行的undo log成为一条链表，undo log的链首就是最新的旧记录，链尾就是最早的旧记录。





> ### redo log 和 undo log 的过程模拟

```java
假设有A、B两个数据，值分别为1，2.
A.事务开始.
B.记录A=1到undo log.
C.修改A=3.
D.记录A=3到redo log.
E.记录B=2到undo log.
F.修改B=4.
G.记录B=4到redo log.
H.将undo log写入到 redo log 中
I.将redo log写入磁盘。
K.事务提交
```



# 