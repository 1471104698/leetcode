# HTTP（基于 TCP 协议）



## 1、HTTP 常见的状态码  

**1xx**

提示信息，中间状态，比如我转账给你，钱转过去需要一段处理时间，这段时间可以理解为 1xx



**2xx**

成功状态码，我们最愿意看到的

 「**200 OK**」是最常见的成功状态码，表示一切正常 ，响应体 中可能会有数据

 「**204 No Content**」 也是是最常见的成功状态码，服务器不会返回数据

 「**206 Partial Content**」**用于 断点续传**，表示响应返回的 body 数据是断点续传的方式，可能还存在数据没有发送



**3xx**

表示用户请求的资源位置发生了改变，告知用户需要向新的 URL 发送请求（即旧的 URL 不再提供服务）

 「**301 Moved Permanently**」表示永久重定向，服务器会将新的 url 发送给浏览器，浏览器会 cache，后续就直接访问这个 url

 「**302 Moved Permanently**」表示临时重定向，服务器会将新的 url 发送给浏览器，但浏览器不会 cache，往后访问还是使用旧的 url

 「**304 not modified**」浏览器存在缓存但还没有过期时会发送 get 请求服务器，如果资源没有更新，那么服务器返回 304 告知浏览器继续使用缓存



**4xx**

客户端方面的错误，服务端不能处理

 「**400 Bad Request**」表示客户端请求的报文有错误，服务器无法理解

 「**401  Unauthorized**」表示用户请求还没有认证，用户需要先进行认证

 「**403 Forbidden**」表示用户已经认证了，但是没有权限访问

 「**404 Not Found**」表示请求的 url 在服务器上不存在或未找到



**5xx**

客户端方面没有错误，而是服务端内部出现 了错误

 「**500 Internal Server Error**」与 400 类似，服务器内部错误

 「**502 Bad Gateway**」网关错误或者网关无效，网关或者代理服务器从上游收到无效的响应 ，可能是服务器连接太多，无法处理当前的请求

   [**504 Gateway Time-out**]：网关超时，网关或者代理向上游服务器请求，但没有及时收到上游服务器（DNS 服务器之类的）的响应



## 2、HTTP 请求方式



[GET 和 POST 到底有什么区别？ - 大宽宽的回答 - 知乎](https://www.zhihu.com/question/28586791/answer/767316172)

**get：**获取静态资源，幂等

**post：**让服务器创建指定资源，非幂等

**put：**让服务器更新指定资源，幂等

**delete**：让服务器删除某个指定资源，幂等



我们需要清楚，**GET、POST、PUT 这些语义都是 HTTP 协议制定的规范而已，并没有强制 POST 方法就必须把参数放到请求体，而不能放到 URL 上，只是程序员统一遵循这个规范，而不需要再去协商什么方法的什么参数是放在 Header 好 还是 Body 好**

我们后台也可以调换 POST 和 PUT 的语义，不过这种是约定俗成的规范，要让别人一眼就能看出这个类型的的方法是在干什么事

```java
@RequestMapping(method = RequestMethod.POST)
public ResultVO postSomething(User user){
	//直接插入
	userService.add(user);
}

@RequestMapping(method = RequestMethod.PUT)
public ResultVO postSomething(User user){
	//查询
	User user = userService.quety(user);
	//为空才插入否则更新资源
	if(user == null){
		userService.add(user);
	}else{
		userService.update(user);
	}
}

```





> ### get 和 post 的区别

上面也说了，GET 和 POST 只是一种 HTTP 规范，并不是强制的，因此可以根据特定情况进行修改，下面讲的是基本情况

- get 是请求资源，请求参数会放在 url 后面，post 是创建和更新数据，上传的数据会存储在 请求 body 中

- get 是幂等的， 在浏览器回退没有问题，post 不是幂等的，在浏览器回退会再次创建资源

- get 返回的数据会被浏览器主动 cache，post 不会，需要手动设置

- 一般情况下 post 会分两次发送，第一次发送请求头，第二次发送请求体

  - ```
    分两次发送的目的：
    服务器先获取请求头，获取里面的参数，判断 Athorization 是否能够通过认证、Content-Length 过大 或者 Content-Type 指定的数据格式不支持
    判断完后服务器可以告知客户端是否需要发送请求体，如果拒绝处理，那么浏览器就不需要发送请求体
    
    如果是一次发送，如果此次 post 请求被拒绝的话，那么请求体的数据是无效的，但是它还是被传输过去了，占据带宽
    
    两次发送的缺点就是需要多一次发送的时间
    ```

  - ```
    当然，两次发送也并非是 HTTP 协议强制的，用户是可以自己设置的，可以协定如果 POST 的数据超过 1KB 那么就先发送请求头之类的，如果没有那么就一次发送
    简而言之，所谓的 HTTP 协议都只是一种规范，可以根据具体场景来进行修改
    ```

    



> ### post 和 put 的区别

post 在 HTTP 协议中的语义是 创建资源，put 在 HTTP 协议中的语义是 更新资源

创建资源可以重复创建多个，因此是非幂等的

更新资源都是更新某个指定的资源，因此是幂等的

**一般情况下用 POST 需要在后台进行幂等校验（比如建立流水表）来实现幂等**





## 3、GET 和 POST 携带的消息大小

GET 是将参数放在 URL 上的， HTTP 协议没有明确限制 URL 的长度，因此理论上 URL 多长都可以，即 GET 携带的参数无限

但是，浏览器会对 URL 的长度进行限制，不同的浏览器的 URL 限制不同，比如 chrome 是 2MB

(有种说法就是 IE 浏览器 URL 限制为 2KB，但是最近 IE 浏览器也使用 chrome 的内核了，因此这种说法慢慢的会变成历史)

```
注意：如果是包含中文的传输，中文经过编码后再传输，如果浏览器的编码格式是 UTF-8，那么编码后的一个中文大小为 9 B
```



POST 的 body 在 HTTP 协议上也没有明确的限制，所以理论上 body 多大都可以

但显然，服务器会对这个 body 大小进行限制，总不能每次都传输一个的数据包都是一个 1GB 的文件，tomcat 默认是 2MB





## 4、HTTP 报文结构

[HTTP 报文结构]( https://juejin.im/post/6844904045572800525 )



HTTP 请求报文 由 请求行、请求头、请求体 组成

HTTP 响应报文 由 响应行、响应头、响应体 组成



> ### HTTP 行

请求行包括：HTTP 版本号、请求的 url、请求方式

响应行包括：HTTP 版本号、状态码、状态码描述符（OK 之类的）



> ### HTTP 头部

HTTP 头部分为 `通用头部`、`请求头部`、`响应头部`、`实体头部`四种



**通用头部：请求报文和响应报文通用的头部**

- Date

- Cache-Control

  - ```java
    Cache-Control: no-store	//不缓存
    Cache-Control: no-cache	//如果有缓存，那么请求服务器判断缓存是否需要更新，如果资源没有更新，那么浏览器返回 304，告知浏览器继续使用缓存，一般情况下缓存静态资源时使用
    Cache-Control: max-age=31536000	//表示缓存的有效时间，在浏览器记录 http 和 https 时使用
    ```

- Connection

  - ```java
    Connection: keep-alive	//长连接，默认设置方式
    Connection: close		//短连接
    ```



**实体头部：用来记录 消息体 信息的头部**

- Content-Length：消息体的大小，以字节为单位，一般是发送方携带

- Content-Type：**消息体的 MIME 类型**（媒体类型：Multipurpose Internet Mail Extensions）以及**字符编码**

  - ```java
    Content-Type: text/html;charset:utf-8; //指定 body 中的数据格式为 html 文本，且编码为 utf-8
    ```

- Content-Language：自己能够接收的语言，用来表示 消息体数据 的语言，比如中文、英文

  - ```
    Content-Language: de-DE
    Content-Language: en-US
    Content-Language: de-DE, en-CA
    ```

- Expires：消息体数据过期时间

  - ```java
    Expires: Thu, 01 Jan 1970 00:00:00 GMT
    
    ```

    




**请求头部：**

- Host：请求的服务器的域名

- Referer：请求的来源，告知服务器从哪个页面过来的

- Accept：告知服务器能够接收的 MIME 的类型

  - ```java
    //文本文件： text/html、text/plain、text/css、application/xhtml+xml、application/xml
    //图片文件： image/jpeg、image/gif、image/png
    //视频文件： video/mpeg、video/quicktime
    //应用程序二进制文件： application/octet-stream、application/zip
    
    Accept: text/html,application/xhtml+xml,application/xml;
    
    ```

- Range：客户端请求从某个数据的字节范围，用于断点续传

  - ```
    
    ```

    

**响应头部：**

- Set-Cookie：服务端创建 cooike，然后将 cookie 放在该字段中，让浏览器保存 cookie

- Access-Control-Allow-Origin ：服务器告知服务器允许 指定来源对资源进行访问

  - ```java
    Access-Control-Allow-Origin: https://mozilla.org  
    Vary: Origin	//表示允许 https://mozilla.org 页面来源访问资源，如果是指定特定的来源，那么需要在 Vary 字段添加 Origin
    
    Access-Control-Allow-Origin: *  //表示允许 所有 来源访问资源
    
    ```

- Content-Range：断点传续 可以使用，表示消息体内的数据 在 整个数据流的 位置范围

  - ```java
    Content-Range: bytes 0-499/22400	//0－499 是指当前发送的数据的范围，而 22400 则是文件的总大小。
    
    ```







## 5、断点续传

[断点续传]( https://www.cnblogs.com/findumars/p/5745345.html )

> ### 请求

**请求头添加 Range 字段**，格式为 `Range:bytes = first pos - last pos`

Range: bytes=0-499 表示第 0-499 字节范围的内容 
Range: bytes=-500 表示最后 500 字节的内容 
Range: bytes=500- 表示从第 500 字节开始到文件结束部分的内容 
Range: bytes=0-0,-1 表示第一个和最后一个字节 
Range: bytes=500-600,601-999 同时指定几个范围 



> ### 响应

**响应头添加 Content-Range 字段**，格式为 `Content-Range:bytes = first pos - last pos`

 Content-Range: bytes 0-499/22400 表示文件总大小为 22400，目前 body 中的数据为 0 - 499 字节范围的内容

**响应头中的状态码设置为 206**，表示目前的数据是使用 断点续传 的方式，可能还存在数据没有发送



## 6、短链接跳转到长链接

对于这种跳转，可以使用 301 状态码 和 302 状态码

301 状态码是永久重定向，浏览器会 cache 返回的链接，以后就直接使用该链接访问

302 状态码是临时重定向，浏览器不会 cache，每次访问都会访问原来的链接，然后服务器返回一个临时链接，浏览器再去这个新的链接

一般情况下，如果我们需要统计链接的点击次数，用于大数据 热度统计之类的，那么就要使用 302 状态码，这样每次访问都会请求服务器，服务器可以对点击次数进行一个统计





## 7、HTTP 各个版本的区别 以及 演变的逻辑



### 1、HTTP 0.9

最原始的 HTTP 版本

- 只 支持 GET 请求：由于不支持其他请求，而 GET 是在 URL 中携带请求信息的，因此客户端无法向服务端发送太多信息
- 服务端只能返回 HTML 字符串
- 短连接





### 2、HTTP 1.0

- 新增了 POST、PUT、DELETE 等方式
- 增加了 请求头/响应头、状态码、缓存 (此时的缓存类型较为简单) 等
- 扩充了传输内容，可以传输图片、音频、二进制数据等




HTTP 1.0 的缺点：

- **短连接：**一次  HTTP 请求后都会断开连接（可以通过服务器配置支持多次），每次都需要进行 三次握手、慢启动、四次挥手，频繁通信情况下效率低，   其中 慢启动 主要对文件类大的传输影响大
- **HTTP 队头阻塞：**请求 和 请求之间是有顺序的，下一个请求必须在上一个请求得到服务端响应后才能发出去，如果上一个请求阻塞了，那么后面的请求都会阻塞





### 3、HTTP 1.1

- 长连接：新增了 Connection 字段，内部有个 keep-alive 字段

- 管道化：管道化使得每个请求可以无需等待上一个请求响应即可发出去，不过响应还是按照请求的顺序返回

- 更强大的缓存：Cache-Control

- 断点传输：请求头部使用 Range，响应头部使用 Content-Range




> ### 长连接

由于每次建立和断开 TCP 连接都需要进行 三次握手 和 四次挥手，效率过低

同时每次 TCP 连接建立初期都存在一个慢启动过程，对于大文件传输影响大（因为普通的请求即使慢启动也能较快完成，但是如果是大文件，那么一般需要较长时间，而慢启动显然加长了这个时间）

因此添加一个 keep-alive 字段 使用长连接 以此来 复用连接，减少三次握手 和 慢启动 的开销



> ### 管道化

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





### 4、HTTP 2.0



[HTTP2 的特性]( [https://www.zhihu.com/search?type=content&q=HTTP2.0%20%E4%BA%8C%E8%BF%9B%E5%88%B6%E6%A0%BC%E5%BC%8F%E4%BC%98%E7%82%B9%E5%9C%A8%E5%93%AA](https://www.zhihu.com/search?type=content&q=HTTP2.0 二进制格式优点在哪) )



> ### HTTP2.0 新加入的几个概念

流（Stream）：一个完整的 请求-响应 过程 作为一个流

消息（message）：一个完整的 请求-响应 过程 中的 请求 或 响应 看作一个 message

帧（frame）：每个 message 会分割为多个 frame，它是数据传输的最小单位

- stream identifier：流的id，即用来标识这个帧是属于哪个流的，**HTTP 1.1 中的队头阻塞就是因为响应和请求没有标识符，不知道怎么对应，所以响应才需要按照顺序返回，而这里给定了标识符了，所以就可以打破这个 HTTP 队头阻塞了**



**同个用户 访问 同个域名的 HTTP 请求/响应 都只占用一条 TCP 连接**



> ### HTTP 1.1 和 HTTP 2.0 的区别

数据格式不同：HTTP 1.1 使用的是文本协议；HTTP 2.0 使用的是二进制协议

HTTP 处理 ：HTTP 1.1 一次最多只能开 6-7 个 TCP 连接，每个连接都存在队头阻塞，串行处理； HTTP 2.0 采用多路复用的形式，用一条 TCP 连接同时处理多个请求

头部大小：HTTP 2.0 会压缩头部，因此它的头部比 HTTP 1.1 小



> ### 多路复用

只有一条 TCP 连接，但是可以同时传输多条 stream 的 多个 frame，并且它们互不干扰

因为每个 frame 上面存在 id 标识符 标识了它属于哪条 stream，因此不会出现混乱

只要属于一条 stream 的 frame 按照顺序发送，保证有序性即可

**多路复用 解决了 队头阻塞问题**

<img src="https://upload-images.jianshu.io/upload_images/16844918-b13e6490eedb402c.png?imageMogr2/auto-orient/strip|imageView2/2/w/1200/format/webp" style="zoom:60%;" />

<img src="https://pic.leetcode-cn.com/1599402910-XBmxXM-image.png" style="zoom:60%;" />



> ### HTTP 2.0 的数据格式

HTTP 1.x 使用的是 文本协议，在请求和响应中的都是文本， HTTP 2.0 将 文本协议 替换为 二进制协议

原本 HTTP 1.x 使用文本协议的优点就在于容易进行抓包调试，但是如今大部分网站使用 SSL，SSL 会将数据加密，这样这个优点也没了



**为什么 HTTP 2.0 要使用二进制协议？**

HTTP 2.0 使用二进制协议主要是为了使用 多路复用 和 frame，如果将 HTTP 报文转换为二进制数据，那么就可以拆分一些整体的字符串的传输

如果使用的是文本协议，那么像 Content-Type: text/html;charset:utf-8;  这种是无法进行拆分的，必须一起传输，否则后续难以拼接

而多路复用是将每个 HTTP 报文分割为多个 frame，因此转变为二进制数据流就可以消除这个差异



> ### HTTP 2.0 头部 Header 压缩



压缩原理：

由于每个 HTTP 报文中都存在一些常见的字段，这些字段和值是不会发生改变的

比如 Content-Type: text/html;charset:utf-8; 像这种每个值都是固定的，不会突然 字段 "Content-Type" 变成 "Type"

因此将 HTTP 报文的 常见字段 和 值 维护成一张静态索引表，每个字段和值都存在一个与之对应的索引值，这样在 HTTP 报文中直接使用这个索引值代替，然后发送到 对方主机后，对方主机获取索引值，然后跟自己维护的静态索引表进行映射获取

比如 Content-Type: text/html;charset:utf-8; 的索引值为 2，那么在 HTTP 头部中直接使用 2 来代替它

这里的静态索引表是 HTTP 2.0 定义的一个规范，所有使用 HTTP 2.0 的静态索引表 都是一致的



经过压缩后明显 HTTP 头部的数据存储量少了，这样的话可以在 HTTP 报文大小超过一个 TCP 报文的 MSS 的时候，需要进行 分片，这样压缩后 HTTP 报文小了，能够减少 TCP 报文个数

当然，如果 HTTP 报文大小 <= MSS，那么压缩后也没啥用