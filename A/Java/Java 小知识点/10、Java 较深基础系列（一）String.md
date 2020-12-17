# String

## 1、String 的不可变和不可继承

> #### 不可变

String 的大体架构如下：

```java
public final class String
    implements java.io.Serializable, Comparable<String>, CharSequence {
    
    private final char value[];

    private int hash; // Default to 0
}
```

String 的底层是使用 char[] 数组来实现的，它使用 final 修饰，保证了 value 不可再指向其他的对象，但是这不意味着 value 内的值不可变，只不过它没有对外提供接口去改变这个 char[] 数组的值

同时，它会提供了一个 hash 字段用来存储第一次计算的 hashCode 值



String 设计为不可变有三个优点：

- 值的不可变，防止出现意料之外的改变导致的错误

  ```java
  将 String 变量通过传参传给别的方法，不必担心它修改 String 内部的值导致自己的逻辑出现问题
  因此一般在非拼接字符串的情况下不使用 StringBuilder 和 StringBuffer
  ```

- 可以用作常量放入字符串常量池中进行复用

  ```java
  由于 String 不可变，所以在编译器期间对定义好的字符串字面量，将堆中的引用 存储到字符串常量池中进行复用
      避免多次创建 String 对象，节省内存，避免 GC
  ```

- 只需要计算一次 hashCode()

  ```java
  由于 value 值不可变，所以它的 hashCode 是固定的，因此只需要计算一次，将它存储到 hash 字段中，后续无需再进行计算
  ```



由于 String 不可变，所以字符串拼接都是使用会创建新的字符串对象

编译器会进行优化，底层使用 StringBuilder 进行拼接，提高效率



> #### 不可继承

String 跟 Integer 都是使用 final 修饰的，因此不可继承



个人认为设置为不可继承的目的：

```java
String 是一个核心类，非常常用，很多类都使用到了 String

如果用户自定义类继承了 String，重写了它的方法，但是在方法内部修改了原本的逻辑，违背了 String 原本的行为期望，导致程序出现错误
这同时也是违背了里氏替换原则。
设计者为了避免这个问题，直接设计为 不可继承
```





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

字节码指令中有一个 ldc 指令

ldc #5 表示将 常量池中的 #5 位置的数据 推送到 操作数栈顶，这时候是第一次调用，所以还是符号引用，并不是直接引用，因此这时候会进行解析，查看 字符串常量池 中是否存在这个字符串的引用，这时候是第一次，所以没有，那么会在堆中创建该字符串的 OOP 对象，然后将 引用 存储到 字符串常量池 中，再将引用返回，替换到 常量池 中 #5 的内容

后续再执行这个命令的时候可以直接根据引用找到堆中的对象

可以发现，字符串 "he" 和 "llo" 存在 ldc 指令，因此它们会在解析阶段在堆中创建对象然后将引用放到字符串常量池中，这样别的地方可以直接引用，无需创建



### 2、ldc 指令的各种情况

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
        String a = "a";
        String b = "b";
        String c = "xx" + "yy " + "e".length() + a + "zz" + "mm" + b;
    }
}
```



这种解除语法糖后就是

```java
String s = new StringBuilder()
    .append("xxyy")
    .append("e".length())
    .append("a")
    .append("zz").append("mm")
    .append("b")
    .toString();
```

因为 e 需要求 length()，所以它会单独成为一个字符串，而前面的是字面量的拼接，因此在编译器就可以确定

后面由于存在变量，所以 "zz" 和 "mm" 不会进行合并

```java
        35: ldc           #8                  // String a
        37: astore_3
        38: ldc           #9                  // String b
        40: astore        4
        42: new           #10                 // class java/lang/StringBuilder
        45: dup
        46: invokespecial #11                 // Method java/lang/StringBuilder."<init>":()V
        49: ldc           #12                 // String xxyy
        51: invokevirtual #13                 // Method java/lang/StringBuilder.append:(Ljava/lang/String;)Ljava/lang/StringBuilder;
        54: aload_3
        55: invokevirtual #13                 // Method java/lang/StringBuilder.append:(Ljava/lang/String;)Ljava/lang/StringBuilder;
        58: ldc           #14                 // String zz
        60: invokevirtual #13                 // Method java/lang/StringBuilder.append:(Ljava/lang/String;)Ljava/lang/StringBuilder;
        63: ldc           #15                 // String mm
        65: invokevirtual #13                 // Method java/lang/StringBuilder.append:(Ljava/lang/String;)Ljava/lang/StringBuilder;
        68: aload         4
        70: invokevirtual #13                 // Method java/lang/StringBuilder.append:(Ljava/lang/String;)Ljava/lang/StringBuilder;
        73: invokevirtual #16                 // Method java/lang/StringBuilder.toString:()Ljava/lang/String;

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

因为变量存在不确定性，在编译期间编译器无法确定最终生成的值，比如可能存在 scanner 的情况，因此编译器干脆就不对存在变量拼接的字符串进行 ldc

比如下面的这种情况：编译期间编译器并不知道 a 的值为多少，对于 c 的值也许可以通过上下文获取，不过为了避免多种情况判断，所以编译器干脆不管了

```java
public class A {
    public static void main(String[] args) throws InterruptedException {
        String a = new Scanner(System.in).next();
        String c = "c";
        String d = a + c;
    }
}
```





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



> #### new String("abc") 创建了多少个对象？



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

- 对 "abc" 执行 ldc，此时字符串常量池中没有，那么在堆中创建，然后返回该对象的引用
- 将 ldc 返回的对象作为方法参数传入 String 的构造方法

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

String 的底层是维护一个 char[] 数组 value

我们可以看出，String 的入参构造方法是将当前 String 对象的 value 指向 传入对象 的 value

这意味着什么？ 意味着 **s 中的 value 和 ldc 指令创建的 "abc" 底层使用的是同一个 char[] 数组**

但虽然它们底层的 char[] 数组是同一个，但是由于 s1 == s2 比较的是外层的 String 对象，由于 String OOP 对象不同，所以即使内部的 char[] 数组是同一个对象，也会返回 false

<img src="https://pic.leetcode-cn.com/1603788082-ICxpKm-image.png" style="zoom:40%;" />



综上，new String("abc") 应该是创建了 3 个对象：

1. ldc 指令创建的字符串字面量 "abc" 外层 String OOP 对象

2. 表示字面量 "abc" 的 char[] 数组对象 value

3. new String() 创建的外层 String OOP 对象



## 3、使用 String 作为锁对象

sync 锁 是通过 OOP 对象头的 Mark 字段中的 monitor 对象来记录锁信息的，因此它只认堆中的对象

对于 String，即使它们的值是相同的，但如果在堆中是不同的对象，那么最终锁的也是不同的对象

因此，使用 String 作为锁对象的时候，需要注意保证锁的是全局的对象

可以使用 intern() 方法来保证每次获取的都是常量池中的唯一对象