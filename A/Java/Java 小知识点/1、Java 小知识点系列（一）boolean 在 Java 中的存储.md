# boolean 在 Java 中的存储



```java
public class A {
    public static void main(String[] args) throws Exception {
        boolean b = true;
        boolean c = false;
        boolean[] bs = new boolean[10];
    }
}
```

反编译获取 JVM 指令：

```java
  public static void main(java.lang.String[]) throws java.lang.Exception;
    descriptor: ([Ljava/lang/String;)V
    flags: ACC_PUBLIC, ACC_STATIC
    Code:
      stack=1, locals=4, args_size=1
         0: iconst_1
         1: istore_1
         2: iconst_0
         3: istore_2
         4: bipush        10
         6: newarray       boolean
         8: astore_3
         9: return
      LineNumberTable:
        line 20: 0
        line 21: 2
        line 22: 4
        line 23: 9
      LocalVariableTable:
        Start  Length  Slot  Name   Signature
            0      10     0  args   [Ljava/lang/String;
            2       8     1     b   Z
            4       6     2     c   Z
            9       1     3    bs   [Z
    Exceptions:
      throws java.lang.Exception

```

（测试环境： HotSpot 1.8 ）

可以看出 JVM 中没有专门用来处理 boolean 变量的 JVM 指令，使用的是 int 型变量的指令 iconst

```jav
0: iconst_1
1: istore_1
2: iconst_0
3: istore_2
```

对于 true 使用的是 iconst_1，即使用 int 型的 1 来代表 true

对于 false 使用的是 iconst_0，即使用 int 型的 0 来代表 true

即 boolean 变量占用为 4B



而对于 boolean 数组，使用的是  byte  数组的 指令 bastore 存储数据的

```java
4: bipush        10
6: newarray       boolean
8: astore_3
```

意味着对于 boolean 数组，每个元素都是 1B

