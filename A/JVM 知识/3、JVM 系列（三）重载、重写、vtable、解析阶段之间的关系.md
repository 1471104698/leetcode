# 重载、重写、vtable、解析阶段之间的关系

具体看： https://my.oschina.net/xiaolyuh/blog/3168216 



## 1、方法调用的字节码指令

- `invokestatic`：表示当前调用静态方法。
- `invokespecial`：表示当前调用实例构造器<init>()方法、private 方法 和 父类中的方法。
- `invokevirtual`：表示当前调用某个虚方法，生成虚方法表。
- `invokeinterface`：表示调用接口方法，生成接口方法表。
- `invokedynamic`：先在运行时动态解析出调用点限定符所引用的方法，然后再执行该方法。



方法分为虚方法和非虚方法

**非虚方法：**在编译期间就可以确定调用的是哪个类的哪个具体方法的，一般情况下是不能被继承的方法

- 静态方法
- private 方法
- final 方法
- 构造方法

这些在编译成字节码期间就完全可以确定调用的是哪个方法了，因为它们无法被继承和重写，像这样的方法直接通过常量池的符号引用就直接确定了，无需去查找是父类还是子类的方法，因此它只属于一个类的



**虚方法：**不能在编译期间确定的，因为可能会发生方法重写，所以不能具体确定调用的是哪个子类还是父类的方法

- public 、protected、默认访问修饰符的方法

对虚方法的调用会创建一张虚方法表，以此来实现方法重写调用

这种的不能在编译期间就确定调用的是谁的方法，因此需要 JVM 类加载的 解析阶段自己去根据符号引用查找 当前类以及父类的元数据判断调用的是谁的方法，然后再将符号引用转换为直接引用，这种的需要 JVM 去做的，所以被称作运行期解析



## 2、方法重载

方法重载指的是一个类中的多个方法名相同，而参数不同的情况

而编译器就需要根据传入的参数来判断调用的具体是哪个方法



以下代码的 sayHello() 使用了方法重载

```java
public class StaticDispatch {

    static abstract class Human {
    }
    static class Man extends Human {
    }
    static class Woman extends Human {
    }
    public void sayHello(Human guy) {
        System.out.println("hello,guy!");
    }
    public void sayHello(Man guy) {
        System.out.println("hello,gentleman!");
    }
    public void sayHello(Woman guy) {
        System.out.println("hello,lady!");
    }
    public static void main(String[] args) {
        Human man = new Man();
        Human woman = new Woman();
        StaticDispatch sr = new StaticDispatch();
        //上述有多个重载方法，通过传入参数不同来调用不同的重载方法
        sr.sayHello(man);
        sr.sayHello(woman);
    }
}
```

输出结果：

```javva
hello,guy!
hello,guy!
```

调用的都是父类的 sayHello()



通过 javap -c 查看字节码指令

```java
  public static void main(java.lang.String[]);
    Code:
       0: new           #7                  // class cur/StaticDispatch$Man
       3: dup
       4: invokespecial #8                  // Method cur/StaticDispatch$Man."<init>":()V
       7: astore_1
       8: new           #9                  // class cur/StaticDispatch$Woman
      11: dup
      12: invokespecial #10                 // Method cur/StaticDispatch$Woman."<init>":()V
      15: astore_2
      16: new           #11                 // class cur/StaticDispatch
      19: dup
      20: invokespecial #12                 // Method "<init>":()V
      23: astore_3
      24: aload_3
      25: aload_1
      26: invokevirtual #13                 // Method sayHello:(Lcur/StaticDispatch$Human;)V
      29: aload_3
      30: aload_2
      31: invokevirtual #13                 // Method sayHello:(Lcur/StaticDispatch$Human;)V
      34: return

```



我们可以发现 26 和 31 的方法，在后面注释写明了调用的是 Human 的 sayHello()

```java
Human man = new Man();
Human woman = new Woman();
```

在上面的语句中， Human 是静态类型， Man 和 Woman 是实际类型

编译器在编译期间完成重载方法的具体调用，由于静态类型是不会发生改变的，后续执行会发生改变的是实际类型

要确定调用的是哪个重载方法，参数类型、参数个数 来判断，这里重载的 sayHello() 只有一个参数，那么只能通过参数类型来判断，由于这里 man和 woman 指针在编译期间编译器并不知它实际指向的类型，所以它只会根据必定不会发生变化的静态类型来进行参数指定

那么对于 编译器 来说，方法的调用代码如下：

```java
sr.sayHello((Human)man);
sr.sayHello((Human)woman);
```

因此它会调用参数为 Human 的重载方法



Human 有三个方法，重载的方法编译成字节码时，在常量池中的位置为 #1 #2 #3

这样的话，通过方法参数判断调用的是哪个方法，假设是 #2，然后写成字节码指令 invokevirtual #2

这就意味着会调用第 2 个重载的方法

但是需要注意的是，重载的方法是可能会被子类重写的，因此这里只是确定了同个类中多个重载方法中的某个方法

而这个方法如果被子类重写，那么在父类 和 子类中都存在这个方法，那么具体是调用父类还是子类的方法，在编译期间是无法确定的，因为说了编译器在编译期间是不知道具体的引用对象类型的，所以需要在 JVM 在运行期间自己去判断调用的是谁的方法

因此就涉及到 方法重写的原理了



> ### 为什么编译器在编译期间不知道引用指针引用的实际类型

比如

```java
public class A {

    class B extends A{}
    class C extends A{}
    public static void main(String[] args) {
        A b = new B();
        A c = new C();
    }
}
```

上面不是很容易就可以在编译期间确定引用的对象类型吗？

但是，如果是这样的一种情况：

```java
public class A {
    class B extends A{}
    class C extends A{}
    public static void main(String[] args) {
        int i = 2;
        A a;
        if(i == 2){
            a = new B();
        }else{
            a = new C();
        }
    }
}
```

这种在 if() 条件里面的赋值，具有不确定性

不过这种的照样是可以通过上下文的 i 值来确定具体引用的对象类型

那么，如果是下面的这种呢？

```java
public class A {
    class B extends A{}
    class C extends A{}
    public static void main(String[] args) {
        int i = new Scanner().nextInt();
        A a;
        if(i == 2){
            a = new B();
        }else{
            a = new C();
        }
    }
}
```

这种需要根据用户行为来指定引用对象的，是编译期间无法确定的

因此，编译器就不想针对某种情况做些多余的事了，直接根据静态引用类型来判断方法重载调用了

没必要去猜测具体引用的是什么对象类型，让 JVM 运行的时候去做就行了



## 3、方法重写

如下代表存在方法重载和方法重写

```java
public class A {

    public void sayHello() {
        System.out.println("hello, A");
    }
    public void sayHello(String str) {
        System.out.println("hello, A, String");
    }
    public void sayHello(int i) {
        System.out.println("hello, A, int");
    }
    static class B extends A {
        @Override
        public void sayHello() {
            System.out.println("hello, B");
        }
        @Override
        public void sayHello(String str) {
            System.out.println("hello, B String");
        }
        @Override
        public void sayHello(int i) {
            System.out.println("hello, B, int");
        }
    }

    public static void main(String[] args) {
        A a = new A();
        A b = new B();
        a.sayHello(1);
        b.sayHello(1);
    }
}
```

输出：

```
hello, A, int
hello, B, int
```



通过 java -p 可以查看 字节码指令

```java
  public static void main(java.lang.String[]);
    descriptor: ([Ljava/lang/String;)V
    flags: ACC_PUBLIC, ACC_STATIC
    Code:
      stack=2, locals=3, args_size=1
         0: new           #7                  // class cur/A
         3: dup
         4: invokespecial #8                  // Method "<init>":()V
         7: astore_1
         8: new           #9                  // class cur/A$B
        11: dup
        12: invokespecial #10                 // Method cur/A$B."<init>":()V
        15: astore_2
        16: aload_1
        17: iconst_1
        18: invokevirtual #11                 // Method sayHello:(I)V
        21: aload_2
        22: iconst_1
        23: invokevirtual #11                 // Method sayHello:(I)V
        26: return

```

在方法重载的时候讲了，编译期间只能根据静态类型、方法名和方法参数 唯一确定重载的方法，比如上面 18 和 23 的 invokevirtual 后面对应的参数都是 #11，表示调用的是同一个方法



```java
//A 类具有的方法如下：
sayHello()
sayHello(String str)
sayHello(int i)

//B 类具有的方法如下：
sayHello()
sayHello(String str)
sayHello(int i)
```

编译器判断出了是选择的 sayHello(int i)

但是它并没有指定调用的是 A 的 sayHello(int i) 还是 B  的 sayHello(int i)，从字节码可以看出统一默认都是 A 的 sayHello(int i)

但是最终输出结果表明调用的并不是同一个方法，A 和 B 都调用了自己的方法

不是不能确定调用的是子类还是父类的方法么？那么这个结果是怎么实现的？

通过 JVM 解析阶段 + 虚方法表



## 4、解析阶段

具体看[JVM里的符号引用如何存储？](https://www.zhihu.com/question/30300585)

​	[JVM 常量池解析](<https://blog.csdn.net/luanlouis/article/details/40301985?utm_medium=distribute.pc_relevant_t0.none-task-blog-BlogCommendFromMachineLearnPai2-1.edu_weight&depth_1-utm_source=distribute.pc_relevant_t0.none-task-blog-BlogCommendFromMachineLearnPai2-1.edu_weight>)

[虚方法表存储的是什么？]( https://www.zhihu.com/question/27459122/answer/36736246 )



 虚拟机规范之中并未规定解析阶段发生的具体时间，只要求在执行 getfield、invokestatic、invokevirtual 之类的字节码指令之前，先对它们所使用的符号引用，比如  invokevirtual #2 中 #2 指代的常量池中的内容进行解析，将符号引用转换为直接引用

JVM 在类加载的时候会将 Class 各个部分解析为 JVM 的内部数据结构，比如 Klass、Method（methodblock）、FIeld 等

在刚加载好一个类的时候， 常量池 和 每个方法的字节码（Code 属性）会被原样拷贝到内存中，也就是说还是处于 "符号引用" 的状态，等到真正被调用执行字节码指令的时候才会进行解析



### 1、Code 属性的重要性

```java
    Code:
      stack=2, locals=3, args_size=1
         0: new           #7                  // class cur/A
         3: dup
         4: invokespecial #8                  // Method "<init>":()V
         7: astore_1
         8: new           #9                  // class cur/A$B
        11: dup
        12: invokespecial #10                 // Method cur/A$B."<init>":()V
        15: astore_2
        16: aload_1
        17: iconst_1
        18: invokevirtual #11                 // Method sayHello:(I)V
        21: aload_2
        22: iconst_1
        23: invokevirtual #11                 // Method sayHello:(I)V
        26: getstatic     #2                  // Field java/lang/System.out:Ljava/io/PrintStream;
        29: getstatic     #12                 // Field c:I
        32: invokevirtual #13                 // Method java/io/PrintStream.println:(I)V
        35: return

```

Code 属性是一个方法的内部代码转换成的 JVM 指令，Code 属性才是真正的 JVM 执行的指令

JVM 执行程序实际上就是在执行各个方法中 Code属性的 指令

我们可以看出，javap -v 后得到的字节码指令，包含 常量池和 各个方法的 Code、局部变量表之类的

而常量池是为 方法的 Code 中的指令服务的，而 Klass 中 Method 的元数据实际上就是存储方法名、方法参数之类的，并不会存储 Code 属性，但它存储了虚方法表的索引，通过虚方法表获取方法的字节码地址，这个方法字节码就是 Code 属性里的指令

因此，我们可以发现，整个解析阶段也是为 Code 属性里的指令服务的

对 invokevirtual #11 进行解析，得到调用方法的字节码地址

对 getstatic     #2 进行解析，得到调用变量的偏移量



### 2、方法重写：方法解析

**首先我们需要先知道虚方法表：**

虚方法表是一个存储 vtableEntry 结构体的数据，每个 vtableEntry 存储的是 Method*，

虚方法表在解析之前就已经初始化好了，JVM 会将父类的虚方法表拷贝给子类，如果子类重写了父类的方法，那么将对应虚方法表位置的 vtableEntry  指向自己重写方法的 Method，如果是子类自己定义的，那么在虚方法表尾部接着创建新的 vtableEntry 节点，指向自己定义的 Method

这意味着**子类和父类 的所有共有方法在虚方法表中的 偏移量（索引）是相同的**，这是多态实现的一个关键

以下这张图就体现了：子类和父类的虚方法表的索引位置都是一致的

![img](https://oscimg.oschina.net/oscnet/up-3eaf3361f273c0f758a8e772f56f3d885e0.png)



**我们还需要知道 invokevirtual #11 的含义：**

invokevirtual 指的是调用某个方法，注意是调用，而不是声明某个方法，是执行某个方法的时候在内部又调用别的方法的指令

后面的 #11 指的是调用的这个方法在常量池中的 符号引用 Methodref 结构体

下面是常量池的内容，可以看到 #1 #4 #8 #10 #11 全部都是 Methodref 结构体，并且注释对应的就是一个方法

```java
Constant pool:
   #1 = Methodref          #12.#38        // java/lang/Object."<init>":()V
   #2 = Fieldref           #39.#40        // java/lang/System.out:Ljava/io/PrintStream;
   #3 = String             #41            // hello, A
   #4 = Methodref          #42.#43        // java/io/PrintStream.println:(Ljava/lang/String;)V
   #5 = String             #44            // hello, A, String
   #6 = String             #45            // hello, A, int
   #7 = Class              #46            // cur/A
   #8 = Methodref          #7.#38         // cur/A."<init>":()V
   #9 = Class              #47            // cur/A$B
  #10 = Methodref          #9.#38         // cur/A$B."<init>":()V
  #11 = Methodref          #7.#48         // cur/A.sayHello:(I)V
```



Methodref 结构体包含了方法的字符串信息：所属的类 class_index、方法名 name_index、方法修饰符 descriptor_index

![img](https://img-blog.csdn.net/20141110150930996)



JVM 通过 定位到常量池的 #11 获取 Methodref 结构体

根据 Methodref 里面的 class_index 获取全类名然后根据全类名查找到对应类的元数据，然后再根据 name_index 获取方法名，根据方法名到 该类的 Klass 上查找 _methods 获取 方法元数据所在的 methodblock 结构体（存储一个方法的元数据，在 C++ 中叫做 methodblock，在 Java 中叫做 Method），然后将 methodblock 的指针引用（地址）拷贝到常量池的 #11 位置替换掉 Methodref，后续访问 #11 后就无需再去解析 Methodref，直接通过 methodblock 指针访问即可

（**这里有个问题，虽然 Methodref 存储的是父类的全类名，但是真正的调用应该是看当前对象，因此我感觉应该是获取调用的方法名，然后根据获取当前对象的元数据，查找是否存在这个方法，如果不存在，再去查找父类元数据，而不是直接去查找父类元数据，包括下面将常量池中 #11 替换，如果当前对象有 这个方法，应该替换的是当前对象的 Method**）

它会将该位置的 invokevirtual 指令修改为 invokevirtual_quick，表示这个指令已经解析过了，后续调用无需解析了

methodblock 结构体中记录了该方法在 vtable 中的索引位置 vtable index 以及 方法参数个数 size，将 invokevirtual 后面的 #11 替换为这两个值，比如原本的 指令是：

```
[B6] [00 0b]
```

其中 [00] [0b] 这两个字节表示 #11

解析后，得知 vtable index = 6， size = 1，那么替换后变成了

```
[D6] [06 01]
```

两个字节，vtable index 和 size 各占一个字节

这样的话，**经过解析后指令就变成了 invokevirtual_quick 6 1**





### 3、字段解析

这里说下解析阶段的字段解析，可以发现常量池中还存在 Fieldref，这个结构体跟 Methodref 类似，存储的变量的符号引用

![img](https://img-blog.csdn.net/20141021093957765)



有以下代码

```java
public class A {
    static int c = 2;
    public static void main(String[] args) {
        System.out.println(c);
    }
}
```



使用 javap -v 得到字节码指令如下：

```java
Constant pool:
   #1 = Methodref          #10.#37        // java/lang/Object."<init>":()V
   #2 = Fieldref           #38.#39        // java/lang/System.out:Ljava/io/PrintStream;
   #3 = String             #40            // hello, A
   #4 = Methodref          #41.#42        // java/io/PrintStream.println:(Ljava/lang/String;)V
   #5 = String             #43            // hello, A, String
   #6 = String             #44            // hello, A, int
   #7 = Fieldref           #9.#45         // cur/A.c:I

  public static void main(java.lang.String[]);
    descriptor: ([Ljava/lang/String;)V
    flags: ACC_PUBLIC, ACC_STATIC
    Code:
      stack=2, locals=1, args_size=1
         0: getstatic     #2                  // Field java/lang/System.out:Ljava/io/PrintStream;
         3: getstatic     #7                  // Field c:I
         6: invokevirtual #8                  // Method java/io/PrintStream.println:(I)V
         9: return

```

可以发现 getstatic     #7 中的 #7 指向的是 Fieldref，并且显示调用的是 A.c

当 JVM 运行 main() 方法的时候，第一次调用 getstatic 时，发现 #2 仍然是符号引用，那么就会找到常量池对应的位置，获取 Fieldref 结构体，然后通过 class_index 全类名找到对应的类的元数据，然后根据 name_index 变量名找到对应的 Filed 结构体，获取偏移 量，将常量池 #7 的 Filedref 替换为 Filed 结构体指针，将 getstatic     #7 中的 #7 参数替换为在 OOP 对象体中的偏移量 offset

这样后续再继续调用的时候，可以直接获取调用对象的 OOP 对象体，然后根据偏移量 offset 获取对应的数据



我个人进行猜测，**子类不会继承父类的静态变量和静态方法，但是可以使用**

当调用 getstatic 的时候发现在子类的 Klass 中没有找到该 static ，因此层层往父类上找该变量和方法，找到后返回引用

比如

```java
public class A {
    public static int c = 2;
}
class B extends A{
    public static void main(String[] args) {
        System.out.println(B.c);
    }
}
```

通过 javap -v 得到字节码

```java
Constant pool:
   #1 = Methodref          #6.#21         // cur/A."<init>":()V
   #2 = Fieldref           #22.#23        // java/lang/System.out:Ljava/io/PrintStream;
   #3 = Fieldref           #5.#24         // cur/B.c:I
   #4 = Methodref          #25.#26        // java/io/PrintStream.println:(I)V
   #5 = Class              #27            // cur/B
   #6 = Class              #28            // cur/A

```

发现调用的是 B.c，但是 B 中并没有定义 c，因此解析的时候会查找父类 A 元数据，找到对应的 Field 结构体后返回指针，并获取对应的偏移量

我们要知道，static 全局只有一份，因此子类是不会拷贝父类的 static 的，而是子类和父类共用这一份 static，子类的修改同时也会影响到父类



```java
public class A {
    public static int c = 2;
}
interface C{
    int c = 2;
}
class B extends A implements C{
    public static void main(String[] args) {
        System.out.println(B.c);
    }
}
```

这种代码则会编译错误，因为 B 的父类父接口中都有 static int c，因此直接调用 B.c 的话，查找是从 B 开始查，然后再获取父类查找，发现 A 和 C 中都有 c，那么无法确定调用的是哪一个，所以这里直接编译错误



但是如果换成调用 A.c 或者 C.c 就不会了，因为在编译时字节码就指定了查找 A 和 C，而不会去查找 B

```java
public class A {
    public static int c = 2;
}
interface C{
    int c = 2;
}
class B extends A implements C{
    public static void main(String[] args) {
        System.out.println(A.c);
    }
}
```



或者如果在 B 中定义一个 static int c，这样的话就直接能够在 B 中查找到这个变量了，而不会去查找 A 和 C

```java
public class A {
    public static int c = 2;
}
interface C{
    int c = 2;
}
class B extends A implements C{
    static int c = 1;
    public static void main(String[] args) {
        System.out.println(B.c);
    }
}
```



> private 变量

父类的 private 变量会被子类继承，并且会分配在 OOP 对象体中，但是子类没有访问权限，相当于就是占了内存