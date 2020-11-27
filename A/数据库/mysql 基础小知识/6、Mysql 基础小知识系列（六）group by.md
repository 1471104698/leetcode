# group by



## 1、group by 实现效果

当我们 group by name 的时候，表示是将 name 相同的列聚合起来，其他列不同的整合为一个 list 集合

比如下图，name 相同的整合为一个列， id 和 number 不同的存储到该列的集合中去

 ![img](http://images.cnitblog.com/blog/639022/201501/162343319172617.jpg) 

这样我们使用聚合函数 min() max() count() 就是直接在集合中判断

注意，group by 在没有使用聚合函数的情况下，会返回**分组后每个分组中的第一行数据**，不一定是排序好的，同时如果使用了聚合函数，同样也只会返回聚合的那个函数

group by 返回的是每个分组的对应的结果，是一个结果集，而不单单只是一条数据



## 2、group by 和 order by 同时使用的坑点

如果 group by 和 order by 一起使用，**会先执行 group by，再执行 order by**，而 order by 是对 group by 后的结果进行排序

因为 group by 对于每个分组只会返回一条数据，所以 order by 是对多个分组的一条数据进行排序，而不是我们想的对 分组后的每个分组中的数据进行排序

 

这里列出 group by 和 order by 的使用场景：



**场景一：**每个人都多次收到奖金，我们要获取到每个人收到的奖金的最大值，然后将这些最大值按照降序排序

那么就可以使用 group by 按照每个人进行分组，判断使用 max() 获取每个分组的最大值，然后使用 order by 对这些最大值进行排序



**场景二：**如果我们是想要获取每个人**自己的多个奖金**的排序

不使用 group by，直接使用 order by name, salary 即可，即先对 name 进行排序，这样同个人就在同一个范围内，然后对 salary 进行排序，这样就是对同个人的 salary 进行排序



