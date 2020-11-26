# drop、truncate、delete



## 1、drop、truncate、delete 的区别

三个 删除表 的 sql 语句：

- drop table `user`
- truncate `user`
- delete from `user`



讲解；

- drop 是 **删除表**，不会保存表结构
- truncate 是 **重置表**，按照 数据页 进行删除，保留表结构、索引、触发器，重置自增 id，即仅仅是删除数据
- delete 是 **清空表**，由于不存在 where 条件，所以默认删除查询到的数据，即整个表的数据。会开启事务，然后一条一条删除，同时会记录 redo/undo/binlog 日志，不会重置自增 id





## 2、关于 delete 的坑点

 [mysql删除操作其实是假删除](https://juejin.cn/post/6844903847102513166)

 

对于不带 where 条件的 delete 语句，mysql 会直接删除磁盘上的数据

对于带 where 条件的 delete 语句，Mysql 实际上并不会直接删除掉磁盘上的数据，而单单只是将数据行的 删除标识符 delete_bit 置 1，这就是在 undo log 的时候把 delete 当作 update 的原因了，我们称之为 **假删除**

这样的话，假设表数据 10G，我们想要删除掉某些数据缩减表，使用了 delete ，那删除的数据还在磁盘文件上



mysql 不删除数据的原因是为了**复用**这些数据行的位置，比如 id  300 - 600 之间是删除一条 id = 500 的记录，mysql 不会把这条数据真正的删除，而是标记为 "删除"，当下次要插入一条 id = 400 的数据时，mysql 就会复用 id = 500 的位置，将 id = 400 的数据行去覆盖标记为删除的 id = 500 的数据行



虽然目的是为了复用，但是这些被删除的数据实际上是一个空洞数据，占着茅坑不拉屎 的感觉，不仅占着内存空间，还可能会影响效率：

- mysql 存储数据是以 数据页 作为基本单位的，如果一个数据页可以存储 10 条数据，那么我们删除了其中的 9 条，但是它不会真正的删除，意味着下次我们读取存活的那条数据的时候，根据磁盘局部性原理，它会把 已经删除的那 9 条数据也给读取进来
- 同时，如果本来一个 数据页可以存储的数据，却因为没有删除而将数据分散为 好几个数据页，其中大部分数据都是空洞数据，当范围查询的时候显然增加了 IO 次数

这些被删除了的数据占据的数据页的空间我们称之为碎片



解决的方法有两个：

- Mysql 官方推荐使用 optimize 命令来解决数据碎片的

  ```sql
  optimize table user
  ```

  该命令会查询出 表 A 所有没有被标记为删除的数据，然后放到一张临时表 B 中，然后使用 临时表 B 替换 表 A，完成表的重建，解决了大量的碎片问题

问题就是 optimize 执行的时候会将表锁住，不要在业务高峰期使用

- 使用逻辑删除，在表中加 3 个字段

  ```sql
  `is_deleted` tinyint(4) NOT NULL DEFAULT '0' COMMENT '是否逻辑删除：0：未删除，1：已删除',
  `create_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间'
  ```

  每次删除的时候都是利用 update 来更新这个 is_deleted，即从应用层方面来做 mysql 底层标记删除的事

  当数据需要清除的时候，我们创建一张表 B，然后根据 create_time 选定需要保留的数据范围，然后将这个范围内 is_delete = 0 的数据查询出来存储到 表 B 上，

