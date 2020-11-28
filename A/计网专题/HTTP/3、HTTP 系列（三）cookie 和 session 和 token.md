# cookie 和 session 和 token



## 1、cookie、session

[cookie 机制 和 session 机制](https://www.cnblogs.com/lonelydreamer/p/6169469.html)



如今 “session“ 这个词已经被滥用了，在不同的地方具有不同的含义。

1. 在最开始，session 表示一次会话，即 浏览器窗口打开到关闭的这个期间 	**①**

2. 在涉及到 web 服务器的 ”保持状态“ 的时候，session 又变成了一种用来保持 客户端和服务器 连接状态的机制	**②**

3. 而大部分时候，我们和 cookie 一起讲的时候，session 又是一种上面那种解决方案的存储结构，比如说 “把 xxx 保存在 session 中”	**③**

4. 而在各种用于 web 开发的语言在一定程度上都支持这种解决方案的时候，session 也被用来指代该语言下的解决方案，比如将 Java 提供的  javax.servlet.http.HttpSession 称作 session（注：session 机制不只是 Java 特有，它是一种抽象）	 **④**



可以看出，session 有很多种含义，因此后面我们为了区分各个场景下 session 的含义，约定如下：

- ”浏览器会话期间“ 表示 含义①
- ”session 机制“ 表示 含义 ②
- "session" 表示 含义③
- "HttpSession" 表示 含义④



> #### HTTP 协议的无状态 和 ”保持状态“

在最开始 HTTP 协议出现的时候，本身是无状态的，因为客户端只需要简单的向服务器请求下载某些文件，无论是客户端还是服务器都没有必要去记录这些行为，每一次请求都是独立的。

然后随着网络技术的发展，需求越来越大，因此需要 一种机制来 保持状态，而在不改动 HTTP 协议的情况下，cookie 机制就出现了，**cookie 的作用了为了解决 HTTP 无状态的缺陷**

而后来出现的 **session 机制也是另外一种在客户端和服务器之间保持状态的解决方案**



cookie 机制 和 session 机制之间的区别和联系，用以下例子来解释：

 一家咖啡店（服务器）有喝 5 杯咖啡免费赠一杯咖啡的优惠，然而一次性消费 5 杯咖啡的机会微乎其微，这时就需要某种方式来纪录某位顾客（客户端）的消费数量。想象一下其实也无外乎下面的几种方案：  

1. 该店的店员很厉害，能够记住所有顾客的消费情况。（这种做法就是协议本身就支持状态）
2. 店员给顾客一张卡片，上面记录着顾客的个人信息和消费情况，并且卡片存在有效期限，每次消费时顾客都携带卡片来消费。（这种做法就是客户端在保持状态）
3. 店员给顾客一张卡片，不过上面只有一个随机且不重复的卡号，每次顾客来消费的时候，店员在本子上根据卡号查找对应的消费情况。（这种就是服务器在保持状态）

这个例子看起来是不怎么贴切，不过明白大致情况就行

HTTP 协议由于种种原因，所以在后续也不想设置为有状态的，而是仍然保持无状态的

```java
无状态就意味着服务端可以根据需要将请求分发到集群的任何一个节点，对缓存、负载均衡有明显的好处
    
如果 HTTP 是有状态的，那么上一次用户请求是 服务器A 处理的，而如果该用户下一个请求如果没分发到这个服务器 A，就会拿不到上一次请求留下的状态，这样会影响负载均衡和缓存的实现
```

因此，保持状态就只能选择后面的两种方案，而 session 机制需要客户端 存储一个卡号（JSESSIONID），因此这里就跟 cookie 挂钩了，一般是使用 cookie 来存储这个 卡号的



> #### 1.1、cookie 机制

cookie 机制通过扩展 HTTP 协议来实现，在 HTTP 响应头上添加一个 "set-Cookie" 字段来实现服务器生成 cookie 让客户端保存

**它需要客户端开启对 cookie 的支持**

cookie 是以 key-value 的键值对形式存储的，下面是 HTTP 响应头，里面的 "set-Cookie" 存储了服务器生成的 cookie

```http
HTTP/1.1 302 Found 
Location: http://www.google.com/intl/zh-CN/ 
Set-Cookie: PREF=ID=0565f77e132de138:NW=1:TM=1098082649:LM=1098082649:S=KaeaCFPo49RiA_d8; 
expires=Sun, 17-Jan-2038 19:14:07 GMT; path=/; domain=.google.com 
Content-Type: text/html 
```



在没有设置有效时间的情况下，cookie 的生命周期 为 ”浏览器会话期间“，它存储在浏览器的内存中，一旦关闭 cookie 所在的浏览器窗口，那么 cookie 就消失了，这种 cookie 称作 "session cookie"（会话 cookie）

在设置了有效时间的情况下，cookie 的生命周期即为这个有效时间，它持久化在硬盘上，当关闭浏览器再打开时，cookie 仍然存在，这种 cookie 称作 ”persistent cookies“（持久化 cookie），现在一般使用的都是 持久化 cookie



> #### 1.2、session 机制

单纯的使用 cookie 机制来解决 ”保持状态“ 的问题不可靠，原因如下：

- 因为 cookie 保存在客户端，用户信息容易被窃取（当然可以加密）
- cookie 数据量大时会很占据网络带宽，高并发情况下网络压力大



因此出现了 session 机制

session 机制是一种应用于服务器的机制，服务器存储的是 session，session 中包含了用户的信息，而客户端仅仅只需要存储 JSESSIONID 

当客户端请求的时候，服务器会从请求中判断是否存在 JSESSIONID ，如果存在，那么根据 JESSIONID 获取 session，如果不存在，那么创建一个 session 保存起来，并且将对应的 JSESSIONID 返回给客户端，下次客户端请求的时候带上这个 JSESSIONID ，服务器即可知道用户已经登录，不会让客户端跳转到登录界面



session 机制一般是配合 cookie 机制来使用的，利用 cookie 来保存 JSESSIONID，客户端每次请求的时候都带上 JSESSIONID

但是客户端可能会关闭 cookie 机制，导致无法使用 cookie 来存储 JSESSIONID

因此这时候就涉及到另外一种常用的方式：**URL 重写**，即把 JSESSIONID 附加在客户端请求的 URL 路径上，比如：

```http
http://...../xxx?jsessionid=ByOK3vjFD75aPnrF7C2HmdnV6QZcEbzWoWiBYEnLerjQ99zWpBng!-145788764 
```



在说 session 机制的时候，有可能会听到这么一种 **误解**：”当关闭浏览器的时候，session 就消失了“

实际上并不是这么回事，除非客户端通知服务器删除 session，防止服务器会一直保留，直到到达有效时间（默认 30min）

而浏览器关闭之前，也不会主动去通知服务器它将要关闭了（想象一下我们平时直接关闭浏览器，它根本没有机会通知）。

之所以会有这种误解，是因为有的网站使用的 cookie 是会话 cookie，当浏览器关闭时，这个 cookie 就消失了，导致再次打开浏览器的时候，由于不存在原本的 cookie，也就不存在 JSESSIONID，因此用户就需要再次登录了。

而实际上服务器上的 session 并没有删除，只有等到过期了才会删除



> #### 1.3、cookie 和 session 的区别

**cookie 和 session 的区别：**

- cookie 保存在客户端；session 保存在服务器

- 浏览器限制的 cookie 的存储容量有限，一般是 4KB；session 没有上限，但是一般不能存储过多，否则服务器压力过大

- cookie 只能存储 key-value 这种键值对类型的文本信息；session 没有大小、类型限制，可以存储任意数量、任意类型的数据

- cookie 支持跨域，比如将 domain 设置为 ` .test.com` ，那么访问一切以该 domain 为后缀的域名都能够携带该 cookie；session 不支持跨域，session 仅在它所在的域名有效



在分布式情况下，用户第一次访问的时候，session 只会存储在它访问的服务器上，而下次访问的时候由于负载均衡的问题，如果将请求转发到别的服务器上，那么就会出现问题了，因此存在以下解决方法：

- 粘性 session：将某个用户请求固定转发到某台服务器上，可以根据 id hash 服务器数 得到转发的服务器编号
- session 复制：将用户的 session 同步到所有的服务器上
- session 缓存：利用 redis 这种中间件来缓存 session，服务器本身不存储 session，每次请求都去 redis 中以 JSESSIONID  作为 key 获取



## 2、token

由于 session 机制需要服务器来保存用户信息，一旦在高并发的情况下，就有很多的 session 来存储，对于服务器来说压力大（当然可以选择存储在 redis 中，这里是讲服务器存储 session 的方案）

因此出现了 token 的验证方式来 ”保持状态“，服务器根据 用户信息 和 密钥 secret 结合一个 hash 函数来计算出 信息摘要，然后返回给客户端存储，此时服务器不需要存储什么，只需要保留 secret 即可，当下次客户端访问的时候，服务器只需要获取请求中的 信息摘要，然后经过相同的 hash 计算得到另一个 信息摘要，然后两个进行比对，如果一致，那么表示用户已经登录。（这里的校验是为了防止篡改）

通过 token 代替 session 可以减轻服务器的压力，但是 token 也存在一些问题，所以它无法完全的代替 session，同时由于 session 可以保存在 redis 中，不需要服务器保存了，所以 session 更加不会被代替。

token 由于续签问题，所以一般只会用于一次验证的情况，比如我们有时候需要验证，网站都是发送一封邮件让我们点击邮件进行验证，并且存在有效期，这种就可以使用 token 实现，因为不需要涉及到续签



token 一般实现是 JWT







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