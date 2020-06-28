# **数据结构 api**



## **1、TreeSet**

```java
由于 TreeSet 是红黑树实现的有序的树，因此方便查找 大于等于 或 小于等于 或 严格大于 或 严格小于 某个值的 值
因此以下这些 api 是 TreeSet 特有的

higher(int num)：找最小的 严格大于 num 的元素（通俗讲，如果 num 存在，那么即 num 所在节点的右子节点）
lower(int num)：找最大的 严格小于 num 的元素（通俗讲，如果 num 存在，那么即 num 所在节点的左子节点）
floor(int num)：找最大的 小于等于 num 的元素（通俗讲，如果 num 存在，那么即 num 本身，否则假设 num 虚拟存在，那么即为 num 的左子节点）
ceiling(int num)：找最小的 大于等于 num 的元素（通俗讲，如果 num 存在，那么即 num 本身，否则假设 num 虚拟存在，那么即为 num 的右子节点）

//注意
使用上面这些 api 的时候，引用类型必须是 TreeSet，不能是 Set
因为是 TreeSet 特有的方法，如果使用 Set 那么只能使用 Set 固定的方法，不能使用子类的方法
```

