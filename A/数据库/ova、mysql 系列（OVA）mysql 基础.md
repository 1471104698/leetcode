# mysql 基础



## **1、四个基本语句**

**select * from table** where xxx

**update table** set id = 1 where xxx

**delete from table** where xxx

**insert table** (x,xx,xxx) values (x,xx,xxx);

需要 from 的有 select 和 delete



## 2、清空表：

- truncate table_name
- delete from table_name
- drop table table_name

truncate table_name 是按照数据页删除数据的，不会记录 redo log 和 undo log 和 bin log，并且会重置自增 id，重新从 1 开始自增，但是 **truncate 不能清空有被其他表进行外键引用的表。**

delete from table_name 是由于不存在 where 条件，所以删除的是整个表的数据，实际上就是使用执行 用户 sql 的形式执行的，只不过没有指定 where 条件，它会开启事务，然后一条数据一条数据删除，会记录 redo log、undo log、bin log，自增值 id 也不会改变

drop table table_name 是删除表，整个表都删除了



总结：

- drop 是删除表，不会保存表结构
- truncate 是清空表，删除的单位是数据页，因此速度比 delete 快，但是会保留表结构，重置自增 id
- delete 是按照事务的方式一条一条删除数据行，速度慢，会记录 日志，不会重置自增 id



## 3、Sql 语句执行过程

具体看  https://zhuanlan.zhihu.com/p/95082274 

 https://zhuanlan.zhihu.com/p/70295845 



以下是 select 语句的执行流程：

 ![img](https://pic1.zhimg.com/80/v2-28ed550e0fcf925829ea167508e01d78_720w.jpg) 

流程包括以下几个步骤：

- 连接
- 查询缓存
- 语法解析器和预处理
- 查询优化器
- 执行计划



> ### 1、连接

当服务器发送请求时，数据库和服务器之间会建立 TCP 连接

数据库和服务器之间的连接是 **半双工** 的，这意味着同一时间只能有一方发送数据，一方接收数据，不能同时进行

由于是 TCP 连接，所以为了防止无限制的建立连接并且频繁的创建和销毁连接，使得数据库崩溃或者降低效率，所以需要使用数据库连接池管理和复用连接（参考线程池），因此数据库并发度是看数据库连接池的连接数量的



> ### 2、查询缓存

数据库内部维护了一个缓冲区，先从缓冲区中查询是否存在数据 （一个大小写敏感的哈希表实现的） ，如果存在则直接返回，如果不存在则执行 sql 解析



> ### 3、语法解析器和预处理

语法解析器是解析 mysql 关键字，查看是否存在错误的关键字、关键字顺序是否正确

要是 sql 语句存在错误，那么就会收到如下提示：

```
You have an error in your SQL syntax
```



当语法解析器通过后，就会将 sql 语句传送给 预处理器

预处理是根据 mysql 的规则检查 sql 语句是否合法，比如 表 和 字段是否存在

如果存在字段错误之类的，那么就会收到如下提示：

```
Unknown column xxx in ‘where clause’
```



总的来说，**语法解析器解析的是下面的 绿色字段，预处理器解析的是下面的灰色字段**

 ![img](https://pic3.zhimg.com/v2-9d878461f3c58523bfdea526d6ac3966_b.jpg)

> ### 4、查询优化器

查询优化器收到的是 预处理器发送过来的 **语法树**，因此也是在 语法树的基础上进行优化

查询优化器的作用就是将 sql 语句转换为执行计划，一条 sql 语句存在不同的查询方式，比如使用哪个索引之类的，最终返回的结果都是相同的，但是执行的效率不同

查询优化器的作用就是找到执行效率最高，成本最低的执行计划



> ### 5、执行计划

执行器收到从 查询优化器 发送过来的执行计划，此时就会按照执行计划一步步执行

最后，mysql 会将查询结果返回







## 4、Mysql 查询优化

1、给频繁查询的列 加索引

2、使用 %abc% 之类的查询，可以考虑使用全文索引

3、建立联合索引的时候，**区分度 高的在前，这样能够过滤掉更多的数据**（对于慢 sql，就是很难在短时间内过滤掉不需要的数据，其中一个原因就是联合索引建立的区分度不对）

4、避免给经常修改的列加索引

5、对于定长字段使用 char 而不是 varchar，因为 char 定长的检索会更快，而 varchar 的话需要先读取字符串长度偏移量，再根据偏移量获取数据

6、只含数字的列就使用 int 型，因为如果使用 char 的话会比较的时候是一个字符一个字符进行比较的，而 int 这种只需要比较一次

7、查询的时候如果知道具体需要的字段，并且加了索引，那么避免使用 select *，最好满足覆盖索引，避免回表

8、索引失效的情况：这里说明一下，使用的 user 表中 (b, c, d) 是一个联合索引，并且它们的类型都是 varchar

- 查询优化器判断不走索引

- where 条件存在 表达式 或者 函数 会使得索引失效

  - ```sql
    select * from user where b + '1' = '2';	--左边存在表达式计算，索引失效
    select * from user where CAST(b AS char) = '2';	--使用函数，索引失效
    ```

- 索引字段 存在 隐式类型转换 会使索引失效，因为隐式类型转换实际上就是借助的 CAST 函数 实现的

  - ```sql
    select * from user where b = 2;	--发现 b 的数据类型对不上，因此会使用 CAST 函数强转为 int ，索引失效
    ```

    *![image.png](https://pic.leetcode-cn.com/1603877556-FmqHkR-image.png)*

  - 但存在特例的是，如果查询的字段不是 *，而是索引树上的字段，那么索引不会失效

    ```sql
    select b,c,d from user where b = 2;	--索引不失效
    ```

    *![image.png](https://pic.leetcode-cn.com/1603877237-htTYfA-image.png)





## 5、join 连接

我们分成两部分来讲解 join，分别是 实现效果 和 底层实现，根据实现效果能够更加容易了解底层实现，了解底层实现就更加容易优化 sql



### 1、实现效果





> ###  inner join

内连接，效果类似 where

 ![img](https://pic4.zhimg.com/v2-a30dcd91fe73eebb27feee0e35a91c2f_b.png) 



> ### left join

左连接，对于 on 的条件，左边表会列出所有的数据，右边表如果有匹配的那么就进行拼接，没有的就忽略，显示为 null

 ![img](https://pic3.zhimg.com/v2-b6f2cddd37986e542a346241638de676_b.png) 



> ### right join

右连接，跟 left join 反过来

 ![img](https://pic2.zhimg.com/v2-300cd485334edcfcbd7647427cdf1671_b.png)

 

### 2、底层实现



[join 底层三种实现 方式讲解](https://zhuanlan.zhihu.com/p/54275505)



> ### 1、Simple Nested-Loop Join（ 简单嵌套循环连接 ）

比如 tableA join tableB，这种匹配方法是将 遍历 tableA 的每一条数据，然后根据查询条件，在 tableB 中找到满足条件的数据行

如果 tableA 有 100 条数据，tableB 有 200 条数据，那么数据行总的遍历次数为 100 * 200 = 20000

**跟 笛卡尔乘积 一个数量级的**

比如 inner join ，条件为 a.d = b.d，那么就是找 tableA 的某条数据行，再遍历 tableB 的整个表，分别获取它们的 d 字段进行比较，如果一致，那么进行拼接，如果 tableB 中有 3 条数据行的 d 跟 tableA 的当前数据行一致，那么就会拼接出三条数据行

最终显示出来的都是 tableA 的数据行能够在 tableB 找到对应的，如果没有对应的那么不会显示出来

而 left join 和 right join 就是固定显示某个表的所有数据，如果有的进行拼接，如果没有的显示为 Null

<img src="https://pic1.zhimg.com/v2-2b9d48da48c6c436283fdec14db9d174_b.jpg" style="zoom:150%;" />



伪代码

```java
List<Row> result = new ArrayList<>();
for(Row r1 : t1){
	for(Row r2 : t2){
		if(r1.d == r2.d){
            result.add(r1.join(r2));	//r1 r2 进行拼接，并添加进结果集
        }	
	}
}
return result;
```



**这种暴力匹配的方法一般不会在数据库中使用**



> ### 2、Index Nested-Loop Join（ 索引嵌套循环连接 ）

很显然， Index 就是索引，即在索引树中查找，在上面那个暴力方法中，我们每次都是遍历 tableB 整张表，但实际上我们要找的就是 d 字段满足某个值的数据行

因此我们可以对 tableB 的 d 建立索引，这样的话，遍历 tableB 就不需要直接遍历整张表，而是遍历索引树，使用二分查找，相比暴力匹配 要 高效得多



以下是 右表 level 需要对 user_id 建立索引，左表 user_info 不需要，因为 join 中左表是驱动表，需要全部扫描一遍

而 右边是匹配表，只需要找出匹配的数据行即可，因此可以简化，不需要全部扫描

 ![img](https://pic4.zhimg.com/v2-c8790aa879ca6fedb83d529558bb40e3_b.jpg) 



但是这种方法**只适用于添加了索引的字段**，但实际上我们使用 join 的时候，条件并不全是添加了索引的，并且在关联多个条件的情况下，不会保证 on 的条件 是 联合索引，这样就退化成了第一种方法，但是显然不能这么做，因此需要一种在没有索引的情况下更加高效的匹配方法



> ### 3、Block Nested-Loop Join（ 缓存块嵌套循环连接 ）

BNLJ 是 基于 SNLJ 优化的，SNLJ 过于暴力，效率太低

BNLJ 加入了 缓存 join buffer，每次查询不再是获取外层表的一条数据，而是一次性缓存外层表的多条数据，然后遍历内层表，跟 缓存 中的数据进行匹配

嗯？好像也没有减少比较次数啊。。。

外层表 100 条数据，内层表 100 条数据

使用 SNLJ，循环比较次数为 100 * 100

使用 BNLJ，缓存一次存储 20 条数据，这样就相当于从 两层循环变成三层循环，那么就是 100/20 * 20 * 100 = 100 * 100

好像使用 SNLJ 和 BNLJ 循环次数是一样的啊，没什么差别



实际上 BNLJ 涉及到两部分的优化：

1. 缓存使用的是 HashTable：这样的话可以根据 key 排除掉一些数据，然后遍历 目标 key 的链表，能够减少比较次数
2. 减少内层表的扫描次数，减少磁盘 IO：由于实际场景表的数据过大，不可能全部装入到内存中，肯定是需要频繁进行磁盘 IO 的，如果使用 SNLJ，那么 对 外层表的每一条数据，内层表都需要进行一次扫描，进行多次 IO；但是使用 BNLJ，由于一次缓存了 外层表的多条数据，内层表的一条数据可以同时跟外层表的多条数据进行比较，那么就相当于是减少了内层表的扫描次数，从而减少磁盘 IO，要知道磁盘 IO 效率贼低，能减少就尽量减少

<img src="https://pic3.zhimg.com/80/v2-0e81dd7fe538f67559bc24c0a5a3207e_720w.jpg" style="zoom:150%;" />





## 6、group by

> ### group by 实现效果

当我们 group by name 的时候，表示是将 name 相同的列聚合起来，其他列不同的整合为一个 list 集合

比如下图，name 相同的整合为一个列， id 和 number 不同的存储到该列的集合中去

 ![img](http://images.cnitblog.com/blog/639022/201501/162343319172617.jpg) 

这样我们使用聚合函数 min() max() count() 就是直接在集合中判断

注意，group by 在没有使用聚合函数的情况下，会返回**分组后每个分组中的第一行数据**，不一定是排序好的，同时如果使用了聚合函数，同样也只会返回聚合的那个函数

group by 返回的是每个分组的对应的结果，是一个结果集，而不单单只是一条数据



> ### group by 和 order by 同时使用的坑点

如果 group by 和 order by 一起使用，会存在我们意料之外的问题

**group by 和 order by 一起使用时，会先执行 group by，再执行 order by**

并且 order by 是在 group by 返回的结果集上进行排序

即是对 group by 返回的每个分组的一条数据进行排序，而不是对 group by 每个分组内部进行排序

**即 order by 是分组外的排序，不关乎分组内的**，因为 order by 是在 group by 后面执行，group by 已经将每个分组对应的一条数据找出来返回了

 

上面这个是比如每个人都多次受到奖金，我们要获取到每个人收到的奖金的最大值，然后将这些最大值按照降序排序

那么就可以使用 group by 按照每个人进行分组，判断使用 max() 获取每个分组的最大值，然后使用 order by 对这些最大值进行排序



如果我们是想要获取每个人的奖金排序，那么直接使用 order by name, salary 即可，即先对 name 进行排序，这样同个人就在同一个范围内，然后对 salary 进行排序，这样就是对同个人的 salary 进行排序