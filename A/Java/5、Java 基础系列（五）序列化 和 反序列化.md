# 序列化 和 反序列化



在此之前，先讲下创建 OOP 对象的四种方式：

- new
- 反射
- clone
- 反序列化



## 1、序列化的作用

序列化 是为了解决 **跨平台存储、网络传输** 的问题，我们跨平台存储和网络传输都是使用的 IO，而 IO 支持的数据格式就是字节数组

但是单纯的将对象转换为字节数组还不行，因为没有一定的规律，无法通过字节数组再还原回原来的对象

因此将对象按照制定的规则转换为字节数组（序列化），然后再按照规则将字节数组转换为对象（反序列化）



跨平台存储和网络传输 类似 我们要把一栋房子运到别的地方去，那么就是先设计出房子的图纸（规则），然后再拆成一快快的砖头（序列化），然后传输到别的地方去，按照图纸（规则）对房子进行还原（反序列化）



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



## 3、序列化 需要注意的点

**1、static 变量 不会被序列化**

- 因为序列化保存的是 OOP 对象的状态，static 不属于 OOP 对象的状态，同时，OOP 实例对象也无法访问静态变量，所以反序列化得到的对象在它的视角里也不知道是否序列化了静态变量

**2、transient 修饰的变量不会被序列化**

注意，这里的不会被序列化是指不会保存它序列化前的值，而不是把这个变量从 对象中剔除



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





**3、序列化版本号**

```java
private static final long serialVersionUID = 4043760055673899739L;
```

每个类都可以生成一个序列化版本号

在序列化的时候，会将类的序列化版本号 写入到字节流中

它的作用是在远程调用的时候传输过来的 序列化字节流中，通过比较字节流内部的 序列化版本号 和 当前实体类的 序列化版本号是否一致，如果相同，那么就认为 字节流中的实体对象 和 当前实体类 的一个实例，那么可以进行反序列化

如果不相同，那么会反序列化失败，出现异常



比如 User 类中有 age 和 name 字段，它的实体类在网络中进行传输，某天需要对 user 加一个 email 字段，如果重新获取序列化版本号是会发生改变的，这样就会导致已经存在于网络中的实体类对象无效，因此为了使得新旧版本对象兼容，在新增或者删除字段的时候，可以不改变序列化版本号，那么接收到的字节流中已经删除的字段会被舍弃，新增的字段会赋予默认值