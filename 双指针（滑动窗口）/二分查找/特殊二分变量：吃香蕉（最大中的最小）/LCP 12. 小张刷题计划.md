# [LCP 12. 小张刷题计划](https://leetcode-cn.com/problems/xiao-zhang-shua-ti-ji-hua/)

*![image.png](https://pic.leetcode-cn.com/5e62e3cda6d0beec314bd4100152306a1b888c76b42cc90229635fd2d19b60a0-image.png)*

## 题意描述

给定一个数组，time[i] 表示 A 做完第 i 道题的时间

一天中 A 有 1 次求助机会，可以对某道题进行场外求助，那么这道题花费的时间为 0

给定 m 天，按照顺序做题，求这 m 天中 所有天数的刷题时间 T 中最大值的最小值

即求得一个数组分割方法，使得分得的这些数组之和的最大值最小

此题是求最大值的最小值，使用二分法，对 时间 T 进行二分

类似 401 分割数组最大值，相当于是将数组分割为 m 个，然后求所需数组容量的最小值

**能二分的主要原因是**：顺序分割，而不是任意分割组合

(同样也类似 珂珂吃香蕉 和 D 天送包裹)



**限制：**

- `1 <= time.length <= 10^5`
- `1 <= time[i] <= 10000`
- `1 <= m <= 1000`

## **示例 1：**

```java
输入：time = [1,2,3,3], m = 2

输出：3

解释：第一天小张完成前三题，其中第三题找小杨帮忙；第二天完成第四题，并且找小杨帮忙。这样做题时间最多的一天花费了 3 的时间，并且这个值是最小的。
```



## **示例 2：**

```java
输入：time = [999,999,999], m = 4

输出：0

解释：在前三天中，小张每天求助小杨一次，这样他可以在三天内完成所有的题目并不花任何时间。
```



## 方法①、二分时间 T

### 实现思路

我们对时间 T 进行二分，将 T 当作容量

判断在 mid 容量内是否能够分割出 m 个数组

这道题的特殊之处在于可以场外求助，即每一个数组都可以消除掉一个值

即如果数组内的元素和大于容量，那么我们可以选择消除掉一个数，让数组的元素和重新小于容量

那么根据贪心，消除的必定是数组中的最大值，这样才能够存放更多的值

因此，我们判断过程中，记录当前分割的子数组元素内的最大值，如果数组溢出了，那么判断减去最大值是否能够防止溢出，如果不能，表示需要开辟一个新的数组，如果能，那么表示机会就用在这个最大值上了

**注意**：我们是在 `sum += val` 后才进行判断是否溢出的，如果溢出了，那么将前面的元素分割为一个数组，将当前元素作为一个新数组的起始元素，即我们是在当前元素不能塞进数组的情况下才分割出上一个数组，

这时候m--， m 表示的是剩余的可分割的数组数目，如果 m == 0，那么意味着可用数组为 0，但是我们还有当前元素以及后面的剩余元素还没有塞进数组，因此 mid 容量无法满足要求，返回 false



这样的话，如果当前 val 不适用，m-- 后， m == 0

```java
private boolean check(int[] time, int mid, int m){
    int sum = 0;
    int maxVal = 0;
    for (int i = 0; i < time.length;) {
        //剩余数组数为 0，但是仍然存在元素尚未分配
        if (m == 0) {
            return false;
        }
        sum += time[i];
        maxVal = Math.max(maxVal, time[i]);
        //将求助机会留给最大值，减去最大值后还是超过容量，表示我们需要重新开一个数组
        if (sum - maxVal > mid) {
            m--;
            sum = 0;
            maxVal = time[i];;
            continue;
        }
        i++;
    }
    return true;
}
```



### 实现代码

```java
class Solution {
    public int minTime(int[] time, int m) {
        /*
            这 tm 还是 D 天运包裹的问题
        */
        int left = 0;
        int right = 0;
        for(int num : time){
            right += num;
        }
        while(left < right){
            int mid = (left + right) >>> 1;
            if(!check(time, mid, m)){
                left = mid + 1;
            }else{
                right = mid;
            }
        }
        return left;
    }
    //将 time 中元素划分为 m 个子数组，其中每个子数组的容量为 mid，并且每个子数组可以有一个消除大数的机会
    private boolean check(int[] time, int mid, int m){
        int sum = 0;
        int maxVal = 0;
        for (int i = 0; i < time.length;) {
            //剩余数组数为 0，但是仍然存在元素尚未分配
            if (m == 0) {
                return false;
            }
            sum += time[i];
            maxVal = Math.max(maxVal, time[i]);
            //将求助机会留给最大值，减去最大值后还是超过容量，表示我们需要重新开一个数组
            if (sum - maxVal > mid) {
                m--;
                sum = 0;
                maxVal = time[i];;
                continue;
            }
            i++;
        }
        return true;
    }
}
```

