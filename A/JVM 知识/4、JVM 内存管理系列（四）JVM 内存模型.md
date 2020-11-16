# JVM 内存模型



JDK 1.8 的 JVM 内存空间如下：

![img](https://pic4.zhimg.com/80/v2-fb2f583edea32cfde008ecb5390244b9_720w.jpg?source=1940ef5c)



## 1、程序计数器（线程私有）



程序计数器用来**记录下一条将要用来解释的 字节码指令的地址**，这里的字节码指令并不能直接让 CPU 执行，而是需要通过 JVM 解释器解释成二进制数才能被 CPU 执行

也许有这么一个疑问：直接让 JVM 解释器 按照字节码指令的顺序 解释下去就行了，为什么需要使用程序计数器来记录下一条执行的字节码指令，这不是显得多余吗？

在单线程情况下，的确是不需要程序计数器的，但是大部分情况下是多线程执行，操作系统使用的是 CPU 时间片轮转的调度方式，当一个线程的 时间片用完后，需要将线程挂起，CPU 会去调用别的线程，JVM 解释器会去解释别的线程的字节码指令，即 JVM 解释器本身不会保存哪个线程执行到哪条字节码指令的，当 CPU 重新调用该线程时，需要一个东西 来告知 JVM 解释器应该从哪里开始执行，这个东西就是程序计数器，可以当作是一个存储 JVM 指令地址的 long 型变量（当然不可能这么简单）。



当运行 native 方法时，由于 native 方法不是 Java 代码，所以 code 中不存在该方法的字节码指令，那么就不会存在该字节码指令的地址了，所以程序计数器值为空



> #### 无 OOM

程序计数器 是 JVM 内存模块中唯一不会 OOM 的区域



## 2、虚拟机栈 和 本地方法栈（线程私有）

虚拟机栈 和 本地方法栈 都是线程私有的，即每个线程都有一个虚拟机栈和本地方法栈



- 虚拟机栈存储的是调用当前线程调用的 Java 方法的栈帧，**栈帧中存储了入参地址、方法返回值地址、操作数栈、局部变量表**（内部细节具体看 [栈帧包含什么？](<https://blog.csdn.net/qian520ao/article/details/79118474>)）

- 本地方法栈存储的是调用 本地 native 方法（比如 C 函数）的栈帧，由于 C 函数不是 JVM 执行，所以**本地方法栈的栈帧中不存在局部变量表 和 操作数栈，只有入参地址 和 方法返回地址**

位于栈顶的栈帧表示当前正在运行的方法



有时候 Java 方法调用 C 函数，那么 C 函数会在 本地方法栈中创建一个 栈帧，而在 C 函数可能又会回调 Java 方法，因此两个栈的栈帧情况如下：

 ![img](https://images2015.cnblogs.com/blog/990532/201608/990532-20160827203431726-2050515871.png) 



**需要注意的是：**

```
上面讲的是普通的 JVM 内存模型，而实际上 HotSpot 将 虚拟机栈 和 本地方法栈 合并为一个栈，即目前现在默认使用的 JVM 中没有区分 虚拟机栈 和 本地方法栈
```



> #### StackOverflowError 和 栈 OOM

栈空间存在大小限制（emmm，用内存来表示的都有大小限制），所以如果无限制的递归调用方法，会导致栈帧占满了栈空间，无法为新的方法的栈帧分配空间，导致 抛出 StackOverflowError 异常

每个线程的创建都会分配一个栈空间，大小差不多为 10M，默认情况下 JVM 并没有限制可分配的所有栈空间总和，理论上是一个进程可分配的内存空间大小，如果无限制的创建线程，那么会无限制的分配栈空间，导致挤满进程空间，出现 栈OOM



一个 Java 程序最大可能占用内存：

```text
-Xmx 指定的最大堆内存大小 
+ 
-Xss 指定的每个线程栈内存大小 * 最大活跃线程数量
+ 
-XX:MaxDirectMemorySize 指定的最大直接内存大小 
+
MetaSpace 大小
```



## 3、堆内存（线程共享）

堆内存 是 JVM 内存中最大的一块内存，基本上所有 的实例对象都是在这里分配内存，但是有的不一定，因为存在逃逸分析，会在栈上分配



新生代：老年代 = 1：2

E区 ：S0：S1 = 8：1：1

 ![1](https://images0.cnblogs.com/blog/587773/201409/061921034534396.png)



> #### 堆内存 OOM 

当 E区无法给对象分配空间时，会发生 young GC

如果 young GC 后还无法分配，那么触发 full GC

如果 full GC 后还无法分配，那么产生 堆内存OOM



## 4、方法区（线程共享）

方法区 是 JVM 一个抽象的规范，而 永久代 和 元空间 则是方法区的实现

- JDK 8 之前使用的是 永久代， **永久代 和 老年代 是进行 GC 绑定的，一旦其中一个区域占满，那么这两个区域都会同时进行 full GC**

- JDK 8 的时候，使用元空间代替永久代，元空间不在 JVM 内存中，而是在本地内存中（即我们的计算机内存，比如我这台电脑 8G），这样理论上它能够使用的内存空间是没有限制的，也就很难出现 GC 和 OOM，并且 **运行时常量池 从方法区转移到堆内存**



> #### 字符串常量池 和 运行时常量池 位置变动

**字符串常量池：**

​	JDK 7 时从 方法区 转移到 堆内存

​	JDK 7 之前存储 字符串字面量对象， JDK 7 之后存储 对堆中 字符串字面量 的引用



**运行时常量池：**

​	JDK 8 时由于元空间代替了永久代，并且元空间是堆外内存，所以将 运行时常量池 从方法区 转移到 堆内存

​	**即 JDK 8 的时候 字符串常量池 和 运行时常量池 都存在于堆中了**



**运行时常量池存储的是什么？**

一个 class 文件中关于一个类的信息的部分，我们可以看作分为两部分：

- 类的父类、子类、类加载器、方法（方法名、访问修饰符、Code 属性、虚方法表的索引等）、字段（变量名等） 等信息
- 常量池：这里的常量池不是 JVM 运行时自动生成的字符串常量池和运行时常量池，而是编译器生成的类用来表示类中的各个方法信息，比如内部调用了哪个类的哪个变量、调用了哪个类的哪个方法，即 Methodref 和 Fieldref 

在类加载的时候第一部分会转变成 Klass 实例存储在方法区中，在类加载后会将**常量池的内容存储到运行时常量池中**

```java
class A{
	public int read(){
		int i = 0;
		int j = 2;
		return i + j;
	}
	public void say(){
        /*
        方法的 code 属性会使用一个 invokevirtual #12 指令来表示方法调用
        #12 表示常量池的位置，该位置存储了一个 Methodref 结构，它来表示调用的方法信息，比如调用的是 B 对象的 read()
        */
		new B().read();
	}
}
class B{
    public void read(){
        System.out.println("read");
    }
}
```



> #### 方法区 OOM

方法区存储的是类的元数据，它也是有内存空间大小限制的，如果类太多，导致方法区存储了类的元数据过多，而无法为新的类元数据分配空间时，那么就会出现 方法区OOM

可以通过动态代理 无限创建类来产生，不过 JDK 8 方法区改为了元数据，并且转移到了物理内存上，所以基本不会出现 方法区 OOM，只有永久代才可能出现，并且会伴随着 full GC，同时还会一起回收 old 区



## 5、直接内存（非 JVM 内存，堆外内存）

**我们需要先明白什么是堆内内存，什么是堆外内存？**

（具体看 [Java直接内存是属于内核态还是用户态](https://www.zhihu.com/question/376317973/answer/1052239674)）

堆内内存就是 JVM 内存，它是受 JVM 管理的，也就是**一个进程所持有的内存**，它位于用户态中

堆外内存是 进程以外的内存，它也是位于用户态中

比如物理内存有 4G，用户态内存占了 3G，假设只有一个进程在运行，实际分配给该进程的只有 100MB（不是虚拟内存），那么除去该 100MB 剩下的用户态内存都是堆外内存



直接内存的分配方式有 2 种：

- 使用 NIO 包下的 ByteBuffer 的 ByteBuffer.DirectByteBuffer()
- 使用 unsafe.allocateMemory()

这里说下 ByteBuffer ，它能够分配两种内存：堆内存 和 直接内存，但是实际上 ByteBuffer 的 allocateDirect() 内部也是调用unsafe.allocateMemory() 来实现的

```java
//1、创建堆内存，内部直接 new byte[]
ByteBuffer heapBuffer = ByteBuffer.allocate(1024);
//2、创建直接内存，内部调用 unsafe 的 allocateMemory()
ByteBuffer directBuffer = ByteBuffer.allocateDirect(1024);

//3、利用 unsafe 分配直接内存，返回直接内存的起始地址
long l = unsafe.allocateMemory(1024);
//释放直接内存，由于直接内存不受 JVM 管理，所以需要手动释放内存
unsafe.freeMemory(l);
```



**两种内存分配方式的区别：**

ByteBuffer 内部是封装了 unsafe ，利用 unsafe 来分配直接内存，但是它通过一个 DirectByteBuffer 的 OOP 的对象来管理这个堆外内存，它内部没有提供释放直接内存的 api，所以是 GC 在回收该对象时，会自动将直接内存释放，即变相的由 JVM 进行管理

```java
public static ByteBuffer allocateDirect(int capacity) {
    return new DirectByteBuffer(capacity);
}
```

unsafe 没有经过任何封装，效果跟 c 的 allocate() 一样，需要手动调用 freeMemory() 来释放直接内存，由于 JVM 无法管理这块内存，如果忘记释放，那么就会导致内存泄漏

```java
public native long allocateMemory(long var1);
```



**使用直接内存（堆外内存）的好处：**

当一个 Java 程序读取一个磁盘文件的时候，需要经过 磁盘-> 内核->堆外->堆内 的拷贝，这个堆内内存就是我们读取数据时申请的 byte 数组

而如果使用 堆外内存 的 byte 数组来代替 堆内内存的 byte 数组，Java 程序能够直接读写这个内存，那么就省去了 堆外-> 堆内 的拷贝，只需要 磁盘-> 内核->堆外->堆内 的拷贝，**好处是 减少了一次拷贝，同时这个 byte 数组不在堆中，减轻了 GC 的压力**

```java
在 Netty 的 NIO 中使用的都是 ByteBuffer 返回的封装了分配的堆外内存 的 DirectByteBuffer 对象
```



> #### 直接内存 OOM

直接内存不受 JVM 控制，理论上是没有限制大小，但是它分配的内存是来自物理内存的，所以 直接内存 受到 物理内存 的限制

但是，我们为了防止无限分配内存，导致机器卡死，可以通过 -XX:MaxDirectMemorySize=10M 参数设置当前进程可分配的直接内存的大小



在没有设置 -XX:MaxDirectMemorySize 参数时，基本不会出现直接内存 OOM，因为无限分配直接内存时会导致机器卡死，程序基本不会运行了，所以没机会出现 OOM

因此要出现直接内存 OOM 需要通过该参数限制 直接内存的大小



**当使用参数限制时**，两种方法都会出现 直接内存 OOM

```java
while(true){
    ByteBuffer directBuffer = ByteBuffer.allocateDirect(1024 * 1024 * 1024);
    long l = unsafe.allocateMemory(1024 * 1024); //-XX:MaxDirectMemorySize=10M
}
```

*![image.png](https://pic.leetcode-cn.com/1605495840-fVFduG-image.png)*



**当没有参数限制时**

unsafe 分配会出现机器卡死

ByteBuffer 分配则不会出现 机器卡死，但是会频繁出现 full GC

*![image.png](https://pic.leetcode-cn.com/1605495930-OClQbq-image.png)*

这是因为 unsafe 分配的直接内存不受 JVM 管理，我们没有手动释放时，它会一直无限分配，然后占满了物理内存，导致机器卡死

而 ByteBuffer 是通过一个 DirectByteBuffer 对象来管理直接内存的，当我们无限分配直接内存的时候，同时也是在无限创建这个 OOP 对象，因此当堆内存无法分配内存给该对象时，会触发 GC，同时会回收之前创建的 DirectByteBuffer 对象，并且释放它们的直接内存，因此不会导致机器卡死，但是会频繁的 full GC



## 6、内存泄漏



**内存泄漏是导致 堆内存 OOM 的原因之一**

**JVM 对内存进行全权管理，从自动分配到垃圾回收，为什么还会发生内存泄漏？**

导致内存泄漏的两个原因：

- 直接内存使用完后没有手动释放内存
- ThreadLocal 使用完对象后没有调用 remove()（可能会存在内存泄漏，弱引用无法完全解决内存泄漏）
- 集合类对象使用完某个对象后没有置 null，比如下面的例子

```java
class stack {
    Object[] data = new Object[1000];
    int top = -1;

    public void push(Object o) {
        top++;
        data[top] = o;
    }

    public Object pop(Object o) {
        top--;
        return data[top + 1];
    }
}

```

对于 push 进去的元素， pop 的时候并没有失去对它的引用，而是简单的对 top 指针进行变化

这样的话， pop 后的栈顶对象已经使用完了，但是 data 数组还留着对它的引用，导致它无法被垃圾回收

只能等到 push 到该位置时，使用一个新的元素来代替 data 对它的引用，它才能被 JVM 回收

而如果 push 了 1000 个元素，然后再 pop 1000 个元素，那么这时宏观上来看 data 中是不存在任何元素的

**但是，实际上 data 并没有放弃对那 1000 个元素 的引用，这样的话这 1000 个元素不会被 JVM 回收，也不会被再次使用，相当于是垃圾堆放在了内存空间中，这样就导致了内存泄漏，进一步可能会导致 内存溢出**





## 7、各种变量、常量 存储位置

**静态变量：** JDK 1.7 后从方法区中移除，放入到 Class 对象中，而 Class 对象存储在 堆中

**全局变量：**某个类实例化时进行初始化，是属于某个类实例的，因此跟类实例一样存储在堆中

**局部变量：**属于某个方法的，方法的具象化是用栈帧来表示的，局部变量存储在局部变量表中，基础类型的局部变量（int）存储的是值，非基础类型的局部变量（User）存储的是引用，而局部变量表是栈帧的一部分



**final 和 static final 常量：**

```java
public class A{
    final static long l = 2L;
    final String s = "abc";
    final String s1 = new String("abc");
    final double d = 123.00987d;
    final float f = 10.001f;
    final int N = 2;
}
```

javap -v 后如下：

```java
//该字节码描述的是 A 的构造方法 ，即如果调用 new A()，那么初始化的变量如下，由于 l 不是 OOP 对象，所以不会在这里初始化
public cur.A();
    descriptor: ()V
    flags: ACC_PUBLIC
    Code:
      stack=4, locals=1, args_size=1
         0: aload_0
         1: invokespecial #1                  // Method java/lang/Object."<init>":()V
         4: aload_0
         5: ldc           #2                  // String abc 
             		//需要先将 符号引用 #2 转换为直接引用
             					1.1、执行 s = "abc"，ldc 指令，如果常量池没有，则在堆中创建 "abc"，将引用放入常量池
         7: putfield      #3                  // Field s:Ljava/lang/String;
             					1.2、将上面 lac 返回的引用赋值给 s
        10: aload_0
        11: new           #4                  // class java/lang/String
            					2.1、执行 s1 = new String("abc") 中的第一步，创建 String 的 OOP 对象
        14: dup
        15: ldc           #2                  // String abc
            					2.2、执行 ldc 执行，这里获取的是上面 ldc 存入的引用
        17: invokespecial #5                  // Method java/lang/String."<init>":(Ljava/lang/String;)V
            					2.3、执行 new String("abc") ，调用构造方法
        20: putfield      #6                  // Field s1:Ljava/lang/String;
            					2.4、将 OOP 对象赋值给 s1
        23: aload_0
        24: ldc2_w        #7                  // double 123.00987d
        27: putfield      #9                  // Field d:D
        30: aload_0
        31: ldc           #10                 // float 10.001f
        33: putfield      #11                 // Field f:F
        36: aload_0
        37: iconst_2
        38: putfield      #12                 // Field N:I
        41: return
}
```

final 和 static final 修饰的变量在编译时期产生的字节码 与普通的变量没有区别（虽然这里看不出 static final，但实际上一样的）

意味着是在编译阶段就保证了 final 的不变性，即通过各个代码判断是否被发生改变

如果发生了改变，那么编译就不通过，而在后续编译完成的 字节码就是当作普通的变量来对待

所以，被 final 修饰的变量，该存哪还是存哪，final 就存储在堆的 OOP 对象中，static final 就存储在 堆中的 Class 对象中





## 8、引用指针 存储的位置

```java
public class Main{
	A a = new A();
	public void h(){
		B b = new B();
	}
}
```

有两个引用类型指针 a 和 b

a 指向的是成员变量，是属于整个 OOP 对象的，不属于任何一个 栈帧，因此不会存储在 虚拟方法栈中，所以跟 OOP 对象一样存储在堆中

b 是属于方法栈的，所以它存储在栈中