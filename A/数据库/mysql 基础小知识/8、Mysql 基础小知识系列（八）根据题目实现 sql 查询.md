# 根据题目实现 sql 查询



## 1、求各科成绩前三的学生

[各科求前三详解](https://blog.csdn.net/qq_41933709/article/details/84928157 )



mysql 中有这么一道经典的题目：求 各科成绩前三的学生



第一印象来说，感觉应该是对 科目 进行 group by，然后对每个分组进行 order by，再对每个分组 limit 0, 3 获取前 3 名的学生 id

但是这样实现就很难受了，因为 group by 和 order by 一起使用时，order by 并不会对 分组内排序，而是对分组外排序，这样的话显然就无法直接实现 每个分组 order by



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





## 2、有一个帖子表、评论表和用户表，求出最近一个月内25岁以内男生评论的帖子，按评论数降序排序

首先我们分析，存在三张表 ，帖子表：t，评论表 p，用户表 u

每张表内属性是：

- 帖子表有主键 t.id

- 评论表有主键 p.id，每条评论属于哪个帖子 p.tid，发评论的用户 p.uid，评论的日期 p.datetime
- 用户表有主键 u.id 年龄 u.age 性别 u.sex



```sql
select t.id, (
	select count(*) from p, u
    where 
        t.tid = p.tid -- 根据 tid 锁定帖子信息，这里的 t 表是外层 select 的，而不是这里子查询声明的 t，这里也没有声明
            and 
        p.uid = u.uid -- 根据 uid 查找用户信息
            and 
        u.age <= 25 -- 限制年龄 25 岁以内
            and 
        u.sex = 1 -- 限制性别为 男
            and 
        DATE_SUB(curdate(), interval 1 month) <= p.datetime -- 限制 datetime 为一个月内
) as c 
from t
order by c desc 
limit 0, 10;
	-- 取评论数为前 x 名的帖子
```

这道题的 sql 写出来跟 `1、求前三学生` 的思路是一样的

上面这个 sql 的意思就是，外层表为 帖子表 t

在子查询中没有声明 t，而是直接使用的外层表的 t

因此我们可以当作是**对 外层表 t 的每一行数据，都进行一次子查询，**

扫描一次 评论表 p，根据 `t.tid = p.tid` 找到评论表中属于该帖子的所有评论，然后再根据找到的评论上面的 uid 定位用户信息，然后根据 其他的查询条件 判断用户信息是否满足，如果不满足，那么这条评论就直接过滤掉，如果满足那么就算是最终结果中的一条数据

这样我们利用 count() 得到 t 表中每个帖子 满足条件的评论数，然后设置别名为 c，这里的 tid 和 count() 是一一对应的，因为子查询是根据外层的 tid 来获取 count() 的。

在外层使用 order by 对 c 进行排序，即可得到最终结果



伪代码如下：

```java
//扫描帖子表
for(Data t_data : t){
    //扫描评论表，这里是不存在
    for(Data p_data : p){
        if(t_data.tid == p_data.tid){
            //扫描用户表
            for(Data u_data : u){
                if(t_data.uid == u_data.uid){
                    //继续判断 age、sex、datetime 是否满足条件
                    if(满足 where 条件){
                        //满足条件，将当前评论存储起来
                        add(p_data);
                    }else{
                        continue;
                    }
                }
            }
        }
    }
}
```

Server层 和 存储引擎层交互的伪代码：

```java
/*
这里是 Server 层
*/
boolean first_read = true;  //是否是第一次读取
while (true) {
	
    //扫描 t 表
    if (first_read) {
        first_read = false;
        //调用存储引擎接口方法获取数据
        err = index_read(...);  //调用存储引擎接口，定位到第一条符合条件的记录;
    } else {
        err = index_next(...); //调用存储引擎接口，读取下一条记录
    }
    
    if (err = 存储引擎的查询完毕信息) {
        break;  //结束查询
    }
    
    /*
    这里省略了，应该需要再调用存储引擎接口， 查询 p 表，然后将数据跟 err 数据行判断 t.tid == p.tid
    然后再调用 调用存储引擎接口， 查询 u 表，再查询 u 表的每个数据行，判断其他的 where 条件是否满足
    */
    
    if (是否符合 where 条件) {
        send_data();    //将该记录发送给客户端 或者 存储起来;
    } else {
        //跳过本记录
    }
}
```

