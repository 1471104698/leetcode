# EXPLAIN 解析



## 1、EXPLAIN 重要字段

EXPLAIN 表示的是 sql 执行计划，即查询优化器 计算得出的执行计划，可以通过 EXPLAIN 获取 sql 执行计划的关键信息

主要是针对 select 查询，insert 、update、delete 这种就没什么好优化的了



一般情况下 EXPLAIN 中存在以下字段

*![image.png](https://pic.leetcode-cn.com/1603687617-FNvioe-image.png)*

但需要重点关注的有以下几个字段：

| 列名    | 备注                                                         |
| ------- | ------------------------------------------------------------ |
| type    | 本次查询表联接类型，从这里可以看到本次查询大概的效率         |
| key     | 最终选择的索引，如果没有索引的话，本次查询效率通常很差       |
| key_len | 本次查询用于结果过滤的索引实际长度，参见另一篇分享（[FAQ系列-解读EXPLAIN执行计划中的key_len](http://imysql.com/2015/10/20/mysql-faq-key-len-in-explain.shtml)） |
| rows    | 预计需要扫描的记录数，预计需要扫描的记录数**越小越好**       |
| Extra   | 额外附加信息，主要确认是否出现 **Using filesort、Using temporary** 这两种情况 |



## 2、type 字段

| 类型   | 备注                                                         |
| ------ | ------------------------------------------------------------ |
| ALL    | 执行**全表扫描 或者 回表扫描 **，这是**最差**的一种方式      |
| index  | 使用索引全表扫描，但无需**回表**，比ALL略好                  |
| range  | 利用索引进行范围查询，比index略好，比如 where a > 1，并且 a 上有索引 |
| ref    | 基于索引的等值查询，或者表间等值连接，比如 where a = 1，并且 a 上有索引 |
| eq_ref | 表 join 连接时基于主键或非NULL的唯一索引完成扫描，比ref略好  |
| const  | 基于主键或唯一索引查询，并且where 查询是常量，比如 where id = 1，最多返回一条结果 |
| system | 查询对象表只有一行数据，这是最好的情况                       |



> 全表扫描 和 回表

全表扫描就是扫描整个索引树，一般是 无索引 或者 没有 where 过滤条件

回表即需要查询查询 聚簇索引的节点，即需要获取数据行的所有数据



> index 比 All 快的原因

user 表中 id 为主键，那么 聚簇索引树 就是按照 id 进行排序的，而 叶子节点则是数据行

有以下两条 sql：

```sql
select * from user;
select id from user
```

第一个是 all，第二个是 index，很显然，这么看 第二条 sql 的执行效率 比 第一条 sql 要高

但不都是在同一棵 聚簇索引树上进行扫描的吗？为什么会存在这些差距？

以下是 B+ 索引树的结构

如果我们 select * from user，那么需要查找到叶子节点，并且将内部的 data 全部搜索出来

如果我们 select id from user，那么只需要查找到叶子节点就可以获取到所有的 id，不需要再去搜索 data，减少了磁盘 IO

![img](http://cdn.17coding.info/WeChat%20Screenshot_20190621220003.png)



> eq_ref

join 连接查询，**连接的右表使用了 主键索引 或者 唯一索引**

```sql
EXPLAIN select u.id from user u left join user u1 on u.id = u1.id;
```

*![image.png](https://pic.leetcode-cn.com/1604488890-qxOZlW-image.png)*



如果右表使用的是普通索引，那么是 ref

```sql
EXPLAIN select u.id from user u left join user u1 on u.a = u1.a;
```

*![image.png](https://pic.leetcode-cn.com/1604489021-Exwsot-image.png)*





## 3、Extra 字段



| 关键字                   | 备注                                                         |
| ------------------------ | ------------------------------------------------------------ |
| Using filesort           | 将用外部排序而不是按照索引顺序排列结果，数据较少时从内存排序，否则需要在磁盘完成排序，代价非常高，**需要添加合适的索引** |
| Using temporary          | 需要创建一个临时表来存储结果                                 |
| Using index              | 查询使用了覆盖索引，不需要回表，即不需要访问数据，单单索引树节点就可以满足要求 |
| Using where              | 存在 where 条件查询，需要根据 where 条件对查询的数据进行筛选 |
| Using where，Using index | 查找使用了覆盖索引，并且存在 where 条件查询，需要进行筛选    |



> ### Using Index 和 Using where：

[Using index 和 Using where 的解释](<https://segmentfault.com/q/1010000004197413>)



> ### Using temporary

Using temporary 表示要创建一个临时表来存储结果

比如 union 需要将多表查询的结果关联成一个临时表，然后方便去重

如果使用的是 union all 就不允许去重操作，那么也就不会创建临时表，直接拼接输出即可



执行以下 sql 语句：

```sql
explain 
SELECT * FROM `user` WHERE b = 1 
union 
SELECT * from `user` where b = 12;
```

*![image.png](https://pic.leetcode-cn.com/1603866098-TiCUJW-image.png)*

需要创建临时表



执行以下 sql 语句：

```sql
explain 
SELECT * FROM `user` WHERE b = 1 
union all 
SELECT * from `user` where b = 12;
```

*![image.png](https://pic.leetcode-cn.com/1603866205-qCRaIe-image.png)*

不需要创建临时表





> ### Using filesort

Using filesort 表示需要对查询出来的结果集进行排序，即 直接 根据 索引 查询出来的 结果集 顺序 不满足 要求

如果数据量大，那么使用外排序，即磁盘排序，效率低得一批

如果数据量小的，那么使用内存排序，效率还可以

因此我们要尽量保证联合索引的建立满足 order by 的顺序



执行以下 sql 语句：

```sql
explain 
SELECT id FROM `user` 
where b = 2 and c = 2 
order by d; 
```

*![image.png](https://pic.leetcode-cn.com/1603690290-GrYbdw-image.png)*

联合索引的建立是 (b，c，d) ,因此按照该联合索引树查询出来的结果集中 d 是无序的，而最终需要 order by d，那么就需要对查询出来的结果集 重新进行排序了，因此出现了 Using filesort



执行以下 sql 语句：

```sql
explain 
SELECT id FROM `user` 
where b = 2 and c = 2 
order by b; 
```

*![image.png](https://pic.leetcode-cn.com/1603690221-EODPDL-image.png)*

联合索引的建立是 (b，c，d) ,因此按照该联合索引树查询出来的结果集中 b 是有序的，orfer by b 已经完成了，无需重新排序