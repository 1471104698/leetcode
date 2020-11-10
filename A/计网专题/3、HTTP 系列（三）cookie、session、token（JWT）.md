

## 1、cookie、session、token

> ### 故事背景

[发展史](https://zhuanlan.zhihu.com/p/63061864 )



最开始的互联网时代，服务器和客户端并不会去存储用户的状态。这段时间对服务端和客户端来说都很 happy

随着互联网的发展，像购物网站之类的兴起，越来越需要记住用户的状态，比如哪些人登录了系统，哪些人在购物车中放了哪些东西，这些全部需要将每个人区分开来，由于 HTTP 是无状态的，因此出现了一个新的事物 sessionID，作为一个会话的唯一标识，简单讲就是一个独一无二 的字符串，每个人收到的都不一样。因此发送 HTTP 请求的时候，只需要将 sessionID 一起发送过来，就知道是谁是谁了

但是，这样对用户来说爽了，对服务器来说可不是，因为用户只需要存储自己的 sessionID 即可，而服务器需要存储所有用户的 sessionID，当存在几千万，几亿个用户的时候，那对服务器内存来说就是一个巨大的开销

并且，在后来，出现了分布式，多台机器作为一个集群提供同一个服务，这样不同的用户就会被 推送 到不同的机器上去，而每台内存是不共通的，这样的话，假设 用户 A 在第一次请求同时进行登录，被推送到机器 1，机器 1 存储了用户 A 的 sessionID，用户 A 第二次请求被推送到机器 2，而机器 2 没有 用户 A 的 sessionID，因此会要求 用户 A 进行登录，这样显然是不友好的。因此就出现了这么几个方法：

- **粘性 session：**固定将某个用户推送到固定的某台机器上去
- **session 复制：**一个用户的 session 通过复制发放到每台机器上去
- **session 共享：**通过 redis 等缓存中间件存储 session，这样每台机器只需要到对应的 缓存中间件上去访问 session 即可，不过需要做成集群防止宕机

但是，有人就开始思考，为什么 tm 的服务器要保存这些可恶的 session 呢？只让每个客户端去保存多好

可是如果 服务端不保存 session，怎么验证客户端发送过来的 sessionID 是服务端生成的而不是伪造的呢？

嗯，只需要验证即可，只要能够做到验证，就可以舍弃掉 session

用户 A 登录了系统，服务端生成一个 token(n令牌)，里面包含了用户 A 的 userId，下次用户 A 再次请求的时候，直接将 token 发送过来，服务端进行验证即可。。。但是这本质上和 sessionId 没有什么区别，任何人都可以伪造啊。。。得想点办法

加密就行了，做成类似 CA 证书的那种，自己生成一个密钥，然后对混合到用户数据中，使用特定的 加密算法加密，做成一个 token，发送给客户端，让客户端自己存储，等到下次请求的时候，客户端将 token 发送回来，这样的话，服务端只要 使用对应的密钥 和 用户数据混合，再使用相同的加密算法加密，判断生成的 token 和 客户端发送过来的 token 是否一致，如果一致，则验证通过

这样的话，服务端就无需保存什么东西了，使用 token 代替 session，只需要让 客户端自己保存 token，服务端进行验证即可



> ### cookie 和 session

[cookie session 详解]( https://www.cnblogs.com/huchong/p/8481779.html )



**cookie 和 session 用来解决 HTTP 无状态的问题**

cookie 保存在浏览器，session 保存在服务器



cookie 和 session 的联系使用的是 sessionId，当浏览器第一次访问服务器时，服务器会将 为用户生成一个随机数作为 sessionId，将它存储到 cookie 和 session 中，然后将 cookie 设置到响应头的 set-cookie 字段中，返回给浏览器；

由于 cookie 是保存在浏览器的，所以不安全，如果保存用户信息的话可能会被盗取或者篡改，解决方法有二：

- 将用户信息加密然后存储到 cookie 中
- 将用户信息保存到 session 中

一般使用的是第二种方法，由 session 保存用户的信息，但是这种方法的缺点是服务器保存用户信息，对服务器压力大

验证：浏览器再次发送请求，这次请求中携带了 cookie，服务器获取到 cookie，得到里面的 sessionId，然后通过 sessionId 获取对应的 session，如果不为空，那么表示用户已经登录了，我们可以直接从 session 中获取到用户的信息，这样就记录了用户的状态，免去了用户登录的过程



**cookie 和 session 的区别：**

- cookie 保存在客户端；session 保存在服务器（不存储在内存中，而是存储在数据库，分布式情况下存储在 redis）

- 浏览器限制的 cookie 的存储容量有限，一般是 4KB；session 没有上限，但是一般不能存储过多，否则服务器压力过大

- cookie 只能存储文本信息；session 没有大小、类型、安全限制，可以存储任意数量、任意类型、任意安全级别的数据

  ```java
  HTTP/1.1 200 OK
  Content-type: text/html
  Set-Cookie: "name = value; path=/"	//键值对的形式，key=value
  Other-header: other-header-value
  ```

- cookie 支持跨域，比如将 domain 设置为 ` .test.com` ，那么访问一切以该 domain 为后缀的域名都能够携带该 cookie；session 不支持跨域，session 仅在它所在的域名有效（可以通过存储到 redis 解决）



**当浏览器禁用 cookie 时，存在两种验证方式：**

- 浏览器只保存 sessionID，然后请求的时候在 URL 上携带 sessionId
- 浏览器保存 token





## 2、JWT

### 1、JWT 组成

[JWT 组成](http://blog.leapoahead.com/2015/09/06/understanding-jwt/ )



JWT（JSON Web Token） 的数据形式都是 JSON 

 JWT 实际上就是一个字符串，它由三个部分组成：头部 Header、载荷 Payload、签名 Signature 



**头部 Header：**

```json
{
  "typ": "JWT",		//token 类型
  "alg": "RS256"	//签名使用的加密算法
}
```

描述 该 JWT 的基本信息，以及生成签名使用的 散列算法

使用 base64 编码后变成

```java
eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9
```



**载荷 payload：**

```json
{
    "iss": "server JWT",	//签发者
    "iat": 1441593502,		//签发时间
    "exp": 1441594722,		//过期时间
    "aud": "www.example.com", //接收该 token 的域名
    "sub": "userId",		//接收该 token 的用户
}
```

payload 存储了 JWT 的签发信息和过期信息

使用 base64 编码后变成：

```json
eyJmcm9tX3VzZXIiOiJCIiwidGFyZ2V0X3VzZXIiOiJBIn0
```



**签名：**

将上面的 头部 和 载荷 **base64 编码后的字符串** 进行拼接，中间用 `.` 隔开，变成：

```java
eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJmcm9tX3VzZXIiOiJCIiwidGFyZ2V0X3VzZXIiOiJBIn0
```

然后需要再提供一个 密钥（secret），比如我们使用 secret = "mystar" 作为密钥，然后跟上面的拼接字符串一起使用 HS256 算法进行 hash，那么得到的 签名为：

```java
rSWamyAYwuHCo7IFAgd1oRpSP7nzL7BF5t7ItqpKViM
```

<img src="http://blog.leapoahead.com/2015/09/06/understanding-jwt/sig1.png" style="zoom:50%;" />





 最终得到的 JWT 为：头部 + 载荷 + 签名

```
eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJmcm9tX3VzZXIiOiJCIiwidGFyZ2V0X3VzZXIiOiJBIn0.rSWamyAYwuHCo7IFAgd1oRpSP7nzL7BF5t7ItqpKViM
```



> ### 签名的作用

签名的作用是为了防止别人篡改伪造 JWT，比如当中间人篡改了 payload，然后使用相同的 散列算法生成 signature，

由于信息发生了改动，并且中间人并不知道生成 signature 时的密钥（secret），因此最终生成的 signature 是不一致的

<img src="http://blog.leapoahead.com/2015/09/06/understanding-jwt/sig2.png" style="zoom:50%;" />



 因此服务器根据 Header 中提供的散列算法以及自己存储的密钥 对 Header 和 Payload 进行 hash，如果发现和 JWT 中的签名不一致，那么意味着 JWT 被篡改，那么服务器应该拒绝这个请求



![preview](https://pic3.zhimg.com/v2-f1556c71042566d4a6f69ee20c2870ae_r.jpg)

> ### 信息暴露

我们可以发现，JWT 中的 Header 和 Payload 相当于是明文传输，因此 JWT 不用来存储敏感信息，一般就是存储用户的 id

如果想要存储重要信息，那么可以使用 HTTPS 来进行传输



### 2、JWT 的鉴定流程



注意，JWT 是用来代替 session 记录用户的登录状态的，服务器不用自己存储减少压力，同时还能防止 CSRF 攻击



目前项目都是使用的微服务，将一个个功能模块分割出来，一般情况下的鉴定流程：

- 用户第一次请求登录
- 网关 Zuul（模块 1） 拦截到请求后，将请求转发到 授权中心（模块 2）
- 授权中心生成 JWT，返回给 Zuul，Zuul 将 JWT 返回给 用户（注意，使用的都是 https）
- 用户其他请求携带 JWT
- Zuul 将 JWT 交给 授权中心
- 授权中心 对 token 的 header 进行 base64 解码，获取签名的加密算法，然后使用该加密算法对 header 和 payload 以及一个密钥 进行加密，然后进行验证，验证通过请求放行

 ![在这里插入图片描述](https://img-blog.csdnimg.cn/20190603112751260.png?) 

 



我们可以看出，对于普通的 JWT 验证，所有用户的验证请求导致  Zuul 和 授权中心的频繁交互，系统间的网络请求频率过高，效率差，因为对于 "验证" ，需要越快越好

- 因此我们使用 RSA 作为加密算法，先使用 RAS 算法生成一对 公钥 和 私钥，公钥交给 Zuul 以及 其他的微服务，私钥 授权中心自己保管
- 用户第一次请求登录
- Zuul 拦截请求，发现没有 JWT，将请求转发给 授权中心
- 授权中心 生成 JWT，并且使用 私钥 对 JWT 进行加密，返回给用户
- 用户其他请求携带 JWT
- Zuul 拦截请求，使用 公钥对 私钥加密了的 JWT 进行解密，获取 原生的 JWT，然后对 JWT 进行验证，验证通过就放行，这样就无需再访问授权中心



这里整体讲一下使用 RSA 和 不使用 RSA 的区别：

- 没有使用 RSA，那么生成签名的密钥只能由 授权中心 保管，防止泄漏导致其他人伪造 jwt，因此每次都验证都需要转发到 授权中心，效率低；
- 使用了 RSA，那么公钥可以下发到 Zuul 和 其他微服务，这样的话验证就可以让它们来进行，不需要经过授权中心，授权中心只负责签发，同时由于 私钥 在 授权中心，只要保证授权中心的私钥不泄漏，那么就无法伪造 jwt，因为就算 公钥泄漏了，它们也只能使用公钥加密，这样在 Zuul 中是无法使用公钥解密的，有效防止 伪造 JWT 的问题
- 但实际上，**如果使用了 RSA，JWT 可以省去 签名 了，因为公钥可以解密，表示没有被篡改，不再需要使用 签名验证**

**因此在微服务场景下，使用 RSA 加密的 JWT，（按理说）可以省去 签名，同时可以降低授权中心的压力，将验证分摊到各个微服务中，同时保证了安全**





## 3、token 和 session 的优劣

token 相比 session 的优点：

- session 
  - 需要服务端存储，当有大量用户的时候会占内存
  - 在分布式的情况下需要考虑 session 共享问题
  - 依赖于 cookie，容易发生 CSRF 攻击

- token
  - 服务端无需存储，节省内存
  - 无需考虑分布式的问题
  - 不依赖于 cookie，可以放在 HTTP header 中，预防 CSRF 攻击

session 相比 token 的优点：

- session 
  - 不需要考虑续签问题，session 默认有效期为 30min，如果在 30min 内用户存在访问，那么 session 会重新刷新为 30min
- token 
  - 存在续签问题，JWT 是使用 payload 中的 exp 字段来记录过期时间的，并且 payload 是参与签名生成的，一旦 payload 中的任何一个字段发生改变，生成的签名都会发生改变，因此不能够轻易修改 exp 字段，即 JWT 的特性导致它天然不支持续签



token 无法续签的这个特性使得它无法在单点登录这方面替代 session，现在 Spring Security 仍然在使用 session

一般情况下，服务器无法主动注销 token，如果修改 secret 的话，会使得所有使用该 secret 的 token 全部失效，只能等待 token 主动失效，但这样的话，就存在安全性问题了，如果一个用户修改密码，那么按照正常流程来说，旧的 token 应该是不能用了的，需要重新登录获取新的 token，但是由于服务器不能主动使 token 失效，因此就导致旧的 token 仍然有效。

对此，解决方法有以下：

- 添加 token 黑名单，无效的 token 加入黑名单，这样可以主动注销某个 token，只要请求的 token 位于该黑名单中，那么请求无效
- 为每个用户分配一个 secret，而不是所有用户共用一个 secret，这样可以针对某个用户修改 secret，使得他的 token 无效



但实际上 **token 更加适合 短期的、一次性验证 的场景**： 比如用户注册后需要发一封邮件让其激活账户，通常邮件中需要有一个链接，这个链接需要具备以下的特性：能够标识用户，该链接具有时效性（通常只允许几小时之内激活），不能被篡改以激活其他可能的账户…这种场景就和 jwt 的特性非常贴近，jwt 的 payload 中固定的参数：iss 签发者和 exp 过期时间正是为其做准备的。 