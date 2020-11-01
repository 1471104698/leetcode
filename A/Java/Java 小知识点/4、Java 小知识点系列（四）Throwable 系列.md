# Throwable 系列

## 1、异常（Exception）和 错误（Error）

Exception 和 Error 都是 Throwable 的子类

Exception 讲究的是程序方面的错误，比如 Null 异常、数组越界异常、除 0 异常、SQL 运行异常、IO 异常 等

Error 讲究的是 JVM 层面的错误，比如 NotClassDefFoundClass、OOM

**只有 Exception 类型才能被 try-catch**



**非运行时异常：**IOException、SQLException、ClassNotFoundExecption这种在编译的时候就强制要求进行 try-catch 或者 throws

**运行时异常：**即只有程序运行时才会发生的异常，一般是由于程序的逻辑产生的，比如 Null 异常、数组越界异常



**当发生异常我们不进行 catch 处理，而是一层层往上抛，如果最终到达子线程的 run() 或者 主线程的 main() 还是没有处理，那么就会交给 JVM 进行处理，一旦 JVM 进行处理，那么就会终止线程**



## 2、NotClassDefFoundClass 和 ClassNotFoundClass 的区别

具体看  https://www.cnblogs.com/xiao2shiqi/p/11740563.html 



**ClassNotFoundClass 是 Exception 类型**，它可以被 tey-catch，产生的原因是比如 Class.forName() 时没有找到对应的 class 对象

**NotClassDefFoundClass 是 Error 类型**，它表示编译时存在对应的 Class 文件，但是在类加载的时候就没有找到了，比如我们在启动的时候删除了某个类的 class 文件，那么就会抛出 NotClassDefFoundClass 



## 3、throw 和 throws

throw 是在方法内部的，表示抛出一个异常，可以位于方法的任意一个位置

throws 是位于方法声明上的，对于一些非运行时异常，如果没有进行 try-catch，那么就必须进行 throws



throw 后面跟的是异常的实例对象，throws 后面跟的是异常的类型

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
public void h throw ArrayIndexOutOfBoundsException(int[] arr, int i){
    if(i < 0){
        throw new ArrayIndexOutOfBoundsException("索引越界");
    }
    //do something
}
```



使用 throw 可以我们自己受控的在指定位置指定情况下抛出某个指定的异常，并且指定异常的错误信息

需要注意的是，**throw 后同时还需要在 方法上添加 throws，即使用了 throw 同时也必须使用 throws**



## 4、try-catch-finally、return 的执行时机

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



