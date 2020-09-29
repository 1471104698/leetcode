# Redis 底层数据模型

## 1、使用 redis 的原因，以及 redis 的特点

> ### 为什么使用 缓存？

对于某些热点数据，如果不使用缓存，那么每次请求都打到数据库上，对于并发量超高的情况，数据库根据处理不过，可能会导致数据库服务崩溃，同时数据库查询需要 IO 操作，对于 redis 这种基于内存的操作来说，时间效率上完全不存在可比性

redis 主要是当作缓存中间件

它速度快，完全是基于内存的（关机数据就消失了，所以需要持久化成文件），使用 C 语言实现，并且是单线程的，避免了多线程频繁上下文切换的开销以及竞争，网络层方面 使用 epoll IO 多路复用模型 解决单线程处理多连接问题，

**注意：单线程仅仅是说在网络请求上使用单线程处理用户请求，像持久化这种是会另外开一个线程去做的**



```java
redis 作者说 redis 的瓶颈不在 CPU，即，而是在网络 IO 或者 机器内存上，即电脑的 CPU 单线程足以支持处理速度
```



> ### redis 快的原因

- 完全基于内存操作，不需要进行 磁盘 IO，因为 磁盘 IO 比 内存操作的时间要高出很大的数量级
- 单线程，避免了频繁的线程上下文切换和锁的竞争（比如 dict 不够用进行 rehash 的时候，无需加锁，因为不存在别的线程）
- 网络层 使用 epoll 模型
- 高效的数据结构 + 合理的编码格式，redis 内部自己实现的数据结构，简单而且效率高，根据数据的情况会进行转变



> ###  为什么命令执行使用单线程就快了

如果是多线程的，那么需要线程上下文切换

 一个CPU主频是 2.6GHz，这意味着每秒可以执行：2.6*10^9 个指令，那么每个指令的时间大概是0.38ns！

而一次上下文切换，将近需要耗时2000ns！而这个时间内，CPU什么都干不了，只是做了保存上下文都动作！

同时，如果是多线程执行的话，那么防止共享资源的数据错乱，所以又需要加锁，又会产生锁的争夺



> ### redis 的瓶颈

具体看： https://www.cnblogs.com/aspirant/p/11704530.html 



redis 的瓶颈不是 CPU，因为 CPU 的速度已经足够支撑命令的执行了

redis 的瓶颈在于 内存 和 网络带宽

内存这个不用讲，而网络带宽需要知道 redis 客户端 和 服务端通信的过程

一般 redis 客户端是在我们自己运行的服务器上调用的，而 redis 服务端则是在另外一台机器

redis 执行一条命令的过程：发送命令、命令排队（由于是单线程）、执行命令、返回结果

而发送命令 和 返回结果 都是需要在网路上传输的，即需要 TCP 等，这里就有个 RTT（往返时间）

如果 我们的服务器在 上海， redis 服务器在北京， 两地直线距离约为1300公里，那么1次RTT时间=1300×2/（300000×2/3）=13毫秒（光在真空中传输速度为每秒30万公里，这里假设光纤为光速的2/3），那么客户端在1秒内大约只能执行80次左右的命令，这就和Redis的高并发高吞吐特性背道而驰啦！所以一般情况下，都是就近部署！ 





## 2、redis 数据结构



### 1、自定义数据结构：redisObject

redis 内部底层是如何描述 5 种数据类型的？  使用一个数据结构：redisObject

![img](https://picb.zhimg.com/80/v2-92afb6f1dd844e640fe40c242dede27d_720w.jpg)

```C
/*
 * Redis 对象
 */
typedef struct redisObject {

    // 数据类型，指代这 五种基本数据类型
    unsigned type:4;

    // 编码方式，指代对应数据类型的编码方式，比如 string 就是 int、raw、embstr
    unsigned encoding:4;

    // 指向对象的值
    void *ptr;
    
    //...

} robj;
```



比如我们存储了一个 string = "abc"

```properties
redisObject = {
    type:string, 
    encoding:embstr,
    ...}
```



### 2、string 底层实现（int、SDS(ebmstr、raw)）

string 主要命令行：

```java
set key value
```



string 总共有 3 种编码方式，分别是 int、raw、embstr



int：假如存储的数据可以转换为整数，比如 set num 123，那么使用的编码方式就是 int，ptr 指针直接保存 123

*![image.png](https://pic.leetcode-cn.com/1600176102-lmPjgU-image.png)*



当不能转换为整数的时候，encoding 编码会使用  embstr 和 raw，都是使用 SDS 实现的

当数据长度 < 39 字节时，使用 embstr，并且只需要一次内存分配，redisObject 和 SDS 内存连续

当数据长度 >= 39 字节时，使用 raw，需要两次内存分配，一次分配 redisObject，一次分配 SDS



### 3、hash 底层实现（ziplist、dict）

hash 主要命令行

```java
hashset key field value;
```



hash 底层是通过 ziplist 和 HashTable 实现的



**使用 ziplist 的条件：**

- 哈希对象保存的所有键值的字符串长度小于64字节；

- 哈希对象保存的键值对数量小于512个；



**如何存储 key - value？**

使用两个相邻的 Entry，前一个存储 key，后一个存储 value，新插入的 key-value 存储在 ziplist 尾部



**使用 ziplist 的优点及缺点：**

由于 ziplist 内存空间是连续的，查找效率还可以，因为不存储指针所以很节省内存

但是当数据量大的时候，查询效率就会很差，并且可能出现连锁更新，因此会转换为 dict



### 4、list 底层实现（ziplist、linkedlist）



list 可以用作双向链表，也可以用作（阻塞）队列

它的实现由 ziplist 和 linkedlist



- 最开始使用 ziplist
- 由于 ziplist 数据量大时查询增删慢，并且可能出现连锁更新，所以需要转换为 linkedlsit，它能够进行前后插入没有什么风险，并且都是 O(1)
- ziplist 节省内存，linkedlist 需要存储前后指针，所以比较耗费内存



### 5、set 底层实现（intset、dict）

set 主要命令

```java
hset key value
```



set 是无序集合，底层使用 intset 和 HashTable 来实现

数据量小的时候使用 intset，当数据量大 或者 出现非整数时，会转换为 dict 编码



set 使用 HashTable 很特别，**将 value 作为 Entry 的 key，将 null 作为 Entry 的 value**

即当我们使用 hset key value 的时候，redis 会获取 key 对应的 HashTable，然后插入 Entry = {value, null}



> ### 存储过程

首先判断 对应的 set 是否存在，这个 set 是 redisObject，它的 ptr 指向数据存储集合

如果不存在，创建一个新的 set，否则直接获取已经存在的 set

判断 set.encoding，如果是 HT，那么使用 Entry 形式插入，如果是 intset，那么判断要插入的数据类型，如果是整数，那么直接插入，如果不是整数，那么将 intset 转换为 HT，再以 Entry 形式插入

```C
void saddCommand(redisClient *c) {
    robj *set;
    int j, added = 0;
    // 取出集合对象
    set = lookupKeyWrite(c->db,c->argv[1]);

    // 对象不存在，创建一个新的，并将它关联到数据库
    if (set == NULL) {
        set = setTypeCreate(c->argv[2]);
        dbAdd(c->db,c->argv[1],set);
    // 对象存在，检查类型
    } else {
        if (set->type != REDIS_SET) {
            addReply(c,shared.wrongtypeerr);
            return;
        }
    }
    // 将所有输入元素添加到集合中
    for (j = 2; j < c->argc; j++) {
        c->argv[j] = tryObjectEncoding(c->argv[j]);
        //调用下面的方法，插入数据
        setTypeAdd(set,c->argv[j]);
    }
}


/*
 * 添加成功返回 1 ，如果元素已经存在，返回 0 。
 */
int setTypeAdd(robj *subject, robj *value) {
    long long llval;

    // set 的 encoding 是 HT
    if (subject->encoding == REDIS_ENCODING_HT) {
        // 将 value 作为键， NULL 作为值，将元素添加到字典中
        if (dictAdd(subject->ptr,value,NULL) == DICT_OK) {
            incrRefCount(value);
            return 1;
        }

    // set 的 encoding 是 intset
    } else if (subject->encoding == REDIS_ENCODING_INTSET) {
        
        // 如果对象的值可以编码为整数的话，那么将对象的值添加到 intset 中
        if (isObjectRepresentableAsLongLong(value,&llval) == REDIS_OK) {
            uint8_t success = 0;
            subject->ptr = intsetAdd(subject->ptr,llval,&success);
            if (success) {
                // 添加成功
                // 检查集合在添加新元素之后是否需要转换为字典
                // #define REDIS_SET_MAX_INTSET_ENTRIES 512
                if (intsetLen(subject->ptr) > server.set_max_intset_entries)
                    setTypeConvert(subject,REDIS_ENCODING_HT);
                return 1;
            }

        // 如果对象的值不能编码为整数，那么将集合从 intset 编码转换为 HT 编码
        // 然后再执行添加操作
        } else {
            setTypeConvert(subject,REDIS_ENCODING_HT);

            redisAssertWithInfo(NULL,value,dictAdd(subject->ptr,value,NULL) == DICT_OK);
            incrRefCount(value);
            return 1;
        }

    }

    // 添加失败，元素已经存在
    return 0;
}
```



### 6、zset 底层实现（ziplist、skiplist + dict）

zset 主要命令行

```java
zadd key score1 member1 score2 member2 ...	//在 key 集合中添加成员，权重为 socre，成员名称为 member
zrange key left right	//按照 score 从 [left, right] 范围查询
```



zset 底层实现有 ziplist、 skiplist 与 dict 的组合



> ### ziplist

![img](https://upload-images.jianshu.io/upload_images/6990035-4d859c25df76393e.png?imageMogr2/auto-orient/strip|imageView2/2/w/771/format/webp)

由于 zset 存在成员变量 和 权重，因此 ziplist 使用了两个相邻的 Entry 在逻辑上作为一个节点

比如第一个节点为 member1 ，第二个节点为 member1 的 权重 score1

第三个节点为 member2 ，第四个节点为 member1 的 权重 score1

节点之间排序是按照 score 从小到大进行排序的，因此可以进行范围查询



节省内存，但是数据量大的时候就不太行了，同样是查询效率慢 + 连锁更新问题 ，需要转换为  skiplist + dict



> ### skiplist（跳表）+ dict



zset 数据结构

```C
typedef struct zset{
     //跳跃表
     zskiplist *zsl;
     //字典
     dict *dice;
} zset;
```

**跳表存储所有的数据，并且按照 score 进行排序，dict 存储 member 和 score 之间的映射**



使用跳表可以在 O(logN) 之间完成范围查询，而当单值查询的时候，可以使用 HT，在 O(1) 时间内即可完成查询

```java
//值得一提的是，跳表 和 HT 的节点之间是共用的，这样就节省了内存
```



节点之间按照 score 进行排序

分为很多层，最下面一层包含了所有的节点，可以进行范围查询



**跳表数据更新：**

插入和删除很简单，插入和删除后需要更新对应的层数，但是对于更新，score 的变化会带来影响

如果更新后的 score 节点的排序不变，那么直接修改 score 即可

如果更新后的  score 节点位置需要发生变化，那么应该怎么做呢？ redis 的做法是先将要更新的节点删除，然后更新 score，然后再插入到 skiplist 中



**元素排名计算：**

redis 在 forword 指针（相当于 next 节点）上添加了一个字段 span，表示跨度，即从当前层的上一个节点跳到当前节点需要跨过多少个节点，这样到达目标节点，我们只需要记录 搜索路径 上所有的 span 之和即可

```C
typedef struct zskiplistNode {
    // member 对象
    robj *obj;
    // 分值
    double score;
    // 后退指针
    struct zskiplistNode *backward;
    // 层
    struct zskiplistLevel {
        // 前进指针
        struct zskiplistNode *forward;
        // 这个层跨越的节点数量
        unsigned int span;
    } level[];
} zskiplistNode;

//forward 指针转换为 java 语言就是
class Node{
    Node next;
    int span;
}
```



**如果 skiplist 中所有的 score 都一样，那么查询 给定某个 member 和对应的 score 获取排名是否会退化为 O(n):**

不会，因为 redis 中的 skiplist 排序不仅仅是依据 score，还会根据 member 的比较，如果 score 相等，那么就会比较 member

因此可以通过 比较 member 来获取节点，同样可以看作是有序的

比如下面的代码，可以看出是 score 相同，就比较 member 的大小

```java
unsigned long zslGetRank(zskiplist *zsl, double score, robj *o) {
    zskiplistNode *x;
    unsigned long rank = 0;
    int i;

    x = zsl->header;
     /*循环遍历并累加每层的span值, 获取总的排名*/
    for (i = zsl->level-1; i >= 0; i--) {
        while (x->level[i].forward &&
            (x->level[i].forward->score < score ||
             //如果 score 相等，那么比较 member 的大小
                (x->level[i].forward->score == score &&
                compareStringObjects(x->level[i].forward->obj,o) <= 0))) {
            rank += x->level[i].span;
            x = x->level[i].forward;
        }

        if (x->obj && equalStringObjects(x->obj,o)) {
            return rank;
        }
    }
    return 0;
}
```





## 3、redis 底层数据结构

### 1、SDS



对于 string 类型，如果数据是字符串，那么如果字符串长度 小于等于 39 个字节，编码方式为 embstr，大于 39 个字节，编码方式为 raw

底层都是使用 SDS（简单动态字符串） 来实现的

SDS 有 3 个变量，buf 是用来存储字节数据的，len 和 free 记录 buf 的使用情况

```java
class SDS{
	//已用长度	
    int len;
    //可用长度
    int free;
    //字节数组
    char[] buf;
}
```



当我们 set 数据的时候

如果字符串长度小于等于 39 个字节，那么就是 embstr，它的 redisObject 和 SDS 是一次分配完成的，即内存空间是连续的

如果字符串长度大于 39 个字节，那么就是两次内存分配，一次分配 redisObject，一个分配给 SDS，并将 ptr 指向 SDS

![img](https://xiaoyue26.github.io/images/2019-01/raw.png)

![img](https://xiaoyue26.github.io/images/2019-01/embstr.png)



> ### 为什么 raw 和 embstr 默认界限是 39

```C
#define LRU_BITS 24
typedef struct redisObject {    // redis对象
    unsigned type:4;    // 类型,4bit
    unsigned encoding:4;    // 编码,4bit
    unsigned lru:LRU_BITS;  // 24bit
    int refcount;   // 引用计数 4B
    void *ptr;  // 指向各种基础类型的指针 8B
} robj;
typedef struct sdshdr {
    unsigned int len;	//4B
    unsigned int free;	//4B
    char buf[];			//不存储数据时为 0，开辟存储数据时，存在一个 1个字节存储 字符 "\0" 作为结束符
}sds;
```

我们可以看出 redisObject 占 16 个字节 (16B)

4bit + 4bit + 24bit + 4B + 8B = (32bit / 8)B + 12B = 16B

而 sds 的 buf 在没有数据情况下， len 和 free 占了 4B + 4B = 8B

而 redis 底层使用的 jemalloc 分配器每次分配默认都是 8、16、32、64 等字节

而 embstr 是一次分配完成的，即 redisObject 和 sds 是内存地址连续的

如果一次分配 32 个字节，那么剩下的 buf 可存储数据为 ：32B - 16B - 8B - 1B（"\0" 结束符） = 7B，这样只能存储长度为 7B 的字符串，这显然太小了，因此选择了一次分配 64B

那么 buf 可存储数据为 64B - 16B - 8B - 1B（"\0" 结束符） = 39B，因此 39B 就作为了 embstr 和 raw 的分界线



后续版本改成了 44B，主要应该是在 len 和 free 上动手脚，因为 len 和 free 都 4 个字节，即 32 位，还是无符号的，那么表示最大值为 40多亿，而 buf 最长才 39，完全不是一个数量级的，所以可以对 len 和 free 进行缩减



> ### SDS 和 C 语言字符串的差别

SDS 跟 C 语言字符串的存储有很大的不同：

- SDS 内部维护了数组长度，而 C 语言没有，各自查询数组长度的时间复杂度为 O(1) 和 O(n)
- SDS 和 C 的字符串数据都是以 字符 '\0' 结尾的，但是 C 是二进制不安全的，假设数据为 "abc\0 def\0"，它读取到第一个 '\0' 的时候就会退出了，会忽略后面的 def，而 SDS 不会，因为它维护了一个 len 字段，读取数据不是按照 '\0' 来结束读取的，而是按照 len 长度，因此 SDS 是二进制安全，既 SDS 可以存储 图片、音频等文件，因为里面会存在很多的 '\0'，而 C 不行
- SDS 存在 预分配 和 惰性空间释放 ，预分配就是每次分配的 buf 会比实际需要的更长，减少字符串增长是带来的重新分配次数，惰性空间释放 就是字符串缩短时，不会立即回收不需要的空间，而是记录在 free 字段里，后面不需要再释放
- 避免缓冲区溢出（数据覆盖）：对于 C ，如果在内存中存在两个相邻的字符串 s1 和 s2，如果对 s1 进行扩容的时候，由于没有重新进行内存分配，因此扩容的部分会将 s2 的数据覆盖掉，而 SDS 不会，它会根据 len 和 free 检查空间是否足够，如果不足，那么进行内存分配，防止数据覆盖



空间预分配：

- 若修改之后 sds 长度小于1MB,则多分配现有len长度的空间
- 若修改之后sds长度大于等于1MB，则扩充除了满足修改之后的长度外，额外多1MB空间

![img](https://i6448038.github.io/img/redis-data-struct/sds.gif)

惰性空间释放：

为了避免内存重新分配，缩短字符串后不会立马释放空间，可能后续字符串添加会使用到

![img](https://i6448038.github.io/img/redis-data-struct/sds_free.gif)





### 2、ziplist



 ziplist 是压缩链表，是**一段连续的内存空间**，它省略了双向链表的 pre 和 next 指针

仅仅使用地址偏移量来获取前后节点位置，只需要在当前位置 +/- 某个偏移量就可以定位到指定位置

**zlbytes**：表示整个 ziplist 的长度
**zltail**：表示最后节点的偏移量，可以直接获取到 ziplist 的最后位置
**zllen**：表示ziplist节点个数
**zlend**：表示ziplist结束的标识符

后面是一个个连续的 节点 Entry，**每个 Entry 节点都存储了上一个节点的长度 prevlen**

当前节点的起始位置 - prevlen 可以得到上一个节点的起始位置

当前节点的起始位置 + 当前节点长度可以得到下一个节点的起始位置

省去了 pre 和 next



**ziplist 结构：**

![img](https://i6448038.github.io/img/redis-data-struct/ziplist_total.png)

**Entry 结构：**

![img](https://i6448038.github.io/img/redis-data-struct/ziplist_entry.png)

对于 hash 和 zset 这种需要存储 key-value 形式的，key 会使用一个 Entry 节点存储，value 会使用另一个 Entry 节点存储

但是它们是连续的 Entry



**元素遍历：**先定位到最后一个节点，然后从后往前遍历

![img](https://i6448038.github.io/img/redis-data-struct/ziplist_bianli1.gif)

![img](https://i6448038.github.io/img/redis-data-struct/ziplist_bianli2.gif)



> ### ziplist 连锁更新问题：

ziplist 存在一个 连锁更新问题



每个 ziplist 节点都存储了上一个节点的长度 prevlen，而这个 prevlen 是可变长度

当上一个节点总的字节长度 < 254 时，那么 prevlen 为 1 个字节

当上一个字节总的字节长度 >= 254 时，那么 prevlen 变为 5 个字节

(主要是因为 1个字节 8 位，可以表示 256 的长度，redis 规定界限为 254，如果超过 254 时就变成 5 个字节，这样可以避免 entry 上一个节点发生变化数据很大又再次修改)



**而这个 prevlen 发生改变会发生什么事呢？**

假设存在一个压缩列表，其包含e1、e2、e3、e4…..，e1 的大小为253字节，那么 e2.prevlen 的大小为1字节

这时候在 e1 前面插入一个 em, 变成了 e1、em、e2、.....

它本身的字节长度 加上记录 e1 的prevlen 刚好是 254 个字节，那么这时候 e2.prevlen 就需要进行修改了，变成 5 个字节，而我们说了 ziplist 是一段连续的内存空间，这样的话后面所有节点都需要进行后移，数据移位 通过 拷贝，当数据量大的时候，性能就降低了

当然，不只插入，删除也可能会造成连锁更新：假设排列 e1 e2 e3，e1 的字节长为 254，e2 的字节长为 200，e3 的字节数为 253，那么对于 e3 来说它的 prevlen 为 1 个字节，假如这时候删除了 e2，e3 的上一个节点变成了 e1，这时候 e3 的 prevlen 就需要发生改变了，因此可能会发生连锁更新



所以，ziplist 在数据量大的时候，查询性能不仅会降低，而且可能会发生这种连锁更新，数据迁移拷贝导致效率降低



### 3、linkedlist 

linkedlist 就是 java 中的 双向链表，使用 pre 和 next 指向前后节点

同时还有 head 和 tail 两个 dummy，可以快速进行头节点和 尾节点插入，用于队列

实现 O(1) 插入和删除，O(n) 查找，插入和删除 相比 ziplist 效率较高，但是更加耗费内存



### 5、intset

intset 实际上是 int 数组，只有在 key 上的所有元素都为整数 并且 数据量不大 时才使用

```C
typedef struct intset {
    
    // 编码方式
    uint32_t encoding;

    // 集合包含的元素数量
    uint32_t length;

    // 保存元素的数组
    int8_t contents[];

} intset;
```

intset 保证内部数据是有序的，因此每次插入一个数据 会进行元素迁移，当然这是在数据量小的时候使用

**保证数据有序的目的是为了使用 二分查找 数据，加快查询速度**



### 5、dict



哈希表跟 java 的 hashMap 差不多，有两种解决哈希冲突的方法：开放地址法 、拉链法

redis 使用的是 拉链法



> ### dict 结构

redis 中的 dict 是专门根据 redis 特性进行设计的，类似如下：

```java
class dict{
    //ht[0] 用来存储数据， ht[1] 用来 rehash
    HT[2] ht;
    //记录 rehash 进程， -1 表示没进行 rehash
    int rehashidx;
}
class HT{
    Entry[] table;
    //table 数组大小，即槽位个数
    int size;
    //用来 hash 计算 key 应该存储的槽位的，实际上就是 size - 1
    int sizemask;
    //元素个数，每插入一个元素， used + 1
    int used;
}
```

![img](https://i6448038.github.io/img/redis-data-struct/hash1.png)

> ### rehash

**rehash 包括 扩容 和 收缩**

扩容：如果 元素个数 used 快要占满 table，对 table 进行扩容，然后 rehash 计算所有 key 的位置

收缩：如果 元素个数 used 远远小于 table，对 table 进行收缩，然后 rehash 计算所有 key 的位置



**rehash 后的数组大小：**

扩容：第一个大于等于 ht[0].used * 2 的 2的幂，比如原数组长度为 8， used = 6，used * 2 = 12，大于等于 12 的 2的幂 是16，那么扩容后的数组大小为 16

收缩：第一个大于等于 ht[0].used 的 2的幂，比如原数组长度为 64， used = 8，大于等于 8 的 2的幂 就是 8，所以收缩后的数组大小为 8



**扩容：**

![img](https://i6448038.github.io/img/redis-data-struct/hash_refresh.gif)

**收缩：**

![img](https://i6448038.github.io/img/redis-data-struct/hash_refresh_release.gif)

> ### 渐进式 rehash

具体看：<http://redisbook.com/preview/dict/incremental_rehashing.html>



如果 redis 某个 hash 只有 4 个 key-value，那么很快就能 rehash 完成

但由于 redis 每个 dict 可能存储 几千万甚至上亿个 key，假如对这些 key 全部一次性 rehash 的话，那么将会导致服务器在一段时间内停止服务，因为这么庞大的 hash 计算也是需要时间的

**因此 redis 使用的是 渐进式 rehash，即分为多次 rehash**



它将 rehash 分摊到 查询、插入、删除、更新中，即每次 增删改查不会只是做指定的操作，还会将 rehashidx 指向的 ht[0] 槽位上的 key-value 迁移到 ht[1] 上，当完成后将 rehashidx + 1

这样就避免了集中式处理导致大量的计算使得服务需要暂停的问题



同时在 rehash 过程中，对于新增的 key-value，不会再插入到 ht[0] 中，而是插入到 ht[1] 中

而查询、删除、更新的时候，会先查询 ht[0]，如果 ht[0] 没有，再查询 ht[1]，这样就保证了 ht[0] 的 key-value 只减不增，并随着 rehash 的执行最终变成空表

当 rehash 结束后，ht[0] 和 ht[1] 会进行交换





### 6、skiplist



查询插入删除时间复杂度 O(logN)，最坏情况下为 O(n)

底层使用 score 进行排序，如果 score 相同，那么使用 member 进行排序



![image](http://qiniu.debrisflow.cn/20200506perfectSkipList.png)



为什么最坏是 O(n)？

因为当每个节点随机得到的高度都相同时，就退化为链表了，所以是 O(n)





## 4、redis 为什么使用跳表而不使用红黑树

跳表和红黑树 对单个节点的 增删改查的时间复杂度都是一样的，O(logn)

但在 redis 中，zset 还需要范围查询，跳表底层的链表能够简单实现范围查询

同时，跳表由于一个节点只需要 1 个指针，而红黑树一个节点需要两个指针，所以需要的内存比红黑树少得多，更加节省内存，而内存对于 redis 来说非常重要

跳表比红黑树还要更加容易 实现 和 维护（redis 作者说由于 跳表的简单性，所以别人能够看得懂，因此收到了一个补丁）





**既然跳表那么好，那么为什么不直接使用 跳表代替红黑树呢？为什么 hashmap 中不使用跳表呢？**

大概是因为红黑树的稳定性，跳表极端情况下会退化为 O(n)，虽然概率小，而 红黑树稳定的 O(logn)