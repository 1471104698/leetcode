# 反射



## 1、反射的概念

反射就是获取类的 Class 对象，然后通过 Class 对象去访问方法区中的 Klass 对象

Class 对象是用于反射的工具，是通过这个工具去访问 Klass 对象获取数据的方法叫做反射

Klass 对象中包含了某个类的所有元数据信息



> ### 为什么需要反射？

对于 new 这种的操作，是我们在运行前就知道需要什么对象了，所以能够写死直接 new 出来

但是有时候我们只有在运行的过程中才知道需要什么对象，那么就是方法参数传入的 全类名字符串来创建对象，比如 aop 就是在运行过程中传入的，因为 spring 这个框架在写出来的时候并不知道用户实际需要什么对象，因此不能直接 new，只能靠用户传入需要进行代理的 全类名，进行反射出对象，然后进行对该对象的代理



## 2、反射 api

主要 API：

- 成员变量：Field
- 成员方法：Method
- 构造方法：Constructor

获取 Class 对象的方式：

- 类`.class`
- `Class.forName()`
- 实例对象`.getClass()`

 getFields、getMethods 和 getConstructors 获取该类的 public 变量 和 方法，包括父类的 public 变量 和 方法，包括 static 

 getDeclaredFields、 getDeclaredMethods 和 getDeclaredConstructors 可以获取该类的所有 变量 和 方法，但是无法获取父类的变量和方法



## 3、反射 api 执行流程

> ### clazz.newInstace() 执行流程

clazz.newInstance() 就是在堆中按照 Klass 创建一个 OOP 对象

当调用 clazz.newInstance() 后，执行流程为：

- 通过 Class 对象中的元数据指针找到方法区中的 Klass
- 通过 Klass 模板在堆中创建 OOP 对象
- 将 OOP 对象的引用返回，即 Object o = clazz.newInstance()



> ### Field a = clazz.getDeclaredField("a") 执行流程

clazz.getDeclaredField("a") 获取某个变量的 Filed 实例

在 Klazz 中，方法 被封装成 Method，变量被封装成 Field

而 Field 中存储了变量名、变量类型、变量数据在 OOP 对象的偏移量

因此就可以直接使用 Field 获取以上的数据，同时可以通过修改 OOP 对象中的变量数据



## 4、反射修改 final 和 private 修饰的值



```java
public class Reflect {
    private final int a = 1;
    private int getA(){
        return a;
    }
    private Reflect(){}
}
class C{
    public static void main(String[] args) throws ClassNotFoundException, IllegalAccessException, InvocationTargetException, InstantiationException, NoSuchFieldException, NoSuchMethodException {
        Class<?> clazz = Class.forName("cur.Reflect");
        Constructor<?> constructor = clazz.getDeclaredConstructor();
        constructor.setAccessible(true);
        Object o = constructor.newInstance();

        Field a = clazz.getDeclaredField("a");
        a.setAccessible(true);
        a.set(o, 2);
		
        Method getA = clazz.getDeclaredMethod("getA");
        getA.setAccessible(true);
        
        //通过 getA() 方法获取 a 值
        System.out.println(getA.invoke(o, null));   // 1
        //直接获取 a 值
        System.out.println(a.get(o));   // 2
    }
}
```

上面我们可以发现，直接 变量 a 的值 和 通过 getA() 方法获取变量 a 的值，结果不一样

直接获取的是 修改后的值，通过方法获取的是修改前的值

这是因为 a 变量使用 final 修饰，在编译的时候编译器将它当作常量，直接在 getA() 方法的指令里返回 1，即 getA() 在指令中是写死返回 1 的



## 5、反射获取方法参数类型、参数名



```java
public class Reflect {
    public void h(int a, int b, int c){
        System.out.println("nothing");
    }
}
class C{
    public static void main(String[] args) throws Exception {
        Class<?> clazz = Class.forName("cur.Reflect");
        Constructor<?> constructor = clazz.getDeclaredConstructor();
        constructor.setAccessible(true);
        Object o = constructor.newInstance();

        Method[] methods = clazz.getDeclaredMethods();
        for(Method method : methods){
            System.out.println("方法名：" + method.getName());
            System.out.println("返回类型：" + method.getReturnType());
            System.out.println("方法修饰符的标识符：" + method.getModifiers());
            for (Class<?> pType  : method.getParameterTypes()) {
                System.out.println("方法参数类型：" + pType.getName());
            }
            for (Parameter parameter : method.getParameters()) {
                System.out.println("方法参数名称：" + parameter.getName());
            }
        }
    }
}
```

输出结果为：

```java
方法名：h
返回类型：void
方法修饰符的标识符：1
方法参数类型：int
方法参数类型：int
方法参数类型：int
方法参数名称：arg0
方法参数名称：arg1
方法参数名称：arg2
```

可以看出，反射可以获取到方法名、方法类型、方法修饰符（1 表示 public）、方法参数类型

但是对应的具体参数名称，却是反射过程中自动生成的 `argx`，没有真正获取到参数名称



在 JDK 8 中，增加了 Parameter 类，可以获取 Method 的所有参数，其他参数可以正常获取，但是 参数名称需要在编译时做处理，添加 -parameters 参数即可

![1599538401846](C:\Users\蒜头王八\AppData\Roaming\Typora\typora-user-images\1599538401846.png)

添加完参数后，输出结果：

```java
方法名：h
返回类型：void
方法修饰符的标识符：1
方法参数类型：int
方法参数类型：int
方法参数类型：int
方法参数名称：a
方法参数名称：b
方法参数名称：c
```

