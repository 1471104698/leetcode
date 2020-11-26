# 求各科成绩前三的学生

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