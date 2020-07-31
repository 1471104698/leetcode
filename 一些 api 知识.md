# **api**



## 1、TreeSet

```java
1、
由于 TreeSet 是红黑树实现的有序的树，因此方便查找 大于等于 或 小于等于 或 严格大于 或 严格小于 某个值的 值
因此以下这些 api 是 TreeSet 特有的

higher(int num)：找最小的 严格大于 num 的元素（通俗讲，如果 num 存在，那么即 num 所在节点的右子节点）
lower(int num)：找最大的 严格小于 num 的元素（通俗讲，如果 num 存在，那么即 num 所在节点的左子节点）
floor(int num)：找最大的 小于等于 num 的元素（通俗讲，如果 num 存在，那么即 num 本身，否则假设 num 虚拟存在，那么即为 num 的左子节点）
ceiling(int num)：找最小的 大于等于 num 的元素（通俗讲，如果 num 存在，那么即 num 本身，否则假设 num 虚拟存在，那么即为 num 的右子节点）

//注意
使用上面这些 api 的时候，引用类型必须是 TreeSet，不能是 Set
因为是 TreeSet 特有的方法，如果使用 Set 那么只能使用 Set 固定的方法，不能使用子类的方法


2、
TreeSet 的 迭代是根据中序遍历来的
for(int num : set){
	//中序遍历
}
```





## 2、TreeMap

```java
1、

higherEntry(T t)：获取比 t 严格大的 entry 节点，如果没有，返回 null
lowerEntry(T t)：获取比 t 严格小的 entry 节点，如果没有，返回 null
ceilingEntry(T t)：获取大于等于 t 的 entry 节点，如果没有，返回 null
floorEntry(T t)：获取小于等于 t 的 entry 节点，如果没有，返回 null
```





## 3、Arrays

```java
1、sort()
Arrays.sort(int[] a) 
    支持基本数据类型和包装类

Arrays.sort(T[] a, Comparator<? super T> c)
可以传入比较器，默认比较方式为升序，不支持基本数据类型

Arrays.sort(int[] a, int formIndex, toIndex)
只排序指定范围
这里有个坑：formIndex 是开始的索引位置，而 toIndex 是结束的位置，但是排序范围并不包含 toIndex
即该方法的指定范围 类似于 String 的 substring()

Arrays.sort(T[] a, int formIndex, toIndex, Comparator<? super T> c)
    
//存在比较器的不支持基本数据类型
    
2、toString()
Arrays.toString()：将数组转换为字符串输出，该方法只支持基本数据类型 int[] boolean[] 之类的，其他的不支持
```





## 4、List / Set 的 toArray()

```java
list.toArray() 方法不支持基本数据类型，只能转换为包装类型 和 String 和 二维数组 int[][] 等
即如果想将 list 中的元素转换为 int 型数组，那么只能自己通过 for 循环来转换

例子：
    
List<Integer> list = Arrays.asList(1, 2, 23);
int[] a = new int[3];
list.toArray(a);
该做法是不允许的 X
    
List<int[]> list = Arrays.asList(new int[]{0}, new int[]{1}, new int[]{3, 4});
int[][] a = new int[3][];
list.toArray(a);
该做法是可以的 √
```



## 5、List 的 set(int idx, int val)

```java
使用 list.set(int idx, int val) 可以直接使用 val 值覆盖原本 idx 位置的值
用于 "381. O(1) 时间插入、删除和获取随机元素 - 允许重复" 中将 list 末尾的值直接覆盖到要删除的 idx 位置的值，
然后删除末尾的值，防止数组的元素移动，达到 O(1) 删除
```



## 6、接口实现 ArrayDeque 和 LinkedList 的区别

- ArrayDeque 不可以添加 null 值，LinkedList 可以添加 null 值

  - 使用的时候需要注意，对于层次遍历之类的，如果队列实现是 ArrayDeque ，那么需要对左右子节点判空再添加，如果是 LinkedList ，则不需要
  - 下面是 两者 add() 方法的底层实现

  ```java
  //ArrayDeque add()方法底层实现
  public void addLast(E e) {
      //遇到空值直接抛异常
      if (e == null)
          throw new NullPointerException();
      elements[tail] = e;
      if ( (tail = (tail + 1) & (elements.length - 1)) == head){
          //扩容
          doubleCapacity();
      }
  }
  
  //LinkedList add() 方法底层实现，没有对空值处理
  void linkLast(E e) {
      final Node<E> l = last;
      final Node<E> newNode = new Node<>(l, e, null);
      last = newNode;
      if (l == null)
          first = newNode;
      else
          l.next = newNode;
      size++;
      modCount++;
  }
  ```

  

- ArrayDeque 底层是数组，LinkedList 底层是链表

