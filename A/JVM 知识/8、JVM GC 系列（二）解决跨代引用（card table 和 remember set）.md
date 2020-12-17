# 解决跨代引用（card table 和 remember set）



解决跨代引用的做法都是使用空间换时间

## 1、Card Table

[卡表详解](https://www.jianshu.com/p/da5717e5b5ad)

[写屏障详解](https://blog.csdn.net/nazeniwaresakini/article/details/105947623)



card table 意为 卡表，在 CMS 中使用，它将整个堆空间分为一个个大小相等的小空间，这个小空间叫做 卡页，每个大小为 512B

并且维护一张 卡表，卡表是一个字节数组，每个字节表示为一个卡表项（1B），每个卡表项对应一个卡页**（类似内存分页技术的 页表 和 页表项，页表项对应一个内存块）**

卡表用来记录 卡页上 是否存在 老年代对新生代的引用：

- 如果存在，那么将该卡页对应的卡表项 标识为 `dirty`，即 卡页 设置为脏页
- 如果不存在，那么对应标识为 `clean`

之后在 young GC 的时候只需要扫描卡表，然后找到所有被设置为 dirty 的卡表项对应的卡表，然后扫描这些卡表的老年代对象，判断它们引用了哪些新生代对象，从而可以获取到 这些新生代对象的可达性

![CardTable](https://user-gold-cdn.xitu.io/2020/7/3/1731052eb999f1a7?imageView2/0/w/1280/h/960/format/webp/ignore-error/1)

在 CMS 中使用了写屏障来更新 引用关系的

所谓的写屏障 跟 volatile 中的 内存屏障不同，它是在每次执行修改操作后，在将数据写入内存前，会将对应的 卡表项 修改为 dirty，然后再将数据写入内存中

下图的 mark card 表示标记为 drity， write memory 表示将数据写回内存

 ![img](https://upload-images.jianshu.io/upload_images/195230-f908506e39c17fd2.png?imageMogr2/auto-orient/strip|imageView2/2/w/512/format/webp) 



> ### 虚共享（伪共享、伪并发） 问题

JDK 7 之前，卡表的写屏障是无条件的，不论更新的引用是否是跨代引用，都会出现一次写屏障，这种情况导致 卡表在高并发情况下 频繁出现写屏障，容易出现 虚共享 问题



CPU 的缓存体系是以 缓存行（cache line） 为单位的，一个缓存行 64B，假设一个缓存行全部存储的是 卡表项，一个卡表项 1B，那么就可以存储 64 个卡表项，而 1 个卡表项对应 堆内存的 一个卡页，每个 卡页 512B，这样 64 个 卡表项映射了 32KB 的堆空间



对于缓存行来说，一旦它内部的卡表项映射的堆空间 某个卡页 存在 引用更新，那么就需要对 卡页对应的 卡表项 标识为 dirty，这就意味着 其他所有 CPU 中，含有该 卡表项的 缓存行全部无效，后续访问时 缓存未命中，需要重新访存

而在高并发情况下，如果 多个 CPU 内部 缓存行 都存在 相同的多个卡页 映射的 卡表项，并且 多个线程都会对 同个卡页上的 相同 或者 不同对象 进行引用更新，由于每次只会存在一个引用更新成功，那么就会频繁的导致其他 CPU 缓存行的失效，缓存未命中，影响性能

因此，频繁的写屏障会导致虚共享问题，降低并发效率



在 JDK7 以后，出现了一个  -XX:+UseCondCardMark   参数，它会开启有条件的写屏障，当发生 引用更新 时，会判断对应卡表中对应 卡表项 是否为 dirty，如果是，那么就不管了，如果不是，再开启写屏障进行标识，这样可以有效减少 写屏障导致的 虚共享问题



## 2、Remember Set



由于 G1 将堆空间划分为了 一个个的 Region ，一个 Region 可能被 其他多个 Region 引用，如果要统计某个 Region 的存活对象，那么就必须知道哪些对象被 其他 Region 引用，因此，一个简单的 卡表数组 已经无法解决这个需求了，因此出现了 Remember Set

RSet 是对 card table 的扩展，实际上就是多个 card table 组成的

它将每个 Region 划分为多个 卡页，每个 卡页 跟 CMS 时一样都是 512B，然后为每个 Region 分配一个 HashTable，key 是 Region 的起始地址，value 为 byte[] 字节数组



每个 Region 都存在一个 RSet，RSet 是一个 HashTable，key 是 每个 Region 的起始地址（意味着存在多少个 Region 就会存在多少个 key），value 是一个字节数组，对应每个 Region 的每个 卡页



**举例：**

在 Region2 的下方，【 1	2	3	4 】表示的是 Rset（HashTable ）的槽位，1号槽位表示 Region1，2号槽位表示 Region2

每个槽位上都是一个 byte[]，记录了该槽位对应的 Region 对当前 Region 的引用情况

Region2 的 card1 存在对象 被 Region1 的 card2 和 card4 中的某些对象引用了。

当回收 Region2 的时候，扫描 Region2 的 Rset，假设扫描到 Region2 的 card1，发现 Region1 中有对象引用了 card1 中的某些对象，为了确定 card1 中哪些对象存活，就必须去扫描 Region1 的 card2 和 card4 中的对象，再从这些对象出发，判断哪些是引用了 Region 2 的 card1 中的哪些对象的

这样在回收的时候就可以根据 存活对象数量 来统计每个 Region 的回收价值

*![image.png](https://pic.leetcode-cn.com/1604937577-UMiHDP-image.png)*



