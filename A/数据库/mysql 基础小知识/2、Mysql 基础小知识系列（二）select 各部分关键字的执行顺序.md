# select 各部分关键字的执行顺序





```sql
FROM ->
WHERE -> 
GROUP BY -> 
COUNT ->
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



