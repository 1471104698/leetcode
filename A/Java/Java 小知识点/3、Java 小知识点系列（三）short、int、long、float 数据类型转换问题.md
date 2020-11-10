# short、int、long、float 数据类型转换问题



以下这种写法有问题吗？如果没有，答案是多少？

```java
short s = 1;
s = s + 1;
```

编译会出现问题，因为 1 默认是 int 类型，而  s = s + 1 会自动类型提升，这样的话就变成 short = short + int，最终结果应该是 int，但是使用 short 接收，所以出现问题



以下这种写法有问题吗？如果没有，答案是多少？

```java
short s = 1;
s += 1;
```

没有问题，答案是 2，因为 s += 1 是 语言支持的特性



以下这种写法有问题吗？

```java
float f = 3.4;
```

有问题，因为浮点型默认是 double 类型，所以相当于是 float = double，所以出现问题

正确写法应该是：

```java
float f = 3.4f;
```



同样的，跟上面的 short 一样存在类似的问腿：

```java
float f = 3.4f;
f += 1.1;	//没有问题，语言支持
f = f + 1;	//没有问题，float 存储的数据量 比 int 大
f = f + 1L;	//没有问题，float 存储的数据量 比 long 大
f = f + 1.1;//有问题，相当于是 float = float + double，最终结果是 double
f = f + 1.1f;//没有问题，相当于是 float  = float + float，最终结果是 float
```



综上：**Java 中 s += 1 自带类型转换，会转换为 s 的类型，而 s = s + 1，右边则是会自动提升到最大的类型，即 int，所以需要手动进行强转，变成 s = (short)(s + 1)**



这里说一个知识点：虽然 float 和 int 都是 4 个字节，即 32 位，但是 float 表示的方式跟 int 不一样，float 存储的是指数，即类似 x ^ y 这种的，是指数级增长的，因此 float 比 int 大，同时，虽然 long 是 8个字节的，但是 float 可存储的数据也比 long 大

简而言之，浮点数最小的 float 存储的数据量比任何一个 整型类型存储的数据量都大







