# Redis 持久化方式



由于 redis 的数据都存储在内存中，关机就没了，为了防止意外关机，所以需要进行持久化防止数据丢失



## 1、RDB

RDB 文件是一个二进制文件，**同时也是一个快照文件，保存的是创建 RDB 文件时的所有的 key-value 数据**

每一次 RDB 操作都是先创建 rdb.dump 文件，然后读取 redis 内存中的所有数据，然后以二进制的形式存储进这个文件当中

由于每次创建的 RDB 文件都是存储的 redis 现有的所有数据，即当前创建的 RDB 文件具有时效性，以前的 RDB 文件就过时了，因此会覆盖掉以前的 RDB 文件



### 1、创建 和 写入 RDB 文件

创建 RDB 文件有两个命令：save 和 bgsave

save 命令在创建 RDB 文件并且读取数据以及进行存储完成前 会阻塞整个进程，这段时间内不会处理任何请求，直到 RDB 文件处理完成

bgsave 命令会 fork() 一个子进程，让子进程去创建 RDB 文件，这时候父进程会照常处理请求



**需要注意的是：RDB 文件是一个快照文件，它保存的是 RDB 文件创建时刻的 redis 数据，而对于后续 RDB 数据写入过程中 父进程新写入的数据 对于 RDB 来说是不可感知的**



bgsave 执行的三个条件：

```java
save 900 1
save 300 10
save 60 10000
```

- 如果在第 900s 内，数据库经过 1 次修改，那么立即触发 bgsave
- 如果在第 300s 内，数据库经过 10 次修改，那么立即触发 bgsave
- 如果在第 60s 内，数据库经过 10000次修改，那么立即触发 bgsave

满足其中之一，bgsave 就会触发

就以 60s 来说，设定为 发生 10000 次修改就进行备份，设置为 10000这么大的数值 主要是避免在 60s 内没修改什么数据就进行 bgsave，导致发生太多次 bgsave，降低性能，同时 10000 次修改又意味着数据变动过大，所以需要进行备份





### 2、加载 RDB 文件

redis 默认设置在 redis 启动时加载 RDB 文件，即是自动加载的

但是 RDB 文件加载是有条件的：

- 只有在 AOF 持久化功能没有开启的时候，才加载 RDB 文件
- 如果 AOF 持久化功能开启了，那么优先使用 AOF 来恢复数据

![img](https://pic2.zhimg.com/80/v2-02846b978a016f3888191ac4c2a55dd0_720w.jpg)



重启加载 RDB 文件的时候，redis 是会陷入阻塞状态的，即跟 save 命令一样无法处理用户请求，因为数据都没准备好



## 2、AOF

默认情况下，AOF 持久化功能是关闭的，需要手动打开

```java
appendonly no;	//默认关闭 AOF
```

AOF 里的数据是命令，跟 RDB 直接存储的数据不一样，它存储的是执行过的写命令

AOF 一个文件就可以重复写入，即是数据追加类型的，类似 StringBuilder 的 append()



### 1、AOF 文件写入

redis 会将 每条写操作先写入到 AOF 缓冲区中，然后再刷盘，写回磁盘的 AOF 文件

因此写入到 AOF 文件中 的是 AOF 缓冲区中的数据



AOF 的写入时机由 appendfsync 变量来进行控制，appendfsync 变量有 3 种值：

- always
  - 从安全性来讲 ，这个策略的安全性最高，每当 redis 完成一次写操作 都会将该命令进行刷盘，那么就算 redis 宕机了，那么丢失的也是最近的一次写操作
  - 从效率来讲，这个策略的效率最低，因为每执行一次写操作就添加进行刷盘，因此会很占据 CPU 的资源
- everysec
  - 从安全性来讲，这个策略还算过得去，它默认每隔 1s 都会将这 1s 内的写操作命令 即 AOF 缓冲区中的数据 进行刷盘，那么就算 redis 宕机了，那么丢失的也是最近 1s 内的数据
  - 从效率来讲，这个策略比 always 要高
- no
  - 从安全性来讲，这个策略的安全性最低，redis 不会主动去将数据刷盘，即 redis 只负责写到 AOF 缓冲区，不负责刷盘，而是依赖于操作系统，一般操作系统是 30s 就将数据刷盘，那么一次丢失的就是 30s 的数据了
  - 从效率来讲，应该算是最高的

redis 中默认的 AOF 写入时机设置为 everysec，可以看作它是从安全性和效率都做了均衡（跟 定期删除 + 惰性删除一样，都是均衡）

```java
appendfsync everysec
```





### 2、载入 AOF 文件

> ### AOF 内部文件命令格式

![img](https://pic2.zhimg.com/80/v2-19c21104282b30f01baf0bcd47c167a7_720w.jpg)



> ### AOF 加载过程

当 redis 重启的时候，如果开启了 AOF 持久化功能，那么优先读取 AOF 文件

由于 AOF 文件存储的是 写命令，因此需要顺序执行 AOF 中的每一条写命令来恢复关机之前的 redis 中的数据



### 3、AOF 文件重写

> ### 为什么需要 AOF 重写

由于使用的是命令追加的方式，因此后面我们看来一条命令就能搞定的 可能会被整成多条命令，这样的话写入到 AOF 文件中的也是多条命令，比如

```java
rpush list "A"
rpush list "B"
rpush list "C"
rpush list "D"
```

对于上面这些命令，其实一条命令即可完成

```java
rpush list "A" "B" "C" "D"
```

但是用户请求就是一条一条插入的，这样导致 AOF 存储了这 4 条命令，太多的冗余命令导致 AOF 文件过大

因此，为了解决这些冗余命令导致的 AOF 文件过大问题，需要对 AOF 文件进行重写



> ### AOF 重写实现原理

AOF 文件重写并不会对现有的 AOF 进行读写，而是读取当前 redis 服务器中所有的 key-value 来实现的

即根据 当前 redis 中存在什么数据，然后就构造对应的 写命令，(基本上都是添加，而不会存在删除命令)

比如 数据库中 list 这个 key 中对应的数据有 "A" "B" "C" "D"，那么根据这些数据可以构成一条命令

```java
rpush list "A" "B" "C" "D"
```

就这样减少命令的冗余，缩减了 AOF 文件





> ### AOF 文件重写过程

如果使用当前线程进行 AOF 重写的话，那么由于 AOF 文件过大，阻塞的时间会非常长，很长时间无法用户请求，这显然是不可取的，因此采取的策略就跟 RDB 数据写入一样，fork() 一个子进程，让子进程进行 AOF 重写，这样当前进程还能继续处理请求

**需要注意的是，子进程操作的数据库数据是对原数据库数据的快照，因此后续进行的数据库数据修改子进程无法感知**

过程：

- 当前进程 fork() 创建子进程，子进程进行 AOF 重写
- 此后 当前进程所执行的所有 写操作，不仅会写入到 AOF 缓冲区，还会写入到 AOF 重写缓冲区
  - 写入到 AOF 缓冲区是为了同步现有的 AOF 文件，防止 redis 突然宕机，而 AOF 重写还没有完成，那么就会丢失现在进行操作的写命令
  - 写入到 AOF 重写缓冲区是因为子进程读取的是 redis 数据库数据的快照，所以不会有父进程 AOF 重写期间执行的写操作的数据，所以同时需要进行记录，免得漏掉了
- 当 AOF 重写完成后，子进程会给 父进程发送信号，父进程会将 AOF 重写缓冲区的数据写入到 新的 AOF 文件中，然后将新的 AOF 文件会替换掉原有的 AOF 文件



以下是重写的代码，分别对应 key 的类型，执行不同的语句

```python
def AOF_REWRITE(tmp_tile_name):

  f = create(tmp_tile_name)

  # 遍历所有数据库
  for db in redisServer.db:

    # 如果数据库为空，那么跳过这个数据库
    if db.is_empty(): continue

    # 写入 SELECT 命令，用于切换数据库
    f.write_command("SELECT " + db.number)

    # 遍历所有键
    for key in db:

      # 如果键带有过期时间，并且已经过期，那么跳过这个键
      if key.have_expire_time() and key.is_expired(): continue

      if key.type == String:

        # 用 SET key value 命令来保存字符串键

        value = get_value_from_string(key)

        f.write_command("SET " + key + value)

      elif key.type == List:

        # 用 RPUSH key item1 item2 ... itemN 命令来保存列表键

        item1, item2, ..., itemN = get_item_from_list(key)

        f.write_command("RPUSH " + key + item1 + item2 + ... + itemN)

      elif key.type == Set:

        # 用 SADD key member1 member2 ... memberN 命令来保存集合键

        member1, member2, ..., memberN = get_member_from_set(key)

        f.write_command("SADD " + key + member1 + member2 + ... + memberN)

      elif key.type == Hash:

        # 用 HMSET key field1 value1 field2 value2 ... fieldN valueN 命令来保存哈希键

        field1, value1, field2, value2, ..., fieldN, valueN =\
        get_field_and_value_from_hash(key)

        f.write_command("HMSET " + key + field1 + value1 + field2 + value2 +\
                        ... + fieldN + valueN)

      elif key.type == SortedSet:

        # 用 ZADD key score1 member1 score2 member2 ... scoreN memberN
        # 命令来保存有序集键

        score1, member1, score2, member2, ..., scoreN, memberN = \
        get_score_and_member_from_sorted_set(key)

        f.write_command("ZADD " + key + score1 + member1 + score2 + member2 +\
                        ... + scoreN + memberN)

      else:

        raise_type_error()

      # 如果键带有过期时间，那么用 EXPIREAT key time 命令来保存键的过期时间
      if key.have_expire_time():
        f.write_command("EXPIREAT " + key + key.expire_time_in_unix_timestamp())

    # 关闭文件
    f.close()
```





## 3、RDB 和 AOF 的区别

- 数据存储形式
  - RDB 文件存储的是 redis 数据库中所有的 key-value 数据，并且是二进制形式的，数据紧凑所以文件体积小，并且由于计算机底层的数据都是二进制形式的，所以读取恢复会很快
  - AOF 存储的是 redis 执行的每条写命令，恢复的时候就需要进行执行每一条命令，同时由于存储的是命令，所以会导致命令冗余，AOF 文件体积过大，故需要进行 AOF 重写
- 文件创建 和 写入
  - RDB 是需要满足 m 秒内数据库修改过 n 次才会触发 bgsave，一旦触发，那么进程就会 fork() 一个子进程创建 RDB 文件，然后将 redis 数据库中所有的 key-value 都存储到 RDB 文件中
  - AOF 是除 AOF 重写外只进行一次创建，每执行一条命令都是先写入到 AOF 缓冲区中，然后在特定时间内进行刷盘，以 append() 的形式将 AOF 缓冲区中的数据追加到 AOF 文件中的；AOF 文件重写的时候跟 RDB 创建一样，fork() 一个子进程，让子进程去处理，并且由于 子进程 处理i的数据是原数据库数据的快照，所以这段时间内父进程执行的写命令会同时写到 AOF 缓冲区 和 AOF 重写缓冲区中，让 子进程完成 AOF 文件后，将 AOF 重写缓冲区中这些新的命令写回到 新的 AOF 文件中
- 优先级
  - 在没有开启 AOF 持久化功能的情况下，redis 重启就会去执行 RDB 文件进行数据恢复
  - 如果开启了 AOF 持久化功能，如果有 AOF 文件，那么就会优先去加载 AOF 文件，如果没有，那么就去加载 RDB 文件
- 数据丢失量
  - RDB 会丢失上一次创建 RDB 文件 到 redis 关机的所有数据量
  - AOF 默认情况下会丢失 1s 内执行的所有写操作





## 4、为什么 RDB 创建 和 AOF 重写都是使用子进程和不是线程



### 1、CopyOnWrite 技术

在讲原因之前，我们需要先了解下 CopyOnWrite 技术

CopyOnWrite 简称为 COW，是一种读写分离思想的实现



比如存在一个集合，它在多线程的情况下使用，那么一般情况下对于 写操作，我们都需要进行加锁，才能保证读的时候不会出现脏数据，即存在数据一致性问题

但是，在不要求读操作读到实时性数据的情况下，可以使用 CopyOnWrite 技术，即执行写操作的时候，将集合 copy 一份出去，写操作的线程/进程 就是操作这个副本，而读操作的 线程/进程 则是操作原来的集合，这样就不需要进行加锁，读写可以同时进行

当然，需要注意的是，写操作还是需要加锁的，即一次只允许一个 线程/进程 进行写

CopyOnWrite 技术的缺点：

- 读操作在 写操作完成并且将集合进行覆盖前读到的都是旧数据，无法获取实时性数据
- 由于写操作需要 copy y一份，那么在 集合数据量大的时候，比如 200M，那么 copy 出来的副本也是 200M，那么将会相当占据内存，可以看作是 空间换时间



在 java 中 CopyOnWriteArrayList  和 CopyOnWriteMap 都是使用了 CopyOnWrite 技术实现读写分离

比如 java 实现的 CopyOnWriteArrayList 里面的 add() 和 get()，它的 add() 加锁，一次只能操作一个线程，但是对于 get()读操作来说没有什么影响

```java
public boolean add(E e) {
    final ReentrantLock lock = this.lock;
    lock.lock();
    try {
        Object[] elements = getArray();
        int len = elements.length;
        //copy 出一个副本
        Object[] newElements = Arrays.copyOf(elements, len + 1);
        newElements[len] = e;
        //将数组的指针指向副本
        setArray(newElements);
        return true;
    } finally {
        lock.unlock();
    }
}

private E get(Object[] a, int index) {
    return (E) a[index];
}
```



### 2、Linux 中的 fork()

由于 RDB 和 AOF 重写都涉及到 fork()，而且使用子进程的方式也是由于操作系统实现的 fork() 函数的特性



**Linux 中的 fork() 使用的就是 写时复制，即 COW**，但是又跟普通的 COW 不一样

我们需要先知道，数据都是以 页 的方式存储的

最开始 子进程 和 父进程 会共享所有的数据页，直到子进程 或者 父进程对某页数据发生修改的时候，子进程才会对该页进行复制



这样做的好处是**节省了内存**，父进程的所有数据假设分为了 100 页，父子进程运行过程中，被它们修改的数据页假设为 10 页，这意味着其他的 90页 中的数据内容是不会发生改变的，那么这 90页 数据 父进程和子进程就可以一起共享了，没必要在复制出一份新的来



### 3、RDB 和 AOF 重写



RDB 和 AOF 重写 中子进程所读取的都是 fork() 时候数据库数据的快照，它们的父进程这段时间都会处理请求，而修改的数据对于 它们来说是不可感知的，不过有一点不同的是，对于 AOF 重写来说，父进程会将新的写命令写入到 AOF 重写缓冲区中



**这里以 RDB 举个例子：**

RDB 在 fork() 子进程的时候，父进程和子进程共享所有的数据页，即 redis 数据库中的数据 父进程和子进程是共享

而只有在 父进程 对其中某一页数据发生修改，并且子进程还没有完成对这一页的备份的时候，子进程才会在修改前将这一页的数据复制出来

redis 的所有数据假设分为了 100 页，而在 RDB 过程中，父进程接收用户请求，会修改的数据页假设为 10 页，这意味着其他的 90页 中的数据内容是不会发生改变的，那么这 90页 数据 父进程和子进程就可以一起共享了，没必要在复制出一份新的来，因为这些页的数据子进程也只是需要读，不需要写，不会修改这些页的数据





> ### 为什么不使用线程

操作系统自己实现了 COW，并且自己管理的多么完美，能够很好的 在保存快照的情况下 节省内存

而如果 redis 实现线程级别的 COW ，大体上也是跟 java 一样需要将所有数据页进行拷贝的，因为不能深入到 操作系统中去控制使得内存共享