# 接口 和 抽象类



学习 接口 和 抽象类 的时候，有以下几个问题：

1、接口和抽象类的区别

2、接口能否替代抽象类？抽象类能否替代接口？

3、JDK 8 后 接口出现了 static、default 可以实现方法，这时候接口是否能够代替抽象类？



## 1、最基本的区别

类的继承和实现：

- 抽象类和普通的类一样，是单继承、多实现

- 接口可以继承多个接口，比如：

```java
interface F extends Runnable, Callable {
    int h();
}
```

状态：

- 接口 是无状态的，即在内部所有的变量都是 public static final 常量，所有的方法都是 public，不存在除 public 外的其他访问修饰符
- 抽象类跟普通的类一样存在状态，唯一的区别就是抽象类存在抽象方法，需要留给子类实现，所以抽象方法不能是 private 、final 、static 修饰

实例化：

- 接口和抽象类都不能直接实例化，只能使用匿名对象

方法实现：

- 抽象类的子类可以不实现它的抽象方法，如果存在抽象方法没有实现，那么该子类也是一个隐式的抽象类，无法直接实例化，只能创建匿名对象。
- 接口方法子类必须实现。



抽象类不能用来代替接口，接口也不能用来代替抽象类

抽象类一般是用来作为模板类，将共有的方法抽象出来并实现好逻辑，比如 AQS，并且对于特定的逻辑留给子类实现

接口则不存在需要提前实现的逻辑，只需要定义好共有的方法名，方便多态调用，并且接口可以多实现，还可以再继承一个类



## 2、JDK8 接口的演变

但是，在 JDK8 中，接口出现了两个方法修饰符：static 和 default

这两个修饰符使得接口可以存在自己的实现逻辑，而不再只是单单的定义方法名

```java
interface F extends Runnable, Callable {
    int h();
    static int h1(){
        System.out.println("static h1");
        return 2;
    }
    default int h2(){
        System.out.println("default h2");
        return 1;
    }
}
```

同样的， static 和 default 修饰的方法也是默认 public 访问修饰符



static 如字面意思，是一个静态方法，可以使用 F.h1() 进行调用

default 是用来描述该方法在接口中需要存在实现的方法体，跟普通类的方法一样，可以被重写、重载、继承等



**那么这意味着 JDK8 的接口就可以代替 抽象类了呢？**

并不能，因为抽象类同样有一个很关键的点：抽象类存在状态

接口是没有状态的，它的所有变量都是常量，无法在一般情况下防止外界对这些变量的访问限制



**那么 JDK8 增加这个特性有什么作用呢？**

作用还是有的，在不需要状态的情况下，接口可以跟抽象类一样定义一些公用的实现，比如 List 接口内部就定义了一个 sort() 实现

```java
public interface List<E> extends Collection<E> {
    //xxx
    
    @SuppressWarnings({"unchecked", "rawtypes"})
    default void sort(Comparator<? super E> c) {	//比较器可以为 null
        //将 List 数组转换为 数组
        Object[] a = this.toArray();
        //调用的是 Arrays.sort()
        Arrays.sort(a, (Comparator) c);
        ListIterator<E> i = this.listIterator();
        //将排完序的数组设置回 list 中
        for (Object e : a) {
            i.next();
            i.set((E) e);
        }
    }
    
    //xxxx
}
```

这个方法的实现同样的也被用到了 Collections.sort() 中

```java
public class Collections {
    public static <T extends Comparable<? super T>> void sort(List<T> list) {
        //调用上面的 sort()
        list.sort(null);
    }
}
```



因此，对于 list 排序，调用 Collections.sort(list) 和 list.sort(null) 是等价的 