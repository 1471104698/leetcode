# Java 综合知识点



## 1、面向对象语言的特点

**像 java 这种面向对象的语言有三大特点：封装、继承、多态**



对于继承，需要注意的是，**子类只会继承父类的非静态方法和成员变量，而静态方法和成员变量是无法继承的**

```java
class A{
    int a = 1;
    static int b = 3;
}
class B extends A{
    int a = 2;
    public static void main(String[] args) {
        A b = new B();
        System.out.println(b.a);//输出父类 A 的 a 值 1
        System.out.println(b.b);//编译时报错，因为 B 不能继承 A 的静态变量 b
    }
}
```



封装就不说了，主要说说什么是多态，以及多态的两种类型（编译时和运行时）



> ### 多态的概念

多态，如字面意思，同种行为具有不同的表现形态

现实中，比如我们按下 F1 键这个动作：

- 如果当前在 Flash 界面下弹出的就是 AS 3 的帮助文档；
- 如果当前在 Word 下弹出的就是 Word 帮助；
- 在 Windows 下弹出的就是 Windows 帮助和支持。

同一个事件发生在不同的对象上会产生不同的结果。



> ### 多态产生的必要条件

**多态存在的三个必要条件**

- 继承
- 重写
- 父类引用指向子类对象

多态是父子之间的关系，父可以是 类 和 接口，子需要 继承类 或者 实现接口

比如：

```java
Parent p = new Child();
```

**函数执行结果的多态：**

一条语句必须同时出现父类和子类，不过是父类的引用和子类的对象

并且要出现一个行为有多种表现形态的话，那么子类就必须重写父类的方法，让方法调用的效果与父类不同



**函数调用传参的多态：**

当我们调用方法传输参数的时候，方法参数设置为父类，那么我们可以传入它的各个子类，方法执行效果如同上面的多态



> ### 重载是不是多态呢？

多态产生的 3 个必要条件中，第二个条件说的是重写，那么重载是不是多态呢？

有的说是多态，因为相同的一个函数名存在多种不同的执行结果

有的说不是，因为重载是发生在一个类中的，不是发生在继承类之间的，即它们认为必须是父子之间的多态行为

当然，这只是一种工具，差不多知道就好，也没有具体的界定



> ### 两种多态类型：编译时多态 和 运行时多态

**编译时多态：编译期间就确定的**，主要是方法重载、 静态方法成员和变量、非静态成员变量

我们写的代码都是静态文件，需要编译成二进制文件才能运行，而编译成的二进制文件其实是一条条指令，CPU 根据按照顺序运行这些指令就是在运行我们的代码，当我们一个类中存在多个同名的方法（重载）的时候，编译时就需要通过我们调用方法传入的参数来确定调用的是哪个方法（这是方法重载，静态方法变量和非静态变量的引用也是这个时候确定的）



**运行时多态：运行时候才确定的** ，主要是方法重写，同个方法（父类和子类之间的角度）调用时出现的不同执行效果

比如 

```java
Parent p = new Child();
p.run();
```

上面这个 p 执行的就是子类的 run() 方法，这是在运行时候才确定调用的是哪个方法

主要是使用虚函数来实现，它指向 我们调用的 run() 方法调用的是谁的方法

如果子类中重写了 run() 方法，虚函数指向的是 子类的 run()，如果没有，那么虚函数指向的是 父类的 run()





## 2、为什么浮点数类型 float 和 double 不能使用 == 进行比较

我们首先需要知道，float 是 4 个字节，32 位， double 是 6 个字节，64 位（类比 int 和 long）

整型默认情况下是 int ，浮点型默认情况下是 double ，两者相反



在 Java 中运行一下代码
`System.out.println(2.00 - 1.10);`
**输出的结果是：0.8999999999999999**

为什么会产生这样的结果？

我们从底层浮点数的存储来看



> ### 浮点数在计算机中的存储

|        | 符号位 | 指数   | 尾数   |
| ------ | ------ | ------ | ------ |
| float  | 1 bit  | 8 bit  | 23 bit |
| double | 1 bit  | 11 bit | 52 bit |



浮点数是有一定存储范围的，我们将一个十进制数存储进计算机的时候，实际上是转换为二进制数

上面的 1.10，整数部分 1 二进制就是 1 ，而小数 0.10 的转换如下：

0.1 * 2 = 0.2，取整数部分 0，余数为 0.2

0.2 * 2 = 0.4，取整数部分 0，余数为 0.4

0.4 * 2 = 0.8，取整数部分 0，余数为 0.8

0.8 * 2 = 1.6，取整数部分 1，余数为 0.6

。。。

。。。

我们可以看出，无论如何 0.1 都无法取到尽头，但是由于 double 类型的尾数部分只能存储 52 bit，因此导致超出的部分被舍去了

导致了精度丢失

而上面输出 0.8999 同样是精度丢失的问题，因为 0.1 无法精确转化为二进制数，只能无限接近，导致转换的值比 0.9 大，因此才出现了 0.0000000...1 的差距



**因此比较都是设置一个 精确度 `exp`，然后 Math.abs(a - b) <= exp 的话就算作满足条件**



> ### 将上面的运算替换为 float 后是什么结果？

我们也说了，浮点数默认是 double 类型，如果我们转换成 

`System.out.println(2.00f - 1.10f);`

**输出的结果是：0.9**

这是因为 float 的精度没有 double 高，因此将多出的比 0.9 大的精度给舍弃了，所以才得到正确的 0.9



> ### float 和 double 比较

```java
float f1 = 0.5f;
double d1 = 0.5;

float f2 = 0.1f;
double d2 = 0.1;

System.out.println(f1 == d1); // true
System.out.println(f2 == d2); // false
```

上面代码出现不同情况的原因同样是因为精度丢失

由于 0.5 能够准确转换为 二进制数，因此 float 和 double 结果是一样的

但是 0.1 无法准确转换为 二进制数，而 float 的尾数位数 比 double 少，因此精度比 double 小，即转换结果为 f2 < d2











## 3、java 如何实现连续的内存分配

使用 new byte[size]





## 4、子类继承和不继承父类的属性方法

这里先给出结论：

- 子类不可以继承 父类的 static 变量、方法 以及 private 变量、方法
- 子类可以继承父类 public 变量、方法 以及 final 变量、方法，但不能重写 final 方法（可以重载） 和 修改 final 变量



```java
public class A extends B{
    public A(){
    }

    private void printA(){
        System.out.println("A printA");
    }
    public void printB(){
        System.out.println("A printB");
    }
}
class B{
    public B(){
    }
    //private 只能在类内部的方法调用
    private void printA(){
        System.out.println("B printA");
    }
    public void printB(){
        System.out.println("B printB");
    }
    public void printAB(){
        printA();
        printB();
    }
}
class C{
    public static void main(String[] args) throws ClassNotFoundException {
        Class<?> aClass = Class.forName("cn.oy.B");
        A a = new A();
        a.printAB();
    }
}
```

输出结果：

```
B printA
A printB
```

public 修饰的 `printB()` 就是打印的子类重写的方法，而 private 修饰的 `printA()`则是打印的父类的方法

这是因为子类并没有继承父类的 private 方法，子类里面的 `printA()` 只是自己定义的一个方法而已，并不是重写

所以对于在父类中的 `printAB()`，由于子类可见父类的`printB()`，并且重写了它，所以调用这个重写的方法

由于父类不可见子类的 `printA()`，所以只能是调用自己内部的 `printA()`





## 5、红黑树的基础

**二叉搜索树：**根节点大于左子节点，小于右子节点，方便查找，但是不平衡，可能退化成链表

像下面这种我们可以看作是顶端优势

![img](https://pic4.zhimg.com/80/v2-062c92b21fc992704bf281530c7d9f97_720w.jpg)



**AVL 树**：平衡，但是旋转到平衡会开销太大



**红黑树** 解决二叉搜索树的【顶端优势】，但又不会跟 AVL 树一样绝对平衡



> ### 红黑树的基本性质

- 节点有红黑两色
- 根节点必定是黑色的
- 没有两个相邻的红色节点（即父子节点不可能同时是红色的）
- NULL 节点是黑色的（我们认为叶子节点下面还有两个 NULL 节点，这肯定是为了方便某种东西的计算）
- 从任意一个节点到达 NULL 节点的路径上都有相同的 黑色节点数（不包括 NULL 节点到 NULL 节点）





## 6、为何重写 equals() 和 hashCode()



> ### equals()

有时候我们想要的并不是判断是否是同一对象，而是比较两个对象的值是否相同，这时候就需要使用 equals()



首先我们看看 Object 内部自带的 equals() 方法

```java
public boolean equals(Object obj) {
    return (this == obj);
}
```

它内部是直接调用的 == 来判断的，这意味着什么？意味着 a.equals(b) 和 a == b 的作用是相同的，都是判断 a 和 b 指向的是否是同一个对象，即是否是同一个内存地址，因为如果内存地址相同，那么必定它们引用的必定是同一个对象

这跟我们之前讲的 **a.equals(b) 判断的是值是否相同** 这句话相悖了，怎么会是判断同一对象呢？？？

**这是因为我们调用的 String 类、Integer 类 等内部都自己重写了 equals() 方法，使得这个 equals() 方法变成了判断值相同了**

**因此，我们说的 equals() 判断值是建立在 重写 equals() 的基础上的**



以下是 String 重写的 equals() 

```java
public boolean equals(Object anObject) {
    if (this == anObject) {
        return true;
    }
    if (anObject instanceof String) {
        String anotherString = (String)anObject;
        int n = value.length;
        if (n == anotherString.value.length) {
            char v1[] = value;
            char v2[] = anotherString.value;
            int i = 0;
            while (n-- != 0) {
                if (v1[i] != v2[i])
                    return false;
                i++;
            }
            return true;
        }
    }
    return false;
}
```



String、Integer 这种基础类自己重写了 equals() 方法，所以我们可以直接调用进行比较，那么对于我们自己写的类由于可能存在多个字段，而需要每个字段的比较逻辑不同，因此我们需要自己重写 equals() 方法



> 在 hashmap 中的应用

我们需要知道 hashMap 中的是 Entry 的 key - value 形式，equals() 重写是必定的

因为 key 是各种类型的，可能是我们自定义的类，假设是 User，那么我们在这个类会重写 equals()，因此调用进行 key 的比较的时候，调用的就是我们重写的 equals()，判断值是否相同，这样的话，就算不是同一个对象，但只要值相同，那么就可以进行 value 的覆盖

比如

```java
class User{
    int name;
    int age;
    public boolean equals(){
        //重写比较
    }
}

User user1 = new User(1, 2);
map.put(user1, 1);

User user2 = new User(1, 2);
map.put(user2, 2);
```

上面的 user 有两个字段 name 和 age

当我们使用 map 进行 put() 的时候，我们想要的是判断 user 这个 key 里面的内容是否相同，如果相同就当作是同一个 user，而不是去比较是否是同一个对象



> ### hashCode()

hashCode() 主要是用来进行 hash 定位存储位置的



我们先看看 Object() 中的 hashCode()

```java
public native int hashCode();
```

没有任何实现，它就是一个本地方法，得到的是对象所在的内存地址，**这个默认方法不同的对象得到的 hashCode 必定不同**



在 hashmap 中，如果不重写 hashCode() 方法，会发生什么事？

```java
class User{
    int name;
    int age;
    public boolean equals(){
        //重写比较
    }
}

User user1 = new User(1, 2);
map.put(user1, 1);

User user2 = new User(1, 2);
map.put(user2, 2);
```

由于我们上面说了，默认的 hashCode() 方法获取的是对象的内存地址，因此如果不是同一个对象，那么内存地址必定不同，那么对于 上面的 user 来说，user1 和 user2 是不同的对象，它们的内存地址必定不同，那么就很大概率不会映射到同一个 Entry 上，这样的话就不会发生覆盖替换了，这不是我们要的结果，因此我们需要重写 user 的 hashCode()，让 name 和 age 相同时，那么得到的 hashCode 必定相同

当然，不同的实现逻辑会导致不同的情况，比如直接使用 name + age 得到的值 和 name 和 age 的值进行交换后 name + age 得到的值相同的，**这就导致 hashCode 相同，但不一定是值相同，因此还需要 equals() 进行判断**



hashmap 重写的 hashCode() 方法，同时调用了 key 和 value 的 hashCode() 保证 key value 相同，那么 hashCode 必定相同

```java
public final int hashCode() {
    return Objects.hashCode(key) ^ Objects.hashCode(value);
}
```



String 重写的 hashCode() 方法

```java
public int hashCode() {
    int h = hash;
    if (h == 0 && value.length > 0) {
        char val[] = value;

        for (int i = 0; i < value.length; i++) {
            h = 31 * h + val[i];
        }
        hash = h;
    }
    return h;
}

System.out.println("0".hashCode()); //48
System.out.println("a".hashCode()); //97
System.out.println("00".hashCode()); //1536
```









## 7、try-catch-finally 和 return

> ### finally 代码块什么时候不会执行

- 在 try 代码块前就进行 return
- 在 try 代码块中有 System.exit(0)，这个代码是退出 JVM 的，JVM 退出了，肯定就不能执行了



> ### finally 执行的各种情况



**1、finally 中 没有 return 语句**

```java
public class A{
    public static void main(String[] args) {
        System.out.println(h());
    }
    private static int h(){
        try {
            return 1;
        } finally {
            System.out.println("finally");
        }
    }
}
```

输出结果：

```java
finally
1
```

即如果在 try 中 return 了，也会执行完 finally 中的语句再 return



**2、finally 中有 return 语句**

```java
public class A{
    public static void main(String[] args) {
        System.out.println(h());
    }
    private static int h(){
        try {
            return 1;
        } finally {
            System.out.println("finally");
            return 2;
        }
    }
}
```

输出结果：

```java
finally
2
```

即如果 try 和 finally 中都有 return ，那么执行的是 finally 中的 return，

那么 try 中的 return 有没有执行呢？是执行了被覆盖了 还是 压根没有执行？



**3、try 和 finally 都有 return，并且 try 的 return 进行表达式运算**

```java
public class A{
    public static void main(String[] args) {
        System.out.println(h());
    }
    private static int h(){
        int i = 0;
        try {
            return i += 2;
        } finally {
            System.out.println("finally");
            return i;
        }
    }
}
```

输出结果：

```java
finally
2
```

表示 try 中的 return 有执行，只不过 最终方法 return 被 finally 的 return 覆盖了

那么如果 finally 没有 return ，在 try 中有 return，那么在 finally中做的修改的值会映射到 try 的 return 么？



**4、try 有 return ，finally 没有 return, finally 修饰 try return 的值**

```java
public class A{
    public static void main(String[] args) {
        System.out.println(h());
        System.out.println(h1().get("key"));
    }
    private static int h(){
        int i = 0;
        try {
            return i;
        } finally {
            i += 2;
        }
    }
    private static Map<String, String> h1(){
        Map<String, String> map = new HashMap<>();
        try {
            map.put("key", "try");
            return map;
        } finally {
            map.put("key", "finally");
        }
    }
}
```

输出结果：

```java
0
finally
```

可以看出如果是值传递的话， finally 的修改不会映射到 try 上

如果是引用传递的话，finally 的修改会映射到结果上，当然，前提是 return 的是引用传递的类型

那么在 try-catch-finally 外面的 return 就没有什么作用了么？



**5、try-catch-finally 的 return**

```java
public class A{
    public static void main(String[] args) {
        System.out.println(h());
    }
    private static int h(){
        try {
            int i = 1 / 0;
            return 1;
        }catch (Exception e){
            e.printStackTrace();
            //return 3;
        }finally {
			//return 2;
        }
        return 3;
    }
}
```

输出结果：

```java
java.lang.ArithmeticException: / by zero
	at cur.A.h(A.java:19)
	at cur.A.main(A.java:15)
3
```

即如果 try 在没有 return 前发生了异常，并且进行了 catch，那么就不会执行 try 的 return，这时 return 有几种情况了：

- finally 中没有 return
  - catch 中没有 return，那么就会执行 外面的 return
  - catch 中有 return，那么就会执行 catch 的 return
- finally中有 return
  - catch 中有 return，先执行 catch 的 return，然后再执行 finally 的 return
  - catch 中没有 return，那么直接执行 finally 的 return,不会执行外面的 return





综上，finally 是负责断后的，即无论是 try 正常执行 还是 发生异常后 catch ，finally 都是在最后执行

并且 return 最后是 finally 的 return 有效，但是 try 和 catch 的也会执行，不过不会返回

外面的 return 能够执行的必要条件就是 finally 没有 return，并且 try 正常执行的情况下没有 return，异常情况下 catch 中没有 return









## 8、异常（Exception）和 错误（Error）

> ### 异常（Exception）和 错误（Error）

**异常 和 错误 都继承自 Throwable**

异常（Exception）则是程序发生的 错误，比如  i /= 0， IO 异常 等，这种是可控的

错误（Error）则是硬件方面的错误， 比如 OOM 等，这种程序员是无法控制的，当出现 OOM 时 JVM 会终止当前线程



try-catch-finally 是异常相关的三件套

**try** 用来监听内部代码段

**catch** 用来捕获 try 内部出现的异常，一旦出现异常就中止 try 的继续执行，然后对异常进行处理

**finally** 用于做收尾工作，比如 关闭流等



**非运行时异常：**IOException、SQLException，这种在编译的时候就强制要求进行 try-catch 或者 throws

**运行时异常：**即只有程序运行时才会发生的异常，一般是由于程序的逻辑产生的，比如 i /= 0 这种，或者 空指针异常



**当发生异常我们不进行 catch 处理，而是一层层往上抛，如果最终到达子线程的 run() 或者 主线程的 main() 还是没有处理，那么就会交给 JVM 进行处理，一旦 JVM 进行处理，那么就会终止线程**



> ### throw 和 throws

throw 是运行在 函数内部的，表示抛出一个异常，想什么时候抛就什么时候抛，可以位于方法的任意一个位置

throws 是位于方法声明上的，表示这个方法随时可能会出现异常，并且没有进行处理，需要调用该方法的方法进行处理

当调用 throw 抛出一个异常的时候，必须在方法上使用 throws 声明该异常类

throw 后面跟的是异常的实例，throws 后面跟的是异常的类型

```java
public class A{
    public static void main(String[] args) throws Exception {
        try {
            h();
        } catch (Exception e) {
            throw new Exception();	//一般是在 catch 中抛出异常
        }
        System.out.println(1);
        throw new Exception(); //在任意地方都可以抛出异常
    }
}
```



我们可以根据对某一特定的情况进行 throw

```java
String str = "abc"
if("abc".equals(str)){
    throw new NumberFormatException();
}else{
    System.out.println(str);
}
```



而一般情况下是 try-catch + throw 进行错误集中处理，因为我们有时候并不能列举出所有的异常情况，有的情况我们意想不到，因此需要使用 try-catch，而我们又不想自己去处理异常，因此，直接在里面 throw 出异常给上层处理，同时可以进行一些 log 记录

当然，可能会说为什么不直接 throws，因为 catch 后可以进行一些日志处理之类的特殊处理

```java
public void h() throws Exception{
    try{
    //
    }catch(Exception e){
    	logger.info("出现异常了");
        thorw new Exception();
    }
}
```





## 9、private 能够被反射获取，存在的意义是什么？

private 存在的意义不是为了绝对安全设计的，而是对用户使用 java 的一种规范，用来实现封装

让用户 从外部调用对象时，能够清晰的看到类的内部结构，屏蔽掉用户不需要知道的细节