# String

## 1、String 的不可变和不可继承

### 1、不可变

> ### 什么是不可变？

所谓的不可变，就是一个类的实例创建完成后，不能再修改它的成员变量值



> ### String 为什么不可变

String 类源码如下：

```java
public final class String implements java.io.Serializable, Comparable<String>, CharSequence {
    private final char value[];
    
    //do something
}
```

String 底层实现是一个 char[] 数组，跟 C 和 SDS 的字符串实现一样

这个 char[] 数组使用 final 修饰，这样就导致了不能将 char[] 数组 引用变量 重新指向新的引用

但是需要注意的是，final 修饰的 char[] 只是引用不可变，但是内部数据是可变的

而如果提供修改 char[] 数组内部数据的 api，那么就跟 StringBuilder 一样了，就失去了 String 的不可变这一特点



> ### String 为什么设计为 不可变

有两个优点：

- 安全

- 可以用作常量放入字符串常量池中进行复用



**安全：**

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



**复用：**

String 借助这个不可变性还有一个常量池属性，即它可以添加到常量池中，被多个引用进行复用，而不用创建重复的对象，节省空间 

如果可变，那么这个就失去了意义了，因为这个值随时可能被改变，这样就会影响到其他的引用



### 2、不可继承



> ### String 为什么不可继承

```java
//使用 final 修饰
public final class String implements java.io.Serializable, Comparable<String>, CharSequence {
    //do something
}
```

String 类使用 final 修饰，表示该类不可被继承



> ### String 为什么设计为不可继承

设置为不可继承，那么方法就不能被重写，这样才通用，**String 是一个基础类，很多的 JDK 定义的基础类都使用了 String**

如果用户的一个类继承 String 重写了 String 的方法，那么在其他类中本来是传入 String，使用的是 String 的方法逻辑的，却传入了用户自己写的类，导致对应的方法逻辑出现问题





## 2、String 的 intern()

[字符串常量池](https://www.zhihu.com/question/55994121 )



intern() 涉及到 字符串常量池，这里根据字节码来说明下字符串常量池的存储



### 1、字符串常量池

**JDK 6 以前字符串常量池存储在在方法区中，而 JDK 7 则放到了堆中**

JDK 7 的字符串常量池中只存储了对堆中字符串的引用，不会存储任何的对象



存在以下代码：

```java
class NewTest2 {
    public static void main(String[] args) {
        String s1 = new String("he") + new String("llo");
    }
}
```



通过 javap -v 获取字节码指令

```java
  public static void main(java.lang.String[]);
    descriptor: ([Ljava/lang/String;)V
    flags: ACC_PUBLIC, ACC_STATIC
    Code:
      stack=4, locals=2, args_size=1
         0: new           #2                  // class java/lang/StringBuilder
         3: dup
         4: invokespecial #3                  // Method java/lang/StringBuilder."<init>":()V
         7: new           #4                  // class java/lang/String
        10: dup
        11: ldc           #5                  // String he
        13: invokespecial #6                  // Method java/lang/String."<init>":(Ljava/lang/String;)V
        16: invokevirtual #7                  // Method java/lang/StringBuilder.append:(Ljava/lang/String;)Ljava/lang/StringBuilder;
        19: new           #4                  // class java/lang/String
        22: dup
        23: ldc           #8                  // String llo
        25: invokespecial #6                  // Method java/lang/String."<init>":(Ljava/lang/String;)V
        28: invokevirtual #7                  // Method java/lang/StringBuilder.append:(Ljava/lang/String;)Ljava/lang/StringBuilder;
        31: invokevirtual #9                  // Method java/lang/StringBuilder.toString:()Ljava/lang/String;
        34: astore_1
        35: return

```

可以看出字节码中涉及一个 ldc 指令，它操作的是非静态变量，如果是静态变量，那么对应的指令应该是 putstatic/getstatic

ldc #5 表示将 常量池中的 #5 位置的数据 推送到 操作数栈顶，这时候是第一次调用，所以还是符号引用，并不是直接引用，因此这时候会进行解析，查看 字符串常量池 中是否存在这个字符串的引用，这时候是第一次，所以没有，那么会在堆中创建该字符串的 OOP 对象，然后将 引用 存储到 字符串常量池 中，再将引用返回，替换到 常量池 中 #5 的内容

后续再执行这个命令的时候可以直接根据引用找到堆中的对象

可以发现，字符串 "he" 和 "llo" 存在 ldc 指令，因此它们会在解析阶段在堆中创建对象然后将引用放到字符串常量池中，这样别的地方可以直接引用，无需创建



### 2、创建对象的情况

**情况一：**

```java
class A {
    public static void main(String[] args) {
        String s1 = new String("he") + new String("llo");
    }
}
```

就是上面的例子了，"he" 和 "llo" 会在堆中创建对象，然后将引用存储到字符串常量池中



**情况二：**

```java
class A {
    public static void main(String[] args) {
        String s = "a" + new String("b");
    }
}
```

通过 javap -v 获取字节码指令（省略了部分字节码指令）

可以发现，"a" 和 "b" 都存在 ldc 指令，即会在堆中创建对象，然后将引用存储到字符串常量池中被重用

```java
  public static void main(java.lang.String[]);
    descriptor: ([Ljava/lang/String;)V
    flags: ACC_PUBLIC, ACC_STATIC
    Code:
      stack=4, locals=2, args_size=1
         7: ldc           #4                  // String a
         9: invokevirtual #5                  // Method java/lang/StringBuilder.append:(Ljava/lang/String;)Ljava/lang/StringBuilder;
        16: ldc           #7                  // String b
        18: invokespecial #8                  // Method java/lang/String."<init>":(Ljava/lang/String;)V
        21: invokevirtual #5                  // Method java/lang/StringBuilder.append:(Ljava/lang/String;)Ljava/lang/StringBuilder;
        24: invokevirtual #9                  // Method java/lang/StringBuilder.toString:()Ljava/lang/String;
        27: astore_1
        28: return

```



**情况三：**

```java
class A {
    public static void main(String[] args) {
        String s = "a" + "b" + "c" + "d" + "e";
    }
}
```

通过 javap -v 获取字节码指令（省略了部分字节码指令）

可以发现，只有 "abcde" 存在 ldc 指令，而 "a"、"b" 等 没有对应的 ldc 指令

即如果是直接使用字面量拼接，那么只有拼接后的字符串才会在堆中创建，用来拼接的字面量则不会

只有这种完全字面量的拼接不会使用 StringBuilder 进行拼接

```java
  public static void main(java.lang.String[]);
    descriptor: ([Ljava/lang/String;)V
    flags: ACC_PUBLIC, ACC_STATIC
    Code:
      stack=1, locals=2, args_size=1
         0: ldc           #2                  // String abcde
         2: astore_1
         3: return
```



**情况四：**

```java
public class A {
    public static void main(String[] args) {
        String s = "a" + "b" + "c" + "d" + "e".length();
    }
}
```



这种解除语法糖后就是

```java
String s = new StringBuilder().append("abcd").append("e".length()).toString();
```

因为 e 需要求 length()，所以它会单独成为一个字符串，而前面的是字面量的拼接，因此在编译器就可以确定

因此最终 "abcde" 和 "e" 存在 ldc 指令，并且还需要使用 StringBuilder 进行拼接

```java
    Code:
      stack=2, locals=2, args_size=1
         0: new           #2                  // class java/lang/StringBuilder
         3: dup
         4: invokespecial #3                  // Method java/lang/StringBuilder."<init>":()V
         7: ldc           #4                  // String abcd
         9: invokevirtual #5                  // Method java/lang/StringBuilder.append:(Ljava/lang/String;)Ljava/lang/StringBuilder;
        12: ldc           #6                  // String e
        14: invokevirtual #7                  // Method java/lang/String.length:()I
        17: invokevirtual #8                  // Method java/lang/StringBuilder.append:(I)Ljava/lang/StringBuilder;
        20: invokevirtual #9                  // Method java/lang/StringBuilder.toString:()Ljava/lang/String;
        23: astore_1
        24: return

```



**情况五：**

```java
public class A {
    public static void main(String[] args) {
        String a = "a";
        String b = "b";
        String c = a + b;
        String d = "a" + b;
    }
}
```

通过 javap -v 获取字节码指令（省略了部分字节码指令）

可以发现，"a" 和 "b" 都存在 ldc 指令，而 拼接的 c 和 d 没有对应的 ldc 指令

即如果拼接的过程中 存在变量，那么由于存在不确定性，所以不会在堆中创建对象并且将引用存储到字符串常量池中

```java
    Code:
      stack=2, locals=5, args_size=1
         0: ldc           #2                  // String a
         2: astore_1
         3: ldc           #3                  // String b
        37: aload_2
        38: invokevirtual #6                  // Method java/lang/StringBuilder.append:(Ljava/lang/String;)Ljava/lang/StringBuilder;
        41: invokevirtual #7                  // Method java/lang/StringBuilder.toString:()Ljava/lang/String;
        44: astore        4
        46: return

```

因为变量的值是可变的，并且是不可估计的，编译器无法确定变量的值，比如

```java
public class A {
    public static void main(String[] args) throws InterruptedException {
        String a = new Scanner(System.in).next();
        String c = null;
        if(a.equals("1")){
            c = "2";
        }else{
            c = "3";
        }
        String d = a + c;
    }
}
```

这种情况下编译器怎么知道 a 和 c 的值是多少。。。所以是无法进行拼接的



**情况六：**

```java
public class A {
    public static void main(String[] args) throws InterruptedException {
        final String a = "a";
        final String b = "b";
        String c = a + b;
    }
}
```

可以看出 a 和 b 和 拼接的 c 都存在 ldc 指令，这是因为 a 和 b 是 final 变量，并且已经是确定的了，那么意味着不会发生改变，这样的话 c 也是确定的了，所以可以在堆中创建并将引用存储到 字符串常量池 中

类似完全字面量，不需要 StringBuilder 拼接

```java
    Code:
      stack=1, locals=4, args_size=1
         0: ldc           #2                  // String a
         2: astore_1
         3: ldc           #3                  // String b
         5: astore_2
         6: ldc           #4                  // String ab
         8: astore_3
         9: return

```



**情况七：**

```java
public class A {
    public static void main(String[] args) {
        final String a = new Scanner(System.in).next();
        final String b = "b";
        String c = a + b;
    }
}
```

可以看出 只有 b 有 ldc，因为 a 需要等待用户输入，final 只能保证不可变，但是如果值不确定还是不行，所以 a 和 c 都没有 ldc

```java
    Code:
      stack=3, locals=4, args_size=1
         0: new           #2                  // class java/util/Scanner
         3: dup
         4: getstatic     #3                  // Field java/lang/System.in:Ljava/io/InputStream;
         7: invokespecial #4                  // Method java/util/Scanner."<init>":(Ljava/io/InputStream;)V
        10: invokevirtual #5                  // Method java/util/Scanner.next:()Ljava/lang/String;
        13: astore_1
        14: ldc           #6                  // String b
        16: astore_2
        17: new           #7                  // class java/lang/StringBuilder
        20: dup
        21: invokespecial #8                  // Method java/lang/StringBuilder."<init>":()V
        24: aload_1
        25: invokevirtual #9                  // Method java/lang/StringBuilder.append:(Ljava/lang/String;)Ljava/lang/StringBuilder;
        28: ldc           #6                  // String b
        30: invokevirtual #9                  // Method java/lang/StringBuilder.append:(Ljava/lang/String;)Ljava/lang/StringBuilder;
        33: invokevirtual #10                 // Method java/lang/StringBuilder.toString:()Ljava/lang/String;
        36: astore_3
        37: return

```





### 3、intern()

```java
class NewTest2 {
    public static void main(String[] args) {
        String s1 = new String("he") + new String("llo");
        String s2 = new String("h") + new String("ello");
        String s3 = s1.intern();
        String s4 = s2.intern();
        System.out.println(s1 == s3); 
        System.out.println(s1 == s4);
        System.out.println(s1 == s2);
    }
}
```

输出结果：

```
true
true
false
```

上面会存在 ldc 指令的有 "he"、"llo"、"h"、"ello"

s1 和 s2 都是 "hello"

执行 s1.intern() ，会去字符串常量池中查找是否存在 "hello" 的引用，如果存在，那么直接返回，这里是不存在的，因此会将 s1 在堆中的引用存储到字符串常量池中

执行 s2.intern()，同样会去查找字符串常量池，发现已经存在了，因此直接将引用返回

这意味着 s3 和 s4 持有的引用都是指向的 s1，因此 s1 == s3 == s4 != s2



> ### 关于 new String("abc") 个人推测

首先，上面已经说了 intern() 会先去字符串常量池中查找是否存在该字符串，如果不存在那么将 调用该方法的字符串的在堆中的引用存储到 字符串常量池中，如果存在则直接返回

如果是在代码中显示的表示字面量，比如 `String s = "abc"`，那么会自动对 "abc" 执行一条 ldc 指令，如果是第一次出现 abc，那么这时候会自动在 堆中 创建 值为"abc" 的 String 的 OOP 对象体，然后将引用返回，存储到字符串从常量池中

这样的话，对于如下代码：

```java
String s = new String("abc");
```

经过 javap -v 得到 JVM 指令如下：

```java
 public static void main(java.lang.String[]) 
    descriptor: ([Ljava/lang/String;)V
    flags: ACC_PUBLIC, ACC_STATIC
    Code:
      stack=3, locals=2, args_size=1
         0: new           #2                  // class java/lang/String
         3: dup
         4: ldc           #3                  // String abc
         6: invokespecial #4                  // Method java/lang/String."<init>":(Ljava/lang/String;)V
         9: astore_1
        10: return

```

- 先执行 new 指令，在堆中创建一个 OOP 对象体

- 对 "abc" 执行 ldc，此时字符串常量池中没有，那么在堆中创建，然后返回引用
- 调用 String 的构造方法

重点就是这个构造方法

```java
public final class String{
    private final char value[];
    private int hash; // Default to 0
    
    //构造方法
    public String(String original) {
        //当前 String 对象 和 original 对象的 char[] 数组指向同一个
        this.value = original.value;
        this.hash = original.hash;
    }
}
```

String 的底层是维护一个 char[] 数组

我们可以看出，String 的这个入参构造方法是将传入的 String 的 char[] 数组赋值给 当前 String 的 char[] 数组

而 new String("abc") 应该是从 字符串常量池中获取 "abc" 字符串的引用，这个引用所引用的对象是调用了 ldc 指令并在堆中创建了的 OOP 对象

这意味着什么？ 

意味着 String s 中的 char[] 数组 和 ldc 指令创建的 "abc" 字符串底层使用的是同一个 char[] 数组

<img src="https://pic.leetcode-cn.com/1603788082-ICxpKm-image.png" style="zoom:70%;" />

虽然它们底层的 char[] 数组是同一个，但是由于 s1 == s2 比较的是外层的 String 对象，由于 String OOP 对象是不同的，所以即使内部的 char[] 数组是同一个对象，也会返回 false



## 3、使用 String 作为锁对象

sync 锁 是通过 OOP 对象头的 Mark 字段中的 monitor 对象来记录锁信息的，因此它只认堆中的对象

对于 String，即使它们的值是相同的，但如果在堆中是不同的对象，那么最终锁的也是不同的对象

因此，使用 String 作为锁对象的时候，需要注意保证锁的是全局的对象

可以使用 intern() 方法来保证每次获取的都是常量池中的唯一对象