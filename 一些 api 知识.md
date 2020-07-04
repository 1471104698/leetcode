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



## 2、Arrays

```java
1、sort()
Arrays.sort(T[] a) 
不支持 int[] 、boolean[] 之类的基本数据类型数组，因为传参类型为 T，基本数据类型不属于泛型范围，只能是 Integer[] 等包装类 数组 或 String[] 

Arrays.sort(T[] a, Comparator<? super T> c)
可以传入比较器，默认比较方式为升序

Arrays.sort(T[] a, int formIndex, toIndex)
只排序指定范围
这里有个坑：formIndex 是开始的索引位置，而 toIndex 是结束的位置，但是排序范围并不包含 toIndex
即该方法的指定范围 类似于 String 的 substring()

Arrays.sort(T[] a, int formIndex, toIndex, Comparator<? super T> c)
    
2、toString()
Arrays.toString()：将数组转换为字符串输出，该方法只支持基本数据类型 int[] boolean[] 之类的，其他的不支持
```





## 3、List / Set 的 toArray()

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

