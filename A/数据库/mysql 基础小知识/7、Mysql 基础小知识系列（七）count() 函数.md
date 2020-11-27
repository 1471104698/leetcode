# count() 函数





count() 函数用于统计行数

​	有时候 Mysql 选择全表扫描，扫描整个聚簇索引树；有时候会选择扫描 辅助索引 的非聚簇索引树

**count(*) 和 count(1) 通过 explain 后可以发现它们的执行计划是一样的**

对于查询优化器而言，count(*) == count(1)

有些人把 count(1) 当作是按照 第一列字段 来查询行数，其实不是这样的，1 实际上是一个伪列，它不是一个真实存在的列



> #### count(1) 和 group by 一起使用

当 count() 没有跟 group by 使用时，那么统计的是整个表的数据

<img src="https://pic.leetcode-cn.com/1606454120-QRlKlC-image.png" style="zoom:90%;" />

当 count() 跟 group by 使用时，那么统计的是 group by 每个分组的信息

<img src="https://pic.leetcode-cn.com/1606454058-KyLMOe-image.png" style="zoom:80%;" />

因此，count() 实际上是对分组进行统计，**在没有使用 group by 的情况下，count() 将整张数据表当作一个分组**

而当使用了 group by 后，count() 就是按照分组进行统计



> ####  count(1) 和 order by 1 的区别

order by 1, 2 这种表示先 按照第 1 列排序，再按照第 2 列排列，1 和 2 都是表示对应的列

而 count(1) 单纯只是表示查找所有数据行，1 没有实际意义，我们还可以写 count(2)、count(3)、count("fuck")，效果都是一样的

所有的写法都跟 count(*) 得到的执行计划一致，因此**不存在效率差别**



> ####  count(column) 和 count(distinct column(s)) 

count(column)：表示按照某个字段来进行查询行数，它会忽略掉 查询列值为 NULL 的行数

 count(distinct column(s)) ：表示查找多个列 都不为 NULL 的行数，并且会将相同的数据行进行去重

存在以下表数据：

*![image.png](https://pic.leetcode-cn.com/1604850172-kntgTm-image.png)*

```sql
select count(f1) from test;
select count(distinct f1, f2) from test;
```

输出结果：

```java
4
2
```



count(f1) 会自动将 f1 列中含有 NULL 值的数据行忽略，所以第一句 sql 忽略 id = 2 的数据行，结果为 3

count(distinct f1, f2) 表示统计 f1 和 f2 列 中都不为 NULL 的数据行，并且如果存在两个以上的数据行  f1 和 f2 都相同，那么进行去重，因此被忽略的 id = 2, 4 是因为存在 NULL，被忽略的 id = 5 或 id = 3 是因为它们的 f1 和 f2 都相同，所以进行去重

最终剩下 id = 1, 3 两个数据行，结果为 2

