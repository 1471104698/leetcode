# JWT

## 1、JWT 组成

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



## 2、JWT 的鉴定流程



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