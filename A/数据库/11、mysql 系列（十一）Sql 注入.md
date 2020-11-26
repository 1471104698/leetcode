# Sql 注入

## 1、Sql 注入产生的原因

在我们访问页面的时候，web 服务器会向 dao 层发起 sql 查询请求，如果权限验证通过就会执行 sql 语句

这种网站内部直接发送的 sql 语句一般不会有问题，但实际情况是需要结合用户传送过来的参数来构造动态的 sql 语句

如果用户传入的是恶意 sql 代码，而 Web 服务器又没有对参数进行校验，这样的话就会导致 sql 注入



简单的sql注入攻击可以这样理解：
用户在网页上填的登录信息，实际上是会被传入后台按照算法组合成sql数据库的查询语句的。然后sql攻击就是通过填写一些有特殊符号的信息，让服务器按照sql的语法规则错误地生成了其它语句并执行的过程。要想进行攻击，实际上就是找出从表单数据到sql语句转换算法的漏洞。

## 2、Sql 注入的危害

- 能够猜测后台数据库、使用的表、字段等
- 数据库存储的信息被盗取
- 数据库被恶意操作，比如修改数据库的一些字段值
- 绕过认证



## 3、Sql 注入示例

### 1、猜测数据库

一般情况下，根据用户传参，我们执行的语句是这样的，

```sql
SELECT a, b FROM test WHERE id = '1';
```

通过用户传入的 id 进行 sql 拼接查询需要的信息

但是，如果用户在传参的时候恶意输入 `1' order by 1#`

那么实际的 sql 语句就变成

```sql
SELECT a, b FROM test WHERE id = '1' order by 1#';
```

本意上，我们想要的 sql 语句的 id 参数应该是 `1' order by 1#`

而实际上， sql 语句变成了 查询 id = 1 的数据，并且按照 第 1 列的数据进行排序

**为什么？**

因为本来数据库对参数会使用 单引号 括起来，即 `' 1' order by 1# '`，但是在数据库中 `#` 是注释的意思

这样的话，后面的单引号就会被这个 `#` 注释掉了，而传入的参数中， 1 后面有个 单引号，代替后面被注释掉的单引号跟前面的单引号进行配对，导致 后面的 order by 1 变成了 sql 的一部分，而不再是参数



有了这一步，我们就可以猜测出服务器 执行的 sql 语句 select 的有多少个字段了

通过 传参 `1' order by 1#` 和 ``1' order by 2#``，按照查询出来的第一列 和 第二列进行排序，发现都能够正常执行

而当 传参 `1' order by 3#` ，按照第三列进行排序的时候，发现提示不存在第三列



**这样的话，我们就会知道，这条 sql 语句查询出来的列数只有 2 列了**



根据这一点，可以使用 `union select` 来获取数据库信息了

union 会将联合起来的多条 select 查询出来的结果合并为一个结果集，前提是联合的 select 查询的列数相同

因此，我们知道了 sql 语句查询的列数，就可以根据这个列数 利用 union 将我们真正想要的数据合并到这个结果集中

输入`1' union select database(),user()#`进行查询 ：
- database()将会返回当前网站所使用的数据库名字.
- user()将会返回执行当前查询的用户名.

```sql
SELECT a, b FROM test WHERE id = '1' union select database(),user()#';
```

查询结果为：

```
a 			b
1			1
mybatis		root@localhost
```

这样我们就能知道当前表使用的数据库名称为 mybatis 了



同理，再传参 `1' union select version(),@@version_compile_os#` 进行查询

- version() 获取当前数据库版本.
- @@version*compile*os 获取当前操作系统。

```sql
SELECT a, b FROM test WHERE id = '1' union select version(),@@version_compile_os#';
```

查询结果为：

```sql
a 			b
1			1
5.7.12-log	Win32
```



同理，再传参 `1' union select table_name,table_schema from information_schema.tables where table_schema= 'mybatis'#` 进行查询

数据库会自己维护一个 tables 表，里面存储了所有的表名，table_name 表示表名，table_schema 表示表所在的数据库

这里的 table_schema 是来凑齐 两个列的，方便进行 union 查询

```sql
SELECT a, b FROM test WHERE id = '1' union select table_name,table_schema from information_schema.tables where table_schema= 'mybatis'#`;
```



### 2、绕过认证

假设执行认证的 sql 为：

```sql
select * from users where username = 'xxx' and password = 'xxx'
```

而用户传参的时候，传的是 `123'or 1 = 1#` 和 `123'or 1 = 1#`

这样实际执行的 sql 语句是

```sql
select * from users where username = '123'or 1 = 1#' and password = '123'or 1 = 1#'
```

`username = '123'or 1 = 1` 由于  1 = 1 必定成立，所以 `username = 123`不会起到任何作用，而后面的 password 被 `#` 注释掉了，所以 sql 语句简化为：

```sql
select * from users where 1 = 1
```

这样就直接登录成功了，没有经过任何的认证





## 4、sql 注入防止

- 避免使用 sql 拼接，尽量使用预编译，即 sql 语句已经编译好了，参数往里面填充，DB 不会再进行语法解析，而是直接执行
- 生产环境的时候，关闭 web 服务器的错误信息，比如 提示字段错误 之类的
- 对用户传入的参数中的特殊符号比如 引号 进行过滤