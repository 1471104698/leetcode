# String



## 1、new String() 创建了多少个对象

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





## 2、String 为什么设计为不可变和不可继承

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





## 3、关于 String 锁（intern()）



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