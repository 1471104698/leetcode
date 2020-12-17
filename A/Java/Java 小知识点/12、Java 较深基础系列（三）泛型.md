# 泛型



 [泛型概述(上)：泛化与特定 - 知乎 (zhihu.com)](https://zhuanlan.zhihu.com/p/240003959) 

 [泛型概述(下)：泛型实现机制 - 知乎 (zhihu.com)](https://zhuanlan.zhihu.com/p/255264414) 



## 1、泛型出现的原因

在 JDK 1.5 之前，没有泛型，对于 ArrayList 的实现是这样子的：

```java
public class ArrayList {
    private Object[] array;
    private int size;
    public void add(Object e) {...}
    public void remove(int index) {...}
    public Object get(int index) {...}
}
```

直接使用 Object 数组，不能指定任何的类型

而获取数据的时候就需要程序员自己强转类型：

```java
ArrayList list = new ArrayList();
list.add("Hello");
// 获取到Object，必须强制转型为String:
String first = (String) list.get(0);
```

但这样做就很容易出现 强转异常 ClassCastException，因为 Object 可以存储任何类型的对象，这样就很容易强转错误

因此一种解决方法就是对应每一种类型的数据都创建一个对应的 ArrayList:

```java
public class StringArrayList {
    // 因为这种ArrayList只存String，所以不需要用Object[]兼容所有类型，只要String[]即可
    private String[] array;
    private int size;
    public void add(String e) {...}
    public void remove(int index) {...}
    public String get(int index) {...}
}

public class IntegerArrayList {
    private Integer[] array;
    private int size;
    public void add(Integer e) {...}
    public void remove(int index) {...}
    public Integer get(int index) {...}
}
```

但这显然是疯狂的，上千万上亿个类，想想就好

于是，为了解决这个问题，就必须把 ArrayList 设计为一种模板

如设计模式中的模板，就是把我能做的都做了，剩下的我决定不了的就给你可以实现**（抽象类实现了大部分相同的逻辑，特殊的逻辑留给子类实现）**

**因此出现了泛型**，将 ArrayList 做成一个模板类，把所有的基本功能都实现好，唯一不能指定的就是数据类型，这个需要程序员来指定。这里的 ArrayList 就类似抽象类，而程序员指定数据类型就类似抽象类的子类

```java
public class ArrayList<T> {
    private T[] array;
    private int size;
    public void add(T e) {...}
    public void remove(int index) {...}
    public T get(int index) {...}
}
```



## 2、泛型底层实现原理

 [Java泛型-4（类型擦除后如何获取泛型参数） - 简书 (jianshu.com)](https://www.jianshu.com/p/cb8ff202797c) 



这里简述下泛型的实现原理：

```java
Java 的泛型是伪泛型，相当于是一个语法糖：
它只在程序的源代码中出现，而在编译的时候，通过泛型擦除，将泛型参数全部替换为原生类型（如果没有设置下界，那么默认是 Object），而在相应调用的地方将 原生类型 添加上类型强转的代码

因此对于运行时期的 ArrayList<Integer> 和 ArrayList<String> 来说，实际上它们都是 ArrayList<Object>
    只不过在调用的时候会进行强转，比如 (Integer)list.get(0) 和 (String)list.get(0)
    
因此这个伪泛型的意思就是，通过代码来指定某个类的泛型类型，但是编译底层是通过代码强转来实现的
```



以下是泛型实现的具体讲解：

```java
class Test<T> {
    private T data;
    private Set<String> set = new HashSet<>();

    public boolean isBoolean(Test<T> data) {
        Map<String, String> map = new HashMap<>();
        map.put("hello", "world");
        map.put("你好", "世界");
        System.out.println(map.get("hello"));
        return true;
    }
    public static void main(String[] args) {
        Test<Integer> test = new Test<>();
    }
}
```



反编译 class 文件得到编译时期的类源代码：

```java
class Test {
    private Object data;
    private Set set;
    Test(){
        set = new HashSet();
    }
    public boolean isBoolean(Test data) {
        Map map = new HashMap();
        map.put("hello", "world");
        map.put("你好", "世界");
        System.out.println((String)map.get("hello"));
        return true;
    }
    public static void main(String args[]) {
        Test test = new Test();
    }
}
```

通过反编译字节码可以发现以下几点改变：

- 当没有构造器的时候编译器会自动加上无参构造器
- 泛型 T 全部被替换成 Object 或者 删除
- 泛型擦除后，编译器会自动在调用的地方添加类型强转的代码



因此，我们可以作此结论：	

- 编译期间会将所有的泛型全部擦除，删除 <T> 或者转换为原生类型，在只声明为 T 的情况下，T 会转变成 Object，如果是 <T extends B> 的话，那么就是将 T 变成 B，这里的 Object 和 B 称为原始类型

- 在 JVM 中是没有泛型的，只有普通的类和方法，因为在编译成 class 文件期间就已经被擦除了，指向的时候 JVM 已经不知道是什么类型了
- 泛型擦除的只是字节码表面上的泛型，元数据中实际上还是保存了类的泛型类型的，因此 JVM 可以通过反射获取泛型





## 3、泛型擦除的问题



### 1、基本数据类型不能使用泛型

使用泛型擦除，将泛型 T 转换为 原生类型（Object）

这也意味着 基本数据类型 是不能作为泛型的，因为它们不是一个类，没有原生类型 的说法

因此才会出现 Integer 之类的包装类来代替 int 这些基本数据类型

（作者在发布 泛型的时候 同时加入了 自动装箱和拆箱 这个语法糖）



### 2、方法重载问题

当进行 方法重载 时，参数中泛型不同不能作为重写的依据，因为它们在编译的时候都会进行泛型擦除

```java
public int h(List<Integer> list){
    return 1;
}
public String h(List<String> list){	//编译错误
    return "";
}

//泛型擦除
public int h(List<Object> list){
    return 1;
}
public String h(List<Object> list){	//编译错误
    return "";
}
```

这意味着无法去唯一区分它们，所以编译不通过，重写失败

而返回值不同也不能作为重写的依据是因为：

```java
public static void main(String[] args){
    h(new ArrayList<String>());
    h(new ArrayList<Integer>());
}

//泛型擦除
public static void main(String[] args){
    h(new ArrayList<Object>());
    h(new ArrayList<Object>());
}
```

当我们调用两个不同泛型参数的方式，因为在编译的时候会进行泛型擦除，所以方法的唯一区别也没有了，当我们没有接收返回参数，就根本不知道调用的是哪个方法



### 3、方法重写问题



[泛型擦除造成的方法重写问题](https://www.cnblogs.com/wuqinglong/p/9456193.html)



定义一个 父类 A 代码如下：

```java
class A<T> {  //泛型 T
    private T value;  
    public T getValue() {  
        return value;  
    }  
    public void setValue(T value) {  
        this.value = value;  
    }  
}
```

子类 B 继承 A 并且指定泛型

```java
class B extends A<String> {  //指定泛型 String
    @Override
    public void setValue(String value) {
        super.setValue(value);
    }
    @Override
    public String getValue() {
        /*
        这里调用的父类方法，虽然在父类方法中返回的是 T
        但是我们在上面 A<String> 指定了父类的泛型为 String，因此 父类的 T 会转换为 String
        即 return super.getValue() 返回的是 String 类型
        */
        return super.getValue();
    }
}
```

这样咋一看好像没有什么题，`@Override` 标签也出现了，那么重写完成，但是实际上真的是就看起来这么简单吗？

在编译的时候对父类进行泛型擦除，那么父类方法变成：

```java
public Object getValue() {  	//T 变成 Object
    return value;  
}  
public void setValue(Object value) {  //T 变成 Object
    this.value = value;  
}  
```

而子类代码不变：

```java
public String getValue() {  
    return super.getValue();  
}  
public void setValue(String value) {  
    super.setValue(value);  
}  
```

这样的话已经不是重写了，而是方法重载了，父类方法参数是 Object，子类方法参数是 String，这种的在普通的父子类继承种就是方法重载，比如

```java
public void h(String str){
}
public void h(int i){	//方法重载
}
```

因此我们可以得出编译的时候是不会认为这是重写的，而是当作重载

那么编译器是如何实现重写的呢？**通过桥方法**

使用 `javap -c B` 反编译 B 的字节码：

```java
class cur.B extends cur.A<java.lang.String> {
  cur.B();
    Code:
       0: aload_0
       1: invokespecial #1                  // Method cur/A."<init>":()V
       4: return

  public void setValue(java.lang.String);	//我们重写的 setValue()
    Code:
       0: aload_0
       1: aload_1
       2: invokespecial #2                  // Method cur/A.setValue:(Ljava/lang/Object;)V
       5: return

  public java.lang.String getValue();		//我们重写的 getValue()
    Code:
       0: aload_0
       1: invokespecial #3                  // Method cur/A.getValue:()Ljava/lang/Object;
       4: checkcast     #4                  // class java/lang/String
       7: areturn

  public void setValue(java.lang.Object);	//编译时期编译器生成的桥方法 setValue()
    Code:
       0: aload_0
       1: aload_1
       2: checkcast     #4                  // class java/lang/String
       5: invokevirtual #5                  // Method setValue:(Ljava/lang/String;)V  调用我们重写的方法
       8: return

  public java.lang.Object getValue();		//编译时期编译器生成的桥方法 getValue()
    Code:
       0: aload_0
       1: invokevirtual #6                  // Method getValue:()Ljava/lang/String;	 调用我们重写的方法
       4: areturn
}

```

从反编译结果来看，B 类中存在 4 个方法，前两个是我们自己重写的方法，后两个是编译器编译时期生成的桥方法

在桥方法中调用了我们重写的方法，可以看到**两个桥方法的参数都是 Object，即这两个桥方法才是真正意义上覆盖了父类的方法，即真正意义上重写的方法**

即方法重写的实现是：编译器自己生成桥方法覆盖父类的方法，然后在内部调用我们自己意义上重写的方法，然后在方法表上的是桥方法，从而将我们自己重写的方法隐藏起来了，使得重写不会由于 泛型擦除 而失效



因此，**在讲方法重写的时候，如果涉及到泛型，那么从两方面讲起：**

1. JVM 方法区的虚方法表 + 编译期间确定重载的方法 + 运行期间确定重写的方法
2. 在泛型的情况下，需要使用桥接方法来覆盖父类的方法，然后在桥接方法中调用子类用来重写的方法





## 4、泛型 上界 和 下界



在说明 泛型上下界 之前，我们先了解一下：

- ? 是通配符，一般用于变量的泛型声明
-  T 、E 、K、V 是 泛型，一般用于类、方法的泛型声明
  - T：type
  - E：Element
  - K、V：key-value



一般情况下我们声明 ArrayList 的泛型如下：

这种就是指定了固定的元素类型的

```java
List<String> list = new ArrayList<>();
```

但是我们有时候不会去指定泛型类型，比如 Class：

```java
Class<?> clz = Class.forName("cn.oy.A");
```

上面这种代码我们写了很多遍，我们单单只知道它是指定了泛型

但是左边的变量泛型声明 和 右边的对象泛型变量声明是一样的吗？

```java
List<String> list = new ArrayList<>();
```

就比如上面这个代码，我们一直只是声明了左边的泛型，但是右边一直都是省略的，写上也没啥问题，这样右边的这个泛型声明难道没有作用吗？？？

实际上并不是，只是平常没有遇到过，或者说遇到过但是没有去了解过罢了。。。下面来详细说明

我们需要先明白，**左边变量声明的泛型类型 限制了 右边对象能够声明的泛型类型**

- List<String> list 表示指向的对象 ArrayList 必须是存储 String 类型的，即只能是 new ArrayList<String>()，因为已经确定了只能是 String，所以一般我们右边都是省略了的，但是写上也没有问题

  - ```java
    List<String> list = new ArrayList<>();
    List<String> list = new ArrayList<String>();
    ```

- List<?> list 表示变量指向的对象可以是任意的类型，比如可以是存储 String 的 ArrayList，可以是存储 Integer 的 ArrayList，对于这种情况，我们在右边就必须指定好 数据类型：

  - ```java
    List<?> list = new ArrayList<String>();
    List<?> list = new ArrayList<Integer>();
    List<?> list = new ArrayList<A>();
    ```

    

因此，我们对 `List<?> list = new ArrayList<String>();` 来进行一个完整的解读：

以下是 ArrayList 类的源码：

```java
public class ArrayList<E> extends AbstractList<E>
        implements List<E>, RandomAccess, Cloneable, java.io.Serializable
{
	private static final int DEFAULT_CAPACITY = 10;
	transient Object[] elementData; 
}
```

当我们执行 `List<?> list = new ArrayList<String>();` 时，List<?> list 限制的是指向的对象的泛型类型，而 new ArrayList<String>() 则是在被前面变量限制的情况下指定了某个泛型，这里的泛型是 String，它直接作用于创建的 ArrayList 对象，即相当于创建了一个 内部元素只能为 String 的 ArrayList 对象：

```java
public class ArrayList extends AbstractList
        implements List, RandomAccess, Cloneable, java.io.Serializable
{
	private static final int DEFAULT_CAPACITY = 10;
	transient String[] elementData; 
}
```







因此，我们就可以对 泛型的上下界进行说明了：

- <? super A> 表示下界，List<? super A> 表示指向的对象声明的泛型必须是 **A 类 或者 是 A 的父类**

- <? extends A> 表示上界，List<? extends A> 表示指向的对象声明的泛型必须是 **A 类 或者 是 A 的子类**



```java
class Father {	}
class Son extends Father {  }
class GrandSon extends Son {	}

public static void main(String[] args) {
    /*
    <? super Son> 表示创建的对象泛型类型必须为 Son 类 或者 Son 的父类
    因此这里声明泛型为 Object、Father、Son 是可以的
    */
    List<? super Son> list = new ArrayList<Object>();
    list = new ArrayList<Father>();
    list = new ArrayList<Son>();
    //list = new ArrayList<GrandSon>(); 报错，因为 GrandSon 是 Son 的子类

    /*
	<? extends Son> 表示创建的对象泛型类型必须为 Son 类 或者 Son 的子类
    因此这里声明泛型为 Son 是可以的
	*/
    List<? extends Son> list1 = new ArrayList<Son>();
    list1 = new ArrayList<GrandSon>();
    //list1 = new ArrayList<Father>(); 报错，因为 Father 是 Son 的父类
}
```



但是我们需要注意的是，因为变量声明使用过了`通配符 ?`，真正的元素类型是在运行期间才会确定的，

在编译期间编译器是不会知道 变量所指的真正的对象的元素类型的

因此，基于此，编译器必定会做出一定的限制，避免出现数据类型错误。



我们来分析下，对于 上界 和 下界 两种情况 编译期间 在编译器的视角是如何的

假设存在：

```java
public class A extends B implements C，D { 
    
}
class E extends A { 
    
}
class F extends A { 
    
}
```

首先是 `List<? super A>`，编译期间，编译器能得到的消息是 list 所指的对象的数据类型必定是 A 类 或者 A 的父类/父接口

- 对于 get() 方法，由于内部是 A 类 或者 A 的 父类/父接口，它们不一定存在继承关系，可能是 A 实现了多个接口，因此 A 可能存在多个没有任何关系的父类，这样的话编译器就无法确定 list 内部元素是什么类型的，因此就无法使用确定的变量类型进行接收，因此只能使用所有类的 基类 Object 作为变量类型来接收了

  - ```java
    List<? super A> list = new ArrayList<A>();
    Object a = list.get(0);	//编译成功
    
    /*
    A a = list.get(0);	 
    编译错误，因为即使设置了对象的数据类型为 A 
    但是在编译器的视角里它无法确定是 A，而可能是 A 的任意父类中的其中一个
    
    编译器怎么知道你指定的对象类型是 A、B、C、D 中的哪一个，因此不能直接使用其中的一个类型来接收
    只能使用基类 Object
    */
    ```

- 对于 add() 方法，由于编译器只能确定对象的泛型类型必定是 A 类 或者 A 的父类，这样的话能够添加的对象类型也必须基于这一特点，而不会出现数据类型混乱，这样的话它**能够添加的对象类型必须是 A 类 或者  A 的子类**，因为这样的话无论右边指定的对象类型为何，都保证了是 A 类或者 A 的子类，后面可以统一转换为 A 类，满足多态

  - ```java
    List<? super A> list = new ArrayList<A>();
    list.add(new A());	//编译成功
    list.add(new E());	//编译成功
    
    /*
    list.add(new B());
    编译失败
    多态问题
    */
    ```

- 综上，我们可以发现，`<? super A>` 声明的类型 和 内部数据添加的类型是刚好相反的，一旦是 super ，那么添加的数据类型就只能是 A 类 或者 A 的子类，并且接收只能使用 Object，即类的元数据都被擦除了



然后是 `List<? extends A>`，编译期间，编译器能够得到的信息是，右边创建的对象的数据类型是 A 类 或者 A 的子类

- 对于 get() 方法，由于内部元素都是 A 类或者 A 的子类，这样的话接收可以使用 A 类 以及 父类 作为变量类型进行接收，同样的，无法使用子类进行接收，这个其实就很容易理解了，我们总不能写出 `String str = new Object()`

  - ```java
    List<? extends A> list = new ArrayList<E>();
    //以下都编译成功
    A a = list.get(0);
    B b = list.get(0);
    C c = list.get(0);
    
    //以下编译失败，
    E e = list.get(0);
    ```

- 对于 add() 方法，由于内部是 A 类 或者 A  的子类，无法确定到底是哪一种，这样的话就无法直接添加对应的子类对象了，添加父类对象就更不用说肯定不行了，因此编译器直接将 add() 方法就作废，即防止出现数据混乱直接不能添加任何元素

  - ```java
    List<? extends A> list = new ArrayList<E>();
    list.add(new A());//编译错误，很明显，无法满足多态
    list.add(new F());//编译错误，很明显，无法满足多态
    list.add(new B ());//编译错误，很明显，添加父类更加不可能满足多态
    ```

- 综上，`? extends A` 声明下的对象它可以使用 A 类 及其 父类进行接收，但是为了数据安全无法添加任何的对象 