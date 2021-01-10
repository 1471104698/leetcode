# 简单总结二



## 1、mysql ACID 和 隔离级别

```java
1、ACID
A ：原子性，一个事务要么提交成功，要么失败回滚，通过 undo log 来保证
C ：一致性，这个后面讲，需要在应用层来保证
I ：隔离性，事务与事务之间的数据操作具有隔离性，必须事务1 操作的数据事务2 不能进行修改，通过 MVCC + 锁来保证
D ：持久性，一旦事务提交，那么表示永久生效，必须持久化到磁盘上，通过 redo log 来保证

对于 C 一致性，通常说的是 A 给 B 转账 50 元，那么 A 就必须扣 50 元，而 B 必须加 50 元，不能出现只扣不加的情况
这么理解实际上有点肤浅，一致性的含义是说从一个正确的状态转移到另一个正确的状态。
比如 A 有 0 元，此时 A 如果给 B 转账 50 元，那么就会变成 -50 元，这个在数据库中是可以存储的，但是在应用层面它实际上是一个错误的状态。
数据库的作者也说了，事务并不能保证 C，这个 C 只能由开发者在应用层面保证，即我们在转账前需要进行判断，如果钱不够那么就不能进行转账，而不是强行进行转账，或者说不能我们只执行 A 扣减的 sql 而不执行 B 增加的 sql，使得状态发生错误。


2、事务问题
1）数据修改丢失：A 更改了数据 x， B 也同时更改了数据 x，导致 A 修改的数据丢失
2）脏读：事务 A 读取到事务 B 未提交的脏数据
3）不可重复读：事务 A 第一次相同 sql 读的数据跟第二次读的数据不一致，即相同数据的数值发生变化
4）幻读：事务 A 第一次读的时候只有 3 条数据，第二次相同 sql 读的时候变成了 2 条或者 4 条数据，即数据量发生变化

3、隔离级别
1）READ UNCOMMITED：读未提交，可以读取别的事务未提交的数据，即读到的是脏数据，会触发脏读、不可重复读、幻读
2）READ COMMITED：读已提交，可以读取别的事务已经提交的数据，会触发不可重复读、幻读
3）REPEATABLE RAED：可重复读，不会读取到别的事务已经提交的数据，在特定情况下可以避免不可重复读、幻读，但是还是会发生
4）Serialization：串行化，一次只能执行一个事务，不允许并发事务
```



## 2、mysql  MVCC

```java
1、MVCC
MVCC 意为 多版本并发控制，它是一种设计理念，通过 ReadView 实现，以此来控制 RC 和 RR 隔离级别下的读操作


2、ReadView
在讲解 ReadView 如何起作用之前，需要先了解 InnoDB 在每个数据行后面添加的三个隐藏字段：
1）row_id：当没有设置主键同时不存在非空唯一索引时，会使用该列作为聚簇索引
2）trx_id：记录插入或者最近一次修改该数据行的事务 id
3）roll_ptr：回滚指针，记录 undo log 链，新建的数据行的 roll_ptr 为 null


ReadView 称作快照，它记录的是生成的那一刻的各个事务的情况
ReadView 包含以下字段：
1）low_limit_id：ReadView 创建时已分配的最大事务 id + 1，即下一个将要分配的事务 id
2）up_limit_id：ReadView 创建时未提交的事务中最小的事务 id
3）trx_ids：未提交事务的事务 id 列表（不包括当前事务的事务 id）

ReadView 实现事务之间的可见性：
数据行最近一次修改的事务 id 为 trx_id，ReadView 中的三个字段为 low_limit_id、up_limit_id、trx_ids
1）trx_id < up_limit_id：
	表示当前数据行在当前 ReadView 创建之前已经提交了的，那么对于当前事务来说是可见的
	
2）trx_id >= low_limit_id：
	表示当前数据是在当前 ReadView 创建之后提交了的，因为 low_limit_id 记录的是当前 ReadView 创建时下一个将要分配的事务 id，而如果数据行的 trx_id >= low_limit_id，那么意味着该数据行是在生成 ReadView 后才创建的新事务修改的，对于当前事务来说不可见，那么根据 roll_ptr 指针找到上一个修改的版本，继续进行比较，直到找到一个可见的版本为止
	
3）up_limit_id <= trx_id < low_limit_id：
	那么有 3 种情况：
		情况一：trx_idx == 当前事务的 id，表示是当前事务进行的修改，那么对当前事务来说肯定可见
		情况二：在 trx_ids 中使用二分查找，如果 trx_id 不在 trxs_id 中，表示创建 ReadView 时该事务已经提交，那么对于当				前事务可见
		情况三：在 trx_ids 中使用二分查找，如果 trx_id 在 trxs_id 中，表示创建 ReadView 时该事务还没有提交，是在创建 				ReadView 后才提交的，那么对于当前事务来说不可见，那么需要根据 roll_ptr 找到上一个版本 undo log



3、RC 和 RR 的可见性区别
RC 每一次读操作都会创建一个新的 ReadView，因此它每次都能读取最新的数据，所以才会存在 不可重复读 和 幻读的问题
RR 只有第一次读的时候才会创建 ReadView，后面的 select 都是使用这个 ReadView，因此它每次读取的数据都停留在第一次查询

读操作分为 快照读 和 当前读
1）对于 insert、update、delete、select ... for update、select ... lock in share mode 这种都属于当前读，这些操作执行的时候都会创建一个新的 ReadView，因此他们操作的都是最新的数据
2）对于 普通的 select 属于快照读，它会沿用第一次普通 select 创建的 ReadView，如果没有，那么自己创建一个
对于 RC 级别，它相当于所有的操作都相当于是当前读（只是类型像，不过普通 select 还是叫做快照读，不过它具有当前读的性质）
对于 RR 级别，当前读会创建一个 ReadView，而这个 ReadView 不会替换掉第一次普通 select 生成的 ReadView，即具有一次性


4、MVCC 解决了什么问题？
MVCC 解决了 RR 级别下 快照读 的不可重复读和幻读的问题，
但是对于 RR 级别的 当前读，它仍然会创建最新的 ReadView，所以单凭 MVCC 是无法解决的，因此出现了锁（下面讲）
```





## 3、mysql 锁

```java
InnoDB 引擎存在两种类型的锁：表锁 和 行锁


1、表锁：

1）表写锁 和 表读锁：通过 LOCK TABLE … WRITE 和 LOCK TABLE … READ 两个 sql 语句来申请 表读写锁
一旦事务 A 申请了表写锁，那么事务 A 对表中的任意数据都可以进行修改，不过一般情况下是不会使用这个表锁的，它需要显式指定

2）意向排他锁 和 意向共享锁：它不是用来锁表数据的，而是用来告知其他事务当前表是否存在事务操作
比如 事务 A 对 表 table 进行 update 操作，那么在操作前会先给表加上 意向排他锁，表示 table 表有事务正在执行写操作，这样别的事务就不能对 table 加 ”表写锁“

为什么需要这个意向排他锁 和 意向共享锁？
    如果没有的话，事务 A 要给 table 加表写锁，由于表写锁能够任意修改表中的任何数据，所以它需要保证没有别的事务在对该表执行当前读操作，但是它没有任何依据来判断该表是否存在，所以它需要进行扫描全表，对每行数据都进行一次判断，效率太低了
    而存在这个意向锁，那么在加表锁之前，只需要判断该表是否已经被加上了意向锁，如果加上了，那么表示有事务在执行当前读操作，那么就无法申请 表读/写锁
    
需要注意的是，意向锁在执行 sql 的时候是会默认加上的，不需要程序员显式指定
当执行 写操作 或者 select...for update 时会给数据行加写锁，同时会给表加上意向排他锁
当执行 select... lock in share mode 时会给数据行加读锁，同时会给表加上意向共享锁


2、行锁
InnoDB 相比 Mysiam 多了 行锁，具有更小的锁粒度

1）记录锁：如果是写操作 或者 select...for update，那么会对数据行加写锁；如果是 select...lock in share mode，那么会对数据行加读锁


2）间隙锁：当对某个数据行加锁时，会将该数据行的相邻的两个间隙进行加锁，使得无法插入数据
    比如存在表 test 字段为 (id, v1)
    该表有数据：
        id	1	3	4	6
        v1	3	4	6	6
    对于 v1 列来说，它存在以下间隙区间：
        (-∞, 3], (3, 4], (4, 6], (6, 6], (6, +∞)
    间隙锁遵循的是 “左开右闭” 原则
    间隙是按照已有节点来看的，其他事务能否插入实际上也是看是否在加锁间隙的两个节点之间
    比如 (3, 4] 这个间隙，实际上就是 (id = 1, v1 = 3) 和 (id = 3, v1 = 4) 这两个节点之间的间隙，如果这个间隙被加锁了，那么我们如果 insert 的数据在这个间隙中，那么是 inert 失败的
    比如 insert (id = 3, vi = 4)
        如果插入成功，节点间的关系变成 (id = 1, v1 = 3)(id = 3, vi = 4)(id = 3, v1 = 4)
        这意味着插入的这个节点是在间隙中的，那么插入失败
    比如 insert (id = 4, vi = 4)
        如果插入成功，节点间的关系变成 (id = 1, v1 = 3)(id = 3, v1 = 4)(id = 4, vi = 4)
        如果插入成功，那么节点不在加锁的间隙中，所以是插入成功的

    因此，如果是对 v1 = 4 加锁，那么它加锁的间隙应该是 (3, 4], (4, 6]，即将它相邻两边的节点进行加锁（因为在相邻两边中插入的数据很大几率是 v1 = 4），这样的话，在这些间隙中插入的值都会失败，从而解决幻读问题
	同时，如果锁的是 (6, +∞) 这种间隙索，那么如果插入后是在 (id = 6, v1 = 6) 节点后面的，那么全部会插入失败

3）Next-Key 锁：该锁是 记录锁 + 间隙锁 的组合
```



## 4、mysql RC 和 RR 各种索引的加锁情况

```java
RC 级别没有解决幻读，所以它本身不会涉及到间隙锁，它只会加记录锁
RR 级别快照读是通过 MVCC 解决的，当前读是通过 Next-key 锁解决的

在讲加锁情况之前，我们只需要先理清楚 InnoDB 中的索引种类：聚簇索引、非聚簇索引（唯一索引、普通索引）、无索引（走聚簇索引树）

1、RC

执行 select * from t1 where id = 10 for update;

1）聚簇索引
当 id 是聚簇索引时，那么会对聚簇索引中 id = 10 的节点加记录锁

2）唯一索引
当 id 是唯一索引时，那么会对非聚簇索引树上的 id = 10 节点加锁，同时会将 id = 10 对应的聚簇索引树上的节点加记录锁

3）普通索引
当 id 是普通索引时，那么会对非聚簇索引树上所有 id = 10 的节点加锁，同时将这些节点对应的聚簇索引树上的节点加记录锁

4）无索引
当 id 没有索引时，那么走的是聚簇索引树的查询，并且由于对于聚簇索引树来说，id 是无序的，所以需要进行全表扫描，它会先对所有的节点进行加记录锁，然后全表扫描，再将不满足条件的节点解锁（即需要两次扫描）
	由于它不需要防止幻读，所以只需要锁住当前满足条件的数据行即可

2、RR

执行 select * from t1 where id = 10 for update;

1）聚簇索引
当 id 是聚簇索引时，直接加记录锁，这里不需要加间隙锁，因为 id 值是唯一的，不可能会出现幻读，只需要对该数据进行加锁，避免 update 和 delete 即可

2）唯一索引
当 id 是唯一索引时，对聚簇索引树和非聚簇索引树上的节点加记录锁，同样不需要加间隙锁，因为值是唯一的，不可能出现幻读
	（对聚簇索引树上的节点加锁是为了避免别的事务直接扫过非聚簇索引树而对聚簇索引树上的节点进行 update 或者 delete）

3）普通索引
当 id 是普通索引时，由于值是能重复的，所以可能出现幻读，所以需要对非聚簇索引树上的节点添加记录锁和间隙锁，
对聚簇索引树上的节点只需要添加记录锁，不需要添加间隙锁，因为没必要，
	①：因为聚簇索引树上 id 不是有序的，这个间隙不会起作用
	②：聚簇索引树 和 非聚簇索引树 都需要进行维护，只要非聚簇索引树加了间隙锁，插入失败，最后也是失败的（个人认为按照这种理论，InnoDB 在 insert into 数据时，应该是维护非聚簇索引，然后再维护聚簇索引，当非聚簇索引更新失败时，那么就没必要去更新聚簇索引了）
	
4）无索引
当 id 没有索引时，走的是聚簇索引树的全表扫描，由于聚簇索引树上的 id 是无序的，并且 id 是存在重复值的，那么有以下两种情况：
	①：任何一个数据行都可能修改 id = 10
    ②：由于是无序的，所以在任何一个间隙都可以插入 id = 10 的数据
	因此就需要对所有的数据行 加记录锁，对所有的间隙加上间隙锁，防止幻读
这种情况就相当于 “表锁”


RR 级别真正解决 不可重复读和幻读 了吗？
    RR 级别虽然通过 MVCC 解决当前读的不可重复读和幻读的问题，通过 Next-key 解决快照读的不可重复读和幻读的问题
    但是它能够解决的是两次读的类型都是相同的情况，即第一、二次读操作都是当前读或者快照读的情况
    如果第一次是快照读，然后其他事务插入数据，然后第二次执行的是当前读，那么还是会存在不可重复读 和 幻读的情况
    即 RR 级别实际上并没有完成解决 不可重复读和幻读的情况。
    要想真正解决这些问题，只能使用 串行化 来禁止并发事务了
```





## 5、mysql log 日志模块

```java
mysql 中存在 3 种日志模块：undo log、redo log、bin log
其中 undo log 和 redo log 是 InnoDB 特有的，bin log 是 Server 层的，即所有存储引擎都有的



1、undo log
undo log 用于 事务回滚 和 帮助配合 ReadView 实现 MVCC
当执行一条写操作前，会生成一条 undo log 记录修改前的数据行，即旧值

undo log 分为两种类型：
1) insert undo log：
当事务 inseet 一条新的数据时，会生成对应的 undo log，这条 undo log 中隐藏字段 trx_id 和 roll_ptr 都为 null
并且由于当前事务才插入的，所以对于其他事务来说，一般是不可见的，同时也不存在旧值问题，因此 MVCC 不需要使用到该 undo log，所以该 undo log 在事务提交后会直接删除。因为数据本身就已经是相当于 insert undo log 了，不需要再连接到 roll_ptr 上

2）update undo log：
InnoDB 将 delete 定义为 update 类型，这是因为 mysql 底层对于 delete 并不会直接物理删除，而是将它的删除标识符置 1，标识为已删除，但是实际上还是存在于磁盘文件中的，因此 update 和 delete 都共用一种 undo log。
该 undo log 在事务提交后需要保存，因为 MVCC 需要使用到该 undo log。
这种 undo log 当然不会一直保存，当后台线程检测到没有事务需要引用到该版本的 undo log 时，会进行清理。

undo log 版本链：
每当存在一个事务修改数据，那么生成的 undo log 会连接到新数据行上的 roll_ptr 上，形成 undo log 版本链，用于 MVCC。
同时将新数据的 trx_id 设置为修改该数据的事务 id




2、redo log
redo log 叫做重写日志
当执行一条写操作后，会生成一条 redo log 记录该写操作 修改/添加 的值

为什么需要 redo log？
    数据库内部维护了一个缓存 buffer，用来存储数据，在没有 redo log 的情况下，事务提交后需要将 buffer 中的脏页刷盘，避免宕机后已提交的数据丢失。这么做有一个问题：每次事务提交都需要进行刷盘，而脏数据所处的数据页并非是连续的，这样的话就相当于是随机 IO 了，而在高并发的情况下，频繁的随机 IO 很耗费性能。
    因此设计出了 redo log，redo log 实际上包含两个部分：内存中的 redo log buffer 和 磁盘上的 redo log file。
    在事务过程中，使用 redo log 来记录每一条写操作，这些 redo log 记录在 redo log buffer 中，然后在事务提交的时候不是将数据库 buffer 中的脏数据刷盘，而是将 redo log buffer 中的数据刷盘，由于是日志的形式，所以它不需要去在意哪条 log 的位置，它是以 append 的方式追加数据的，这种做法是 顺序 IO，大大减少了随机 IO 的寻道时间，效率很高（IO 拖慢性能主要就是在于寻道，而磁盘读写的速度实际上是很快的）。
    在记录了 redo log 后，一旦出现宕机，那么数据库 buffer 中的脏数据就没了，但是我们在 redo log 中记录了所有写操作，因此不需要担心已提交事务数据的丢失，只需要跑一遍 redo log 就可以恢复数据了。

redo log file 的结构：
    redo log file 的大小是有限的，它固定是几个文件，以 append 的形式进行写，它有两个指针：check_point 和 write_pos
    write_pos 是每次 append 数据的位置，check_point 是脏数据的起点，即刷盘的位置，当 write_pos == check_point 时，表示 redo log file 写满了，需要将一部分数据进行刷盘，从 check_point 位置开始推进。
    

redo log、undo log 的关系：
对于宕机恢复的问题，有两种策略：
	1）只重做已提交的事务
	2）重做所有事务（已提交和未提交），然后使用 undo log 回滚未提交的事务
mysql 采用的是 2）策略，因此需要将 undo log 进行刷盘，而为了方便实现，mysql 将 undo log 作为 redo log 数据的一种，写入到 redo log 中，然后在事务提交时跟 redo log 一起刷盘。
（这也就是为什么在两阶段提交中发现 redo log 未提交，需要回滚事务，原因就是 mysql 重做了所有的 redo log，然后再进行回滚）

redo log 和 回滚 的关系：
redo log 本身并不能回滚，所以执行回滚操作时它不会将已经记录的 redo log 给删除掉，
而是使用另外一种方法：将回滚也当作写操作的一种，因此回滚执行的写操作也会记录到 redo log 中，
因此在数据恢复的时候，redo log 会执行回滚前的操作，然后再执行回滚时的操作去删除掉前面的操作





3、bin log

bin log 是 Server 层的日志，是一个二进制文件（binary log）

bin log 的作用：bin log 主要用来备份、主从数据同步、另外可以用来实现 缓存和数据库的一致性（基于 binlog 订阅的异步更新）
    1）bin log 跟 redo log 不一样，它没有大小限制，一旦写满了一个文件，那么另外一个文件进行写，这是如此，bin log 可以说是记录了所有的数据，一旦数据库被不小心删除了，那么可以通过 bin log 来进行恢复
    2）bin log 用于主从节点同步数据，由于集群中 master 用于写，slave 用于读，所以最新数据的在 master 中的，那么 slave 就需要同步 master 的数据，master 会向 slave 发送 bin log，slave 通过执行 bin log 来同步数据。因此可以说 redo log 影响 master 的数据，bin log 影响 slave 的数据
    3）缓存和数据库的一致性：基于 bin log 订阅的异步更新算是最优的实现方案

bin log 会记录 create、alter table、insert、update、delete 等所有的写操作，但不会去记录 select、show 等读操作
它内部是以 event 事件的形式 来记录每条 sql 的，event 包括 header 和 body 两部分（其实还有第三部分，不过不太重要）

header 内部有一个 event_type，用来记录事件的类型，event_type 的所有类型如下：
    0x02 QUERY_EVENT			//开启事务 begin 使用的是这个类型，当然还有其他的事件也是这个类型，这里不讲
    0x04 ROTATE_EVENT
    0x0f FORMAT_DESCRIPTION_EVENT
    0x10 XID_EVENT				//事务提交，记录事务的 xid
    0x13 TABLE_MAP_EVENT		//insert 等写操作对应的 table 表名
    0x1d ROWS_QUERY_EVENT
    0x1e WRITE_ROWS_EVENTv2		//insert 事件
    0x1f UPDATE_ROWS_EVENTv2	//update 事件
    0x20 DELETE_ROWS_EVENTv2	//delete 事件
    0x21 GTID_EVENT
    0x22 ANONYMOUS_GTID_EVENT
    0x23 PREVIOUS_GTIDS_EVENT
根据 event_type 来标记不同的事件事件，而比如 insert，它插入的数据会存储在 body 字段中
比如执行如下sql：
	insert into te(id, salary, create, update, name) values (1,2.222222222,now(),now(),'abc');
那么在 bin log 记录中必定会存在以下四个事件：（具体看下图）
Query：begin，表示开启事务
Table_map：记录 sql 操作的 table 表名，这里对应上面的 sql 就是 te
Write_rows：对应 insert 事件，在该 event 的 body 中存储了插入的数据
Xid：commit，表示事务提交，同时会记录 事务id -- xid



4、2PC 两阶段提交
Mysql 5.7 是默认关闭 bin log 日志的，因此不需要使用 2PC
当我们开启 bin log 时，由于同时存在 redo log 和 bin log，所以它会使用 2PC

什么是 2PC？
第一阶段：当事务提交时，会先将 redo log buffer 中的数据刷盘，然后将该事务标记为 prepare
第二阶段：将 bin log buffer 中的数据刷盘，刷盘完成后，将 redo log 中 prepare 修改为 commit

为什么需要 2PC？
我们上面说了，redo log 影响 master 的数据，bin log 影响 slave 的数据。
如果将 redo log 刷盘后宕机了，bin log 还没有刷盘，那么会 bin log 会缺失数据，导致 slave 跟 master 数据不一致（反过来也是一样的，会导致 redo log 缺失数据）
所以我们需要一种机制，来保证 redo log 和 bin log 的数据同步，以此来保证主从节点数据一致，2PC 就是基于这个目的产生的

2PC 宕机恢复过程：
mysql 会将 redo log 和 bin log 同时存在时的事务当作分布式事务来处理，它会为每个事务分配一个分布式事务 id -- xid。
当 redo log 和 bin log 提交时，会在后面记录该事务的 xid。
宕机恢复后，扫描 redo log 恢复数据，然后 redo log 中如果存在 prepare 状态，那么获取该事务的 xid 以及 bin log 中最后一个事务的 xid（因此宕机了，那么最后一个事务必定是宕机前的事务，当然可能宕机的时候 bin log 没有刷盘完成，所以这里的 xid 是上一个已经成功提交的事务 id）
如果 redo log 中的 xid 和 bin log 的 xid 一致，表示事务提交成功，只是还没来得及将 prepare 修改为 commit。
如果 不一致，表示 bin log 没有刷盘完成，那么为了保证数据一致性，使用 undo log 将未提交的数据进行回滚


按照上面的说法，undo log 按照作用分为两种：
    1）记录在数据行隐藏字段 roll_ptr 中的 undo log 版本链，用于 MVCC，一旦没有 ReadView 需要引用它时会删除
    2）持久化到磁盘上的 undo log 日志，用于 redo log 的回滚
```



bin log 日志数据格式：

![image.png](https://pic.leetcode-cn.com/1607436385-iHzbfy-image.png)







## 6、mysql 三大范式 / InnoDB 和 Mysiam 的区别

```java
1、数据库三大范式：

1）第一范式：一个列必须是原子不可分割的。
	比如不能存在一个 info 列，同时记录用户的 age、addr，比如 "13岁，广东省汕头市"，它实际可以分割为 age 和 addr 两个字段，可以当作是我们需要查询的时候，最小的匹配列，因为我们有时候需要按照 age 查询，有时候需要按照 addr 查询，必须将它们分离
	
2）第二范式：对于联合索引来说的，非主键列不能对部分主键存在依赖，而必须是对整个主键存在依赖，否则会存在大量的冗余数据。
	比如订单号和产品号，用户的一个订单能够包含多个产品，每个产品的产品号是不同的，而多个用户可以下单同一个产品，所以只有订单号 + 产品号 才可以唯一确定不同用户订购的同一产品，即 订单号 + 产品号 作为一个联合主键。而一个订单存在 下单日期、订单金额等信息，如果将这些信息 糅合在该联合主键表中，那么将会出现数据冗余
	例子如下：
		oid		pid		pnum	o_money		o_create
		1		101		2		100			2020.12.9
		1		102		1		100			2020.12.9
		1		103		3		100			2020.12.9
		2		105		6		50			2020.11.25
	可以看出，o_money 和 o_create 只依赖于 订单号，只要订单号出现多少次，这些相同的数据就出现多少次，数据冗余，不直观
	因此，需要将 oid 和 o_money、o_create 再抽取出来组成一张表
	table①：								table②：
		oid		pid		pnum				oid		o_money		o_create
		1		101		2					1		100			2020.12.9
		1		102		1					2		50			2020.11.25
		1		103		3
		2		105		6

3）第三范式：非主键不能对主键存在传递依赖。
	这个所谓的传递依赖就是说间接依赖，比如学生表中存在字段：学号、学生姓名、所在学院、学院电话
	这里面的学院电话就属于传递依赖（间接依赖）的关系，通过 学号 -> 所在学院 -> 学院电话，但实际上学院电话跟学生并没有什么直接关系，所在学院跟学生才有直接关系，这个学院电话是因为学生所在学院而附带出来的，多个学生都在相同的学院，那么后面的学院电话字段会发生冗余，当学院电话需要修改的时候，也需要修改大量的数据行
        sid		dpid	dp_phone
        1		1		110
        2		1		110
        3		1		110
        4		1		110
        5		2		119
	因此，需要将 所在学院 和 学院电话 抽取出来作为一张表，减少字段的冗余，同时也方便修改
	table①：						table②：
		sid		dpid				dpid	dp_phone
        1		1					1		110
        2		1					2		110
        3		1
        4		1
        5		2


2、InnoDB 和 Mysiam 的区别
    1）Mysiam 不支持事务，sql 执行完就是成功了的，不存在回滚操作；InnoDB 支持事务，所以 InnoDB 存在 undo log（redo log 不是为事务诞生的，而是为了刷盘效率）
    2）Mysiam 只存在表锁，锁的粒度较大，写不支持并发，一张表一次只能有一个事务（串行化）；InnoDB 存在 表锁 和 行锁，默认情况下都是加行锁，锁的粒度小，支持并发事务（这也是为什么 Mysiam 适合多读，InnoDB 适合多写的原因之一）
    3）Mysiam 内部会维护一个 count 字段，可以 O(1) 获取整张表的数据；InnoDB 没有维护什么字段，只能通过全部扫描统计数据
    4）Mysiam 因为索引树的类型只有非聚簇索引树，不强制要求存在主键；InnoDB 由于存在聚簇索引和非聚簇索引，所以强制要求存在一个主键作为聚簇索引树，如果没有设置主键，那么使用第一列非空的唯一索引作为主键，形成聚簇索引，如果没有唯一索引，那么使用隐藏字段 row_id 作为主键，形成聚簇索引
    5）Mysiam 索引树使用的是 B+ 树（当然还存在 hash 索引），而由于它只存在非聚簇索引，所以叶子节点不会存储数据，它将索引和表数据分为两个不同的文件，叶子节点存储的是数据在数据文件中的地址，可以通过该地址直接定位到数据；InnoDB 聚簇索引和表数据是整合在一起的，聚簇索引树的叶子节点就是表数据行，非聚簇索引树的叶子节点是所在数据行的主键，所以在不满足覆盖索引的情况下，需要进行回表扫描，即两次扫描，查询的效率比起 Mysiam 要低
    6）Mysiam 支持全文索引(full text)；InnoDB 不支持全文索引（但是后面的版本是支持的）
    7）Mysiam 不支持外键；InnoDB 支持外键，所以带有外键的 InnoDB 表无法转换为 Mysiam 表
```



## 7、mysql explain 解析

```java
explain 用来获取 select 的执行计划，分析该 select sql 效率如何
explain 字段几个重要字段包括 type、key、key_len、extra

type：此次查询的情况，比如 all、index、ref
key：此次查询使用了哪些索引
key_len：主要是用来分析联合索引使用的情况，通过长度可以判断使用了联合索引的多少个字段
extra：查询的额外信息，判断是否需要回表、是否需要对结果集排序、是否需要创建一张临时表


1、type

1）All：效率最低，没有使用到索引，需要聚簇索引的全表扫描

2）index：
	①、使用到了索引
	②、不能回表（这里的回表讲的是 select 字段 和 where 条件，如果其中一个需要回表都不行），这限制了 不能存在 where 条件或者 where 条件只能是 id，即不能通过 where 条件过滤数据，只能全表扫描
		（一旦能够通过 where 条件过滤数据，那么就是 range 和 ref 了，即 index 是 range 和 ref 的前提）
	比如：
		explain SELECT id FROM `user` force index(idx_b_c_d);	
			//满足覆盖索引，不存在 where 条件，不需要回表
		explain SELECT d FROM `user` force index(idx_b_c_d) where id > 2;
			//满足覆盖索引，where 条件中的 id 是索引树的叶子节点，不能用来过滤数据，不需要回表
			
3）range：使用到了索引，并且存在范围查询的 where 条件 同时 该 where 条件的字段是索引树上的（不需要满足覆盖索引）
	比如：
		explain SELECT d FROM `user` force index(idx_b_c_d) where id > 2 and b > '2';
			//存在范围查询的 where 条件 同时 该 where 条件的字段在索引树上
		explain SELECT * FROM `user` force index(idx_b_c_d) where id > 2 and b > '2';
			//select * 存在回表，但是可以通过 where 条件 b > '2' 在索引树上过滤掉一些数据，减少了回表的次数
			
4）ref：使用到了索引，并且存在等值（单值）查询的 where 条件 同时 该 where 条件的字段是索引树上的（不需要满足覆盖索引）
	比如 
		explain SELECT * FROM `user` force index(idx_b_c_d) where id > 2 and b = '2';
			//需要回表，但是可以先在索引树上的 b = '2' 过滤掉一些数据，减少回表次数
			
5）const：基于主键/唯一索引的等值（单值）查询，最多只返回一条数据
	比如
		explain SELECT * FROM `user` force index(idx_unique_k) where k = 1;
			//唯一索引 k，单值查询 k = 1，最多只返回一条数据
			

2、extra

1）Using index：使用了索引，不涉及回表查询（这里的回表不只是关于 select 字段的覆盖索引，同时也包括 where 条件的回表）
	比如：
		explain SELECT c FROM `user` force index(idx_b_c_d);
			//使用了索引，满足覆盖索引，需要非聚簇索引树的全表扫描，不需要回表
		explain SELECT c FROM `user` force index(idx_b_c_d) where b > '2';
			//使用了索引，满足覆盖索引，可以使用 b > '2' 进行过滤，不需要回表
    反例：
    	explain SELECT * FROM `user` force index(idx_b_c_d) where b = '2';
    		//不满足 Using index，虽然使用了索引，但是 select * 需要回表
		explain SELECT c FROM `user` force index(idx_b_c_d) where a > '2' and b = '2';
			//不满足 Using index，虽然使用了索引，但是 a > '2' 需要回表
	可以说 Using index 是 type 中 index + 部分 range + 部分 ref
    
2）Using where：当查询出数据后，需要在 Server 层根据 where 条件过滤数据，如果没有使用索引，那么将全表扫描得到的数据在 Server 层使用 where 进行过滤；如果使用了索引，如果存在 where 条件不适用于索引树，那么 InnoDB 会进行回表，将回表后的数据发送给 Server 层，然后 Server 层根据 where 条件对回表的数据进行过滤
	比如：
		explain SELECT * FROM `user` where k > 1;
			//没有使用索引，Server 层调用存储引擎中从聚簇索引中一条一条获取数据，然后在 Server 层利用 k > 1 进行过滤
		explain SELECT c FROM `user` force index(idx_b_c_d) where a > '2' and b = '2';
			//type = ref，由于需要回表，所以不存在 Using index
			//存储引擎在索引树上查询，由于需要回表，所以它会回表查询，将会被后的一条一条完整数据返回给 Server 层
			//Server 层将回表的数据根据 a > '2' 进行过滤，这里就是 Using where

3）Using index condition：需要回表，但是会利用索引下推来减少回表次数
	比如：
		explain SELECT * FROM `user` force index(idx_b_c_d) where id > 2 and b = '2';
			//存储引擎查询出数据，需要回表，但是在回表前会先根据 id > 2 这个条件判断，如果不满足那么就不进行回表，这里叫做索引下推，减少回表次数
		explain SELECT * FROM `user` force index(idx_b_c_d) where b = '2' and d > '2';
			//只能使用到 b = '2' 这个查询条件，但查询出数据后，会先根据 d > '2' 这个条件判断，如果不满足就不进行回表

4）Using filesort：根据顺序查询出来的结果集进行排序，即查询出来的结果集不满足我们 order by 的要求

5）Using temporary：将查询出来的结果集（表），通过一些操作，经过这些操作后将新的结果集放到临时表中
					比如我们的去重操作，查询出来的结果表为 t1，我们需要扫描 t1，将不重复的结果存储到临时表 t2 中
	比如：
		explain SELECT * FROM `user` WHERE b = 1 union SELECT * from `user` where b = 12;
			//union 关联两张表的数据，由于需要去重，所以需要创建一张临时表，然后再遍历这张表去重，出现 Using temporary
		explain SELECT * FROM `user` WHERE b = 1 union all SELECT * from `user` where b = 12;
			//union all 关联两张表的数据，不需要去重，所以直接将两个表的数据存储到一个结果集中返回即可
```





## 8、mysql 为什么使用 B+ 树？

```java
1、Hash
mysql 中支持 Hash 索引，它是使用数组 + 链表的方式，它需要通过字段值来计算 hash 值
hash 节点的数据结构如下：
    class Node{
        //hash 值
        int hash;
        //指向数据行指针，可以直接获取对应的数据
        Data data;
        //下一个节点
        Node next;
    }
hash 索引可以 O(1) 查找数据行，但是它有 4 个缺点：
1）hash 索引不满足最左匹配原则，因为是需要计算 hash 值，所以查询的时候必须包含联合索引的全部字段，如果缺少字段那么得到的 hash 值也不同，无法进行定位
2）hash 索引无法范围查询，全部是 hash 值，相当于是乱序的，where a > 1 and b > 2 这种范围查询根本不支持
3）hash 索引必定不能避免回表扫描，hash 索引只能用于非聚簇索引，而它的节点中并没有保存数据，只有指向完整数据的指针，所以需要回表得到完成数据
4）hash 索引在 hash 冲突严重情况下效率低下

2、二叉搜索树
不稳定，特定情况下会退化成链表

3、AVL 树 和 红黑树
AVL 树 和 红黑树 如果作为索引实际上差不多，问题在于当数据量过大时，它们的高度也会过大，每一层都需要进行一次 磁盘 IO，效率低

4、B 树
B 树称作平衡多叉树，每个节点都存储真实的数据行
mysql 将叶子节点设置为 数据库的一页 16KB，B 树可以利用到局部性原理，一次磁盘 IO 查询出一页的数据，然后进行二分查找定位数据
由于 B 树是多叉树，所以在数据量大的时候它的层数也不高，磁盘 IO 的次数小
最好的查询时间复杂度为 O（1），最坏的查询事件复杂度为 O（logn）

不过 B 树不适用于范围查询，它的范围查询需要递归实现，效率低，而 mysql 需要频繁的范围查询，所以 B 树不合适

5、B+ 树
B+ 树是 B 树的变种，它的非叶子节点存储的是索引值，叶子节点存储的是真实的数据（聚簇索引存储的是数据行，非聚簇索引存储的是主键值），由于索引值占用空间不大，所以它一个节点相比 B 树来说可以存储更多数据，同样的，二分查找可以过滤掉更多的数据
B+ 树稳定的查询效率为 O(logn)

B+ 树的叶子节点会串成一条链表，范围查询时只需要定位到第一个满足条件的叶子节点，然后顺着链表一直往下读即可，效率相比 B 树来说更高
```





## 9、mysql select 查询 10 大优化

```java
1、为频繁查询的列建立索引，如果是联合索引，那么注意最左匹配原则，同时按照区分度进行建立
2、避免给频繁修改的列添加索引，因为频繁修改那么意味着索引树可能需要频繁的进行分裂
3、如果只有数字，那么使用 int，因为 int 比如 char、varchar 这种只需要比较一次，而 char、varchar 需要一个字符一个字符进行比较
4、对于定长字段，使用 char，因为 char 在获取时可以根据长度进行获取，即使数据没有达到指定长度也会使用空格补齐，而 varchar 由于是可变长的，所以需要两次查询：第一次查询数据长度，第二次根据数据长度获取真实的数据
5、尽量避免 select *
    1）select * 在很大情况下查询优化器不会走索引，而是走聚簇索引全表扫描，效率低
    2）select * 会查询出很多无用的字段，在数据库通过网络传输到服务器的数据量越大，耗费的时间就越长
    3）select * 杜绝了覆盖索引出现的可能，即使使用了索引，也必定需要进行回表扫描
6、避免 where 条件 = 左边出现表达式或者函数，否则会导致索引失效
7、避免隐式类型转换，比如 varchar 的类型在 where 条件中要加单引号，比如 where a = '2'，不要直接写程 where a = 2，否则很大可能会出现索引失效，因为这个隐式类型转换底层使用了 CAST 函数，即满足第 7 条的问题
8、索引下推，减少存储引擎层回表次数，这个是 mysql 底层自动优化的
9、force index() 强制指定索引，当执行计划的索引不是最佳的时候，可以使用该函数强制指定索引
10、当查询数据量大导致的速度慢时，可以使用 limit 分页查询，当使用分页查询时，如果查询的偏移量比较大，其实是可以将上一页的参数最大值作为查询条件的，以此来减少数据量
    比如 select * from test order by create_time limit 1000000,10;
	我们可以根据上一页的 create_time 的最大值作为查询条件,前提是 create_time 需要建立索引。
        select * from test where create_time > '2017-03-16' order by create_time limit 0，10;
```





## 10、select 语句执行顺序

[关于sql和MySQL的语句执行顺序(必看！！！)](https://blog.csdn.net/u014044812/article/details/51004754 )

```
1、一般 select 执行顺序如下：
    from （从这里开始，后面都能够使用表的别名）
    join
    on 
    where
    group by（从这里开始，后面都能够使用 select 的别名）
    聚合函数 SUM() AVG() count()
    having
    select
    distinct
    order by
    limit
    
实际上这些都是在 Server 层进行处理的，存储引擎层只负责查询数据，然后将查询到的数据返回。
    
例子：存在以下 sql 语句
    
	select distinct max(总成绩) as max总成绩 
 
    from tb_Grade 

    where 考生姓名 is not null 

    group by 考生姓名 

    having max(总成绩) > 600 

    order by max总成绩 

    limit 0, 1;
    
    
1）首先执行 from，找到查询的表

2）执行 where，根据查询条件 调用 存储引擎层 接口，如果有索引那么在非聚簇索引树中查找，如果没有索引，那么在聚簇索引树中全表扫描，如果 select、group by、order by 中字段需要回表，那么回表查询出整行数据，然后将数据返回给 Server 层。

3）Server 层拿到数据后，根据 group by 对数据进行分组

4）对每组数据进行 max(总成绩)，找到每组数据中 总成绩最大的数据行

5）执行 having，根据每组数据中总成绩最大的数据行再次进行筛选，找到总成绩 > 600 的数据行

6）执行 select， 将筛选出来的数据行找出需要返回的字段
    
7）执行 distinct，将筛选出来的数据行中 "distinct 列" 该列存在重复值的数据行进行去重

8）执行 order by，将去重后的数据行进行排序

9）执行 limit，将排序后的数据行进行分页获取

10）将结果返回
    

2、distinct
distinct 用来去重
    比如 
    id	a	b	c
    1	1	3	1
    2	1	4	2
    如果 sql 中存在 select distinct a，那么它是对 第一列删除还是对第二列删除？
    实际上 distinct 不会在意这些，因此当存在 distinct 的时候， select 的字段必须跟 distinct 存在一对一的关系。
    如果我们 select b, distinct a ，这样是错误的 sql 语法，因为查询出来 a 有 1 行， b 有 2 行，同时就算 a 只有 1 行，但是 a 和 b 之间不存在任何的映射关系，所以它们不能作为一行数据行进行显示，因为 select a 是查询出所有数据的 a，而 select distinct b 是查询出所有不重复的 b，它们之间没有任何的映射关系。
    
	一旦 select 中存在 distinct，那么 select 中就必须只存在 distinct 的字段
    比如 select a, distinct b 是错误的语法，而 select distinct a, b 是对的，因为这表示按照 a 和 b 一起去重
    
    由于 select distinct b from test order by a limit 0, 1；
    它会先根据 a 进行排序，然后再进行去重，然后再进行  limit 截取，所以有理由相信 distinct 实际上比 order by 还要慢执行
    所以个人感觉 select 的执行顺序是
        select （此时数据还没有删除，所以可以进行 order by）
        order by
        distinct (这里会将根据数据行进行去重)
        limit

2、子查询
    select 子查询中返回的数据量应该是跟其他字段相关的
    比如 
	select id, (select name from test) as user_name 
    		from test order by user_name
    这种明显是有问题的，在 select 子查询中， 很明显， user_name 这个子查询单纯是扫描表得到所有的 name，
    这就出现 一对多并且毫无对应的关系的问题了
    子查询只能出现一条相关的数据，出现多条也不行，那么就只能改成
    select id, (select count(name) from test) as user_name 
    		from test order by user_name


3、order by、group by 和 子查询
	order by 和 group by 都能够使用 select 字段的别名，但是 group by 在 select 前面执行，order by 在 select 后面执行
	所以对于 order by 能够看到 select 的别名来说没什么疑问，问题在于 group by 为什么能够看到 select 的别名？
	个人认为，mysql 底层会进行 sql 重写，将 group by 的字段重写为 select 别名对应的 字段/ sql
	比如 
		select id, (select count(*) from user where id = uid) as c 
		from test 
		group by c;
	mysql 底层会改写为
		select id, (select count(*) from user where id = uid) as c 
		from test 
		group by (select count(*) from user where id = uid);
	这里我们看到 group by 会将子查询执行一遍，这个结果跟 select 是一样的，所以我们有理由相信，当执行 select 的时候不会再去执行这条子查询 sql，而是会获取 group by 已经执行并且得到的 sql 结果
	因此，当存在 group by 的时候，实际上就是将 select 中的子查询/函数 提前 执行，如果是字段则直接进行替换。
	（当然上面是纯属个人猜测，感觉挺有道理的）
```

 ![img](https://iknow-pic.cdn.bcebos.com/cb8065380cd79123f5525b96a3345982b3b780a6) 





## 11、char、varchar

[InnoDB 存储引擎对 CHAR 类型的处理](https://blog.csdn.net/wppkind/article/details/74909932)

[InnoDB存储引擎中char的行存储结构](https://zhuanlan.zhihu.com/p/86259276)

```java
char(10) 和 varchar(10) 表示的都是字符的个数，而不是字节的个数

如果是 latin1（单字节字符集编码），那么 char(10) 稳定占 10B，不足 10B 的数据后面会使用 \x20 进行填充（00100000），这个是定长的，效率较高
如果是 utf8（多字节字符集变长编码），uft8 每个字符的范围为 [1B, 3B]（这是 mysql 的 uft8，而不是真正意义上的 uft8，真正的应该是 utf8mb4，最长为 4B），那么这个 char(10) 可以存储的字节范围为 [10B, 30B]，InnoDB 底层是当作 varchar 来存储的，即会使用一个字段来记录字符的长度
比如 'ab'，那么每个字符占 1B，总的为 2B，那么它会存在 1B 来记录这个长度 2
比如 '你好'，那么每个字符占 3B，总的为 6B，那么它会存在 1B 来记录这个长度 6
即如果是多字符集下的编码，char 跟 varchar 是一样的


varchar(N) 最多能够存储 65535 B，但是 N 表示的不是字节，而是字符
mysql 的字符集编码占用的字节数：
1）latin1：1B
2）gbk：1B - 2B
3）utf8：1B - 3B
4）utf8mb4：1B - 4B

1、如果表只有 1 个 varchar 字段
如果是 latin1（单字节字符集编码），那么一个字符最大占 1B，那么可以存储 (65535 - 1 - 2) / 1 = 65532 个字符，
	减 1 是因为实际数据行存储从第二个字节开始
	减 2 是因为前面会使用 2B 来表示后面数据的字节长度
	/ 1 是因为每个字符占用的字节数
	那么最大可定义为 varchar(65532)
如果是 gbk（多字节字符集编码），那么一个最大字符占 2B，那么可以存储 (65535 - 1 - 2) / 2 = 32766 个字符
	减 1 是因为实际数据行存储从第二个字节开始
	减 2 是因为前面会使用 2B 来表示后面数据的字节长度
	/ 2 是因为每个字符占用的字节数
	那么最大可定义为 varchar(32766)
如果是 utf8mb4（多字节字符集编码），那么一个字符最大占 4B，那么可以存储 (65535 - 1 - 2) / 4 = 16383 个字符
	减 1 是因为实际数据行存储从第二个字节开始
	减 2 是因为前面会使用 2B 来表示后面数据的字节长度
	/ 4 是因为每个字符占用的字节数
	那么最大可定义为 varchar(16383)

2、如果表有多个字段
	t4 表被定义为 
	create table t4(
    	c1 int, 
    	c2 char(30), 
    	c3 varchar(N)
	) charset=utf8;
    那么 N = (65535 - 1 - 2 - 4 - 30 * 3) / 3 = 21812 个字符;
	减 1 是因为实际数据行存储从第二个字节开始
	减 2 是因为前面会使用 2B 来表示后面数据的字节长度
	减 4 是因为 int 占用 4B
	减 30 * 3 是因为 char(30) 在 utf8 下最大占用 30 * 3 = 90B
	/ 3 是因为每个字符占用的字节数
	那么最大可定义为 varchar(21812)
从这里也可以看出，varchar 最大字节数 65535 实际上包含了其他字段占用的字节数
```

