# JVM 知识点

## 1、类编译 到 类加载 过程 及 产物

> 类编译

当我们写完代码后，生成的是一个 .java 的静态文件，这个文件在磁盘中



> 类加载

当我们点击运行 .java 文件的时候，它会先开始编译，将代码转变成二进制数据，变成 .class 文件（字节码文件）

然后将 .class 文件中的二进制数据加载进内存中，创建出一个 `java.lang.Class` 对象，类加载的最终产物就是 JVM 中的 Class 对象



### 3.1、什么是 Class 类（类加载的最终产物）

> `java.lang.Class` 类 和 我们自己写的类的关系

除了 int 等基本数据类型之外**（基本数据类型不是类）**，**Java 的其他类型（包括 interface）都是 Class 类的实例**

其他类（接口）是 Class 类的实例？ 那么 Class 类是谁写的？

Class 类是 `jvm` 动态加载进内存的，它是 private 类型的，用户无法自己创建

```java
public final class Class {
    private Class() {}
}
```



Class 类里包含了 某个其他类的所有方法变量信息，当 JVM 加载一种新的其他类 class 的时候，比如 String 类，那么就会**获取 String 的信息，为这个 String 类创建一个 Class 类实例，然后关联起来**

```java
Class cls = new Class(String);
```



也就是说，**除了 Class 外的其他所有类 class，每个类 class 对应一个 Class 类实例，在 JVM 中的类都是 Class 实例**

```ascii
┌───────────────────────────┐
│      Class Instance       │──────> String
├───────────────────────────┤
│name = "java.lang.String"  │
└───────────────────────────┘
┌───────────────────────────┐
│      Class Instance       │──────> Random
├───────────────────────────┤
│name = "java.util.Random"  │
└───────────────────────────┘
┌───────────────────────────┐
│      Class Instance       │──────> Runnable
├───────────────────────────┤
│name = "java.lang.Runnable"│
└───────────────────────────┘
```



**注意：是一个 class 类对应一个 Class 实例，而不是一个 class 对象对应一个 Class 实例，即相当于我们平常的内存中的 Cat 类，是一个 Class 对象，然后使用这个 Class 对象创建出更多的 Cat 实例对象**





> 反射的概念



换言之，所有 class 类的信息都存储在对应的 Class 类实例中，那么我们只需要获取对应的 Class 类实例，就意味着获取了这个 class 类的所有信息，**通过`Class`实例获取`class`信息的方法，就是反射**



### 3.2、双亲委派模型（类加载的操作者）



我们需要知道，**JVM 只有在 两个类 的 类名（包名 + class 类名）相同 并且 由同一个类加载器加载的时候，才算作是同一个类**



> 什么是双亲委派模型？



当某个类加载器要加载类的时候，会将这个类一层层传递给父类加载器加载，如果父类加载器有这个类，加载成功，那么返回成功，否则就自己加载，这就是双亲委派模型



> 双亲委派模型的好处



最开始也说了，JVM 认定类的原理，如果我们不使用双亲委派模型，自己写了一个 `java.lang.Object` 类，那么将会由 `AppClassLoader` 去加载

由于官方的 Object 类 和 我们自定义的 Object 类的类加载器不同

那么内存中就存在两个 Object 类，那么当我们使用 Object 类的时候，导入的` java.lang.Obejct`，也不知道是哪一个类

编译不会出错，但是运行会出错

```java
package java.lang;
//自定义 java.lang.Object 类
public class Object {
    public static void main(String[] args) {
        Object object = new Object();//编译不报错，运行报错
    }
}

```

  