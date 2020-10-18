# Java 综合知识点





## 1、为什么浮点数类型 float 和 double 不能直接使用 == 进行比较

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



比如  0.32767×10^4，其中 32767 就是尾数， 4 就是指数

注意，由于在计算机中维护 `.`很麻烦，所以直接统一换算成 0.xxx 的形式，将后面的 xxx 作为尾数部分，`0.` 作为默认前缀，这样转换的时候就是将 `0.` 和 xxx 进行拼接变成 0.xxx

**因此 flaot 可表示范围为 【 -2 ^ -127，2 ^ 127】，即  [-3.4028235E38, 3.4028235E38] **



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











## 2、java 如何实现连续的内存分配

使用 new byte[size]





## 3、子类从父类继承的变量和方法

这里先给出结论：

- 子类不可以继承 父类的 static 变量、方法 以及 private 变量、方法，但可以使用父类的 static 变量和方法
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





## 4、为何重写 equals() 和 hashCode()



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









## 5、try-catch-finally 和 return

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









## 6、异常（Exception）和 错误（Error）

> ### 异常（Exception）和 错误（Error）

Exception 和 Error 都是 Throwable 的子类

Exception 讲究的是程序方面的错误，比如 Null 异常、数组越界异常、除 0 异常、SQL 运行异常、IO 异常 等

Error 讲究的是 JVM 层面的错误，比如 NotClassDefFoundClass、OOM

**只有 Exception 类型才能被 try-catch**



**非运行时异常：**IOException、SQLException、ClassNotFoundExecption这种在编译的时候就强制要求进行 try-catch 或者 throws

**运行时异常：**即只有程序运行时才会发生的异常，一般是由于程序的逻辑产生的，比如 Null 异常、数组越界异常



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





## 7、private 能够被反射获取，存在的意义是什么？

private 存在的意义不是为了绝对安全设计的，而是对用户使用 java 的一种规范，用来实现封装

让用户 从外部调用对象时，能够清晰的看到类的内部结构，屏蔽掉用户不需要知道的细节





## 8、ArrayList 的坑点 - subList()

在 阿里巴巴开发手册中，告知要慎用 ArrayList 的 subList() 方法，规定如下：

 ![-w1379](https://user-gold-cdn.xitu.io/2019/6/25/16b8c5809732a52d?imageView2/0/w/1280/h/960/format/webp/ignore-error/1) 

 ![img](https://user-gold-cdn.xitu.io/2019/6/25/16b8c58095d31b5e?imageView2/0/w/1280/h/960/format/webp/ignore-error/1)



> ### SubList 无法强转为 ArrayList

现在就来解析下 subList() 方法

```java
List<String> subList = list.subList(1, 3);

public List<E> subList(int fromIndex, int toIndex) {
    //创建一个 subList 对象，并且将当前 ArrayList 对象 this 传入
    return new SubList(this, 0, fromIndex, toIndex);
}
```

 当我们调用 subList() 的时候，内部实际上是创建了一个 SubList 对象



```java
private class SubList extends AbstractList<E> implements RandomAccess {	
    private final AbstractList<E> parent;
    private final int parentOffset;
    private final int offset;
    int size;
    //构造方法
    SubList(AbstractList<E> parent,
            int offset, int fromIndex, int toIndex) {
        //这里的 parent 就是 ArrayList 对象
        this.parent = parent;
        //偏移量
        this.parentOffset = fromIndex;
        this.offset = offset + fromIndex;
        this.size = toIndex - fromIndex;
        this.modCount = ArrayList.this.modCount;
    }
}
```
SubList 对象是 ArrayList 的一个内部类，它继承了 AbstractList，实际上就是一个 List 类，但是**不是一个 ArrayList 类，所以强转为 ArrayList 会失败**





> ### 并发异常问题

从上面代码可以看出，通过构造方法它并没有截取 指定索引范围的 ArrayList 中的元素，将其放到一个新的容器里，而是直接将 ArrayList 对象 和 偏移量给保存了起来，这意味着是要在 ArrayList 对象上直接操作啊。。。

那么操作 SubList 就会影响到 ArrayList，操作 ArrayList 就会影响到 SubList



```java
public E set(int index, E e) {
    rangeCheck(index);
    checkForComodification();
    E oldValue = ArrayList.this.elementData(offset + index);
    ArrayList.this.elementData[offset + index] = e;
    return oldValue;
}

public E get(int index) {
    rangeCheck(index);
    checkForComodification();
    return ArrayList.this.elementData(offset + index);
}

public void add(int index, E e) {
    rangeCheckForAdd(index);
    checkForComodification();
    parent.add(parentOffset + index, e);
    this.modCount = parent.modCount;
    this.size++;
}
```

通过查看 SubList 的 get() 、set()、add() ， 我们也可以发现，它实际上还真是直接对 ArrayList 进行操作

因此，当我们调用 SubList 插入元素的时候，实际上就是在 ArrayList 对应偏移量上进行元素插入，对 SubList 的操作全部会影响到 ArrayList 上

```java
public class A {
    public static void main(String[] args) throws InterruptedException {
        ArrayList<String> list = new ArrayList<String>() {{
            add("H");
            add("O");
            add("L");
            add("L");
            add("I");
            add("S");
        }};

        List<String> subList = list.subList(1, 3);

        subList.add("1");
        System.out.println(list);
        System.out.println(subList);
    }
}
```

输出结果为：

```java
[H, O, L, 1, L, I, S]
[O, L, 1]
```

可以看出 ArrayList 也被影响了



当我们操作 ArrayList 时

```java
public class A {
    public static void main(String[] args) throws InterruptedException {
        ArrayList<String> list = new ArrayList<String>() {{
            add("H");
            add("O");
            add("L");
            add("L");
            add("I");
            add("S");
        }};

        List<String> subList = list.subList(1, 3);
        // List<String> strings = Collections.singletonList("1");

        list.add("1");
        System.out.println(list);
        System.out.println(subList);
    }
}
```

输出结果：

```java
[H, O, L, L, I, S, 1]
Exception in thread "main" java.util.ConcurrentModificationException
	at java.util.ArrayList$SubList.checkForComodification(ArrayList.java:1231)
	at java.util.ArrayList$SubList.listIterator(ArrayList.java:1091)
	at java.util.AbstractList.listIterator(AbstractList.java:299)
	at java.util.ArrayList$SubList.iterator(ArrayList.java:1087)
	at java.util.AbstractCollection.toString(AbstractCollection.java:454)
	at java.lang.String.valueOf(String.java:2994)
	at java.io.PrintStream.println(PrintStream.java:821)
	at cur.A.main(A.java:31)
```

可以发现出现了并发异常



1、ArrayList 结构性修改 导致 Sublist 并发异常的原因：

如果结构性修改了 ArrayList，modCount 会 + 1，而 SubList 就跟 Itr 迭代器机制一样，在初始化的就使用一个**期望值**记录最开始 modCount，每次 遍历或者 调用方法 前都会进行判断是否发生过修改，如果修改就抛出并发异常

2、SubList 结构性修改 不会导致 ArrayList 并发异常的原因：
因为 ArrayList 的方法没有判断什么期望值，只有 ArrayList 内部的 Itr 和 SubList 才会存储一个期望值去判断



## 9、Arrays.asList() 和 Collections.singletonList() 的坑点

同上面的 ArrayList.subList() 类似的还有 Arrays.asList() 和 Collections.singletonList()，它们内部都是返回的 Arrays 自己的内部类

因此是无法进行强转的，但都实现了 List 接口，只能使用 List 进行接收

```java
public class Arrays {
    public static <T> List<T> asList(T... a) {
        return new ArrayList<>(a);
    }
    //内部类还 ArrayList 的名字。。。
    private static class ArrayList<E> extends AbstractList<E>
        implements RandomAccess, java.io.Serializable
    {
        private final E[] a;

        ArrayList(E[] array) {
            a = Objects.requireNonNull(array);
        }
        //xxxx
    }
}
```

```java
public class Collections {
    private static class SingletonList<E>
        extends AbstractList<E>
        implements RandomAccess, Serializable {

        //因为 SingletonList 只存储一个值，所以不需要数组接收 
        private final E element;

        SingletonList(E obj){
            element = obj;
        }
    }
}
```



并且它们不能执行 remove() 和 add() 方法

```java
List<String> asList = Arrays.asList("1", "2");
asList.remove(1);
```

输出结果：报 UnsupportedOperationException 错误

```java
Exception in thread "main" java.lang.UnsupportedOperationException
    at java.util.AbstractList.remove(AbstractList.java:161)
    at cur.A.main(A.java:17)
```



我们看它们继承的 AbstractList 内部的方法

```java
public void add(int index, E element) {
    throw new UnsupportedOperationException();
}

public E remove(int index) {
    throw new UnsupportedOperationException();
}
```

没有任何实现，只是抛出异常，而 Arrays.ArrayList 和 Collections.SingletonList 内部又没有重写这个方法，因此不能进行结构性修改，即表示**它们仅仅是作为一个视图来进行遍历操作的**



## 10、NotClassDefFoundClass 和 ClassNotFoundClass 的区别

具体看  https://www.cnblogs.com/xiao2shiqi/p/11740563.html 



ClassNotFoundClass 是 Exception 类型，它可以被 tey-catch，产生的原因是比如 Class.forName() 时没有找到对应的 class 对象

NotClassDefFoundClass 是 Error 类型，它表示编译时存在对应的 Class 文件，但是在类加载的时候就没有找到了，比如我们在启动的时候删除了某个类的 class 文件，那么就会抛出 NotClassDefFoundClass 





## 11、数据类型转换问题



以下这种写法有问题吗？如果没有，答案是多少？

```java
short s = 1;
s = s + 1;
```

编译会出现问题，因为 1 默认是 int 类型，而  s = s + 1 会自动类型提升，这样的话就变成 short = short + int，最终结果应该是 int，但是使用 short 接收，所以出现问题



以下这种写法有问题吗？如果没有，答案是多少？

```java
short s = 1;
s += 1;
```

没有问题，答案是 2，因为 s += 1 是 语言支持的特性



以下这种写法有问题吗？

```java
float f = 3.4;
```

有问题，因为浮点型默认是 double 类型，所以相当于是 float = double，所以出现问题

正确写法应该是：

```java
float f = 3.4f;
```



同样的，跟上面的 short 一样存在类似的问腿：

```java
float f = 3.4f;
f += 1.1;	//没有问题，语言支持
f = f + 1;	//没有问题，float 存储的数据量 比 int 大
f = f + 1L;	//没有问题，float 存储的数据量 比 long 大
f = f + 1.1;//有问题，相当于是 float = float + double，最终结果是 double
f = f + 1.1f;//没有问题，相当于是 float  = float + float，最终结果是 float
```



这里说一个知识点：虽然 float 和 int 都是 4 个字节，即 32 位，但是 float 表示的方式跟 int 不一样，float 存储的是指数，即类似 x ^ y 这种的，是指数级增长的，因此 float 比 int 大，同时，虽然 long 是 8个字节的，但是 float 可存储的数据也比 long 大

简而言之，浮点数最小的 float 存储的数据量比任何一个 整型类型存储的数据量都大