# select 执行流程





select 语句的大致

执行流程：

![img](https://img2018.cnblogs.com/blog/1446270/201910/1446270-20191006025827107-243871764.png)

  

流程包括以下几个步骤：

- 服务器连接数据库
- 查询缓存
- SQL 解析
- 查询优化器
- 执行计划



## 1、服务器连接数据库

最开始，服务器会通过我们在 spring.yml 中指定的 user 和 password 跟 数据库的连接器 建立连接，当数据库身份校验成功后，服务器 和 数据库就可以创建 connection 进行数据通信了

**服务器和数据库之间的 connection 使用的是 TCP 连接**，所以为了防止 无限制的建立连接 并且 频繁的创建和销毁连接，所以使用数据库连接池 管理和复用连接（参考线程池），因此数据库并发度是看数据库连接池的连接数量的



数据库和服务器 的 TCP 连接是 **半双工** 的，同一时间只能有一方发送数据，一方接收数据，不能同时进行



## 2、查询缓存

当建立好 connection 连接后，可以执行 select 语句请求数据库进行查询

**Mysql 内部维护了一个哈希表 作为缓存区，将 select 语句作为 key，将查询结果作为 value**

Mysql 在获取 select 语句后，不会直接去解析执行这个 sql，而是会查看内部缓存，看看之前是否执行过这条 sql 语句

如果执行过，那么通过 select 语句可以获取到查询的数据

如果没有执行过，获取执行过但表被修改过了缓存清空了，那么就会去解析执行 sql



但一般不建议使用缓存，因为如果表需要频繁的修改，那么每次 select 查询完，将查询结果放入到缓冲中，还没有使用到缓存时该表就被更新了，那么会导致缓存中关于该表的缓存数据全部失效。**对于频繁更新的表，缓存的命中率非常的低**，因此缓存的作用几乎为零，同时还需要耗费资源去处理缓存，得不偿失

只有在不需要频繁更新的表上使用缓存才有意义，但是在 Mysql 8.0 上删除了缓存这一功能，Mysql 设计者也觉得这个功能鸡肋



当查询缓存无果后，会进入到 sql 解析的步骤，这里涉及到 语法解析器 和 预处理器



## 3、SQL 解析：语法解析器 和 预处理器

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





## 4、查询优化器

当 SQL 解析完成后，查询优化器 会收到解析出来的 **语法树**，此时的语法树是合法的

一条 sql 存在不同的执行方案，执行顺序不同但是最终结果相同，然后效率也不同，比如 使用索引 和 不使用索引最终的结果相同，但是使用了索引的效率 一般比 不使用索引的效率高

查询优化器的作用就是根据 语法树 来找出最佳的执行计划，然后将执行计划推送给执行器执行，**最终选定的执行计划就是我们 explain 可以看到的**



## 5、执行计划

执行器 收到从 查询优化器 发送过来的执行计划，此时就会按照执行计划一步步执行

最后，mysql 会将查询结果返回

