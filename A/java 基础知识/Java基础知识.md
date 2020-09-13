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





## 3、String str = new String(`"abc"`) 创建了多少个对象

```java
String s1 = new String("abc");
String s2 = "abc";
System.out.println(s1 == s2); //false
```

当常量池中不存在 `abc` 这个字符串时，那么 `new String("abc")` 创建了两个对象，一个存放在常量池中，一个存放在 堆内存中

然后 s1 指向了堆中的对象，s2 指向常量池中的对象



```java
String s1 = "abc";
String s2 = "abc";
System.out.println(s1 == s2); //true
```

只创建了一个对象，存放在常量池中，s1 和 s2 指向的都是常量池中的对象



```java
String s1 = "a" + "bc";
String s2 = "abc";
System.out.println(s1 == s2); //true
```

如果常量池中不存在 `a` `bc` `abc` 这3 个字符串时，那么创建了 3 个对象，都在常量池中

然后 s1 和 s2 都指向了常量池中的对象







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