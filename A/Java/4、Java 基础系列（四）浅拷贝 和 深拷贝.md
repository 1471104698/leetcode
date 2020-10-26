# 浅拷贝 和 深拷贝



## 1、何为浅拷贝，何为深拷贝？

浅拷贝：开辟新的内存创建新的 OOP 对象，但是 OOP 对象中的变量内容全部跟 原来的对象一样，如果是变量是引用对象，那么相当于是拷贝一份指针

深拷贝：内部的变量全部重新创建，而不是拷贝引用指针



## 2、浅拷贝实现 - clone()

clone() 是 Object 内部的 native 方法，所以实现不是 java 语言实现的（后续会根据个人的猜想如何实现的）

因为 Object 是所有的类的父类（包括 Class 类），因此它们都存在 clone() 方法

**使用 clone() 的前提条件：必须实现 Cloneable 接口**



```java
class F {
    //年龄
    public int age;
}

public class A implements Cloneable {
    F f;
    StringBuilder sb;
    int i;
    public static void main(String[] args) throws Exception {
        A a = new A();
        a.f = new User();
        a.f.age = 1;
        a.i = 3;
        a.sb = new StringBuilder();
        A a1 = (A) a.clone();
        h(a, a1);
    }

    private static void h(A a, A a1){
        System.out.println(a == a1);    //false，这里可以看出确实创建了一个新的 OOP 对象
        System.out.println(a.f == a1.f);//true，这里可以看出新的 OOP 对象内部的引用变量和原对象是同一个
        
        a1.f.age = 2;
        System.out.println(a.f.age);    //2
        System.out.println(a1.f.age);   //2

        a1.sb.append("toString");
        System.out.println(a.sb);    //toString
        System.out.println(a1.sb);   //toString

        a.i = 2;
        a1.i = 4;
        System.out.println(a.i);    //2
        System.out.println(a1.i);   //4
    }

    /*
    这里重写的 clone() 方法没有任何作用，因为内部实际上调用的 super.clone() 还是调用的 Object 的 clone() 
    我们自身并没有做什么别的操作
    */
    @Override
    protected Object clone(){
        A a = null;
        try {
            a = (A) super.clone();
        } catch (CloneNotSupportedException e) {
            e.printStackTrace();
        }
        return a;
    }
}
```



我们可以看出 Object 自己的普通的 clone() 方法只是创建一个外层的 OOP 对象，但是对于内层的变量数据全都是简单的拷贝一份，比较基本数据类型就是直接拷贝数据，对于引用类型就是直接拷贝引用指针，即引用类型是直接原对象 和 拷贝对象共用的

我们可以推测 Object 的 clone() 实现是，**创建一个外层的一个 OOP 对象 a1，然后将 原对象 a 的 OOP 对象体 整个复制 到 克隆的 OOP 对象 a1 的 对象体中**

这实际上就是类似于以下代码：

```java
public A clone(){
	A a1 = new A();
    a1.i = this.i;
    a1.f = this.f;
    a1.sb = this.sb;
}
```



如果要使用 clone() 完成深拷贝，可以在完整的重写 clone() 方法，然后对返回的 a1 对象内部的 所有引用变量 重新赋值，这涉及大概到递归的过程了，因为 A 类复合 B 类，B 类可能复合 C 类，C 类可能复合 D 类

**因此一般使用 clone() 是用来实现浅拷贝的，深拷贝实现使用 序列化 和 反序列化**



## 2、深拷贝实现 - 序列化 和 反序列化

序列化 和 反序列化 创建出来的对象是一个完整的全新对象，

毕竟你想想，序列化文件 从本机 远程传输到别的主机，怎么可能别的主机 创建出来的对象 内部的引用指针还能够指向本机的内存对象，因此创建出来的对象内部的变量都是新的



**序列化 和 反序列化的前提：实现 Serializable 接口**

```java
class F {
    //年龄
    public int age;
}

public class A implements Serializable {
    F f;
    StringBuilder sb;
    int i;
    public static void main(String[] args) throws Exception {
        A a = new A();
        a.f = new User();
        a.f.age = 1;
        a.i = 3;
        a.sb = new StringBuilder();
        
        //序列化
        String path = "F:\\temp.txt";
        FileOutputStream fos = new FileOutputStream(path);
        ObjectOutputStream oos = new ObjectOutputStream(fos);
        oos.writeObject(a);
		//反序列化
        FileInputStream fis = new FileInputStream(path);
        ObjectInputStream ois = new ObjectInputStream(fis);
        //创建新对象
        A a1 = (A) ois.readObject();
        
        h(a, a1);
    }

    private static void h(A a, A a1){
        System.out.println(a == a1);    //false，这里可以看出确实创建了一个新的 OOP 对象
        System.out.println(a.f == a1.f);//false，这里可以看出新的 OOP 对象内部的引用变量和原对象 不是 同一个
        
        a1.f.age = 2;
        System.out.println(a.f.age);    //1
        System.out.println(a1.f.age);   //2

        a1.sb.append("toString");
        System.out.println(a.sb);    //""
        System.out.println(a1.sb);   //toString

        a.i = 2;
        a1.i = 4;
        System.out.println(a.i);    //2
        System.out.println(a1.i);   //4
    }
}
```

