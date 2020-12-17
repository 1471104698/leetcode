# Throwable 系列

## 1、异常（Exception）和 错误（Error）

Exception 和 Error 都是 Throwable 的子类

Exception 讲究的是程序方面的错误，比如 空指针异常、数组越界异常、算术异常（比如 1 / 0）、SQL 运行异常、IO 异常 等

Error 讲究的是 JVM 层面的错误，比如 NotClassDefFoundClass、OOM

**只有 Exception 类型才能被 try-catch**



**非运行时异常：**IOException、SQLException、ClassNotFoundExecption，在编译的时候就强制要求进行 try-catch 或者 throws

**运行时异常：**即只有程序运行时才会发生的异常，一般是由于程序的逻辑产生的，比如 空指针异常、数组越界异常、算术异常



**当发生异常我们不进行 catch 处理，而是一层层往上抛，如果最终到达子线程的 run() 或者 主线程的 main() 还是没有处理，那么就会交给 JVM 进行处理，一旦 JVM 进行处理，那么就会终止线程**



## 2、NoClassDefFoundError 和 ClassNotFoundException 的区别

 [聊聊面试-NoClassDefFoundError 和 ClassNotFoundException 区别 - 博客园 (cnblogs.com)](https://www.cnblogs.com/xiao2shiqi/p/11740563.html) 



**ClassNotFoundException 是 Exception 类型**，它是一个异常，它可以被 try-catch，当 Class.forName() 时没有在堆中找到目标 class 对象，就会抛出该异常

**NoClassDefFoundError 是 Error 类型**，它是一个错误，跟 JVM 类加载相关，它表示在类加载的时候找不到加载的 class 文件，比如 A 的父类为 B，在 A 类加载的时候会先去加载 B，当我们在启动的时候删除了 B 的 class 文件，那么就会抛出该 Error



## 3、throw 和 throws

- throw： 在方法内部使用，可以在任何一个地方使用（当然是在 return 之前），抛出的是一个**异常对象**，new Exception()
- throws：声明在方法上，表示该方法产生 **非运行时异常**，没有 catch 而是抛出了异常，那么在方法上声明抛出的**异常类型**



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



异常的产生有两种：

- 1）由于代码问题产生的 运行时异常 RuntimeException
- 2）方法自己 throws 抛出的异常
  - 2.1）throws 抛出 运行时异常 RuntimeException
  - 2.2）throws 抛出 非运行时异常

我们调用 sleep()、wait() 时需要处理 InterruptedException 就是因为这几个方法内部都抛出了 这个 非运行时异常



当方法调用 throw 抛出运行时异常时，方法上不需要声明 throw 异常类型，因此调用该方法也不需要强制处理该异常

```java
public class A {
    public static void h (int i){
        if(i < 0){
            throw new ArrayIndexOutOfBoundsException("运行时异常：索引越界");
        }
        return;
    }
    public static void main(String[] args)  {
        //不需要强制处理异常
        h(-1);
    }
}
```



当方法调用 throw 抛出非运行时异常，方法上需要声明 throw 异常类型，并且调用该方法的方法需要强制处理异常：继续往上抛 或者 try-catch 

```java
public class A {
    public static void h (int i) throws InterruptedException {
        if(i < 0){
            throw new InterruptedException("中断异常");
        }
    }
    /*
    可以选择 继续往上 throw 或者 try-catch
    这里选择 throw，不过在 main() 这里往上抛就到达 JVM 层面了，一旦任何异常抛出到 JVM，那么主线程就会终止
    													（注意只是主线程，其他线程并不会受到影响）
    对于子线程 Thread 来说，run() 它的生命周期，如果 Thread 的异常抛出到 run()，那么线程终止
    */
    public static void main(String[] args) throws InterruptedException {
        h(-1);
        //由于在 h() 抛出了异常，而 main() 没有进行处理，所以主线程终止，不会执行下面的输出语句
        System.out.println("main");
    }
}
```



**没有捕获异常的方法在异常处是不会继续往后面执行的**，测试如下：

```java
public class A {
    
    public static void h (int i) throws InterruptedException {
        if(i < 0){
            throw new InterruptedException("中断异常");
        }
    }
    
    //调用 h()，这里不处理 h() 的异常，而是抛出异常
    public static void h1() throws InterruptedException {
        h(-1);
        System.out.println("h1");
    }

    public static void main(String[] args) {
        try {
            //调用 h1()，这里处理 h() -> h1() 抛出的异常
            h1();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        System.out.println("main");
    }
}
```

输出：

```java
java.lang.InterruptedException: 中断异常
	at cur.A.h(A.java:31)
	at cur.A.h1(A.java:35)
	at cur.A.main(A.java:41)
main
```

可以看出，由于 h1() 没有处理异常，所以不会继续往下执行，导致没有输出 "h1"

而 main() 捕获了异常，所以不会继续往上抛，所以会继续往下执行代码，输出 "main"



## 4、try-catch-finally、return 的执行时机

> ### finally 代码块 什么情况下 才不会执行

- 在 try 代码块前就进行 return
- 在 try 代码块中有 System.exit(0)，这个代码是退出 JVM 的，JVM 退出了，肯定就不能执行了

由此看来，**只要执行了 try，那么必定会执行 finally，即使 try 中抛出了异常，那么 finally 也会执行**



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



**4、try 有 return ，finally 没有 return, finally 修改 try return 的值**

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
            //发生异常
            int i = 1 / 0;
            return 1;
        }catch (Exception e){
            e.printStackTrace();
            //return 3;
        }finally {
			//return 2;
        }
        return 3;	//外层 return 优先级最低，如果 finally  有 return，外层不能有 return，否则编译出错
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

即如果 try 执行过程中发生了异常，那么就不会执行 try 的 return，这时 return 有几种情况了：

- finally 中没有 return
  - catch 中没有 return，那么就会执行 外面的 return
  - catch 中有 return，那么就会执行 catch 的 return
- finally中有 return
  - catch 中有 return，先执行 catch 的 return，然后再执行 finally 的 return，最终 finally 覆盖 catch 的 return 
  - catch 中没有 return，那么直接执行 finally 的 return,不会执行外面的 return



> #### 总结

return 的优先级：finally return > 【try / catch】return  > 外层 return 

**finally 是负责断后的**，当 finally 存在 return 时，那么最终返回的结果是 finally return 的值

当 try - catch 中存在 return 时，在执行 finally 的 return 前会先执行它们的 return，正常情况下执行 try，异常情况下执行 catch

而外层的 return 的出现情况：

- 如果 finally 存在 return，那么 外层 return 不能出现
- 如果同时存在 try-catch，那么如果 try-catch 都存在 return，那么 外层 return 不能出现，因为不论是正常还是异常，都不会执行到外层的 return；如果 try-catch 存在一个没有 return，那么 外层 return 可以出现，因为存在一种情况会执行到 外层return
- 如果只存在 try，不存在 catch，那么如果 try 存在 return，那么 外层 return 不能出现



