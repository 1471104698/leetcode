# [vivo 面试：605. 种花问题（有点难的贪心）](https://leetcode-cn.com/problems/can-place-flowers/)

![1596001203073](C:\Users\蒜头王八\AppData\Roaming\Typora\typora-user-images\1596001203073.png)

## 题意描述

给定数组 flowerbed，表示现在花圃的种花情况，1 表示该位置已经种花，0 表示该位置空着

现在给定 n，表示能否在花圃中再种 n 朵花，种花的要求是左右相邻位置不能有花

比如 [1,0,0,0,1]，如果是位于中间，那么中间的 0 可以种一朵花

比如 [1,0,0,1,1]，不存在相邻位置都空着的位置，因此不能种花

但是，需要注意边界问题

如果是 [0,0,1,1] 的花，左边界的 0 由于左边是边界，那么可以当作是不存在花的，那么可以种一朵花

右边界同理 [1,1,0,0]，右边界可以种一朵花



## 方法①、贪心

### 实现思路

根据上面的题意分析，我们可以看出，左边界和右边界相当于左右两边都存在一个 0

那么我们可以直接在原数组的左右边界添加一个 0，这样就避免了多余的边界判断

能够种花的时机：当当前位置和左右相邻位置都为空着时，那么当前位置可以种一朵花

同时，需要在数组中将该位置置为 1，表示已经种花了（不要忘记这一步）

```java
if(temp[i - 1] == 0 && temp[i] == 0 && temp[i + 1] == 0){
    n--;
    temp[i] = 1;
}
```

时间复杂度 `O(n)`

空间复杂度 `O(n)`：开辟了一个数组，添加首尾 0

当然，如果要 `O(1)`，空间复杂度，那么我们就必须添加边界判断

比如当 `temp[i] == 0` 时，我们分别判断左右两边的情况

- 如果 `i == 0 || temp[i - 1] == 0`，那么左边通过
- 如果 `i == len - 1 || temp[i + 1] == 0`，那么右边通过
- 如果左右两边都通过，那么该位置可以种花

```java
if(temp[i] == 0 && (i == 0 || temp[i - 1] == 0) && (i == len - 1 || temp[i + 1] == 0)){
    n--;
    temp[i] = 1;
}
```



### 实现代码

```java
class Solution {
    public boolean canPlaceFlowers(int[] flowerbed, int n) {
        int len = flowerbed.length;
        //首尾添 0，避免边界判断
        int[] temp = new int[len + 2];
        System.arraycopy(flowerbed, 0, temp, 1, len);

        for(int i = 1; i <= len; i++){
            if(n == 0){
                break;
            }
            if(temp[i - 1] == 0 && temp[i] == 0 && temp[i + 1] == 0){
                n--;
                temp[i] = 1;
            }
        }
        return n == 0;
    }
}
```

