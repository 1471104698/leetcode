# Linux 基本命令



## 1、查看占用端口的进程

```shell
netstat -antp | grep 80
```

a 表示显示所有的 socket 连接

n 表示显示 ip 地址

t 表示显示 tcp 连接

u 表示显示 udp 连接

p 表示显示占用对应端口的 pid 和 进程名



这里使用了 管道 `|`，是先使用 `netstat -antp` 查询出所有的 端口占用信息，再将数据传输给子进程，子进程使用 grep 命令表示从数据中筛选出 80 端口的数据





## 2、杀死进程

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
- 15：终止
- 9：强制终止
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



## 3、查看进程状态

ps 命令用于查看进程的状态，比如 CPU 的使用率、内存的占用率

**ps 得到的是执行命令瞬间的快照，是静态的信息**

```shell
ps -aux | less
ps -aux | grep 80
ps -aux | less | grep 80
```

当查询全部的时候可以配合 less 使用，这样可以拖动上下移动看数据

使用 grep 查询单个进程的情况，就没必要使用 less 了

*![image.png](https://pic.leetcode-cn.com/1601043782-mGgLuk-image.png)*



> ### 查看占用 CPU、内存最多的几个进程

**1、ps 命令**

```shell
ps -aux | sort -k4nr | head -N
```

sort -k4nr：

- k：表示按照某列进行排序

- 4： 表示按照第 4 列进行排序

- n： 表示 number sort，根据数值进行排序

- r：reverse，默认排序结果是升序的，这里将结果反过来，即降序输出，即从大到小输出

head -N：指定输出数据的前 N 行 （对应的是 tail -N）



**2、top 命令**

执行 top 命令，然后按下大写 M，按照内存进行排序，按下 P 按照 CPU 进行排序



## 4、动态查看内存、CPU、磁盘的使用情况

top 获取的是动态的 内存、CPU、磁盘使用情况，跟 ps 得到的快照不同

类似于 windows 的任务管理器

```shell
top
```

*![image.png](https://pic.leetcode-cn.com/1601043756-VSgfbj-image.png)*



```shell
top - 22:27:19 up 318 days, 23:37,  3 users,  load average: 0.00, 0.01, 0.05
Tasks: 102 total,   1 running,  98 sleeping,   3 stopped,   0 zombie
%Cpu(s):  0.7 us,  0.3 sy,  0.0 ni, 99.0 id,  0.0 wa,  0.0 hi,  0.0 si,  0.0 st
KiB Mem :  1882088 total,   122064 free,   650100 used,  1109924 buff/cache
KiB Swap:        0 total,        0 free,        0 used.  1068472 avail Mem  

第一行是服务器的信息：
比如系统运行时间等，这里显示的是运行了 318天 23 小时 32 分

第二行 Tasks：
total：进程数
running：正在运行的进程数
sleeping：睡眠的进程数
stopped：停止的进程数
zombie：僵尸进程数

第三行 Cpu(s) CPU 信息：
us：用户态占用 CPU 比
sy：内核态占用 CPU 比
ni：xxx
id：空闲 CPU 占比
wa：等待输入输出的 CPU 占比
hi：硬件 CPU 中断的占比
si：xxx
st：xxx

第四行 Mem 内存信息：
total：物理内存总量
free：空闲物理内存
used：已经使用的物理内存
cache/buff：用作内核缓存的物理内存

第五行：swap 虚拟内存情况：
total：虚拟内存总量
free：空闲量
used：使用量

```

