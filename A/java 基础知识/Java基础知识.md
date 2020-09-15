# Java 基础知识



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





## 3、new String() 创建了多少个对象

> ### 前置知识

```java
String str = "a" + "b"
```

"a" + "b" 在编译期间就可以直接确定结果是 "ab" 了，因此直接存储进常量池



```java
String b = "b";
String str = "a" + b;
```

b 是符号引用，会调用 StringBuilder 进行 append 拼接，再调用 toString() 转换为字符串



如果全部是字符串常量拼接，那么生成的对象只有一个常量池对象

如果存在变量拼接，那么生成的对象有 一个 StringBuilder 和 一个 String

一般情况下，我们都是忽略 StringBuilder 这个对象，下面的分析也是如此



> ### 情况1

```java
String s1 = new String("abc");
String s2 = "abc";
System.out.println(s1 == s2); //false
```

new String("abc") 的时候，字符串常量池中不存在 abc，因为创建一个，然后再在堆中创建一个，返回的是对堆中对象的引用

后面 s2 指向的是对常量池对象的引用，所以为 false



> ### 情况2

```java
String s1 = "a" + "bc";
```

这里有点特殊，字符串常量池并不会 "a" 和 “bc”，只会创建 "abc"，因此最终在字符串常量池中只存在 "abc"



证明如下：

```java
//常量池中有 aab，但不知道有没有 aa 和 b
String str = "aa" + "b";
String str1 = new String("a") + new String("a");
System.out.println(str1.intern() == str1);	//true
```

如果 str 拼接的时候会在字符串常量池中创建 "aa" 和 "b"，那么 str1.intern() 获取到的应该是原来创建的常量池该对象的引用

那么意味 str1.intern() 和 str1 指向就是不同的内存地址，应该返回 false,但是实际上返回的是 true，表示没有创建 "aa" 和 "b"



```java
//常量池中有 aab，但不知道有没有 aa 和 b
String b = "b";
String str = "aa" + b;
String str1 = new String("a") + new String("a");
System.out.println(str1.intern() == str1);	//false
```

当拼接存在 变量，那么常量池就会创建 "aa"，所以 str1.intern() 获取的就是原来创建的对象的引用

所以导致 str1.intern() 和 str1 的内存地址不同，所以返回了 false





> ### 情况3

```java
String str = "bc"
String str1 = "a" + str;
```

这里由于 str 是引用变量，所以在编译期间不会转换，而字符串常量池会创建 "a" 和 "bc"

所以只要 拼接的字符串中，存在引用变量，那么字符串常量池就会进行存储



> ### 情况4

```java
String str = new String("a") + new String("b");
String str = new String("a" + "b");
```

第一条语句会在字符串常量池中创建 a 和 b，在堆中创建 a 和 b 和 ab

第二条语句会在字符串常量池 和 堆中 都创建 ab



> ### 情况5

```java
String s1 = “abc”;
String s2 = “a”;
String s3 = “bc”;
String s4 = s2 + s3;
System.out.println(s1 == s4); //false
```

常量池中创建了 a、bc、abc

s2 和 s3 虽然在编译期间可以确定，但是存在符号引用，编译器不会在编译期间去替换掉这些引用，导致 s4 在编译期间不能确定而不会放入常量池，并且后续是使用 StringBuilder 进行拼接然后调用 toString() 创建一个新的对象返回的



> ### 情况6

```java
String s1 = “abc”;
final String s2 = “a”;
final String s3 = “bc”;
String s4 = s2 + s3;
System.out.println(s1 == s4);	//true
```

由于 s2 和 s3 用 final 修饰，所以表示值不会改变，编译器在编译期间会将所有使用 s2 和 s3 的地方全部替换为真实值，而不是引用，这样的话就导致 s4 在编译期间确定了，和 s1 指向常量池中同一个对象

如果 s2 和 s3 中任意一个去掉 final，这样也会导致 s4 不在编译期间确定





## 4、java 如何实现连续的内存分配

使用 new byte[size]





## 5、子类继承和不继承父类什么东西

子类可以继承父类的 非 private 修饰的 非静态方法和变量

如果非静态方法和变量是 private 修饰的，那么子类不能继承，即对子类不可见



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



在父类 B 中有 private 修饰的 `printA()`，public 修饰的 `printB()` 和 public 修饰的 `printAB()`, 在`printAB()` 中调用了 `printA()` 和 `printB()`

然后子类 A 中也有一个 private 修饰的 `printA()`，public 修饰的 `printB()`

我们创建一个子类对象 `A a = new A();`，由于继承，所以 子类 A 继承了 父类 B 的 `printAB()`，调用它

而打印出来的结果是

```
B printA
A printB
```

我们可以看出， public 修饰的 `printB()` 就是打印的子类重写的方法，而 private 修饰的 `printA()`则是打印的父类的方法

这是因为子类并没有继承父类的 private 方法，子类里面的 `printA()` 只是自己定义的一个方法而已，并不是重写

所以对于在父类中的 `printAB()`，由于子类可见父类的`printB()`，并且重写了它，所以调用这个重写的方法

由于子类不可见父类的 `printA()`，所以只能是调用自己内部的 `printA()`





## 6、红黑树的基础

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





## 7、sleep() 和 wait() 的区别

sleep() 是 Thread 的静态方法，随时可以 进行调用，`Thread.sleep()`，它跟线程是否持有锁的状态无关，它不需要锁，调用后线程会挂起，如果持有锁的话也不会释放锁，因为说了跟锁无关，只是挂起后不会跟其他的线程争夺 CPU



wait() 是Object 的方法，它是一个实例方法，任何对象都相当于是继承了父类 Object 的这个方法，调用某个对象的 wait() 就表示当前线程 进入以 某个对象为锁对象的同步队列中，它需要配合 syn 锁使用，

- 如果syn 锁 锁的对象为 obj，那么后续调用 wait() 就应该用这个 obj 来调用，这样才能将线程放入 以 obj 作为锁对象的同步队列中
- 如果 syn 锁 锁的对象为 this,那么表示锁 的是当前对象，那么直接调用当前对象的 wait() 即可，它会进入当前对象的同步队列



它们都需要 捕获 中断异常，因为它们都可以被 线程的 interrupt()  方法打断，并且抛出中断异常

```java
public void h(){
    Thread thread = new Thread(() -> {
        synchronized (this) {
            try {
                wait();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    });
    thread.start();
    thread.interrupt(); //可以打断 wait
}


public void h(){
    Thread thread = new Thread(() -> {
        try {
            Thread.sleep(1100);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    });
    thread.start();
    thread.interrupt(); //可以打断 sleep
}
```



> ### wait(1000)与sleep(1000)的区别

sleep(1000) 表示将线程挂起，在未来的 1000ms 内不参与竞争 CPU，在 1000ms 后，会取消挂起状态，参与竞争 CPU ，这时不一定 CPU 就立马调度它，因此它等待的时间的 >= 1000ms



wait(1000) 跟 wait() 的差别在于 wait(1000) 不需要 notify() 来唤醒，它等待 1000ms 后，如果此时没有线程占有锁，那么它会自动唤醒获取锁 





## 8、单例模式

> ### 单例模式是什么？

单例：某个类的对象在堆内存中有且仅存在一个，即一个类的对象只会创建一次在内存中，被多个线程复用

主要是用于某个类作用一样，且防止被重复创建来占用内存



> ### 单例模式的两种类型

懒汉式：跟字面意思一样，很懒，只有在用到的适合才创建单例对象

饿汉式：跟字面意思一样，很饿，饥渴，能有多快创建就多快创建，在类加载的时候就创建了单例对象



比如之前讲 volatile 和 syn 的时候，就写了一个单例模式，就是使用的 懒汉式-双重检查

```java
class Singleton{
    private volatile static Singleton instance = null;
    
    private Singleton(){}
    
    public static Singleton getInstance(){
        if(instance == null){
        	//锁住整个类
            synchronized(Singleton.class){
                if(instance == null){
                    instance = new Singleton();
                }
            }
        }
        return instance;
    }
}
```



还有一种懒汉式-静态内部类

静态内部类跟外部类没什么关系，外部类加载的时候静态内部类并不会加载，只有主动调用它的时候才会加载

这个不加锁怎么保证创建的对象唯一？因为静态变量具有唯一性，底层保证的

```java
class Singleton{
    
    private Singleton(){}
    
    private static class SingletonHolder{
        private static final  Singleton instance = new Singleton();
    }
    public static Singleton getInstance(){
        return SingletonHolder.instance;
    }
}
```



饿汉式的话，就是使用的内部类的形式，静态内部类，类加载的初始化阶段给静态变量赋用户的给定的值，这时候是线程安全的，可以用来创建实例对象

```java
class Singleton{
    //注意：构造方法私有化是只能在类内部使用
    private static final Singleton instance = new Singleton();
    
    private Singleton(){}
    
    public static Singleton getInstance(){
        return instance;
    }
}
```





## 9、反射

> ### 前置知识点

主要 API：

- 成员变量：Field
- 成员方法：Method
- 构造方法：Constructor

获取 Class 对象的方式：

- 类`.class`
- `Class.forName()`
- 实例对象`.getClass()`

获取成员变量以及进行修改

- 获取 public 修饰的所有成员变量：`clazz.getFields()`
- 获取 public 修饰的指定的某个成员变量：`clazz.getField(String name);`
- 获取所有成员变量：`clazz.getDeclaredFields()`
- 获取某个指定的成员变量：`clazz.getDeclaredField(String name)`
- 设置某个成员变量的修改权限（比如 final 修饰）：`field.setAccessible(true);`

获取对象的成员方法

- 获取 public 修饰的所有构造方法：`clazz.getConstructors();`
- 获取 public 修饰的某个构造方法，没有参数就是无参：`clazz.getConstructor();`
- 获取所有构造方法：`clazz.getDeclaredConstructors();`
- 获取某个构造方法，一般是无参：`clazz.getDeclaredConstructor();`
- 通过构造方法实例化对象：`Object o = constructor.newInstance();`
- 获取 public 修饰的成员方法：`Method getA = clazz.getMethod("getA");`
- 获取某个成员方法, 不能包括继承的方法：`Method getA = clazz.getDeclaredMethod("getA");`
- 调用成员方法，参数为指定执行的是哪个对象：`getA.invoke(o);`



> ### 可以修改 final 和 private 修饰的值



```java
public class Reflect {
    private final int a = 1;
    private int getA(){
        return a;
    }
    private Reflect(){}
}
class C{
    public static void main(String[] args) throws ClassNotFoundException, IllegalAccessException, InvocationTargetException, InstantiationException, NoSuchFieldException, NoSuchMethodException {
        Class<?> clazz = Class.forName("cur.Reflect");
        Constructor<?> constructor = clazz.getDeclaredConstructor();
        constructor.setAccessible(true);
        Object o = constructor.newInstance();

        Field a = clazz.getDeclaredField("a");
        a.setAccessible(true);
        a.set(o, 2);
		
        Method getA = clazz.getDeclaredMethod("getA");
        getA.setAccessible(true);
        
        //通过 getA() 方法获取 a 值
        System.out.println(getA.invoke(o, null));   // 1
        //直接获取 a 值
        System.out.println(a.get(o));   // 2
    }
}
```

上面我们可以发现，直接 变量 a 的值 和 通过 getA() 方法获取变量 a 的值，结果不一样

直接获取的是 修改后的值，通过方法获取的是修改前的值

这是因为 a 变量使用 final 修饰，在编译的时候编译器将它当作常量，直接在 getA() 方法的指令里返回 1，即 getA() 在指令中是写死返回 1 的



> ### 反射获取方法参数类型、参数名



```java
public class Reflect {
    public void h(int a, int b, int c){
        System.out.println("nothing");
    }
}
class C{
    public static void main(String[] args) throws Exception {
        Class<?> clazz = Class.forName("cur.Reflect");
        Constructor<?> constructor = clazz.getDeclaredConstructor();
        constructor.setAccessible(true);
        Object o = constructor.newInstance();

        Method[] methods = clazz.getDeclaredMethods();
        for(Method method : methods){
            System.out.println("方法名：" + method.getName());
            System.out.println("返回类型：" + method.getReturnType());
            System.out.println("方法修饰符的标识符：" + method.getModifiers());
            for (Class<?> pType  : method.getParameterTypes()) {
                System.out.println("方法参数类型：" + pType.getName());
            }
            for (Parameter parameter : method.getParameters()) {
                System.out.println("方法参数名称：" + parameter.getName());
            }
        }
    }
}
```

输出结果为：

```java
方法名：h
返回类型：void
方法修饰符的标识符：1
方法参数类型：int
方法参数类型：int
方法参数类型：int
方法参数名称：arg0
方法参数名称：arg1
方法参数名称：arg2
```

可以看出，反射可以获取到方法名、方法类型、方法修饰符（1 表示 public）、方法参数类型

但是对应的具体参数名称，却是反射过程中自动生成的 `argx`，没有真正获取到参数名称



在 JDK 8 中，增加了 Parameter 类，可以获取 Method 的所有参数，其他参数可以正常获取，但是 参数名称需要在编译时做处理，添加 -parameters 参数即可

![1599538401846](C:\Users\蒜头王八\AppData\Roaming\Typora\typora-user-images\1599538401846.png)

添加完参数后，输出结果：

```java
方法名：h
返回类型：void
方法修饰符的标识符：1
方法参数类型：int
方法参数类型：int
方法参数类型：int
方法参数名称：a
方法参数名称：b
方法参数名称：c
```





## 10、为何重写 equals() 和 hashCode()



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





## 11、String 为什么设计为不可变和不可继承

> ### 什么是不可变？

所谓的不可变，就是一个类的实例创建完成后，不能再修改它的成员变量值



> ### String 为什么不可变

String 类源码如下：

```java
public final class String implements java.io.Serializable, Comparable<String>, CharSequence {
    private final char value[];
```

String 内部封装了一个 char[] 数组，String 对应的数据就是存储在这个 char[] 数组中的

这个 char[] 数组使用 final 修饰，这样就导致了不能将 char[] 数组 引用变量 重新指向新的引用



我们需要知道，final 修饰的变量，指针变量不能够发生改变，但是它内部的值可以发送改变，比如

```java
final char[] chs = {'a', 'b'};
chs[0] = 'b'; //这是可以的
chs = new char[2]; //这是不可以的
```



既然 char[] 内部的值可以改变，那么为什么不直接修改呢？因为 char[] 数组使用 private 修饰，我们外面无法访问到，这就导致了 String 的不可变的特性，如果是 public 的，那么我们可以直接获取 char[] 然后修改它的值了



> ### String 为什么设计为 不可变

**为了安全**

因为字符串是最常用的，而它也经常作为方法参数进行传参，在方法内部不可避免的会修改字符串的值，这就是问题所在

与之对应的是 StringBuilder 和 StringBuffer

有如下代码

```java
class A{
    public void static main(Stirng[] args){
        String a = "abc";
        StringBuilder sb = new StringBuiler{"abc};
        h1(a);
        h2(sb);
        System.out.println(a);
        System.out.println(sb.toString());
    }
    public static void h1(String a){
        a += "aa";
    }
    public static void h2(StringBuilder sb){
        sb.append("aa");
    }
}
```

我们可以发现，a 的值没有发生改变，而 sb 的值发生了改变了

很多时间，我们并不希望 字符串的值发生改变，而仅仅只是将它作为一个参数传进方法内给它使用而已

如果它是可变的，那么在方法内很可能发生改变，但是却没有被注意到

```java
比如之前我做的一道二叉树的题目，求根到叶子节点的路径值，节点值都是字符，所以需要使用 StringBuilder，方便回溯，而当时忘了将添加值删掉，导致错误，而如果直接使用 String 的话，就无需去考虑删值了，不过效率会降低
```



并且 String 借助这个不可变性还有一个常量池属性，即它可以添加到常量池中，被多个引用进行复用，而不用创建重复的对象，节省空间 

如果可变，那么这个就失去了意义了，因为这个值随时可能被改变，这样就会影响到其他的引用



> ### String 为什么不可继承

同样是这个源码

```java
public final class String implements java.io.Serializable, Comparable<String>, CharSequence {
    private final char value[];
```

String 类使用 final 修饰，表示该类不可被继承



> ### String 为什么设计为不可继承

设置为不可继承，那么方法就不能被重写，这样才通用，**主要是 String 是一个基础类，很多的类都使用了 String**

如果用户的一个类继承 String 重写了 String 的方法，那么在其他类中本来是传入 String，使用的是 String 的方法逻辑的，却传入了用户自己写的类，导致对应的方法逻辑出现问题





## 12、try-catch-finally 和 return

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





## 13、Java 的线程状态

初始化状态（NEW）：线程刚创建

可运行状态（Runnable）：等待 CPU 调度 - 

运行状态（Running）：被 CPU 调度

阻塞状态（Blocking）：线程因为某种原因停止 CPU 调度，暂时停止运行，直到事件解决后才会重新进入 可运行状态 等待 CPU 调度

- 等待阻塞：调用 wait()，线程进入 等待阻塞状态，需要调用 notify()  或者 interrupt() 对其唤醒或者中断
- 同步阻塞：等待获取锁
- 其他阻塞：调用 sleep()、join()、磁盘 IO、键盘输入

死亡状态（dead）：线程因异常或者 执行完成 退出 run() 

![img](https://pic3.zhimg.com/80/v2-e55996045c7a1a0d669b7308824fe2c9_720w.jpg)



## 14、异常（Exception）和 错误（Error）

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





## 15、关于 String 锁（intern()）



> ### 字符串锁

```java

synchronized (h1()){
	//操作
}

synchronized (h2()){
	//操作
}

public String h1(){
    return "1";
}
public String h2(){
    return new StringBuilder("1").toString();
}
```

我们都知道，synchronized 只要锁的是同一个对象，那么其他线程来获取锁的时候会进入到这个对象所管理的 同步队列中等待唤醒，而如果获取的不是同一个对象锁，那么互不影响

对于上面的代码，h1() 返回的是 常量池中的 "1"，因此后续线程每次调用 h1() 获取的都是 常量池中 "1" 的锁

h2() 返回的是 sb 每次重新创建的 String 对象，因此后续线程每次调用 h2() 获取的都是不同对象的 锁，压根锁不住线程



> ### 字符串锁的实例问题以及解决方法

如果我们需要的业务是进行字符串拼接，比如多个线程获取一个 ip，我们需要上锁，由于 ip 是传参的，那么就无法在编译时期确定，这样的话，后续拼接就肯定是重新创建一个字符串对象，这样的话每个线程获取的都是不同对象的锁，导致锁无效

```java
final String LOCK = "lock_";

public void pro(String ip){
	String lock = h2(ip);
    synchronized(lock){
        //处理
    }
}

public String h2(String ip){
	//获取线程名
	String name = Thread.currentThread.getName();
	//ip 和 线程名进行拼接
	StringBuilder sb = new StringBuilder();
	sb.append(LOCK).append(ip);
	
    return sb.toString();
}
```



解决方法：调用 intern() 方法，这样返回的都是对同一个对象的引用了，关于这个方法，有点复杂，看下面的讲解

```java
return sb.toString().intern();	//调用 intern()
```



> ### 字符串的 intern() 

JDK 6 及以前 和 JDK 7 及以后 这个方法由于 **字符串常量池 的位置不同 **而出现了不同的效果

**JDK 6 以前是在方法区中的，而 JDK 7 将 字符串常量池 放到了 堆中**



目前可信的说法是 JDK 6 的字符串常量池存放的只有对象，JDK 7 的字符串常量池既可以存储对象，又可以存储引用



```java
String a = "abc";
```

调用 a.intern()，根据 JDK 版本不同有两种情况：

- JDK 6 的时候，，如果常量池中没有 "abc"，那么就会在常量池中创建一个 "abc"，然后返回常量池中这个  "abc" 的引用，如果- 有，那么直接返回这个对象的引用

- JDK 7 的时候，如果常量池中有 "abc"，那么直接返回引用，如果没有，那么在常量池中创建一个指向堆中 a 对象的引用，然后返回该引用的引用，即 JDK 6 不同的是， JDK 6 没有的时候创建的是对象，而 JDK 7 没有的时候是复用堆中的对象，创建的是引用

下面是证明：

```java
//常量池中不存在 ab
String a = new String("a") + new String("b");
System.out.println(a.intern() == a);
```

输出结果：

```java
JDK6：false
JDK7：true
```

上面的代码将 a 分开来使用两个 new String() 拼接而不使用 new String("a" + "b") 的原因是避免在编译期间就在常量池中创建了 "ab"，导致结果错误，上面这个代码在常量池中只有 "a" 和 "b"，是没有 "ab" 的

因此，如果是在 JDK 6，那么在常量池中创建的就是新的对象，那么返回的引用肯定跟 a 不一样，因为内存地址不同，返回 false

如果是在 JDK 7 中，在常量池中创建的是指向 a 中对象的引用，返回的是这个引用的引用，因此最终都是指向的 a 对象，因此内存地址相同，返回 true





```java
String str1 = new String("a")+ new String("b");
System.out.println(str1.intern() == str1);
System.out.println(str1 == "ab");
```

输出结果：

```java
JDK6：false;false;
JDK 7：true; true;
```

最开始 字符串常量池中只有 a 和 b，没有 ab

在 JDK 6 中，str1.intern() 直接创建对象，并返回引用，因此跟 str1 不同，后面的也一样

在 JDK 7中， str1.intern() 在常量池中创建 指向 str1 的引用，并返回该引用，因此第一个为 true，而后续直接比较 "ab"，获取的是常量池中的 "ab" 的对象或引用，而此时存在 "ab" 的引用，因此直接返回，而这个引用指向的是 str1 指向的对象，因此为 true



从这里我们可以进行推测，如果是编译期间，那么字符串常量池存储的应该是 字符串对象，比如上面的 "a" 和 "b"，在编译期间就存储进去了，这应该是不会存储引用的，因为都没有在堆中创建这两个对象，也没必要去创建，所以在常量池中创建的应该是对象

而当运行过程中调用 str.intern() 的时候，这时候 str 指向的对象必定已经创建出来了，无论这个 str 是在堆中还是在常量池中，如果是在常量池中，那么直接返回的是对这个对象的引用，跟 str 一样，无需创建引用，如果是在堆中，那么创建指向堆中 str 指向的对象的引用，然后返回

因此 JDK 7 中的字符串常量池 存储 对象 和 引用