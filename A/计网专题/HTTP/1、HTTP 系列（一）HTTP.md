# HTTP



## 1、HTTP 常见的响应状态码  

**1xx**

提示信息，临时响应

 「**100 继续**」请求者应当继续提出请求。服务器响应此状态码表示已受到请求的第一部分，正在等待其余的部分（这里的一个场景是 浏览器 Post 分为两次请求，第一次发送请求头，服务器校验完发现可以处理，因此返回 100 让浏览器继续发送请求体）





**2xx**

成功状态码，我们最愿意看到的

 「**200 OK**」是最常见的成功状态码，表示一切正常 ，响应体 中可能会有数据

 「**201 Created**」Post 请求创建的资源（不是对象，而是图片之类的）已经创建，并且资源的 URL 存储在 Localtion 中

 「**202 Accepted**」服务器接收到了请求，但是还没有处理

 「**204 No Content**」 服务器请求处理成功，服务器的响应 不会返回任何数据

 「**206 Partial Content**」**用于 断点续传**，表示响应返回的 body 数据是断点续传的方式，可能还存在数据没有发送



**3xx**

表示用户请求的资源位置发生了改变，告知用户需要向新的 URL 发送请求（即旧的 URL 不再提供服务）

 「**301 Moved Permanently**」表示永久重定向，服务器会将新的 url 放在响应头 **Location** 中，**浏览器会保存该新的 URL**，后续就直接访问这个 url

 「**302 Found / Moved Temporary**」表示临时重定向，服务器会将新的 url 放在响应头 **Location** 中，**浏览器会去访问新 URL 的内容但是不会去保存该 URL**，往后访问还是使用旧的 url

 「**304 not modified**」浏览器存在缓存但还没有过期时会发送 get 请求服务器，如果资源没有更新，那么服务器返回 304 告知浏览器继续使用缓存



**4xx**：客户端方面的错误

 「**401 Unauthorized**」用户请求还没有认证，用户需要先进行认证

 「**403 Forbidden**」用户已经认证了，但是没有权限访问

 「**404 Not Found**」请求的 url 在服务器上不存在或未找到



**5xx**：服务端方面的错误

 「**502 Bad Gateway**」网关错误，网关或者代理服务器从上游收到无效的响应 ，可能是服务器连接太多，无法处理当前的请求

   [**503  Service Unavailable** ]：服务不可用，服务器超载（连接过多）或者临时维护，所以暂时无法处理用户请求，表明这是一个暂时的情况，可能一段时间后会恢复，但有时候是服务器单纯的用来拒绝 socket 连接

   [**504 Gateway Time-out **]：网关超时



> #### 关于 502 和 504

[502 和 504 讲解一（看看就好）](https://cloud.tencent.com/developer/article/1644806)

[502 和 504 讲解二（看看就好）](https://cloud.tencent.com/developer/article/1373924)



502 和 504 都是网关的问题

**什么是网关？**

这里讲的网关不是计算机网络中的 连接两个局域网的设备，但是却代表的意思相同，连接两个不同网络的设备都可以叫做网关，浏览器发起请求访问服务器资源，nginx 将浏览器的请求转发给 服务器，这里的 **nginx 就是 网关**

nginx 收到浏览器请求后，如果请求的是静态页面，那么自己找到静态页面返回，如果是动态页面，那么会交给上游服务器 php-fpm 去处理



**什么是 nginx?**

**什么是 php-fpm？**

[php-fpm 和 nginx 关系详解](https://zhuanlan.zhihu.com/p/20694204)

php-fpm 是一个 PHP 进程管理器，使用的是 master 和 worker 模式，类似 tomcat，master 进程 跟 nginx 服务器进行通信，接收 nginx 的请求，worker 进程 用来处理这些请求。worker 进程的数量是有限的，**一旦 http 请求没有可用 worker 进程，那么会返回错误，这时候 nginx 收到错误后会返回 502 错误，其他 502 情况还有 php-fpm 进程被 kill 掉，导致无法处理请求，nginx 没有收到数据响应**

如果 worker 进程处理请求的时间 超过了 nginx 的超时时间，那么返回 504 错误



## 2、HTTP 请求方式（GET/POST 等）

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





> #### get 和 post 的区别

上面也说了，GET 和 POST 只是一种 HTTP 规范，并不是强制的，因此可以根据特定情况进行修改，下面讲的是基本情况

- get 是请求资源，请求参数会放在 url 后面，post 是创建和更新数据，上传的数据会存储在 请求 body 中

- get 是幂等的， 在浏览器回退没有问题，post 不是幂等的，在浏览器回退会再次创建资源

- get 返回的数据会被浏览器主动 cache，post 不会，需要手动设置

- post 可以分两次发送，第一次发送请求头，第二次发送请求体

  - 分两次发送的目的：

    ```java
    服务器先获取请求头，获取里面的参数，判断 Athorization 是否能够通过认证、Content-Length 过大 或者 Content-Type 指定的数据格式不支持
    判断完后服务器可以告知客户端是否需要发送请求体，如果拒绝处理，那么浏览器就不需要发送请求体
    
    如果是一次发送，如果此次 post 请求被拒绝的话，那么请求体的数据是无效的，但是它还是被传输过去了，占据带宽
    
    两次发送的缺点就是需要多一次发送的时间
    ```

  - Post 两次发送是有条件的：

    ```java
    当 Content-length 即请求体携带的数据 过大时，那么请求头会先发送请求头，并且在请求头中携带 except=100-continue 
        
    服务器接收到后，根据请求头的数据进行验证，如果可处理，那么返回 100 状态码，让浏览器再发送 请求体
    ```



**Get 和 Post 请求分别可携带数据的大小：**

1. GET 是将参数放在 URL 上的， **HTTP 协议没有明确限制 URL 的长度**，因此理论上 URL 多长都可以，即 GET 携带的参数无限

但是，**浏览器会对 URL 的长度进行限制**，不同的浏览器的 URL 限制不同，比如 chrome 是 2MB

(有种说法就是 IE 浏览器 URL 限制为 2KB，但是最近 IE 浏览器也使用 chrome 的内核了，因此这种说法慢慢的会变成历史)

```
注意：如果是包含中文的传输，中文经过编码后再传输，如果浏览器的编码格式是 UTF-8
```



2. **POST 的 请求 body 携带数据的大小 在 HTTP 协议上也没有明确的限制**，所以理论上 body 多大都可以

但显然，**服务器会对这个 body 大小进行限制**，总不能动不动就传输 1GB 的数据吧，所以tomcat 默认是 2MB



> #### post 和 put 的区别

post 在 HTTP 协议中的语义是 创建资源，put 在 HTTP 协议中的语义是 更新资源

创建资源可以重复创建多个，因此是非幂等的

更新资源都是更新某个指定的资源，因此是幂等的

**一般情况下用 POST 需要在后台进行幂等校验（比如建立流水表）来实现幂等**







## 3、HTTP 报文结构

[HTTP 报文结构]( https://juejin.im/post/6844904045572800525 )



HTTP 请求报文 由 请求行、请求头、请求体 组成

HTTP 响应报文 由 响应行、响应头、响应体 组成



> #### HTTP 行

请求行包括：HTTP 版本号、请求的 url、请求方式

响应行包括：HTTP 版本号、状态码、状态码描述符（OK 之类的）



> #### HTTP 头部

HTTP 头部分为 `通用头部`、`实体头部`、`请求头部`、`响应头部`四种



**通用头部：请求报文和响应报文通用的头部**

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



**实体头部：用来记录 请求/响应 body 信息的头部**

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

- 


**请求头部：**

- Host：请求的服务器的域名

- Referer：请求的来源，告知服务器从哪个页面过来的（正确的拼写是 `Referrer` ，不过由于历史原因，所以没有修改）

- Accept：告知服务器能够接收的 MIME 的类型

  - ```java
    //文本文件： text/html、text/plain、text/css、application/xhtml+xml、application/xml
    //图片文件： image/jpeg、image/gif、image/png
    //视频文件： video/mpeg、video/quicktime
    //应用程序二进制文件： application/octet-stream、application/zip
    
    Accept: text/html,application/xhtml+xml,application/xml;
    
    ```

- Except：浏览器的期望行为，用于 Post 请求分两次传输，发送两个 TCP 数据包

- Authorization：用于身份认证，比如可以存储 token

- cookie：请求时携带 cookie

- Range（断点续传，跟 响应头的 Content-Range 和 响应状态码 206 配套）

  

**响应头部：**

- Date
- Set-Cookie：服务端创建 cooike，然后将 cookie 放在该字段中，让浏览器保存 cookie
- Last-Modified：资源最后一次被修改的时间
- Localtion：存储 `201 Created、301 永久重定向 和 302 临时重定向` 时返回给浏览器的 URL
- Content-Range







## 4、断点续传

[断点续传]( https://www.cnblogs.com/findumars/p/5745345.html )

> #### 请求

**请求头添加 Range 字段**，格式为 `Range:bytes = first pos - last pos`

Range: bytes=0-499 表示第 0-499 字节范围的内容 
Range: bytes=-500 表示最后 500 字节的内容 
Range: bytes=500- 表示从第 500 字节开始到文件结束部分的内容 
Range: bytes=0-0,-1 表示第一个和最后一个字节 
Range: bytes=500-600,601-999 同时指定几个范围 



> #### 响应

**响应头添加 Content-Range 字段**，格式为 `Content-Range:bytes = first pos - last pos`

 Content-Range: bytes 0-499/22400 表示文件总大小为 22400，目前 body 中的数据为 0 - 499 字节范围的内容

**响应头中的状态码设置为 206**，表示目前的数据是使用 断点续传 的方式，可能还存在数据没有发送



## 5、短链接跳转到长链接

对于这种跳转，可以使用 301 状态码 和 302 状态码

301 状态码是永久重定向，浏览器会 cache 响应头中 Localtion 字段的 URL，以后就直接使用该链接访问

302 状态码是临时重定向，浏览器不会 cache 响应头中 Localtion 字段的 URL，后续的访问都是会继续访问旧的 URL



议不再是使用 TCP 协议，而是使用 UDP 协议，即是对 UDP 协议的重新改造