# HTTP 各个版本的演变逻辑



## 1、HTTP 0.9

最原始的 HTTP 版本

- 只 支持 GET 请求：由于不支持其他请求，而 GET 是在 URL 中携带请求信息的，因此客户端无法向服务端发送太多信息
- 服务端只能返回 HTML 字符串
- 短连接





## 2、HTTP 1.0

- 新增了 POST、PUT、DELETE 等方式
- 增加了 请求头/响应头、状态码、缓存 (此时的缓存类型较为简单) 等
- 扩充了传输内容，可以传输图片、音频、二进制数据等



HTTP 1.0 的缺点：

- **短连接：**一次  HTTP 请求后都会断开连接（可以通过服务器配置支持多次），每次都需要进行 三次握手、慢启动、四次挥手，频繁通信情况下效率低，   其中 慢启动 主要对文件类大的传输影响大





## 3、HTTP 1.1

- 长连接：新增了 Connection 字段，内部有个 keep-alive 字段
- 管道化：管道化使得每个请求可以无需等待上一个请求响应即可发出去，不过响应还是按照请求的顺序返回
- 更强大的缓存：Cache-Control
- 断点传输：请求头部使用 Range，响应头部使用 Content-Range



### 3.1、长连接

由于每次建立和断开 TCP 连接都需要进行 三次握手 和 四次挥手，效率过低

同时每次 TCP 连接建立初期都存在一个慢启动过程，对于大文件传输影响大（因为普通的请求即使慢启动也能较快完成，但是如果是大文件，那么一般需要较长时间，而慢启动显然加长了这个时间）

因此添加一个 keep-alive 字段 使用长连接 以此来 复用连接，减少三次握手 和 慢启动 的开销



### 3.2、管道化

基于长连接的基础，我们先看没有管道化请求响应：

TCP 没有断开，用的同一个通道

```
请求1 > 响应1 --> 请求2 > 响应2 --> 请求3 > 响应3

```

管道化的请求响应：

```
请求1 --> 请求2 --> 请求3 > 响应1 --> 响应2 --> 响应3

```



管道化虽然解决了请求阻塞的问题，但是响应返回的顺序还是按照请求的顺序返回

这样的话，即使 响应2 比 响应1 事先准备好，但是还是要等 响应1 整完返回，响应2 才能返回

这是为了防止服务器返回 响应的时候，如果不按照顺序的话，那么服务器不知道相应对应哪个请求

**本质上还是没有解决 HTTP 队头阻塞的问题**





## 4、HTTP 2.0



[当我们在谈论HTTP队头阻塞时，我们在谈论什么？ - 云+社区 - 腾讯云 (tencent.com)](https://cloud.tencent.com/developer/news/123577)

[http2是如何解决tcp的队首阻塞的？ - 给剑纯道歉的回答 - 知乎](https://www.zhihu.com/question/65900752/answer/490183849)

[解密HTTP/2与HTTP/3的新特性-InfoQ](https://www.infoq.cn/article/ku4okqr8vh123a8dlccj)

[HTTP/2 头部压缩技术介绍 | JerryQu 的小站 (imququ.com)](https://imququ.com/post/header-compression-in-http2.html)

[HTTP/2 中的常见问题 (halfrost.com)](https://halfrost.com/http2-frequently-asked-questions/)



### 4.1、HTTP 2.0 的新概念



流（Stream）：一个完整的 请求-响应 过程 作为一个流

消息（Message）：对应 一个 requst 或者 response，包含一个或者多个 frame

帧（frame）：每个 message 会分割为多个 frame，它是数据传输的最小单位

- stream identifier：流的id，即用来标识这个帧是属于哪个流的，**HTTP 1.1 中的队头阻塞就是因为响应和请求没有标识符，不知道怎么对应，所以响应才需要按照顺序返回，而这里给定了标识符了，所以就可以打破这个 HTTP 队头阻塞了**



### 4.2、多路复用（解决 HTTP 队头阻塞）



**同个用户 访问 同个域名的 HTTP request-response 只占用一条 TCP 连接**



虽然只有一条 TCP 连接，但是可以同时传输多个 request-response（stream）的多个 frame，它们不会冲突

因为每个 frame 上面存在 id 标识符 标识了它属于哪条 stream，因此各个 stream 之间的 frame 不会出现混乱，

但是要求**一个 stream 上的 frame 必须有序传输，即 FIFO**



<img src="https://upload-images.jianshu.io/upload_images/16844918-b13e6490eedb402c.png?imageMogr2/auto-orient/strip|imageView2/2/w/1200/format/webp" style="zoom:40%;" />

<img src="https://pic.leetcode-cn.com/1599402910-XBmxXM-image.png" style="zoom:40%;" />



HTTP 2.0 的多路复用优点：

- TCP 连接只需要经历一次 TCP 握手 和 一次 SSL 握手，以及一次慢启动过程，效率更高
- 利用 `多路复用 + 数据分帧中的 stream 标识符` 解决了 HTTP1.1 的 **部分** 队头阻塞问题，允许顺序 request，乱序 response



> #### 为什么说 HTTP 2.0 没有完全解决队头阻塞问题？

**队头阻塞分为 HTTP 队头阻塞 和 TCP 队头阻塞 两种**

**HTTP 队头阻塞：**

```java
当同时发送多个 HTTP request 时，服务器由于无法区分 response 是对应哪个 request 的，
所以只能按照 request 的顺序来进行 response
所以导致后来的 request 请求已经完成 response，也必须等到前面的 request 完成 response 为止
```

**TCP 队头阻塞：**

```java
TCP 有一个重传机制，当出现丢包重传时，对于同一条 TCP 连接上 后面的 request 数据包来说它无法发送，必须等待丢包重传完成
即使存在 ---滑动窗口--- 方案，但是窗口大小也是有限的，当可发送窗口为 0 时，仍然需要阻塞等待。
```



**HTTP 1.1** 的管道（pipe） **无法解决 HTTP 队头阻塞**，不过它可以通过建立多个 TCP 连接 **解决了 TCP 队头阻塞问题**

**HTTP 2.0** 的 多路复用 通过为每个 frame 添加 流 id 的方式 **解决了 HTTP 队头阻塞问题**

​	由于用户的所有 request 都是在一个 TCP 连接上的，即多个 request 数据包都是使用一个 滑动窗口，滑动窗口的机制是：

​	一旦出现丢包重传，那么即使该包后面服务器已经处理完毕，但是发送方的滑动窗口的左边界也会一直阻塞，直到收到丢失包的 ACK 后，才会停止阻塞，这就导致了一旦一个 stream 丢包重传，那么将会影响到其他的 stream 后面的数据的传输，因此 **HTTP 2.0 没有解决 TCP 队头阻塞**

```java
目前 HTTP 2.0 都是基于 TCP 实现
HTTP 协议并没有规定 HTTP 的实现必须使用 TCP，只是默认都使用 TCP 而已，实际上也可以使用 UDP，比如后面的 QUIC
```



当网络环境较插，一旦出现丢包重传，导致队头阻塞，那么 HTTP 2.0 效率比 HTTP 1.1 还差，

因为 HTTP 2.0 是将用户的所有 request-response 都放在一个 TCP 连接上，共用一个滑动窗口

而 HTTP 1.1 可以同时打开多个 TCP 连接来解决这个阻塞的问题



### 4.3、HTTP 2.0 的数据格式协议



HTTP 1.x 使用的是 文本协议，在请求和响应中的都是文本， HTTP 2.0 将 文本协议 替换为 二进制协议

```java
原本 HTTP 1.x 使用文本协议的优点就在于容易进行抓包调试，但是如今大部分网站使用 SSL，它会将数据加密，
因此 HTTP 1.1 这个优点也没了
```



**为什么 HTTP 2.0 要使用二进制协议？**（了解即可，目前没有文章详细讲解）

因为二进制协议解析起来更加的高效，并且不容易出错，对空格、大写、空白行等的处理很有帮助



### 4.4、服务器数据 “推送”

HTTP 1.1 中，当浏览器请求页面时，服务器在响应体中发送 HTML，浏览器解析 HTML 并对 HTML 内部需要显示的 图片、js 等发起请求，服务器再响应这些请求，将资源响应给浏览器

例子：

比如浏览器请求了以下 html 页面

```html
<!DOCTYPE html>
<html>
<head>
  <link rel="stylesheet" href="style.css">
</head>
<body>
  <h1>hello world</h1>
  <img src="example.png">
</body>
</html>
```

内部需要 `style.css` 和 `example.png`，因此浏览器解析完 HTML 后会发送两个请求，分别请求这两个资源

```html
GET /style.css HTTP/1.1
GET /example.png HTTP/1.1
```



HTTP 2.0 中，服务器会 “推送“ 它认为客户端需要的响应资源在响应 HTML 时一起发送过去，来减少 request-response

比如上面在浏览器请求 HTML 时，服务器会将 `index.html`、`style.css` 和 `example.png`一起响应给浏览器

比如下图，两个资源的标签都是 `Push`，表示是推送过来的

![img](http://www.ruanyifeng.com/blogimg/asset/2018/bg2018030502.png)



简单使用一张图来表示服务器推送的优点：

![img](https://pic1.zhimg.com/v2-9dd38178052d7ae74a66a79d4616dbb0_b.png)

### 4.5、头部 Header 压缩



**头部压缩的优点：**

HTTP 2.0 通过头部压缩，减少了 HTTP 头部的大部分重复字段的传输，减少了网络带宽压力



**Header 压缩原理：**

每个 HTTP 报文中都存在一些常见的字段，这些字段和值是不会发生改变的

比如 `Content-Type` 这种都是固定的，不会突然 "Content-Type" 变成 "Type"

因此，HTTP 2.0 在浏览器 和 服务器 之间：

- 维护一份相同的静态字典，包含常见的不会修改的 header 字段名称，以及 header 字段 和 值的组合
- 维护一份相同的动态字典，包含此次 TCP 连接中动态变化的 字段值

静态字典部分数据如下，可以看出，它将一些常见的字段和值的映射关系 存储为 索引

比如 :method: GET 对应的索引为 2，这样比如浏览器在请求的时候可以只需要使用 1B（8bit） 存储索引号 2 来代替这个字符串，服务器收到后对比自己的静态字典来获取索引对应的值

| Index | Header Name      | Header Value |
| :---- | :--------------- | :----------- |
| 1     | :authority       |              |
| 2     | :method          | GET          |
| 3     | :method          | POST         |
| 4     | :path            | /            |
| 5     | :path            | /index.html  |
| 6     | :scheme          | http         |
| 7     | :scheme          | https        |
| 8     | :status          | 200          |
| ...   | ...              | ...          |
| 32    | cookie           |              |
| ...   | ...              | ...          |
| 60    | via              |              |
| 61    | www-authenticate |              |



动态字典是动态维护的，比如 url 和 cookie 这种，后面的值不是固定的，每个用户的请求的 url 都是不一致的，而且 cookie 值也是会改变的

比如 `cookie = xxx`，对于 cookie 这个头部名称来说，它是不会发生变化的，所以它存储在上面的静态字典中，索引号为 32，而后面的 cookie 值是会发生变化的，因此它需要存储在动态字典中，每次发生变化浏览器和服务器都需要修改动态字典，然后之后使用动态字典的索引号来代替这个值



### 4.6、HTTP 2.0 使用 TLS/SSL 协议问题

HTTP 2.0 协议没有强制要求使用 TLS/SSL 协议，但是现在主流浏览器 比如 chrome、firefox 都强制要求 HTTP 2.0 使用 TLS/SSL 协议





## 5、HTTP 3.0（QUIC 协议）

[图解|为什么HTTP3.0使用UDP协议 - 51CTO.COM](https://network.51cto.com/art/202009/625999.htm)

 [科普：QUIC协议原理分析 - 知乎 (zhihu.com)](https://zhuanlan.zhihu.com/p/32553477) 



### 5.1、HTTP 2.0 的问题



 HTTP 2.0 的问题如下：

- 未能解决 TCP 队头阻塞问题，同时由于使用的一个 TCP 连接，所以需要保证网络环境健壮，在弱网络环境下会由于队头阻塞问题导致效率很比 HTTP 1.1 还低（TCP 协议问题）
- TCP 三次握手 + TLS/SSL 四次握手（1.5RTT + 2RTT = 3.5RTT），效率较低（TCP 协议问题）

因此为了解决 HTTP 2.0 的问题，出现了 QUIC 协议。



```java
互联网工程任务组（IETF，协作设计网络协议的行业组织）一直致力于制定标准化的QUIC版本，该版本目前与谷歌的原始提案有很大差异。IETF还希望开发一个使用QUIC的HTTP版本，该版本之前名为HTTP-over-QUIC或HTTP/QUIC。然而，HTTP-over-QUIC不是HTTP/2 over QUIC，而是一种为QUIC设计的新的HTTP更新版。

因此，身兼IETF旗下HTTP工作组组长和QUIC工作组组长的马克•诺丁汉（Mark Nottingham）提议，将HTTP-over-QUIC改名为HTTP/3，这个提议似乎已得到了广泛接受。HTTP的下一个版本将QUIC列作一项必不可少的基本功能，那样HTTP/3将始终使用QUIC作为其网络协议。
```

从上面可以看出，**QUIC 协议 就是 HTTP 3.0**，它是**基于 UDP 协议**的



### 5.1、QUIC 为什么选择 UDP 协议

从上面看出，HTTP 2.0 出现的问题都是因为使用了 TCP 协议。

当使用 TCP 的时候，由于 HTTP 2.0 强制使用 TLS/SSL 协议，因此总共是 7 次握手，需要 3.5 个 RTT（报文往返时间）才能真正开始数据传输

```java
RTT 的时间跟 浏览器 和 服务器 之间的物理距离有关，当物理距离越远，那么 RTT 越长
```

同时在丢包重传时，还会出现 TCP 队头阻塞问题。



综上，为了解决这些问题，QUIC 使用了 UDP 协议

由于 UDP 本身无连接这个概念，所以不需要三次握手，能够省去这 1.5 RTT，但还是存在 TLS 四次握手

但是 QUIC 使用的 UDP 协议不是原生的 UDP 协议，它是对 UDP 协议的改造，

从而达到 0 RTT 即可进行数据传输

- 传输层 0 RTT 建立连接
- 表示层 0 RTT 就能建立加密连接





### 5.2、QUIC 可插拔的 拥塞控制算法

QUIC 默认使用了 TCP 的拥塞控制算法：慢启动、拥塞避免、快重传、快恢复，但是进行了改进

QUIC 协议的 拥塞控制算法具有 **可插拔** 的优点：

- 传统的 TCP 是操作系统在内核层面实现的，应用程序只能使用，不能根据自身情况进行修改

- 而 QUIC 协议是在应用程序方面实现拥塞控制算法，不需要操作系统，不需要内核支持，这样的话，每个服务器都可以根据接入的用户不同的网络环境 提供 不同但又更加有效的拥塞控制算法



**QUIC 同样使用 TCP 的滑动窗口来实现 流量控制**，

既然 HTTP 2.0 就是因为 滑动窗口的问题出现了 TCP 队头阻塞，

QUCI 也是使用了滑动窗口，那么 QUIC 是如何解决队头阻塞问题的？



### 5.3、QUIC 如何 解决 队头阻塞问题

 [Web技术（六）：QUIC 是如何解决TCP 性能瓶颈的？_流云-CSDN博客](https://blog.csdn.net/m0_37621078/article/details/106506532) 



QUIC 协议也使用了类似 TCP 的 多路复用，但是它却完美解决了 TCP 队头阻塞问题



首先我们知道 HTTP 2.0  出现 TCP 队头阻塞的原因是：一个 stream 丢包重传而导致滑动窗口阻塞，使得其他 stream 也阻塞，影响了传输效率

因此，如果要解决队头阻塞，最容易想到的解决方案就是 **出现丢包重传时，不让滑动窗口的左窗口阻塞**

QUIC 的实现就是如此。



**QUIC 使用  Packet Number 来代替 TCP 的 seq 序列号机制**，并且重传的包的 Packet Number 不是原来的数，而是比丢失的包的 Packet Number 更大的数，作为一个新的 Packet 包进行传输

QUIC 不要求跟 TCP 那样保证 **有序 ACK 确认**（注意是确认，而不是接收），它允许数据 **乱序 ACK 确认**，QUIC 只要收到一个 ACK，那么当前窗口就会右移，当得知 数据包 Packet N 丢失后，会讲需要重传的数据包放到待发送队列中，重新将数据包编号为 Packet N + M，然后发送给接收方。

QUIC 把 重传包 当作 新的数据包，因此滑动窗口不会阻塞



这样就有一个问题了，既然把重传包 当作 新的数据包，那么接收方如何知道 这是一个重传包 还是 一个新的数据包 ，在不知道的情况下，怎么知道这个数据包是拼接在 stream 哪里的（①）？

使用 UDP 的情况下，单单靠 数据帧 frame 上的 stream id 无法判断是重发包还是新包，因此需要添加一个 stream offset 字段，用来标识该 frame 在 stream 中的偏移量，这样接收方就可以根据偏移量进行拼接

<img src="https://img-blog.csdnimg.cn/20200608160132550.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L20wXzM3NjIxMDc4,size_16,color_FFFFFF,t_70" style="zoom:100%;" />

比如上面的 Packet N 和 Packet N + M，它们的 stream Id 和 offset 都一样，说明它们的数据包内容一致，那么接收端可以根据 stream id 和 offset 字段正确的拼接 stream

这样 stream 就可以乱序传输，从而使得滑动窗口无需阻塞



> #### 为什么 HTTP 2.0 的 stream 需要按序传输？
>
> #### 为什么 TCP 的 序列号机制 不能让 HTTP 2.0 乱序传输？

`start`

这里有一个很大的问题，很多地方基本没讲明白，包括我前面看的时候也没明白，具体看 （①）标注的地方，是我没搞懂的

这里我自己参透了一下

首先，**有这个疑问是因为把 HTTP 和 TCP 的关系给搞混了，HTTP 是我们真实的要传输的数据，而 TCP 是负责把这些数据可靠的送达，并且有序的拼接。**

这里需要注意的是，TCP 所谓的有序拼接是说按照 HTTP 交给它的数据包的顺序进行拼接的，它只保证按照 HTTP 交给它的数据包的顺序拼接，也就是说，如果我们要按照顺序传输 1 2 3 4 四个包，但是 HTTP 发送给 TCP 的顺序是 1 2 4 3，那么在 TCP 的层面来说，它所认为的顺序就是 1 2 4 3，因此，当接收方接收到 1 2 3 后，发现 4 丢失，发送方重发 4 号包，接收方收到后会按照 1 2 4 3 拼接起来

因此，如果发送方的 HTTP stream 是乱序交给 TCP 的，那么接收方那边 HTTP 组装起来的 stream 也是乱序的，这就导致问题的出现了

因此， TCP 的序列号机制不能让 HTTP 2.0 乱序传输，HTTP 2.0 的 stream 还是需要有序传输，所以一旦包丢失，那么 TCP 滑动窗口必须阻塞，然后获取丢失的 TCP 包所对应的 frame，按照原来的 TCP 序列号发送出去



而在 QUIC 中，添加了 stream id，它标识了这个 frame 是在 stream 的哪个位置的，这相当于是 **HTTP 层面的序列号机制**了

因此 QUIC 可以乱序传输，一旦出现包丢失，不需要 TCP 原来的序列号，只需要保证原来的 stream id 和 stream offset 一致即可，那么就可以放到一个新的 TCP 序列号包 中进行传输，不需要阻塞前面的窗口

`end`

