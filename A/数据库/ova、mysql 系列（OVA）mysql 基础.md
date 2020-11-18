# mysql 基础



## **1、四个基本语句**

**select * from table** where xxx

**update table** set id = 1 where xxx

**delete from table** where xxx

**insert table** (x,xx,xxx) values (x,xx,xxx);

需要 from 的有 select 和 delete



## 2、清空表

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



## 3、Mysql 处理 select 的过程

select 语句的大致执行流程：

![img](https://img2018.cnblogs.com/blog/1446270/201910/1446270-20191006025827107-243871764.png)

  

流程包括以下几个步骤：

- 服务器连接数据库
- 查询缓存
- SQL 解析
- 查询优化器
- 执行计划



> ### 1、服务器连接数据库

最开始，服务器会通过我们在 spring.yml 中指定的 user 和 password 跟 数据库的连接器 建立连接，当数据库身份校验成功后，服务器 和 数据库就可以创建 connection 进行数据通信了

**服务器和数据库之间的 connection 使用的是 TCP 连接**，所以为了防止 无限制的建立连接 并且 频繁的创建和销毁连接，所以使用数据库连接池 管理和复用连接（参考线程池），因此数据库并发度是看数据库连接池的连接数量的



数据库和服务器 的 TCP 连接是 **半双工** 的，同一时间只能有一方发送数据，一方接收数据，不能同时进行



> ### 2、查询缓存

当建立好 connection 连接后，可以执行 select 语句请求数据库进行查询

**Mysql 内部维护了一个哈希表 作为缓存区，将 select 语句作为 key，将查询结果作为 value**

Mysql 在获取 select 语句后，不会直接去解析执行这个 sql，而是会查看内部缓存，看看之前是否执行过这条 sql 语句

如果执行过，那么通过 select 语句可以获取到查询的数据

如果没有执行过，获取执行过但表被修改过了缓存清空了，那么就会去解析执行 sql



但一般不建议使用缓存，因为如果表需要频繁的修改，那么每次 select 查询完，将查询结果放入到缓冲中，还没有使用到缓存时该表就被更新了，那么会导致缓存中关于该表的缓存数据全部失效。**对于频繁更新的表，缓存的命中率非常的低**，因此缓存的作用几乎为零，同时还需要耗费资源去处理缓存，得不偿失

只有在不需要频繁更新的表上使用缓存才有意义，但是在 Mysql 8.0 上删除了缓存这一功能，Mysql 设计者也觉得这个功能鸡肋



当查询缓存无果后，会进入到 sql 解析的步骤，这里涉及到 语法解析器 和 预处理器

> ### 3、SQL 解析：语法解析器 和 预处理器

[词法解析 和 语法解析](https://tech.meituan.com/2018/05/20/sql-parser-used-in-mtdp.html)



语法解析器会完成两个动作：

- **词法解析**

词法解析就是将一个完整的 sql 语句 按照分隔符（空格）分割成一个个的 token，同时会对关键字进行校验：

- 判断关键字顺序是否正确,比如 from 在前，select 在后，这就不行了

- 判断关键字是否欠缺，比如只有 select，没有 from

```sql
比如一个简单的 SQL 语句：

select name from user where id = '1' and age > 20;

它会打碎成 12 个 token，同时还记录每个 token 的类型
当分割出所有的 token 后，会检查其中哪些为关键字，其中 有 4 个是 keyword(关键字)，分别为 select、from、where、and
```

如果词法解析过程存在错误，那么就会收到如下提示：

```
You have an error in your SQL syntax
```



- **语法解析**

```sql
select username, ismale from userinfo where age > 20 and level > 5 and 1 = 1 
```

将词法解析得到的 token，经过 语法解析 后，生成以下语法树：

![图2 语法树](https://awps-assets.meituan.net/mit-x/blog-images-bundle-2018a/a74c9e9c.png)



语法解析会将每个关键字后面的参数放到不同的数据结构中

比如 select 后面的字段被放到了 item_list 中，from 后面的字段被放到了 table_list 中，where 后面的字段被放到了 Item 中

![图3 SQL解析树结构](https://awps-assets.meituan.net/mit-x/blog-images-bundle-2018a/8eafb088.png)

where 后面的查询条件最为复杂，所以 item 存储的数据也最为复杂：

![图4 where条件](https://awps-assets.meituan.net/mit-x/blog-images-bundle-2018a/0adcdb23.png)





当语法解析 和 词法解析结束后，将得到的 参数 都发送给 预处理器

**预处理器会检查 参数 数据结构内部的数据 是否正确，比如 item_list 中的列名是否存在，table_list 中的表是否存在，同时会解析别名**

如果存在字段不存在之类的，那么就会收到如下提示：

```
Unknown column xxx in ‘where clause’
```





> ### 4、查询优化器

当 SQL 解析完成后，查询优化器 会收到解析出来的 **语法树**，此时的语法树是合法的

一条 sql 存在不同的执行方案，执行顺序不同但是最终结果相同，然后效率也不同，比如 使用索引 和 不使用索引最终的结果相同，但是使用了索引的效率 一般比 不使用索引的效率高

查询优化器的作用就是根据 语法树 来找出最佳的执行计划，然后将执行计划推送给执行器执行，**最终选定的执行计划就是我们 explain 可以看到的**



> ### 5、执行计划

执行器 收到从 查询优化器 发送过来的执行计划，此时就会按照执行计划一步步执行

最后，mysql 会将查询结果返回





## 4、select 各部分关键字的执行顺序



```sql
FROM ->
WHERE -> 
GROUP BY -> 
SELECT -> 
HAVING -> 
DISTINCT -> 
UNION -> 
ORDER BY
```

**order by 是最后执行的，在它眼里只有最终的数据，没有什么分组不分组的**



对于别名，按照道理来讲，只有在前面执行定义的别名，才可对后面执行的部分可见，比如 from 定义的表的别名对 where 可见

虽然很多文章都说 having 是在 select 之前执行的，但是按理说 select 定义的别名对于 having 应该是不可见的，但实际上却是可见的，同时在 9 中存在以下 sql 语句

```sql
 ①
select s1.id, s1.name, s1.cid, s1.score,
	  ②
	(select count(1) from  sc s2 where  s1.cid = s2.cid and	 s1.score < s2.score) + 1 as rank
from  sc s1
HAVING 
	rank <= 3 
order by 
	s1.cid, 
	rank;
```

我们可以发现 having 竟然能够使用 select ① 经过一系列查表操作后得到的 rank 字段，如果是 having 先执行，显然对于 having 来说这个 rank 还没有求出来，所以是不可知的，但是对于 having 可见，**所以 having 应该是在 select 之后执行**

```
我不关注理论，我只关注结果，结果是如此，那么我就如此认为，也许是 mysql 内部的优化，不过最终呈现的结果就是 having 在 select 之后执行，那么我也如此认为
```



## 5、Mysql 查询优化

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





## 6、join 连接

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





## 7、group by 如何使用？

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





## 8、count() 函数

count() 函数用于统计行数

**count(*) 和 count(1) 通过 explain 后可以发现它们的执行计划是一样的**

对于查询优化器而言，count(*) == count(1)

有些人把 count(1) 当作是按照 第一列字段 来查询行数，其实不是这样的，1 实际上是一个伪列，它不是一个真实存在的列



**count() 和 order by 不同**

order by 1, 2 这种表示先 按照第 1 列排序，再按照第 2 列排列，1 和 2 都是表示对应的列

而 count(1) 单纯只是表示查找所有数据行，1 没有实际意义，我们还可以写 count(2)、count(3)、count("fuck")，效果都是一样的

所有的写法都跟 count(*) 得到的执行计划一致，因此**不存在效率差别**



但是，如果我们 count(列名)，那么就是表示按照某个字段来进行查询行数，它会忽略掉 查询列的列值值为 NULL 的行数，即如果 10 行数据中 查询列名 有 3 行数据为 NULL，那么 count(列名) 得到的结果为 7



**关于 count(distinct column(s)) 函数：**

 count(distinct column(s)) 表示查找所指 列 都不为 NULL 的行数，并且会将相同的数据行进行去重

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





## 9、求各科成绩前三的学生

[各科求前三详解](https://blog.csdn.net/qq_41933709/article/details/84928157 )



mysql 中有这么一道经典的题目：求 各科成绩前三的学生



第一印象来说，感觉应该是对 科目 进行 group by，然后对每个分组进行 order by，再对每个分组 limit 0, 3 获取前 3 名的学生 id

但是这样实现就很难受了，因为 group by 和 order by 一起使用时，order by 并不会对 分组内排序，而是对分组外排序，这样的话显然就无法直接实现 每个分组 order by（显然是我自己能力不够，因此只能另辟蹊径）



（我们这里将题目改为 求 每个班级成绩前三的学生）

我们可以先计算出每个班级中每个学生的排名，如何计算排名？

通过自连接，对于每一行数据行，我们找到 在 **同一班级中，比当前数据行的 score 高的所有数据行**，然后使用 count(1) 统计结果集行数，即可得到排名

```sql
 ①
select s1.id, s1.name, s1.cid, s1.score,
	  ②
	(select count(1) from  sc s2 where  s1.cid = s2.cid and	 s1.score < s2.score) + 1 as rank
from 
	sc s1
order by
	rank;
```

sql 形式格式化后：

```sql
select 
	s1.id, s1.name, s1.cid, s1.score,
	(
        select
        	count(1)
        from 
        	sc s2
        where 
        	s1.cid = s2.cid
        and	
        	s1.score < s2.score
	) + 1 as rank
from 
	sc s1
order by
	rank;
```

查询结果为：

*![image.png](https://pic.leetcode-cn.com/1604910232-SaKJQt-image.png)*



首先讲解一下  select ② 是如何运作的：

可以看出来， select ② 是作为 select ① 的一个查询结果字段的，当 where 查询完，筛选出了满足条件的所有数据行，作为虚拟表 t1，然后进入到 select ① 阶段

在 select ① 这里存在一个 select ②，select ② 会对 虚拟表 t1 再进行一次查询，这里是 t1 表 和 sc 表的条件筛选，生成只有 rank 字段的 t2 表，然后 t1 表 和 t2 表进行拼接，构成 t3 表

最后 order by 对 t3 表按照 rank 进行排序



但是上面的 sql 语句还没有完成，它只是获取了每个学生在他班级中的排名，没有得到每个班级的前三名，因此我们可以使用 having 来根据 rank 进行筛选，having rank <= 3 来获取排名前三的学生

```sql
SELECT 
	s1.*, (
				select 
					count(1) 
				from 
					sc s2 
				where 
					s1.cid = s2.cid 
				and 
					s1.score < s2.score
			) + 1 as rank 
from 
	sc s1 
HAVING 
	rank <= 3 
order by 
	s1.cid, 
	rank;
```

输出结果：

![1604912168673](C:\Users\蒜头王八\AppData\Roaming\Typora\typora-user-images\1604912168673.png)*



但是这种是并列排名的，即如果两个学生的 score 一样的话，那么它们的排名相同，比如上的 name 为 a 和 aa，它们的分数相同，排名也相同，是并列的