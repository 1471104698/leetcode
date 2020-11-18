# socket 编程



linux 中一切皆文件，socket 也是一个文件。

socket() 会返回一个 socket 描述符，它是唯一标识一个 socket 的，作用跟文件描述符一样

后续对 socket 的操作都是传输该 socket 描述符给内核态进行操作的



## 1、socket 通信流程图

![img](https://img-blog.csdn.net/20150510153905472)



## 2、socket 方法介绍



> ### bind()

将该 socket 绑定指定的 ip 和 端口，通过 ip 地址 + 端口 提供服务，相当于是对外开放这个指定的 ip + 端口 的接口

服务端绑定就是对外开放表示客户端来跟这个接口进行通信

客户端绑定就是表示对外跟服务端使用这个接口进行通信



> ### listen() 和 connect()

connect() 是客户端 socket 的方法，listen() 是服务器 socket 的方法

服务端在 bind() 绑定 ip + 端口后，调用 listen()，在内核中监听这个 socket，等待客户端连接

客户端在 bind() 后，调用 connect(server_ip) 向指定服务端发起连接

服务端在 listen() 后如果收到客户端的 connect()，这个 connect() 最开始就相当于 TCP 三次握手中的第一次握手，后续会进行两次握手

这里的 listen() 并不会发生阻塞，它会一直进行监听，类似于我们的后台进程，自己在运行



> ### accept()

accept() 是服务器 socket 的方法

当三次握手完成后，客户端的 socket 会放在 全连接队列（accept queue） 中

服务端只需要调用 accept() 就可以获取 全连接队列中的 socket，创建一个与 该 socket 对应的 新 socket，返回给用户进程

当 accept queue 中没有 socket 时，那么会服务器线程会阻塞



> ### read()、write()

read() 和 write() 都需要深入到 内核中，都是通过系统调用完成的

read() 的系统调用是 recv()，write() 的系统调用是 send()



在内核中存在两个TCP 缓存区：TCP 发送区、TCP 接收区

read() 就是 内核从这个 TCP 接收区中读取数据，然后拷贝到用户态

write() 就是 从用户态将数据拷贝到内核态，然后将数据写入到 TCP 发送区中，写进去后 write() 函数就返回了，什么时候发送数据出去并不会通知，具体发送时机看 TCP 的 Nagle 算法



如果 TCP 发送区已满时，write() 会阻塞

如果 TCP 接收区已满时，read() 会阻塞



> ### close()

**某一方主动调用 close() 函数是四次挥手的开始**

处于主动关闭的一方， 调用 close() 会发送 FIN，进入 FIN_WAIT1 状态

处于被动关闭的一方，在收到 FIN 后会回发一个 ACK，处于 CLOSE_WAIT 状态，然后数据处理完，调用 close() 后发送 FIN，处于 LAST_ACK 状态



## 3、Java 中 socket 编程

客户端 socket 为 `java.net.socket`，服务器 socket 为 `java.net.ServerSocket`



> ### 客户端 socket 

常用构造方法：

```java
Socket(String host, int port) throws UnknowHostException, IOException 
Socket(InetAddress address, int port) throws UnknowHostException, IOException
```



一般情况下我们声明 socket 为：

```java
//指明连接的服务器 socket 的 ip 和 端口
Socket socket = new Socket("127.0.0.1", 80);
```



使用的是 第一个构造方法，逻辑如下：

可以看到它内部调用了第二个构造方法，实际上就是将 服务器的 ip 和 port 封装为 InetAddress

```java
public Socket(String host, int port){
        throws UnknownHostException, IOException
    {
        this(new InetSocketAddress(host, port), null, true);
    }
}
```



第二个构造方法逻辑如下：

可以看出，它在内部自己调用了 connect() 去连接服务器

当调用了 connect()，即开始 三次握手中的第一次握手，发送 SYN 报文，处于 SYN_SENT 状态

```java
private Socket(SocketAddress address, SocketAddress localAddr,
               boolean stream) throws IOException {
    setImpl();

    // backward compatibility
    if (address == null)
        throw new NullPointerException();

    try {
        createImpl(stream);
        if (localAddr != null){
            bind(localAddr);
        }
        //调用 connect() 连接服务器 socket
        connect(address);
    } catch (IOException | IllegalArgumentException | SecurityException e) {
        try {
            close();
        } catch (IOException ce) {
            e.addSuppressed(ce);
        }
        throw e;
    }
}
```



> ### 服务器 socket

常用构造方法：

```java
ServerSocket();
ServerSocket(int port);
ServerSocket(int port, int backlog, InetAddress bindAddr);
```

在 ServerSocket 中不需要去指定监听的 ip 地址，在内部构造方法内部，它会默认监听 `0.0.0.0` ，表示监听本机所有的 ip 地址中所有的指定端口，比如 本机中存在 192.168.3.127 和 192.168.3.128 两个 ip 地址，并且指定 port = 80，那么这个 服务器 socket 就会同时监听 192.168.3.127 和 192.168.3.128 两个 ip 地址的 80 端口

*![image.png](https://pic.leetcode-cn.com/1605104324-tutSJU-image.png)*

不过可以自己调用 bind() 来监听特定的 ip 地址



我们一般声明 服务器 socket 为：

```java
ServerSocket socket = new ServerSocket(80);
```

可以自己调用 bind() 来绑定特定的 ip，而不再是单纯的监听 0.0.0.0 
但这里必须调用 默认构造方法，因为 bind() 只能绑定一次

```java
ServerSocket socket = new ServerSocket();
socket.bind(new InetSocketAddress("127.0.0.1", 8080));
```



构造方法逻辑如下：

它在内部调用了 第三个构造方法：

```java
public ServerSocket(int port) throws IOException {
    this(port, 50, null);
}
```



第三个构造方法逻辑如下：

```java
public ServerSocket(int port, int backlog, InetAddress bindAddr) throws IOException {
    setImpl();
    if (port < 0 || port > 0xFFFF)
        throw new IllegalArgumentException(
        "Port value out of range: " + port);
    if (backlog < 1)
        backlog = 50;
    try {
        /*
            调用 bind() 进行默认绑定 0.0.0.0 ip 地址 和 port
            这里的 InetSocketAddress 对象默认是 0.0.0.0，我们只能后续调用 bind() 重新进行绑定
        */
        bind(new InetSocketAddress(bindAddr, port), backlog);
    } catch(SecurityException e) {
        close();
        throw e;
    } catch(IOException e) {
        close();
        throw e;
    }
}
```



bind() 方法逻辑如下：

这个 bind() 是提供给用户调用的，即上面调用的 server.bind()，实际上该方法只是一个封装方法，真正的 bind() 在内部

同时还会自动执行 listen()，进入 LISTEN 状态

```java
public void bind(SocketAddress endpoint, int backlog) throws IOException {
    if (isClosed())
        throw new SocketException("Socket is closed");
    if (!oldImpl && isBound())
        throw new SocketException("Already bound");
    if (endpoint == null)
        endpoint = new InetSocketAddress(0);
    if (!(endpoint instanceof InetSocketAddress))
        throw new IllegalArgumentException("Unsupported address type");
    InetSocketAddress epoint = (InetSocketAddress) endpoint;
    if (epoint.isUnresolved())
        throw new SocketException("Unresolved address");
    if (backlog < 1)
        backlog = 50;
    try {
        SecurityManager security = System.getSecurityManager();
        if (security != null)
            security.checkListen(epoint.getPort());
        //真正的调用 bind() 绑定 ip 和 端口
        getImpl().bind(epoint.getAddress(), epoint.getPort());
        //调用 listen()，内部是调用 native 的 listen0()，交给内部来监听 客户端 socket 连接
        getImpl().listen(backlog);
        bound = true;
    } catch(SecurityException e) {
        bound = false;
        throw e;
    } catch(IOException e) {
        bound = false;
        throw e;
    }
}
```



调用 socket.accept()，等待客户端 socket 连接

```java
public Socket accept() throws IOException {
    if (isClosed())
        throw new SocketException("Socket is closed");
    if (!isBound())
        throw new SocketException("Socket is not bound yet");
    //预先为将来要连接的 客户端 socket 创建好一个用于服务器自己管理的 socket 对象 s
    Socket s = new Socket((SocketImpl) null);
    //调用 implAccept()，内部等待 客户端 socket 连接，然后存储到 s 中
    implAccept(s);
    return s;
}

protected final void implAccept(Socket s) throws IOException {
    SocketImpl si = null;
    try {
        if (s.impl == null)
            s.setImpl();
        else {
            s.impl.reset();
        }
        si = s.impl;
        s.impl = null;
        si.address = new InetAddress();
        si.fd = new FileDescriptor();
        //进入该方法
        getImpl().accept(si);

        SecurityManager security = System.getSecurityManager();
        if (security != null) {
            security.checkAccept(si.getInetAddress().getHostAddress(),
                                 si.getPort());
        }
    } catch (IOException e) {
        if (si != null)
            si.reset();
        s.impl = si;
        throw e;
    } catch (SecurityException e) {
        if (si != null)
            si.reset();
        s.impl = si;
        throw e;
    }
    s.impl = si;
    s.postAccept();
}

/*
	还是一个同步方法
*/
protected synchronized void accept(SocketImpl s) throws IOException {
    if (s instanceof PlainSocketImpl) {
        // pass in the real impl not the wrapper.
        SocketImpl delegate = ((PlainSocketImpl)s).impl;
        delegate.address = new InetAddress();
        delegate.fd = new FileDescriptor();
        //在这里会真正调用 accept()，阻塞等待 客户端 socket
        impl.accept(delegate);
        // set fd to delegate's fd to be compatible with older releases
        s.fd = delegate.fd;
    } else {
        impl.accept(s);
    }
}
```

