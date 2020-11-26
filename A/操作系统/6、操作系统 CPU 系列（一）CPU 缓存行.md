# CPU 缓存行



最开始知道 CPU 缓存行应该是在 理解 volatile 实现原理的时候，volatile 变量写回内存会使得 CPU 所有核心 中存在该 volatile 变量的缓存行都失效，从而使得缓存中的值是最新值

后面在 GC 的 卡表中涉及到写屏障，而频繁的写屏障又涉及到 虚共享问题，这个虚共享问题就是缓存行造成的



## 1、CPU 缓存行是什么？

[看看自己在评论问中的问题以及楼主解答](https://zhuanlan.zhihu.com/p/135462276)



**CPU 操作数据的基本单位就是 缓存行**，一般的缓存是 64B

内存的划分也是以缓存行来划分的，不同缓存行大小的连续空间在内存中称作 内存块，缓存行 和 内存卡 大小相同，类似 分页机制那样 能够一一映射的

CPU 的缓存行主要分为 5 个部分：组标记、内存数据、有效位（valid bit）、脏位（dirty bit）、使用位（use bit）



一个缓存行 64B，一个 long 型 8B，那么一个缓存行可以存储 8个 long 变量

当 CPU 访问一个 long 数组中的任意一个索引位置的值时，CPU 会 **顺序加载** 内存块中 7 个到缓存中，凑齐缓存行的大小，即 CPU 是以缓存行作为基本存取单位的，所以一次会从内存中获取 64B 数据。因此，由于缓存行的存在，CPU 能够非常快的遍历 类似数组这种 连续内存空间 的数据结构，因为 CPU 一次访存就能够获取到要遍历的大部分数据。

但是如果是 链表 这种内存空间不连续的数据结构，那么一次访问可能只能获取到 1 个真正有用的数据，缓存行中的其他数据都于此次遍历来说没有任何意义。因此，数组 这种数据结构的访问速度 比 链表 快的原因在于此



所以，对于数组的访问，一般 访问每行的列 会比 访问每列的行 要更快

```java
public class Main{
	public static void main(String[] args){
		int N = (int)1e4;
		int[][] mat = new int[N][N];
        //先访问每行的列，速度更快，因为利用了 CPU 缓存行提前缓存附近的数据
        for(int i = 0; i < N; i++){
            for(int j = 0; j < N; j++){
                arr[i][j] = 1;
            }
        }
        //先访问每列的行
        for(int i = 0; i < N; i++){
            for(int j = 0; j < N; j++){
                arr[j][i] = 1;
            }
        }
	}
}
```



## 2、CPU 缓存行 的 虚共享 问题

在上面我们说了，CPU 是以缓存行作为基本单位的，CPU 不能单单只读取一个变量，它必须读取整个内存块

并且由于 CPU 缓存一致性 MESI 协议，它制定了如果一个 CPU 修改了一个缓存行中的任何数据，那么其他 CPU 核心中的该缓存行全部无效，即其他 CPU 核心需要重新再访问主存，获取新数据



**这就导致了一个问题：**

```java
publci class Main(){
	long a;
	long b;
}
```

在上面代码中，变量 a 和 b 存储在连续的内存空间中

假设 变量 a 是 内存块 的起始地址，这样变量 a 所在的内存块也会包含变量 b，即 变量 a 和 变量 b 是绑定在一起的

当 CPU 核心① 读取变量 a 时，会将变量 b 也给读取进去；当 CPU 核心② 读取变量 b 时，会将变量 a 也给读取进去

​							 <img src="https://mmbiz.qpic.cn/mmbiz_png/J0g14CUwaZdrBIMQROWxSKCX3uKvOOFzYzpxtCCTu4s5VPBrH5fuEQOesWEFrbTicIWz1mNNT2JCibOVMwj20qYQ/640?wx_fmt=png&tp=webp&wxfrom=5&wx_lazy=1&wx_co=1" style="zoom:50%;" />

 



1、当 CPU 核心①  修改了变量 a，将它刷新回 cache 前，会在总线上发布消息 通知 CPU 核心② 该缓存行失效，等到收到 失效 ACK 后，CPU 核心① 会将 变量 a 刷新回 cache，然后将缓存行修改为  「已修改」 状态



<img src="https://mmbiz.qpic.cn/mmbiz_png/J0g14CUwaZdrBIMQROWxSKCX3uKvOOFz8ZrrKEbRz5jDItRibqgU3KMfRWPglq4XJwfTffRI3lbeAJ1fKCiauMKA/640?wx_fmt=png&tp=webp&wxfrom=5&wx_lazy=1&wx_co=1" style="zoom:50%;" />



但实际上，对于 CPU 核心② 来说，它并不需要访问 变量 a，但是却因为不需要的变量而导致它缓存失效，相当于 CPU 核心② 的缓存无效了

2、当 CPU 核心② 要访问变量 b 时，发现 缓存行无效，那么会发布消息，通知 CPU ① 将 cache line 写回主存，然后 CPU ② 重新读取主存，CPU ① 和  CPU ② 将 cache line 设置为 「共享」状态

3、然后如果 CPU ② 修改了变量 b，那么就会导致 CPU ① 的缓存行无效

长久如此，这样频繁的交替修改，对于 CPU ① 和 CPU ② 来说它们的 cache 形同虚设，一直都缓存未命中，需要去访问主存，导致并发效率低，这就是伪共享问题



## 3、伪共享解决

> #### 解决方法①：Linux 

在 Linux 中存在宏定义  `__cacheline_aligned_in_smp`  ，用于解决 伪共享 问题

```C++
struct  Main{
	long a;
	long b;
}
```

虽然 a 和 b 是存储在连续的内存空间的，一般情况下它们绑定在同一个内存块上

<img src="https://mmbiz.qpic.cn/mmbiz_png/J0g14CUwaZdrBIMQROWxSKCX3uKvOOFzuWs4pbIN6Ia7cVduQ5CWVYXKr157sUJwGia56VxAXYvicohSOWtmaMKQ/640?wx_fmt=png&tp=webp&wxfrom=5&wx_lazy=1&wx_co=1" style="zoom:70%;" />

在变量 b 后面添加宏定义

```C++
struct  Main{
	long a;
	long b __cacheline_aligned_in_smp;
}
```

它可以强制将变量 b 设置为一个内存块的起始地址，这样变量 a 和 变量 b 就会在两个不同的内存块上了，避免了伪共享问题

<img src="https://mmbiz.qpic.cn/mmbiz_png/J0g14CUwaZdrBIMQROWxSKCX3uKvOOFzgtfELVBUsHnb3TNVr19NexbAas7jPwJKRszCHgQOGVkmtdqqcS9jicg/640?wx_fmt=png&tp=webp&wxfrom=5&wx_lazy=1&wx_co=1" style="zoom:70%;" />



> #### 解决方法②：Java 层面

![img](https://mmbiz.qpic.cn/mmbiz_png/J0g14CUwaZdrBIMQROWxSKCX3uKvOOFzZlUm34yGxd3WL2OY5WhgIUd04uIC0wibYa3936fBNVaE5ZmsVkURYRA/640?wx_fmt=png&tp=webp&wxfrom=5&wx_lazy=1&wx_co=1) 

 在 RingBufferPad 中定义了 7 个 long 型变量

在 RingBufferPad 子类 RingBufferFields 中定义一些常量

在 RingBufferFields 子类 RingBuffer 中定义了 7 个long 型变量

其中 RingBufferPad 中的数据是真正需要使用的，父类 RingBufferPad 和 子类 RingBufferFields 中前前后后 总共定义了 14 个 long 型变量



我们需要先知道，在 JVM 中，子类会继承父类的变量，JVM 定义了 一段连续的内存空间 作为 OOP 对象体，用来存储 OOP 对象的变量，在 OOP 对象体 的最前面优先存储的是父类的变量，后面才是存储的子类变量



因此，在 JVM 中，RingBuffer 的变量的布局如下：

在它的 OOP 对象体中，最前面是超父类 RingBufferPad 的 7 个 long 型变量，中间是它父类 RingBufferFields 的真实访问数据，在最后是自己定义的 7 个 long 型变量

 ![img](https://mmbiz.qpic.cn/mmbiz_png/J0g14CUwaZdrBIMQROWxSKCX3uKvOOFzT3NuCAlBNmWGa2xHpwxTNuO2F8viaXy2YwbQW3kVjygGxuqKCQ3H7mA/640?wx_fmt=png&tp=webp&wxfrom=5&wx_lazy=1&wx_co=1) 

由于一个 long 型变量为 8B，那么一个缓存行最大可以存储 8个 long 型变量，这样的话，当通过 RingBuffer 来读取  RingBufferPad 中的真实数据时，无论缓存行如何填充，都不会超过这 14 个long 型变量的范围，访问真实数据时， CPU 读取的 缓存行最多只会使用这 14 个 long 型变量进行填充，而不会跟 其他类的数据混合在一个缓存行中

这 **14 个 long 型变量是作为填充缓存行的作用，并不会进行读写操作，因此也就不会造成缓存行的修改和无效**

因此，解决了 伪共享 问题，显然是使用空间换时间的做法来解决的