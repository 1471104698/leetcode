# 重载、重写、vtable、解析阶段之间的关系



[JVM 方法调用 -- OSCHINA 网站](https://my.oschina.net/xiaolyuh/blog/3168216)

## 1、虚方法和非虚方法

**非虚方法：**在编译期间就可以确定调用的是哪个类的哪个具体方法的，调用哪个方法是唯一确定的，**一般情况下是因为方法无法被重写而存在唯一性**

- static 方法
- private 方法
- final 方法
- <init> 构造方法
- 通过 super 调用父类的方法（**这是个例外**，父类方法可以被重写，但是通过 super 就指明了是调用父类的方法，而不会是子类的方法，因此在编译期间是可以确定的，不需要虚方法表）



**虚方法：**不能在编译期间确定的，因为可能会发生方法重写，所以不能具体确定调用的是哪个子类还是父类的方法

- 除去非虚方法外都是虚方法

对虚方法的调用会创建一张虚方法表来简化查找操作，以此来实现方法重写调用

这种的不能在编译期间就确定调用的是谁的方法，因此需要 JVM 类加载的 解析阶段自己去根据符号引用查找 当前类以及父类的元数据判断调用的是谁的方法，然后再将符号引用转换为直接引用，这种的需要 JVM 去做的，所以被称作运行期解析



## 2、方法调用的字节码指令

- `invokestatic`：调用静态方法。
- `invokespecial`：调用 <init>() 构造方法、private 方法 和 super 调用父类中的方法。
- `invokevirtual`：一般情况下是调用虚方法，特殊情况是 final 是非虚方法，但是也是使用该指令。
- `invokeinterface`：调用接口方法，并且对象的引用类型需要为接口，生成接口方法表。
- `invokedynamic`：一般 lambda 表达式 或者 函数式接口





## 3、确定方法重载的调用（编译器确定）

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



## 4、确定方法重写的调用（运行时确定)

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

而真正的调用对象必须在运行时才知道，所以一般是在 JVM 解析 字节码指令时，获取真正调用的方法的字节码指令，并且解释执行，具体过程看下面内容



## 5、类加载 的 解析阶段

[JVM里的符号引用如何存储？-- R 大](https://www.zhihu.com/question/30300585)

[虚方法表存储的是什么？ R 大](https://www.zhihu.com/question/27459122/answer/36736246 )

[JVM 常量池结构解析 -- CSDN](<https://blog.csdn.net/luanlouis/article/details/40301985?utm_medium=distribute.pc_relevant_t0.none-task-blog-BlogCommendFromMachineLearnPai2-1.edu_weight&depth_1-utm_source=distribute.pc_relevant_t0.none-task-blog-BlogCommendFromMachineLearnPai2-1.edu_weight>)

[invoke virtual 和 虚方法表 解析 -- 简书](https://www.jianshu.com/p/fb7ce7584c05)



 虚拟机规范之中并未规定解析阶段发生的具体时间，只要求在执行 getfield、invokestatic、invokevirtual 之类的字节码指令之前，先对它们所使用的符号引用，比如  invokevirtual #2 中 #2 指代的常量池中的内容进行解析，将符号引用转换为直接引用

JVM 在类加载的时候会将 Class 各个部分解析为 JVM 的内部数据结构，比如 Klass、Method（methodblock）、FIeld 等

在刚加载好一个类的时候， 常量池 和 每个方法的字节码（Code 属性）会被原样拷贝到内存中，也就是说还是处于 "符号引用" 的状态，等到真正被调用执行字节码指令的时候才会进行解析

**Code 内部需要使用到常量池的符号引用**



### 1、Code 属性

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



### 2、常量池 解析



**invokevirtual #11 的含义：**

invokevirtual 表示调用某个虚方法，JVM 在执行某个方法的时候在内部又调用别的方法

后面的 #11 指的是调用的这个方法在常量池中 tag = 10 位置的 Methodref 结构体

下面是常量池的内容，可以看到 #1 #4 #8 #10 #11 全部都是 Methodref 结构体

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



Methodref 结构体包含了方法的字符串信息：

```C++
CONSTANT_Methodref_info {
    u1 tag;						//字节码指令后面跟着的 #11， 这个 11 就是这个 tag
    u2 class_index;             //指向常量池的其他数据结构，调用该方法的类
    u2 name_and_type_index;     //指向常量池的其他数据结构，存储方法的方法名 和 方法描述符，方法描述符中存储着方法的 参数个数 以及 方法的访问修饰符 之类的
}
```

![img](https://img-blog.csdn.net/20141110150930996)

以下是从 Methodref  中获取信息的 JVM 源码

```C++
func (i *InterpretedExecutionEngine) invokeVirtual(def *class.DefFile, frame *MethodStackFrame, codeAttr *class.CodeAttr) error {
    twoByteNum := codeAttr.Code[frame.pc + 1 : frame.pc + 1 + 2]
    frame.pc += 2

    var methodRefCpIndex uint16
    err := binary.Read(bytes.NewBuffer(twoByteNum), binary.BigEndian, &methodRefCpIndex)
    if nil != err {
        return fmt.Errorf("failed to read method_ref_cp_index: %w", err)
    }

    // 取出引用的方法
    methodRef := def.ConstPool[methodRefCpIndex].(*class.MethodRefConstInfo)
    // 取出方法名
    nameAndType := def.ConstPool[methodRef.NameAndTypeIndex].(*class.NameAndTypeConst)
    methodName := def.ConstPool[nameAndType.NameIndex].(*class.Utf8InfoConst).String()
    // 方法描述符
    descriptor := def.ConstPool[nameAndType.DescIndex].(*class.Utf8InfoConst).String()

    // 从 方法描述符中 计算参数的个数
    argCount := class.ParseArgCount(descriptor)

    // 找到操作数栈中的引用, 此引用即为实际类型
    // !!!如果有目标方法有参数, 则栈顶为参数而不是方法所在的实际对象，切记!!!
    targetObjRef, _ := frame.opStack.GetObjectSkip(argCount)
    targetDef := targetObjRef.Object.DefFile

    // 调用
    return i.ExecuteWithFrame(targetDef, methodName, descriptor, frame, true)
}
```





JVM 通过 #11 获取到常量池中  tag = 11 的 Methodref 结构体

确定调用的真正方法的时候，根据字节码指令，有两种情况：

- 如果是 invoke special 和 invoke static，这种根据 Methodref 就可以唯一确定调用的方法了
- 如果是 invoke  virtual  和 invoke  interface，那么就需要 Methodref 中的方法名、方法的修饰符 以及 根据操作数栈的栈顶元素的数据类型 来确定调用的是谁的方法



这里需要讲一下，对于 invoke static 这样的说唯一确定调用方法 不是说直接就定位到方法所在的类的元数据，而是说不会出现不知道是调用的子类的 A() 还是 父类的 A()，static 是唯一确定的

```java
class B{
	public static void h(){}
}
class A extends B{	
    public static void main(String[] args){
        A.h();
    }
}
```

*![image.png](https://pic.leetcode-cn.com/1605665023-fdxeBt-image.png)*

可以看出，编译器在编译期间识别为了 A 的方法，但是 A 中并没有这个方法，该方法在 B 中，但是这个 h() 方法是唯一确定的，只有一个，最终必定是调用的 B 的 h()

JVM 解析 #13 时，获取 A 的元数据，扫描 _methods，发现没有 h()，那么获取父类 B 的元数据，扫描 _methods，发现 h()，那么就调用 B 的 h()



而像 invoke  virtual 这种的，由于 A 和 B 都有方法 h()，因此需要在运行期间根据 操作数栈的栈顶元素的数据类型 获取元数据，再扫描 虚方法表 获取真正调用的方法

```java
class B{
	public void h(){}
}
class A extends B{	
    @Override
    public void h() {
    }
    
    public static void main(String[] args){
        B b = new A();
        b.h();
    }
}
```



如果是 invoke special 和 invoke static：

- 根据 Methodref 获取全类名，然后根据全类名查找到对应类的元数据

- 在 Methodref 中获取方法名和方法描述符，根据 方法名和方法描述符 在当前类的 _methods 中查找目标方法的 methodblock（JVM 的 Method 对象），如果不存在，那么在父类的元数据中查找，层层往上，直到找到为止
- 然后将 methodblock 指针（地址）拷贝到常量池的 #11 位置替换掉 Methodref

如果是 invoke  virtual  和 invoke  interface：

- 根据 操作数栈 的实际对象类型 获取元数据
  - 注意，实际调用对象不一定是在操作数栈栈顶，因为方法如果存在参数，那么操作数栈栈顶就是存储参数值

- 在 Methodref 中获取方法名和方法描述符，查找当前类的 虚方法表，根据 方法名和方法描述符 进行匹配出 methodblock 
- 然后将 methodblock 指针（地址）拷贝到常量池的 #11 位置替换掉 Methodref



**常量池 #11 位置的 Methodref 替换：**

```java
比如找到的 methodblock 指针是 0x45762300	//这里地址位置是倒序的
那么常量池 #11 位置的 Methodref 结构会被替换为：
[00 23 76 45]
后续其他方法需要访问 #11 时就无需再去解析 Methodref，直接通过 该地址 得到 目标 Method 对象 即可
```



**字节码指令 invokevirtual #11 替换**：

当解析完毕后，JVM 会将 invokevirtual 指令修改为 invokevirtual_quick，表示这个指令已经解析过了，后续调用无需解析了

同时还会**从 methodblock 结构体中获取到 在虚方法表的索引位置**

然后将 #11 的 2B 替换成 虚方法表的下标 和 方法参数个数，后续执行该方法，到该指令时，直接根据 这个下标 到 操作数栈栈顶的对象类型的元数据的虚方法表上获取 methodblock 

```java
原本的字节码指令：invokevirtual #11
invokevirtual [00 0b]	//[00] [0b] 这两个字节表示 #11
    
1、解析完成，将 invokevirtual 替换为 invokevirtual_quick
invokevirtual_quick [00 0b]	

2、从 methodblock 结构体中获取到方法在虚方法表中的位置为 vtable index = 6，并且已经知道方法参数个数 size = 1
将 #11 替换为 虚方法表索引位置 和 方法参数个数
invokevirtual_quick [06 01]	//两个字节，vtable index 和 size 各占一个字节
    
3、最终指令变成 invokevirtual_quick 6 1
```



**为什么不直接将 #11 替换为 methodblock 指针？**

因为一个地址为 32bit - 4B，但 #11 只有 16bit - 2B，不够放

但其实不需要替换的，可以直接根据 #11 找到常量池的 methodblock ，不过 JVM 想要这么设计而已，可能一些细节方面的这样的效率更高



### 3、字段解析

常量池中还存在 Fieldref，这个结构体跟 Methodref 类似，存储变量的符号引用

```C++
CONSTANT_Fieldref_info {
    u1 tag;					//标签，字节码指令对应的 #11 后面的 11
    u2 class_index;             //指向常量池的其他数据结构，调用该变量的类
    u2 name_and_type_index;     //指向常量池其他数据结构，存储该变量的变量名和数据类型
}
```

  ![img](https://img-blog.csdn.net/20141021093957765)  



例子：

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
