# 为何重写 equals() 和 hashCode()



*![image.png](https://pic.leetcode-cn.com/1605798581-YEDEzA-image.png)*



## 1、equals()

首先我们看看 Object 内部自带的 equals() 方法

```java
public boolean equals(Object obj) {
    return (this == obj);
}
```

它内部是直接调用的 == 来判断的，这意味着什么？意味着 a.equals(b) 和 a == b 的作用是相同的

a == b，如果是基本数据类型，那么比较值是否相同，如果是引用类型，那么比较地址值是否相同（因为 a 和 b 作为一个引用变量，存储的就是地址值）

因此，比如 HashMap 这种情况，我们使用 非基本数据类型时，不能保证作为 key 的每次都是相同的一个对象，因此不能直接使用 equals()，而是需要进行重写，要求对象值相同即可



当我们直接使用 Integer、String 作为 key 是因为这些类中已经重写了 equals() 方法

```java
String、Integer 这种基础类自己重写了 equals() 方法，所以我们可以直接调用进行比较，
而对于我们自己定义的类，注意需要自己重写 equals() 方法
```

## 2、hashCode()

我们先看看 Object() 中的 hashCode()

```java
public native int hashCode();
```

没有任何实现，它就是一个本地方法，它有几种实现方法，目前知道的有以下两种：

1. 返回对象所在的地址

2. 使用跟当前线程相关的随机数 以及 三个固定值 利用随机算法进行运算得到一个随机数

而在没有重写 hashCode() 前，生成的这个 hashCode 称为 identity hashCode，它是存储在对象头的 _mark 中的，占据 23bit

因此，不同对象调用 hashCode() 生成的 identity hashCode 是不同的，这样在 HashMap 中无法每次都定位到同一个槽位



因此我们需要类的 hashCode()，让它返回的不再是这种随机值，而是在对象内容一样的情况下，返回的 hashCode 值也一样

不过需要注意的是，我们重写的 hashCode() 不再是 identity hashCode，它不会存储在 _mark 中

同时如果 hashCode 相同，也不代表两个对象的内容相同，因为 hashCode 是 32bit，是可能会发生碰撞的，因此 还需要使用 equals() 进行比较



## 3、为什么 HashMap 大都是使用 String 作为 key 而不是使用自定义类?

首先我们看 String 的源码如下：

```java
public final class String
    implements java.io.Serializable, Comparable<String>, CharSequence {

    private final char value[];

    private int hash; // Default to 0
	
    public int hashCode() {
        int h = hash;
        //如果 h == 0 并且 字符串长度大于 0，那么表示需要计算一次 hashCode
        if (h == 0 && value.length > 0) {
            char val[] = value;

            for (int i = 0; i < value.length; i++) {
                h = 31 * h + val[i];
            }
            hash = h;
        }
        return h;
    }
}

//输出
System.out.println("0".hashCode()); //48
System.out.println("a".hashCode()); //97
System.out.println("00".hashCode()); //1536
```

String 内部维护了 一个 hash 变量，默认为 0

当第一次调用 hashCode() 时，那么就会通过 value 计算出 hashCode，然后将这个 hashCode 赋值给 hash 变量，往后再调用 hashCode()，不需要计算，直接将缓存的 hash 返回

这里先说下，String 作为 key 有以下的好处：

- 由于 String value 的不可变，所以创建出来的 String 对象它的 hash 值是固定的
- String 内部维护了一个 hash 值，只需要计算一次 hashCode，同时也是因为 value 的不可变，所以才可以对 hash 进行缓存
- String 相比 Integer 之类的作为 key 有很大的多样性，因为字符有很多种，可以随意组合，而 Integer 固定就是那些数字



**String 作为 key 时需要注意点：**

```java
StringBuilder sb = new StringBuilder();

for(int i = 0; i < 5; i++){
    sb.append("a");
}
System.out.println(sb.toString().hashCode());	//92567585


for(int i = 0; i < 10; i++){
    sb.append("a");
}
System.out.println(sb.toString().hashCode());	//-799347552

for(int i = 0; i < 1000; i++){
    sb.append("a");
}
System.out.println(sb.toString().hashCode());	//904019584
```

从上面的 demo 我们可以看出，当 String 处于不同的长度时，那么它的 hashCode 有正有负，并且是处于一个循环状态的

即在 int 型变量不断溢出，导致 hashCode 值的变化位为 正->负->正->负->........

正是因为 hashCode 是一个 int 型，所以它最大只能承载 40亿，而当 String 太大，hashCode() 中乘积会溢出，这样的话，就很可能会导致两个不同的 String 出现相同的 hashCode 的



> #### 自定义类 相比 String 作为 key 不足在哪里?

```java
class User{
	String name;
	int age;
	
	@Override
	public int hashCode(){
        int h = name.hashCode() * age;
        //高 16 位 异或 低 16 位
		return (h >>> 16) ^ h;
	}
}
```

自定义了 User 类，并且重写了它的 hashCode() 方法

User 对象 如果多次调用 hashCode()，那么就需要多次计算，不能跟 String 一样只需要计算一次，效率低

我们如果要让 User 学习 String 那样缓存一个 hash 来减少 hashCode 的计算次数，那么就**必须保证该 User 对象属性不会发生改变，因为一旦发生改变，那么对应的 hashCode 也会发生改变**，而我们将 hashCode 该缓存起来了，那么下次调用 hashCode() 返回的是已经缓存起来的旧值。



同时，如果我们将 User 类作为 key，比如 user = ["fuck", 1]，将  user 作为 key， value = 5

而如果 put() 后再将 user.name = "阿giao"，将会存在以下问题：

首先 map.get() 方法逻辑如下：

```java
public V get(Object key) {
    Node<K,V> e;
    //hash(key) 需要调用 user 的 hashCode() 计算 hash 值
    return (e = getNode(hash(key), key)) == null ? null : e.value;
}
final Node<K,V> getNode(int hash, Object key) {
    Node<K,V>[] tab; Node<K,V> first, e; int n; K k;
    if ((tab = table) != null && (n = tab.length) > 0 &&
        (first = tab[(n - 1) & hash]) != null) {
    //这里代码省略了对头节点 和 红黑树节点的判断
        
        do {
            //先比较 hash 值，再比较 equals()
            if (e.hash == hash &&
                ((k = e.key) == key || (key != null && key.equals(k))))
                return e;
        } while ((e = e.next) != null);
    }
    return null;
}
```

原本是让 ["fuck", 1] 和 5 对应，结果 map 中变成了 ["阿giao", 1]，我们后续无论是使用 ["fuck", 1] 还是使用 ["阿giao", 1] 都是无法找到 value 的

- **使用 ["fuck", 1]：**因为 get() 需要比较 hash 和 key，Node 在 put() 时已经将 hash 值缓存下来了，所以 node.key 发生改变没影响，但是在使用 equals() 比较 key 时，由于 (key = ["fuck", 1]) != (node.key = ["阿giao", 1])，所以返回 false，那么就认为不存在数据

- **使用 ["阿giao", 1]：**因为 get() 需要比较 hash 和 key，由于定位数据所在的槽位是通过 key 的 hash 来定位的，而在原本的 Node 中，比如 ["fuck", 1] 定位的槽位为 2，["阿giao", 1] 定位的槽位为 4，而 user 数据发生改变，使得 ["阿giao", 1] 替换了 ["fuck", 1]，从而使得原本不应该在 2 号槽位的 ["阿giao", 1] 位于 2 号槽位了，这样的话，当我们调用 get() 时，key =  ["阿giao", 1] 仍然是定位到 4 号槽位，因此无法获取到 2 号槽位的 key

