# ASCII ？Unicode ？UTF-8 ？GBK ？



## 1、编码的演变

在最开始的时候，为了方便人为表示每一个字符，并且表示出来的字符能够自然转换为计算式能够识别的二进制数

美国人根据自己使用的 26 个大小写字母以及常用的符号创建出了 ASCII 码，它使用 1B 即 8 bit 表示

8bit 中后面 7bit 表示 128 个字符，高位的 1bit 默认为 0，这样 ASCII 码能表示的范围为 [0, 128]

每个字符都有 唯一分配的一个 ID，比如 以下 ASCII 表

字符 '0' 分配的唯一 ID 是 64，编程的时候可以使用 64 来表示 '0'，后续计算机会自动通过 ASCII 码的规则转换为二进制数

*![image.png](https://pic.leetcode-cn.com/1603692397-FIdqtM-image.png)*

正因为 ASCII 码只存储了 英文字母，没有中文，而 C 语言使用的是 ASCII 码，所以 C 语言并不支持中文

后来各国的计算机发展迅速，需要进行通信，因此这简简单单的 ASCII 码无法满足各国的语言，单单常用汉字就存在 几W 个了，因此从 ASCII 码演变出了 Unicode，用来表示各国语言



这里需要讲一下：

- ASCII 和 Unicode 是「字符集」
- UTF-8 是「编码规则」

「字符集」就是用来给每个字符分配一个唯一 ID，「编码规则」是通过一定的规则将这个唯一 ID 编码为 二进制数

Unicode 字符集为每一个字符（各国的字符）分配了一个唯一 ID，比如 中文 「知」的 ID 为 30693，记作 U+77E5（30693 的十六进制数为 77E5，前面加个前缀 `U+` 就变成了 U+77E5）





UTF-8 是 Unicode 字符集的一种编码规则，中文「知」同一个 ID = 30693 可以根据不同的编码规则可以转换成不同的二进制数

常用编码规则为：

- UTF-8
- UTF-16

UTF-8 是以 8bit（1B） 作为一个编码单位的可变长编码，会将一个 ID  编码为 1B - 4B 大小的二进制数：

```java
U+ 0000 ~ U+  007F: 0XXXXXXX	//十六进制数 0000 - 007F 的使用 1B 表示
U+ 0080 ~ U+  07FF: 110XXXXX 10XXXXXX	//十六进制数 0080 - 07FF 的使用 2B 表示
U+ 0800 ~ U+  FFFF: 1110XXXX 10XXXXXX 10XXXXXX	//十六进制数 0800 - FFFF 的使用 3B 表示
U+10000 ~ U+10FFFF: 11110XXX 10XXXXXX 10XXXXXX 10XXXXXX	//十六进制数 10000 - 10FFFF 的使用 4B 表示
```

转换为的字节 首位为 0 时，表示该字符使用 1B 表示

转换为的字节 首位为 1 ，且后续有多少连续个 1，表示该字符使用多少个 字节进行表示

十六进制数 0800 - FFFF 的使用 3B 表示



根据上表中的编码规则，之前的「知」字的码位 U+77E5 属于第三行的范围：

```java
	  7    7    E    5    
    0111 0111 1110 0101    二进制的 77E5
    --------------------------
    	0111   011111   100101 二进制的 77E5
    1110XXXX 10XXXXXX 10XXXXXX 模版（上表第三行）
    11100111 10011111 10100101 代入模版
    E   7    9   F    A   5		(4bit 表示一个十六进制数)
```



## 2、数据库的 utf-8

正宗的 UTF-8 是 1B - 4B，而 mysql 数据库的 UTF-8 实际上最多只有 3B，所以才都说数据库的 UTF-8 不是真正的 UTF-8

因此为了解决这个问题，mysql 后续推出了 UTF-8mb4，它支持 4B，因此这才是真正意义上的 UTF-8





## 3、Java 中存储中文

在 Unicode 中，中文的存储范围为：

| **字符集**                                                   | **字数** | **Unicode 编码** |
| ------------------------------------------------------------ | -------- | ---------------- |
| [基本汉字](http://www.qqxiuzi.cn/zh/hanzi-unicode-bianma.php?zfj=jbhz) | 20902字  | 4E00-9FA5        |
| [基本汉字补充](http://www.qqxiuzi.cn/zh/hanzi-unicode-bianma.php?zfj=jbhzbc) | 38字     | 9FA6-9FCB        |
| [扩展A](http://www.qqxiuzi.cn/zh/hanzi-unicode-bianma.php?zfj=kza) | 6582字   | 3400-4DB5        |
| [扩展B](http://www.qqxiuzi.cn/zh/hanzi-unicode-bianma.php?zfj=kzb) | 42711字  | 20000-2A6D6      |
| [扩展C](http://www.qqxiuzi.cn/zh/hanzi-unicode-bianma.php?zfj=kzc) | 4149字   | 2A700-2B734      |
| [扩展D](http://www.qqxiuzi.cn/zh/hanzi-unicode-bianma.php?zfj=kzd) | 222字    | 2B740-2B81D      |
| [康熙部首](http://www.qqxiuzi.cn/zh/hanzi-unicode-bianma.php?zfj=kxbs) | 214字    | 2F00-2FD5        |
| [部首扩展](http://www.qqxiuzi.cn/zh/hanzi-unicode-bianma.php?zfj=bskz) | 115字    | 2E80-2EF3        |
| [兼容汉字](http://www.qqxiuzi.cn/zh/hanzi-unicode-bianma.php?zfj=jrhz) | 477字    | F900-FAD9        |
| [兼容扩展](http://www.qqxiuzi.cn/zh/hanzi-unicode-bianma.php?zfj=jrkz) | 542字    | 2F800-2FA1D      |
| [PUA(GBK)部件](http://www.qqxiuzi.cn/zh/hanzi-unicode-bianma.php?zfj=puabj) | 81字     | E815-E86F        |
| [部件扩展](http://www.qqxiuzi.cn/zh/hanzi-unicode-bianma.php?zfj=bjkz) | 452字    | E400-E5E8        |
| [PUA增补](http://www.qqxiuzi.cn/zh/hanzi-unicode-bianma.php?zfj=puazb) | 207字    | E600-E6CF        |
| [汉字笔画](http://www.qqxiuzi.cn/zh/hanzi-unicode-bianma.php?zfj=hzbh) | 36字     | 31C0-31E3        |
| [汉字结构](http://www.qqxiuzi.cn/zh/hanzi-unicode-bianma.php?zfj=hzjg) | 12字     | 2FF0-2FFB        |
| [汉语注音](http://www.qqxiuzi.cn/zh/hanzi-unicode-bianma.php?zfj=hyzy) | 22字     | 3105-3120        |
| [注音扩展](http://www.qqxiuzi.cn/zh/hanzi-unicode-bianma.php?zfj=zykz) | 22字     | 31A0-31BA        |
| 〇                                                           | 1字      | 3007             |



常用汉字的 Unicode 范围为：【19968，40,869】



汉字 在 UTF-8 中使用 3B 表示，在 GBK 中使用 2B 表示

GBK 是专门用来对中文编码的，存储了简体中文和繁体中文，因为不容纳其他的字符，并且中文文字又多，所以需要 2B



我们都知道，Java 中的 char 类型是 2B，范围是 [0, 65535]（char 类型表示的是字符，而不是数字，所以不存在正负之说）

为什么 2B 的 char 能够表示中文呢？在我个人感觉里 char 使用的编码规则是 UTF-8，那么到底是不是呢？难道使用的是 GBK 吗？

我们先要知道 char 底层存储的是什么。

char 底层存储的是 每个字符在 Unicode 中分配到的唯一 ID

```java
char c1 = 30693;
System.out.println(c1);
```

输出结果：

```java
知
```

可以看出，跟最上面讲的一样，"知" 的 Unicode 分配到的 ID 为 30693，直接赋值给 char，可以准确打印出来

这也就说明了 char 存储的是 Unicode 码 编码后的 二进制数



```java
char c2 = '齉';
System.out.println((int)c2);
```

输出结果：

```java
40777
```

通过将 char 转换为 int 型，还可以得到某个字符的 Unicode 码，"齉" 的 Unicode 码为 40777



这里可以看出，因为 char 存储的是 Unicode 码，它所能够表示的范围 【0，65535】足以表示 常用汉字的 【19968，40,869】，一个 char 就可以容纳常用汉字了，但是这涉及到 编码规则，如果使用的是 UTF-8 的话，那么常用中文转换后的二进制数为 3B，一个 char 是无法存储的，那么 char 底层是使用什么编码规则呢？

**正确答案是 UTF-16BE** 

