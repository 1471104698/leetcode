# short、int、long、float 数据类型转换问题



```java
short s = 1;

s = s + 1;

编译会出现问题，因为 1 默认是 int 类型，而  s = s + 1 会自动类型提升，这样的话就变成 short = short + int，最终结果应该是 int，但是使用 short 接收，所以出现问题
```



```java
short s = 1;

s += 1;

没有问题，答案是 2，因为 s += 1 是 语言支持的特性，会自动进行类型转换
```



```java
float f = 3.4;

有问题，因为浮点型默认是 double 类型，所以相当于是 float = double，所以出现问题

正确写法应该是：

float f = 3.4f;
```



```java
float f = 3.4f;
f += 1.1;	//没有问题，语言支持
f = f + 1;	//没有问题，float 存储的数据量 比 int 大
f = f + 1L;	//没有问题，float 存储的数据量 比 long 大
f = f + 1.1;//有问题，相当于是 float = float + double，最终结果是 double
f = f + 1.1f;//没有问题，相当于是 float  = float + float，最终结果是 float
```



综上：**Java 中 s += 1 自带类型转换，会转换为 s 的类型，而 s = s + 1，右边则是会自动提升到最大的类型，即 int，所以需要我们自己强转，变成 s = (short)(s + 1)**



> #### 为什么 short s = 1 不会报错，而 float f = 1.0 会报错？

个人理解：

虽然 short s = 1 中 1 是 int 类型的，但是 1 是在 s 的表示范围内的，因此允许直接赋值

而 float f = 1.0 中 1.0 是 double 类型的，而 double 的精度比 float 大，不在 float 的表示范围内，所以不允许直接赋值



当 short s = ? 中右边的数值超过 short 16bit 【 -32768~32767 】 的范围，那么就会报错了

```java
short s = 40000; //报错，需要强转
```







