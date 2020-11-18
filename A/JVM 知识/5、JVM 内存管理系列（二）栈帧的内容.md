# 栈帧的内容



 [栈帧包含什么？](<https://blog.csdn.net/qian520ao/article/details/79118474>)



虚拟机栈运行的是 Java 方法，虚拟机栈 的 栈帧中存储了 方法返回地址、操作数栈、局部变量表、动态连接

本地方法栈 运行的是 C 函数之类的，由于 C 函数不是 JVM 执行，所以本地方法栈的栈帧中不存在局部变量表 和 操作数栈，**只有 方法返回地址**



## 1、方法返回地址

​	这里的 **方法返回地址 指的是 字节码指令地址**

​	当 方法 A 调用了 方法 B，方法 B 栈帧中的 方法返回地址 记录的就是 方法 A 程序计数器 的内容，即方法 A 下一条将要执行的 字节码指令，这样当 方法 B 执行

​	当 方法 B 正常退出时，根据 方法返回地址 回到 方法 A，然后方法 A 继续执行

​	当 方法 B 异常退出时，不会使用到 方法返回地址，异常处理写在异常方法表中，并且不会返回任何的返回值给 方法

main() 中调用了 h()

```java
class Q{
    public static void main(String[] args) {
        //调用了 h()
        int val = h();
    }
    public static int h(){
        return 1;
    }
}
```

以下是 main() 的 Code 属性，可以看出，调用了 h() 后，

由于 h() 有返回值，所以 main() 的下一条字节码指令就是接收方法返回值，这样在 h() 中的方法返回地址就是这个 `3: istore_1`

如果 h() 没有返回值，那么 h() 中的方法返回地址就是 ` 4: return`

*![image.png](https://pic.leetcode-cn.com/1605602954-ELHZdo-image.png)*



> #### 为什么有 程序计数器了，还需要方法返回地址？

首先，我们需要搞清楚，程序计数器是线程私有的，方法 A 调用 方法 B ，仍然是该线程去执行 方法 B

这意味着 程序计数器 内的值 不再是 方法 A 的下一条字节码指令地址，而是调用的 方法 B 的下一条字节码指令地址

这样就相当于没有存储了方法 A 的下一条字节码指令地址

无论是 单线程 还是 线程，方法 A 调用了 方法 B， JVM 解释器 执行完方法 B 后，都并不知道 方法 A 的字节码指令执行到哪里了，所以需要在方法 B 的栈帧中记录方法 A 的下一条字节码指令



如果调用的是 native，在栈帧中也会存在 方法返回地址，所以没啥问题



即 程序计数器 是为了解决多线程情况下 JVM 解释器不知道 当前线程 执行到哪条字节码指令，而 方法返回地址 是为了解决 方法中调用其他方法，导致 JVM 解释器 不知道原来方法执行到哪条字节码指令





## 2、动态链接

[动态链接 -- 知乎路人回答](https://www.zhihu.com/question/347395101/answer/835477736)



在编译期间，方法调用都是 符号引用（使用字符串标明调用的是哪个类的哪个方法），它在常量池中的数据结构是 Methodref

符号引用需要在运行的时候将它解析为直接引用，将常量池中的 Methodref 替换为 Method 对象地址，将 字节码指令 invoke 后面的 #11 替换为方法所在的虚方法表下标 和 方法参数个数



动态链接 就是在栈帧中保存了指向当前栈帧方法的直接引用 Method 对象地址，比如当前栈帧是 方法 A，那么栈帧中会保存 方法 A 在类的元数据的 Method 对象地址

为什么需要保存指向 Method 对象的地址？

因为栈帧中的 程序计数器 只存储了当前方法调用的下一条字节码指令相对于方法 Code 属性的偏移地址

![image.png](https://pic.leetcode-cn.com/1605620624-inULpE-image.png)

比如 `0:iconst_5`，程序计数器存储的就是 0，单纯的根据这个偏移量是无法得到真正的字节码指令的，所以我们还需要知道方法的 Code，然后 Code + 偏移量准确定位到 iconst_5 指令。

那么怎么获取这个 Code？先获取 Method 后再访问它内部的 Code

因此我们需要一个指针，能够访问到 Method，因此就出现了这个动态链接



## 3、局部变量表

局部变量表存储的是 当前方法需要的局部变量（int、float、引用指针 等）

局部变量使用 slot 来存储局部变量，每个 slot 为 32bit，但是每个 slot 只可以存储一个局部变量，对于不够 32bit 的会自动补齐

而对于超过 32bit 的，比如 64bit 的 long、double，那么会分成两个连续的 slot 存储，因此读取的时候需要一次连续读取两个 slot

```java
class Q{
    public static void main(String[] args) {
        int i = h();
        Q q = new Q();
        long l = 2l;
        byte b = 2;
        System.out.println(l);
    }
    public static int h(){
        return 1;
    }
}
```

以下是 main() 的局部变量表，存在五个局部变量：args、i、q、l、b，局部变量表记录了局部变量的 变量名、变量类型、所在的 slot 位置

**![image.png](https://pic.leetcode-cn.com/1605615506-THxlLu-image.png)**

可以看出，在编译的时候局部变量在 局部变量表中的槽位已经确定了

在方法的 Code 字节码中，操作局部变量表也是直接根据 slot 位置来存取

istore_1：将 h() 返回的结果存储到 1 号 slot

astore_2：将 new Q() 得到的对象引用存储到 2 号 slot

ldc2_2 #5：解析常量池中 #5 位置的 Field，然后将得到的 long 型变量推送到操作数栈栈顶

lsotre_3：将操作数栈栈顶的 long 变量存储到 3 号 slot

istore    5：将操作数栈栈顶的 2 存储到 5 号 slot

lload_3：将 3 号 slot 的数据压入到操作数栈上

***![image.png](https://pic.leetcode-cn.com/1605616455-KCjltv-image.png)***



上面显示的是静态方法的局部变量表，而如果是非静态方法，那么 0 号 slot 存储的是当前对象，即每个非静态方法内部都默认存储了一个当前对象作为局部变量

```java
public class A{
    public void h1(){
		
    }
}
```

*![image.png](https://pic.leetcode-cn.com/1605615867-EfTzsk-image.png)*





## 4、操作数栈

操作数栈存储的是当前 JVM 正在操作的变量

如果操作的是局部变量，那么需要将变量从局部变量表中压入到操作数栈

如果操作的是全局变量 或者 静态变量，那么需要将 变量从 OOP 对象体 或者 Class 对象 中获取压入到操作数栈



例子：

```java
public class A {
    public void h(int i, int j){

    }
    public static void main(String[] args) {
        A a = new A();
        int i = 0;
        int j = 0;
        a.h(i, j);
    }
}

```

局部变量表：

```java
 LocalVariableTable:
        Start  Length  Slot  Name   Signature
            0      19     0  args   [Ljava/lang/String;	//main() 方法自带参数
            8      11     1     a   Lcur/A;	//A 对象
           10       9     2     i   I		//int 型的 i
           12       7     3     j   I		//int 型的 j

```

main() 方法字节码：

```java
    Code:
      stack=3, locals=4, args_size=1
         0: new           #2    //为 A 分配内存空间，并将地址压入操作数栈               // class cur/A
         3: dup				    //复制操作数栈顶的地址，并将复制的地址压入操作数栈，即操作数栈栈顶有两个相同的地址
         4: invokespecial #3    //弹出操作数栈栈顶值，它是一个地址，即 A 对象，执行该实例对象的构造方法               // Method "<init>":()V
         7: astore_1		    //弹出操作数栈栈顶值，同样是 A 对象地址，将它存储到 局部变量表的 1 号 slot
         8: iconst_0		    //将 int 型变量的 0 压入到操作数栈
         9: istore_2		    //将 操作数栈顶的 0 弹出，存储到 2 号 slot
        10: iconst_0			//将 int 型变量的 0 压入到操作数栈
        11: istore_3			//将 操作数栈顶的 0 弹出，存储到 3 号 slot
        12: aload_1				//将 1 号 slot 的值压入操作数栈，它是 A 对象地址
        13: iload_2				//将 2 号 slot 的值压入操作数栈，它是 int 型的 0
        14: iload_3				//将 3 号 slot 的值压入操作数栈，它是 int 型的 0
        15: invokevirtual #4    //执行操作数栈对象的 h()              // Method h:(II)V
        18: return

```



我们可以看出，当需要调用到方法时，会将实际调用的对象压入到操作数栈，并且如果这个方法需要参数，同时也会将参数值压入到操作数栈中，比如调用 `a.h()` ，会先将 a 对象压入操作数栈，h() 需要两个参数，因此会将两个参数也一起压入到操作数栈

- 如果方法存在参数，那么操作数栈的栈顶元素就不是实际调用对象，而是方法的参数值

- 如果方法不存在参数，那么操作数栈的栈顶就是实际调用对象

因此，为了准确知道实际调用对象在操作数栈的位置，当解析出 invokevirtual #4 中 指向的常量池 #4 位置的 Methodref 找到方法的 Method，内部会记录方法所需参数个数，比如 h() 需要两个参数，那么就操作数栈的前两个数就为方法参数，第三个就是实际调用对象