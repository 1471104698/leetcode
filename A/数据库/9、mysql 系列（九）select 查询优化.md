# sql 查询优化



## 1、select 优化

1、给频繁查询的列加索引，**区分度不高的列不需要加索引**，因为无法排除多少数据，同时还要花费资源去维护这棵索引树

2、使用 %abc% 之类的查询，可以考虑使用全文索引

3、建立联合索引的时候，**区分度 高的在前，这样能够过滤掉更多的数据**（对于慢 sql，就是很难在短时间内过滤掉不需要的数据，其中一个原因就是联合索引建立的区分度不对）

4、避免给经常修改的列加索引

5、对于定长字段使用 char 而不是 varchar，因为 char 定长的检索会更快，而 varchar 的话需要先读取字符串长度偏移量，再根据偏移量获取数据

6、只含数字的列就使用 int 型，因为如果使用 char 的话会比较的时候是一个字符一个字符进行比较的，而 int 这种只需要比较一次

7、查询的时候 尽量避免使用 select *，最好 select 的字段满足 覆盖索引，避免回表

8、**索引失效的情况：**这里说明一下，使用的 user 表中 (b, c, d) 是一个联合索引，并且它们的类型都是 varchar

- 查询优化器判断不走索引

- where 条件存在 表达式 或者 函数 会使得索引失效

  - ```sql
    select * from user where b + '1' = '2';	--左边存在表达式计算，索引失效
    select * from user where CAST(b AS char) = '2';	--使用函数，索引失效
    ```

- 索引字段 **存在 隐式类型转换 会使索引失效**，因为隐式类型转换实际上就是借助的 CAST 函数 实现的

  - ```sql
    select * from user where b = 2;	--发现 b 的数据类型对不上，因此会使用 CAST 函数强转为 int ，索引失效
    ```

    *![image.png](https://pic.leetcode-cn.com/1603877556-FmqHkR-image.png)*

  - **但存在特例：**如果查询的字段不是 `*`，而是索引树上的字段，那么索引不会失效

    ```sql
    select b,c,d from user where b = 2;	--类型强转，但索引不失效
    ```

    *![image.png](https://pic.leetcode-cn.com/1603877237-htTYfA-image.png)

9、索引下推

10、使用 force index() 强制使用某个索引进行查询。注意要防止出现上面的索引失效情况，防止这里的 force index() 也是无效的





## 2、索引下推

[索引下推](https://www.cnblogs.com/Chenjiabing/p/12600926.html)



在 Mysql 5.6 的时候，出现了 `索引下推` 这一功能，简称 ICP（Index Condition Pushdown），用来优化查询



存在一张 user 表，主要字段有：id、name、age。建立一个联合索引（name, age）

表中数据如下：

*![image.png](https://pic.leetcode-cn.com/1605847361-nLJmld-image.png)*



1、存在 sql 语句：

```sql
select * from user where name like “陈%";
```

根据最左匹配原则，会使用到联合索引的前部分索引，可以避免全表扫描，效率有所提高

这里的查询过程 mysql 5.6 前后的都是一致的，没什么争议



2、存在 sql 语句：

```sql
select * from user where name like “陈%" and age = 20;
```

如果是这么一条 sql 语句，那么 mysql 会如何进行查询呢？



> #### mysql 5.6 之前的版本

对于这条 sql 语句，它只会使用到联合索引的前半部分，对于 age 字段会失效，即 mysql 会根据索引树查询出满足 `陈%` 的 id

然后根据 id 再回表查询，这里存在 2 条数据，一条 age = 10，一条 age = 20，然后再根据 age 将查询出来的数据进行过滤

这个时候存在两个 id，所以需要 **回表两次**

![img](https://gitee.com/chenjiabing666/Blog-file/raw/master/%E7%B4%A2%E5%BC%95%E4%B8%8B%E6%8E%A8/1.png)

> #### mysql 5.6 及 以后的版本

很显然，在查询 name 的过程中，age 也是联合索引树的一部分，可以先通过 age 过滤掉不满足条件的数据，减少回表次数

这就是索引下推

虽然查询索引树的过程中只能使用联合索引的一部分，但是如果 where 查询条件的其他条件也包含在这个联合索引树上，那么可以在回表前根据联合索引树上的值进行过滤，减少回表次数

比如上面的 age 在查询过程中是失效的，但是可以用来过滤 查询后将要回表的数据

![img](https://gitee.com/chenjiabing666/Blog-file/raw/master/%E7%B4%A2%E5%BC%95%E4%B8%8B%E6%8E%A8/2.png)



通过 explain 查看执行计划，可以看出 extra 中出现了 Using index condition，这就是使用了索引下推 减少回表 次数的标记

![img](https://gitee.com/chenjiabing666/Blog-file/raw/master/%E7%B4%A2%E5%BC%95%E4%B8%8B%E6%8E%A8/3.png)



## 3、force index()

[force index() 提高效率的例子](https://blog.csdn.net/bruce128/article/details/46777567)

force：强制

force index() 表示强制 mysql 使用某个索引进行查询，而不让查询优化器进行分析使用哪个索引

因为查询优化器在多个索引中选择的索引可能不是最合适的，毕竟查询优化器不是完全智能的，不能满足所有的情况

因此我们当遇到慢 sql 并且还添加了索引的时候，可以自己进行试验，找到该 sql 语句最高效率的索引，强制 mysql 执行，以此来提高查询效率



默认查询下，mysql 使用 idx_a 作为索引

*![image.png](https://pic.leetcode-cn.com/1605848463-guGkla-image.png)*



使用 force index(idx_a_d) 让 mysql 使用 (a, d) 联合索引

*![image.png](https://pic.leetcode-cn.com/1605848593-gSwHqJ-image.png)*



> #### force index() 注意点

如果查询条件出现强制类型转换等会让索引失效的情况，那么 force index() 会失效

查询优化器生成的执行计划中不会使用索引，比如 a 为 varchar，而下面的查询条件出现了 隐式转换 int -> varchar，所以导致索引失效

![1605848761981](C:\Users\蒜头王八\AppData\Roaming\Typora\typora-user-images\1605848761981.png)



避免隐式转换，索引生效

*![image.png](https://pic.leetcode-cn.com/1606400847-kOaybZ-image.png)*