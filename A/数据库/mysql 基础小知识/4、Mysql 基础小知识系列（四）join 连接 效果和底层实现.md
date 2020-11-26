# join 连接 效果和底层实现



我们分成两部分来讲解 join，分别是 实现效果 和 底层实现，根据实现效果能够更加容易了解底层实现，了解底层实现就更加容易优化 sql

## 1、实现效果





###  1.1、inner join

内连接，效果类似 where

 ![img](https://pic4.zhimg.com/v2-a30dcd91fe73eebb27feee0e35a91c2f_b.png) 



### 1.2、left join

左连接，对于 on 的条件，左边表会列出所有的数据，右边表如果有匹配的那么就进行拼接，没有的就忽略，显示为 null

 ![img](https://pic3.zhimg.com/v2-b6f2cddd37986e542a346241638de676_b.png) 



### 1.3、right join

右连接，跟 left join 反过来

 ![img](https://pic2.zhimg.com/v2-300cd485334edcfcbd7647427cdf1671_b.png)

 

## 2、底层实现



[join 底层三种实现 方式讲解](https://zhuanlan.zhihu.com/p/54275505)





### 2.1、Simple Nested-Loop Join（ 简单嵌套循环连接 ）

比如 tableA join tableB，这种匹配方法是将 遍历 tableA 的每一条数据，然后根据查询条件，在 tableB 中找到满足条件的数据行

如果 tableA 有 100 条数据，tableB 有 200 条数据，那么数据行总的遍历次数为 100 * 200 = 20000



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





### 2.2、Index Nested-Loop Join（ 索引嵌套循环连接 ）

很显然， Index 就是索引，即在索引树中查找，在上面那个暴力方法中，我们每次都是遍历 tableB 整张表，但实际上我们要找的就是 d 字段满足某个值的数据行

因此我们可以对 tableB 的 d 建立索引，这样的话，遍历 tableB 就不需要直接遍历整张表，而是遍历索引树，使用二分查找，相比暴力匹配 要 高效得多



以下是 右表 level 需要对 user_id 建立索引，左表 user_info 不需要，因为 join 中左表是驱动表，需要全部扫描一遍

而 右边是匹配表，只需要找出匹配的数据行即可，因此可以简化，不需要全部扫描

 ![img](https://pic4.zhimg.com/v2-c8790aa879ca6fedb83d529558bb40e3_b.jpg) 



但是这种方法**只适用于添加了索引的字段**，但实际上我们使用 join 的时候，条件并不全是添加了索引的，并且在关联多个条件的情况下，不会保证 on 的条件 是 联合索引，这样就退化成了第一种方法，但是显然不能这么做，因此需要一种在没有索引的情况下更加高效的匹配方法



### 2.3、Block Nested-Loop Join（ 缓存块嵌套循环连接 ）

BNLJ 是 基于 SNLJ 优化的，SNLJ 过于暴力，效率太低

BNLJ 加入了 缓存 join buffer，每次查询不再是获取外层表的一条数据，而是一次性缓存外层表的多条数据，然后遍历内层表，跟 缓存 中的数据进行匹配

嗯？好像也没有减少比较次数啊。。。

外层表 100 条数据，内层表 100 条数据

使用 SNLJ，循环比较次数为 100 * 100

使用 BNLJ，缓存一次存储 20 条数据，这样就相当于从 两层循环变成三层循环，那么就是 100/20 * 20 * 100 = 100 * 100

好像使用 SNLJ 和 BNLJ 循环次数是一样的啊，没什么差别



实际上 BNLJ 涉及到两部分的优化：

1. 缓存使用的是 HashTable：这样的话可以根据 key 排除掉一些数据，然后遍历 目标 key 的链表，能够减少比较次数
2. 减少内层表的扫描次数，减少磁盘 IO：由于实际场景表的数据过大，不可能全部装入到内存中，肯定是需要频繁进行磁盘 IO 的，如果使用 SNLJ，那么 对 外层表的每一条数据，内层表都需要进行一次扫描，磁盘指针每次都需要从表尾定位到表头再开始扫描；但是使用 BNLJ，由于一次缓存了 外层表的多条数据，内层表的一条数据可以同时跟外层表的多条数据进行比较，那么就相当于是减少了内层表的扫描次数，从而减少磁盘 IO，要知道磁盘 IO 效率贼低，能减少就尽量减少

<img src="https://pic3.zhimg.com/80/v2-0e81dd7fe538f67559bc24c0a5a3207e_720w.jpg" style="zoom:150%;" />





## 3、小表驱动大表

在学习了上面的 join 底层实现后，我们进一步学习 **如何优化 join**

使用 join 要记录一句话：**永远要 小表 驱动 大表**

当我们使用以下 sql 时

```sql
select * from a inner join b on a.id = b.id
```

我们是直接指定了 a 表 驱动 b 表，即我们获取 a 的每条记录，然后扫描 b 表的所有记录来跟 a 的每条记录进行匹配

假设 a 表有 1000 条记录，b 表有 20 条记录

那么我们 使用 SNLJ 就需要扫描 b 表 1000 遍，当然，使用 BNLJ 可能优化为扫描 b 表 100 遍



但是，如果换过来，将 sql 改成：

```sql
select * from b inner join a on a.id = b.id
```

这样就是 b 表驱动 a 表，扫描 b 表的每条记录跟 a 表进行匹配

这样反过来扫描 a 表只需要 20 次，经过 BNLJ 优化可能只需要 2 次

虽然总的比较次数不变，但是减少了 内层表的扫描，还是提高了效率的，并且如果被驱动表无法一次装入缓存的话，那么是可以在 BNLJ 的基础上又再次减少了 扫描被驱动表 所需的 磁盘 IO