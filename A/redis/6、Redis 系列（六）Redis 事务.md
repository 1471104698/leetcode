# Redis 事务



## 1、redis 事务的指令

Redis 事务涉及到的几个指令：multi、exec、watch、unwatch、discard

- multi：开启事务，生成一个队列存储命令
- exec：执行队列中的命令
- watch：在事务开启前，执行监测某个 key，如果在事务中，监测的 key 发生了修改，那么将撤销队列中的所有命令
  - 被监视的 key 保存在一个链表中，如果某个 key 被修改了，那么会打上标志，这样在事务中如果存在对这个 key 的操作，那么整个事务无效
- unwatch：事务过程中取消监测某个 key，如果执行了 exec 或者 discard 那么就无需执行了，因为这两个命令执行过后，所有监测的 key 都会被撤销
  - unwatch 只能在事务外使用才有效
- discard：撤销掉队列中所有的命令

![img](https://picb.zhimg.com/v2-ae700baae000720fd058cdb033773275_b.jpg)



## 2、redis 事务执行阶段

Redis 事务分为以下几个阶段：

- 开启事务（multi）
- 命令入队
- 执行事务（exec）、撤销命令（discard）

因此当我们使用 multi 开启事务，后面单单只是将命令入队，而并不是去执行它，只有在最后执行 exec 才会按照顺序去执行这些命令





## 3、redis 事务 命令错误类型

> ### 语法错误

```java
127.0.0.1:6379> multi	//开启事务
OK
127.0.0.1:6379> set uuu 2	//设置 字符串 uuu 的值为  2
QUEUED
127.0.0.1:6379> holy shit	//错误语法
(error) ERR unknown command `holy`, with args beginning with: `shit`, 
127.0.0.1:6379> set uuu 1	//设置字符串 uuu 的值为 1
QUEUED
127.0.0.1:6379> exec		//执行事务
(error) EXECABORT Transaction discarded because of previous errors.

```

当命令入队，如果出现语法错误，这个错误的命令不会入队，后续正确的命令也会入队

但是最终 exec 命令执行队列中的命令的时候就会报错，并且队列中正确的命令也不执行

**即只要队列中出现一条语法错误的命令，那么整个队列的命令都不会执行**



> ### 类型错误

```java
127.0.0.1:6379> multi		//开启事务
OK
127.0.0.1:6379> set uuu 2	//设置 字符串 uuu 的值为 2
QUEUED
127.0.0.1:6379> lpush uuu 3 4	//在 列表 uuu 中插入两个元素 3 和 4（这里实际就是类型不对，因为 uuu 是）
QUEUED
127.0.0.1:6379> set u 1		//设置 字符串 u 的值为 1
QUEUED
127.0.0.1:6379> exec		//执行事务
1) OK
2) (error) WRONGTYPE Operation against a key holding the wrong kind of value
3) OK
127.0.0.1:6379> get uuu		//字符串 uuu 的值是设置成功的
"2"
127.0.0.1:6379> get u		//字符串 u 的值设置成功
"1"

```

命令入队过程中，即时操作的 key 的类型不对劲同样也会入队，因为 redis 并没有在命令入队的时候去检测 key 类型

当执行 exec 命令的时候，发现命令操作的类型不对时会报错，但是不会进行回滚，其他正确的命令也会执行



**为什么 redis 事务不支持回滚？**

因为 redis 的开发者认为，这两种错误都是在开发的时候可预见的，是开发者需要自己处理的

并且为了 redis 的性能，所以忽略了事务回滚



## 4、redis 事务的特性

redis事务 实现了

- 隔离性
- 持久性（RDB、AOF 实现）
- 自己认为的原子性

redis 由于是一个非关系型数据库，所以它没有实现 mysql 这种关系型数据库的原子性：要么都正确执行，要么都不执行

redis 实现的是它自己认为的原子性：要么都执行（exec），要么都不执行（discard）