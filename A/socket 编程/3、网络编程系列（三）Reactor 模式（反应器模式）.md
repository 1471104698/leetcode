# Reactor 模式（反应器模式）



## 1、传统 IO 模型

在传统的 socket 处理中，服务器为每个 socket 连接都分配一个线程去执行，一个 socket 的读写、数据的处理都是由分配到的一个线程来完成的，即 I/O 操作 和 非 I/O 操作都由一个线程来完成

 ![img](https://upload-images.jianshu.io/upload_images/4235178-5f488db8661eefd3.png) 



**这种模型 无论是使用 BIO 还是 NIO，这种做法的问题都很明显：**

- socket 调用 read()、write() ，磁盘 IO 或 网络 IO 过程中数据在内核态和用户态之间的拷贝，这段时间显然线程无事可干

- 需要频繁的创建和销毁线程，服务器压力大
- 每个 socket 都分配一个线程，线程也是重要资源，高并发情况下基本撑不住



## 2、Reactor 单线程模型（redis 6.0 之前的模型）

该模式使用了 NIO + IO 多路复用的单线程，**网络 IO 和 业务处理都是同一个线程**

Reactor 模式分为三个部分：

- Reactor
- Acceptor
- Handler

Reactor 用来监听客户端 socket 的事件，内部有一个 事件分发器 Dispatch，根据不同事件类型分发给不同的处理器处理

当有客户端连接时，将该事件分发给 Acceptor，让它处理 accept()

当有客户端读写请求时，将该事件分发给 Handler，让它处理 read() 和 write() ![img](https://upload-images.jianshu.io/upload_images/4235178-4047d3c78bb467c9.png)



 **这种模型是 IO 多路复用的最初模型，但还是存在问题：**

整个服务模块使用单线程，Reactor 事件监听、所有 socket 的 accept()、read()、write()、业务处理都是使用同一个线程，IO 和 非 IO 都在同一个线程中处理，这样显然会无法及时响应后面的 socket 请求，同时无法利用到 CPU 核心



## 3、Reactor 单线程 IO 多线程业务 模式

Reactor 模型将 业务处理 和 IO 读写进行分离：

- 使用一个 主线程 用来处理 socket IO

- 使用线程池 作为工作线程 来处理用户的业务请求

Reactor 线程监听所有的 socket 事件，并且完成 accept()、read()、write() 操作，当需要进行业务处理时，将请求提交给线程池，让工作线程进行处理

  ![img](https://oscimg.oschina.net/oscnet/up-58e52cf7144aec461df5a5d07222aeecb40.png)  



这种模型的好处在于 IO 和 非 IO 进行分离，主线程可以专心进行 socket IO，能够在一定程度上及时处理 socket IO，不会因为业务处理的原因导致拖慢对后续 socket 请求的响应

**但是这种模式还是存在问题：**

只有一个线程在处理所有的 socket IO，在高并发情况下，大量的 socket 连接存在 IO 请求，有大量的 read()、write() 操作，一个线程显然是无法及时响应的



## 4、Reactor 多线程 IO 多线程业务 模式

这个模式是 Reactor 最终定型的模型，使用 多线程处理 IO 和 多线程处理 业务

将 Reactor 设计为主从 Reactor 模式，只存在一个主 Reactor，主 Reactor 中维护着多个从 Reactor



主 Reactor 监听所有的 socket 事件，通常是一个线程，用来处理 socket 的建立，然后将新建立的 socket 连接注册进 从 Reactor 中，从 Reactor 进行 socket IO 间的数据交互（read()、write()）和 业务处理

同时每个 从 Reactor 都维护了一个线程池，它们本身会处理 socket 的 IO，当需要进行业务处理时，将请求提交给各自线程池的工作线程



 ![img](https://upload-images.jianshu.io/upload_images/4235178-929a4d5e00c5e779.png) 