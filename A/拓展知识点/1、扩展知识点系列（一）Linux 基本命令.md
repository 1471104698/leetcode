# Linux 基本命令



## 1、查看占用端口的进程 - netstat

[netstat 参数讲解](https://blog.csdn.net/m0_37556444/article/details/83000553)



netstat 显示的是 **本机的网络连接的情况**，涉及到 ip、端口、TCP、UDP、socket 之类的都是跟 netstat 有关



netstat 命令行参数如下：

- a：显示所有的 socket 连接

- n：示显示 ip 地址（如果不加 n，那会默认进行 主机名解析，速度较慢，在不需要知道主机名的情况下一般加 -n）

- t：显示 tcp 连接

- u：显示 udp 连接

- p：显示占用对应端口的 pid 和 进程名
- l：显示 处于 LISTENING 状态的 socket 连接

```shell
[root@izm5eb8f6yfdzqoal0f657z ~]# netstat -atnup
Active Internet connections (servers and established)
Proto Recv-Q Send-Q Local Address           Foreign Address         State       PID/Program name    
tcp        0      0 0.0.0.0:22              0.0.0.0:*               LISTEN      1894/sshd           
tcp        0      0 172.31.162.186:22       183.63.119.36:33054     ESTABLISHED 1435/sshd: root@pts 
tcp        0      0 172.31.162.186:22       183.63.119.36:61652     ESTABLISHED 1456/sshd: root@pts 
tcp        0      0 172.31.162.186:53864    100.100.30.26:80        ESTABLISHED 18688/AliYunDun     
tcp6       0      0 :::3306                 :::*                    LISTEN      22794/docker-proxy  
tcp6       0      0 :::6379                 :::*                    LISTEN      5995/docker-proxy   
udp        0      0 0.0.0.0:68              0.0.0.0:*                           768/dhclient        
udp        0      0 172.17.0.1:123          0.0.0.0:*                           542/ntpd            
udp        0      0 172.31.162.186:123      0.0.0.0:*                           542/ntpd            
udp        0      0 127.0.0.1:123           0.0.0.0:*                           542/ntpd            
udp        0      0 0.0.0.0:123             0.0.0.0:*                           542/ntpd            
udp6       0      0 :::123                  :::*                                542/ntpd   
```

以下是 对 netstat 输出字段的解析：

Proto：当前 socket 连接使用的传输层协议（TCP/UDP）

Recv-Q：网络接收队列，收到的数据会存放在 该队列中，如果不为 0 表示存在多少数据没有被进程 recv()

Send-Q：网络发送队列，还有多少数据没有发送出去，如果队列较长时间不为 0，可能是网络拥堵或者对方处理过慢

Local Address：记录当前 socket 连接监听（绑定）了本机上 哪些 ip 和 端口

- ```java
  //0 0.0.0.0:22 表示监听本机服务器的所有的 ip 的 22 端口
  	比如本机服务器有 172.31.162.186 和 172.31.162.210 两个 ip，那么该 socket 连接会监听这两个 ip 的 22 端口
  	一旦有客户端连接上 两个 ip 的 22 端口，那么就会被捕获到，产生对应的 socket 并转交给对应的进程处理
  
  //172.31.162.186:22 表示监听本机服务器 ip 为 172.31.162.186 的 22 端口
  
  //:::3306 ":::" 是 IPv6 的缩写，表示监听本机服务器所有 IPv6 地址的 3306 端口
  
  ```

Foreign Address：与本机对应 Local Address 连接通信的外部 socket

State：链路状态，即 TCP 三次握手、四次挥手显示的状态，比如 LISTENING、 ESTABLISHED\ SYN_SENT 等

PID/Program name：socket 进程的 ID 和 进程名





## 2、杀死进程 - kill

```shell
kill [参数] [进程 id]

比如 
kill 80
kill -9 80
```

其中参数是可选的， 如果参数不选的话，默认是 15，比如上面的 `kill 80`，实际上是 `kill -15 80`

各个参数的含义如下：

- 1：终端断线（不知道啥）
- 2：中断（同 ctrl + c，发送 SIGINT 强制终止进程，比如我们正在操作的进程阻塞，直接 ctrl + c）
- 3：退出（同 ctrl + \）
- **15：终止**（默认）
- **9：强制终止**
- 18：继续（跟 19 相反，fg 命令）
- 19：暂停挂起（同 ctrl + z）

比如我们进入到 redis-cli 中，如果退出可以直接 ctrl + c，直接终止客户端进程

比如我们在 vim 的时候，想出去查询东西而不想关闭掉 vim 的时候，使用 ctrl + z 将 vim 挂起，查询完成后使用 fg 命令回到 vim



> ###  -9 和 -15 的区别

从字面上可以看出区别，一个是强制终止，一个是终止

这就跟线程池的 shutdownNow() 和 shutdown() 一样

当使用 -9 的时候，直接终止进程

当使用 -15 的时候，进程可以选择：

- 立即停止程序
- 释放资源后停止程序
- 忽略信号

因此，当调用 -15 的时候，进程可以做一些准备工作再退出，但是，如果进程在阻塞等，那么就会直接忽略掉这个信号，即进程不会退出

因此我们要杀死进程的时候都是使用 -9



> ### -9 和 ctrl + c 的区别

一般情况下我们当前操作的进程要终止的时候才使用 ctrl + c，相当于快捷键，不需要去查询这个进程的 进程 id 再使用命令行终止

而当后台运行的进程需要终止的时候，我们才查询对应的进程 id，使用 -9 命令终止



## 3、查看进程信息 - ps

[ps 命令](https://www.cnblogs.com/paul03/p/9044997.html)



ps 命令用于捕捉某一时刻下的进程的信息，是进程的快照，如果想要动态查看进程的状态，那么使用 top 命令

进程的信息包括： CPU 的使用率、内存的占用率、进程 ID、进程的状态



> ### ps

简单的 `ps` 只显示在当前终端启动的进程

```shell
[root@izm5eb8f6yfdzqoal0f657z ~]# ps
  PID TTY          TIME CMD	
 1437 pts/1    00:00:00 bash
 1531 pts/1    00:00:00 ps
 3271 pts/1    00:00:00 bash
 3768 pts/1    00:00:00 vim
23018 pts/1    00:00:00 bash
23051 pts/1    00:00:00 mysql
```

TTY：表示进程属于哪个终端，如果与终端无关，那么显示 `?`



> ### ps -aux

我们一般使用的是 ps -aux 命令，关于 `aux` 参数如下：

- a：显示与当前终端相关的所有进程（我们一次可以开多个黑窗口，一个黑窗口就是一个终端，比如在黑窗口 A 开一个 vim 命令，这个 vim 命令只跟当前终端有关）
- x：显示与当前终端无关的所有进程，这样 ax 就是显示所有进程
- u：显示进程所属的用户（一般是 root）、进程开始运行的时间、占用 CPU 百分比、占用内存百分比

ps -u 只会显示当前锁定的用户（一般是 root，可以修改用户）的进程，所以加上 ax 后就是显示所有进程的了

**ps -aux 会显示所有用户的进程 的所有信息，但不包括 父进程 ID**

```shell
ps -aux | less		#当显示数据大，可以使用 less 来滑动窗口
ps -aux | grep 80	#查看 80 进程（无论是否与当前终端相关）的信息，这里是将 ps -aux 查询出来的信息通过 匿名管道 发送给 grep 80，然后再筛选出 80 进程的信息
ps -aux | less | grep 80
```



ps -aux 显示的参数信息如下：

```shell
USER       PID %CPU %MEM    VSZ   RSS TTY      STAT START   TIME COMMAND
root       537  0.0  0.0  26512  1748 ?        Ss    2019   1:44 /usr/lib/systemd/systemd-logind
polkitd    538  0.0  0.5 613016 10380 ?        Ssl   2019   1:02 /usr/lib/polkit-1/polkitd --no-debug
dbus       539  0.0  0.1  58120  2176 ?        Ss    2019   2:56 /usr/bin/dbus-daemon
ntp        542  0.0  0.1  47292  2272 ?        Ss    2019   2:45 /usr/sbin/ntpd -u ntp:ntp -g

#....
```

USER：进程所属的用户							  PID：进程的 id

%CPU：进程占用的 CPU 百分比			   %MEM：进程占用的 内存 百分比

VSZ：加载整个进程到内存中所需的空间	 RSS：实际进程加载到内存的空间

TTY：进程与哪个终端有关，若与终端无关，那么显示 ?

STAT：进程的状态

```java
D 不可中断 Uninterruptible（usually IO）		R 正在运行，或在队列中的进程
S 处于休眠状态								T 停止或被追踪
Z 僵尸进程									 W 进入内存交换（从内核2.6开始无效）
X 死掉的进程
 
//对于BSD格式，还可能会显示：
< 高优先级								 n 低优先级	
L 分页在内存中锁定（对于实时和自定义IO）		s 包含子进程
l 多线程（使用CLONE_THREAD,类似NPTL线程）	+ 位于后台的进程组
```

START：进程开始运行的时间		TIME：进程已经运行的时间

COMMAND：进程的正在执行的命令（可以认为是执行了这个命令所以开启了这个进程）



> ### ps -ef

ps -ef 相比 ps -aux 的优点就是 ps -ef 可以显示进程的父进程 PPID ，但不能显示 内存使用率 之类的

```shell
UID        PID  PPID  C STIME TTY          TIME CMD
root         1     0  0  2019 ?        00:03:58 /usr/lib/systemd/systemd --switched-root --system --deserialize 22
root         2     0  0  2019 ?        00:00:00 [kthreadd]
root         4     2  0  2019 ?        00:00:00 [kworker/0:0H]
root         6     2  0  2019 ?        00:01:33 [ksoftirqd/0]
root         7     2  0  2019 ?        00:00:00 [migration/0]
```



ps -ef 同样可以配合 grep 使用

```shell
ps -ef | grep 80
```

当想完全杀死某个进程的时候，可以这样同时查看它的父进程，将它的父进程一起杀死



## 4、动态查看 进程、内存、CPU、磁盘 信息 - top

`top` ，对进程占用的内存、CPU、进程状态，以及 CPU 、内存、磁盘 使用率 和 剩余空间 进行实时监控，即不单单是对 进程 进行监控，还对 单独对整个 内存、CPU 、磁盘 进行监控

类似于 windows 的任务管理器

*![image.png](https://pic.leetcode-cn.com/1601043756-VSgfbj-image.png)*





## 5、查看进程占用的端口 和 查看占用端口的进程

> ### 查看进程占用的端口

先通过 `ps -aux | grep 进程名 ` 获取进程名对应的所有进程，找到目标进程 id

使用 `netstat -tunlp | grep 进程id` 获取进程的网络状态，找到 Local Adress，获取占用的端口



或者直接使用 `netstat -tunlp | grep 进程名`



区别在于 通过 ps 查出来的进程不一定都是存在网络连接的，因为只有 网络连接才会占用 端口，本机内部自己执行的进程是不会占用端口的，所以 ps 查出来得到端口号需要一个个使用 netstat 查看

而直接使用 netstat 进程名，那么就会获取该进程的所有 socket 连接



```
grep 类似正则表达式，是需要进行匹配的
```



> ### 查找占用端口的进程

使用 `netstat -tunlp | grep 端口号`，可以在 PID 位置看到 进程的 ID 和 进程名



> ### 查看所有被占用的端口号

使用 `netstat -tunlp`直接查看 socket 连接，即可获取被占用的端口号





## 6、查看某个文件某个关键字出现的次数

```shell
grep -o '关键字' fileName | wc -l
```

例子：

```shell
[root@localhost ~]# grep -o '欧阳' a.txt | wc -l
6
```

