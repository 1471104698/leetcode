# redis 分布式锁

具体看    https://juejin.im/post/6844904082860146695 

## 1、setnx + expire

分布式锁的实现 就目前最常听到的就是  setnx + expire 命令了

但不是指 setnx key value + expire key seconds 两条命令，这样的话是没有原子性的

而是使用 set 命令，至此，set 命令已经延伸出很多的参数了

` SET key value [EX seconds|PX milliseconds] [NX|XX] [KEEPTTL] `



set 已经能够完全替代 setnx 命令了，并且能够将 setnx + expires 两条指令的功能组合为一条指令

```java
127.0.0.1:6379> set a 2 EX 30 NX		//setnx + expires
OK
127.0.0.1:6379> ttl a					//查看过期时间
(integer) 27
127.0.0.1:6379> set a 2 EX 30 NX		//a 键已经存在，因此没有操作，返回 null
(nil)

```

set a 2 EX 30 NX 解读：

- set a 2 表示设置 key = a 的值为 value = 2

- EX 30 表示设置过期时间为 30s 

- NX 表示如果不存在就设置，存在则不设置



> ### setnx + expire 的问题

如果不设置超时时间，那么一旦获取锁的进程崩了，那么这个锁将无法得到释放

所以需要设置超时时间



如果设置了超时时间，如果进程 A 获取了锁，设置超时时间为 30s，那么如果进程 A 设置的超时时间过了，而自己的数据还没有处理完，因此锁自己释放了，尔后进程 B 获取了锁，设置超时时间为 30s，然后进程 A 处理完了，执行释放锁的逻辑，导致进程 B 新获取的锁被进程 A 释放了，这样的话进程 C 获取了锁，进程 B 又释放了进程 C 的锁。。。无限套娃，这把锁就起不到任何作用



因此使用 setnx 获取锁的时候，不仅仅需要使用 key，还需要使用 value，每个进程获取锁前都设置一个属于自己的唯一 value，一般使用 uuid，setnx 获取锁的时候将 key - value 进行映射，这样在释放锁的时候，可以先获取 value 判断当前这把锁是不是还是自己的，如果是则释放，如果不是表示已经超时自动释放了，那么就不管了

具体实现如下：

```java
String uuid = xxxx;
// 伪代码，具体实现看项目中用的连接工具
// 有的提供的方法名为set 有的叫setIfAbsent
set Test uuid NX PX 3000
try{
// biz handle....
} finally {
    // unlock
    if(uuid.equals(redisTool.get('Test')){
        redisTool.del('Test');
    }
}
```



但是上面的实现还是有问题的，**在 finally 中 get() 和 del() 不是原子操作，因此可能存在线程安全问题**，比如线程 A 执行完 get() 后发现 锁的 value 是自己的，那么就会进入到 if() 结构体内，但是这时候发生 CPU 切换，同时锁超时释放了，线程 B 获取锁，然后再次发生线程切换，进程 A 执行 del() 命令（del() 只是操作 key，它不会去看 value），将线程 B 设置的 key 给删除了，导致将 线程 B 新获取的锁给释放了，这样的话线程 B 就相当于没有锁了，这把锁又没用了



因此，真正保证线程安全的分布式锁的，需要使用 lua 脚本



## 2、lua 脚本（Redission）

lua 脚本是使用 redis 的 eval 命令来执行

无论 lua 脚本内部多少指令，它都是通过一条 eval 命令去执行的，内部自动保证原子性

```lua
-- lua删除锁：
-- KEYS和ARGV分别是以集合方式传入的参数，对应上文的Test和uuid。
-- 如果对应的value等于传入的uuid。
if redis.call('get', KEYS[1]) == ARGV[1] 
    then 
	-- 执行删除操作
        return redis.call('del', KEYS[1]) 
    else 
	-- 不成功，返回0
        return 0 
end
```



Redis 自己封装了一个 Redission 客户端，它内部使用 ReentrantLock 来实现分布式锁，不过加锁解锁使用的都是 lua 脚本

由于使用了 ReentrantLock ，那么自然能够实现重入锁





> ### 锁的重入

具体看  https://www.jianshu.com/p/a8b3473f9c24 



我们可以推测一下，Redission 是在重写的 tryAcquire() 和 tryRelease() 中使用 lua 脚本 执行加锁和解锁

关于 Redission 对锁的重入，个人推测：步骤如下

- 通过 lua 脚本执行 setnx 尝试获取锁，
- 如果失败了，表示已经有线程获取了锁，那么使用 lua 脚本执行 get() 获取对应的 value，跟当前线程的 value(uuid) 进行比较
- 如果是同一个，那么将锁的重入度 count +1

在上面，所有的 redis 指令都应该是在同一个 lua 脚本的

```java
// 4.使用 EVAL 命令执行 Lua 脚本获取锁
return commandExecutor.evalWriteAsync(getName(), LongCodec.INSTANCE, command,
                                      //exists 命令，查询的是所有类型的 key，存在返回 1，否则返回 0
                                      "if (redis.call('exists', KEYS[1]) == 0) then " +
                                      //设置 key 下 field 的 value = 1
                                      "redis.call('hset', KEYS[1], ARGV[2], 1); " +
                                      //重新设置过期时间
                                      "redis.call('pexpire', KEYS[1], ARGV[1]); " +
                                      "return nil; " +
                                      "end; " +
                                      //hexists 命令，判断 hash 下某个 key field 是否存在
                                      "if (redis.call('hexists', KEYS[1], ARGV[2]) == 1) then " +
                                      //将 key field 的 value + 1，即重入度 +1
                                      "redis.call('hincrby', KEYS[1], ARGV[2], 1); " +
                                      //重新设置过期时间
                                      "redis.call('pexpire', KEYS[1], ARGV[1]); " +
                                      "return nil; " +
                                      "end; " +
                                      "return redis.call('pttl', KEYS[1]);",
```



我们可以看出，redis 分布式锁需要维护两个 key，

一个是分布式锁 和 持有者的 key - value，一个是分布式锁对应的重入度 count 以及对应值的 key - value

但其实这里可以进行简化，这里相当于存在三个变量，一个是分布式锁的 key，一个是持有锁的线程 uuid，一个重入度 count

这样的话我们就可以 使用一个 hash 来存储了，hset key field value