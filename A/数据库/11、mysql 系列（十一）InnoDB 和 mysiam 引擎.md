# InnoDB 和 mysiam 引擎



## InnoDB 和 mysiam 引擎的区别

1、InnoDB 支持事务，mysiam 不支持事务

```
InnoDB 每次都需要将 sql 语句封装进事务里来保证原子性
InnoDB 使用 undo log 来回滚数据，使用 redo log 数据延迟写回磁盘，保证数据库出现事故重启能够快速进行数据恢复
```



2、InnoDB 支持外键，而 mysiam 不支持

```
带有外键的 InnoDB 表无法转换为 mysiam
```



3、InnoDB 支持表锁和行锁，而 mysiam 只支持表锁

```
由于 InnoDB 锁的粒度更小，所以 InnDB 并发度高，在写方面效率更高
```



4、InnoDB 和 mysiam 索引数据结构都是 B+ 树，但是 InnoDB 有 聚簇索引 和 非聚簇索引两种，而 mysiam 只有 非聚簇索引

```
InnoDB 有两个文件，一个是 .frm，表结构，一个是 .ibd，存储数据和索引，而数据就是聚簇索引，其他的非聚簇索引都指向这个聚簇索引
mysiam  有三个文件，一个是 .frm 表结构，一个是 .myd 数据文件，一个是 .myi 索引文件，索引文件中的索引都指向数据文件
```



5、mysiam 内部维护了一个 count 字段记录表中数据量

```
InnoDB 查询表中数据量时，需要调用 select count(*) from t，通过 全表扫描 统计表中数据行数
mysiam 直接获取 count 字段即可
这也侧面说明了 mysiam 主要是用来查询的，因为 insert delete 操作都需要修改这个字段
```



6、InnoDB 必须带主键，mysiam 可以没有

具体看 <https://zhuanlan.zhihu.com/p/98084061>

```
在 InnoDB 表有主键的时候，会将主键作为聚簇索引，如果没有主键，那么会将第一列非空唯一索引作为聚簇索引
如果都没有，那么使用 InnoDB 为每行数据行隐式添加的 3 个字段的其中一个字段 row_id 作为聚簇索引。
而 mysiam 由于都是非聚簇索引，所以有没有主键都没问题

这里再扩充下 InnoDB 没有设置主键，使用 row_id 产生的影响：
row_id 是某个数据库下所有 InnoDB 表全局共享的，
设置了主键的某张表下在数据插入时竞争 自增 id 是只有一张表的并发量
而 row_id 所有表在插入数据的时候，都需要对这个 row_id 加锁 或者 CAS 进行争夺，竞争力度加大
注意：这里的 row_id 只跟没有 主键有关，跟唯一索引无关，因为就算有唯一索引作为聚簇索引，还是会将 row_id 作为主键
```



7、InnodB 不支持全文索引（后续版本支持了），mysiam 支持全文索引

```
如果在 mysql 不支持的版本下，mysiam 对 %abc% 建立全文索引那么查询效率也提高了很多
```



## mysiam 读的速度比 InnoDB 快的原因

- InnoDB 都是封装在事务中的，每次都需要读操作都需要通过 MVCC 获取可见版本，而 mysiam 无需封装成事务，也没有 MVCC，直接查询即可
- mysiam 全部都是非聚簇索引，索引树的叶子节点指向的是数据文件中对应数据的地址，因此可以直接定位过去获取，而 InnoDB 如果是主键索引查询，那么直接可以在叶子节点获取，如果是辅助索引查询，那么需要两次查询，效率较低
  - 如下图，叶子节点指向的是数据地址

![img](https://img-blog.csdn.net/20170307100553013?watermark/2/text/aHR0cDovL2Jsb2cuY3Nkbi5uZXQvZG9uZ2hhaXhpYW9sb25nd2FuZw==/font/5a6L5L2T/fontsize/400/fill/I0JBQkFCMA==/dissolve/70/gravity/Center)