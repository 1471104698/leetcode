# 序列化 和 反序列化



在此之前，先讲下创建 OOP 对象的四种方式：

- new
- 反射
- clone
- 反序列化



## 1、序列化的作用

序列化 是为了解决 **网络传输对象**  的问题，我们跨平台存储和网络传输都是使用的 IO，而 IO 支持的数据格式就是字节数组

但是单纯的将对象转换为字节数组还不行，因为没有一定的规律，无法通过字节数组再还原回原来的对象



序列化和反序列化 就是 指定某种规则，**将对象按照规则转换为字节数组（序列化），然后再按照规则将字节数组转换为对象（反序列化）**



## 2、Java 实现 序列化 和 反序列化

需要序列化的话必须实现 **Serializable** 接口

```java
public class User implements Serializable{
 //年龄
 public int age;
 //名字
 public String name ;
}
```



将 User 对象写入到指定的文件中

```java
//准备好序列化用的 IO 类
FileOutputStream fos = new FileOutputStream("D:\\temp.txt");
ObjectOutputStream oos = new ObjectOutputStream(fos);

User user = new User();
user.age = 10;
user.name = "oy";
oos.writeObject(user);

oos.flush();
oos.close();
```



将 User 对象从指定的文件中读取出来，生成一个 OOP 对象

```java
FileInputStream fis = new FileInputStream("D:\\temp.txt");
ObjectInputStream ois = new ObjectInputStream(fis);

User user = (User)ois.readObject();
System.out.println(user.age);
System.out.println(user.name);
```

输出结果为：

```java
10
oy
```



## 3、序列化的 4 个小知识点

> #### 1、序列化对象内部的对象也必须是可序列化的

A 对象内部维护了一个 B 对象

当我们序列化 A 对象时，那么这个 B 对象必须也是可序列化的，即必须实现 Serializable 接口



> #### 2、static 变量 不会被序列化

​	因为**序列化目的是保存对象的状态，保存的是堆中的 OOP 对象**，而 static 不属于对象的状态，它位于方法区中，因此不会添加到序列化中（可以当作是移除了 static）

```java
只有 在类的内部 或者 使用类名 才能访问静态变量，实例化出来的对象是无法直接访问静态变量的
```



> #### 3、transient 变量不会被序列化

​	这里的不会被序列化是指 不会保存它序列化前的值，即不会保存它的状态，当反序列化后得到的是默认值（0，null）

**它跟 static 的区别在于是否会剔除字段**



比如存在如下对象，它的 mood 变量是使用 transient 修改的，那么理论不会被序列化

```java
public class User implements Serializable {
 //年龄
 int age;
 //名字
 String name;
 //心情
 transient String mood;
}
```



把 User 对象写入到文件中

```java
FileOutputStream fos = new FileOutputStream("D:\\temp.txt");
ObjectOutputStream oos = new ObjectOutputStream(fos);

User user = new User();
user.mood = "愉快";
user.age = 10;
oos.writeObject(user);

oos.flush();
oos.close();
```



从文件中反序列化回 User 对象

```java
FileInputStream fis = new FileInputStream("D:\\temp.txt");

ObjectInputStream oin = new ObjectInputStream(fis);

User user1 = (User) oin.readObject();

System.out.println(user1.getAge());
System.out.println(user1.getMood());
```

输出结果：

```java
10
null
```





> #### 4、序列化版本号

```java
private static final long serialVersionUID = 4043760055673899739L;
```

当实现 Serializable 接口时，都会默认存在一个序列化版本号 serialVersionUID

如果没有**显式指定**，那么在序列化时根据 类 来生成一个序列化版本号，然后写入到字节流中



**序列化版本号作用：**

```java
接收 远程传输过来的序列化字节流，然后比较 字节流的序列化版本号 和 当前类的序列化版本号
	如果相同，那么就认为 字节流中的实体对象 和 当前实体类 的一个实例，那么可以进行反序列化
	如果不相同，那么会反序列化失败，出现异常
```



**显式指定序列化版本号：**

```java
比如 User 类中有 age 和 name 字段，它的实体类在网络中进行传输
    某天需要对 user 加一个 email 字段，如果没有显示指定版本号，那么对于该类来说，它的序列化版本号已经发生改变了
    这样就会导致已经存在于网络中的 User 对象字节流的版本号跟类的序列化版本号对不上，从而导致这些字节流无效
因此为了使得新旧版本对象兼容，可以显示指定序列化版本号，那么在序列化和反序列化时，都是使用的这个版本号，而不会去生成新的
    因此在新增或者删除字段的时候，，那么字节流中对于该类已经删除的字段会被舍弃，新增的字段会赋予默认值
```

