给定一个整数数组和一个整数 k，你需要找到该数组中和为 k 的连续的子数组的个数。

示例 1 :

输入:nums = [1,1,1], k = 2
输出: 2 , [1,1] 与 [1,1] 为两种不同的情况。
说明 :

数组的长度为 [1, 20,000]。
数组中元素的范围是 [-1000, 1000] ，且整数 k 的范围是 [-1e7, 1e7]。

class Solution {
    public int subarraySum(int[] nums, int k) {
        /*
            使用前缀和
            方法一：使用一个 sum 数组，遍历 [1, len] 求得 sum[i] - sum[j] 如果为 k ，那么 res++ ，双重循环 O(n^2)
            方法二：
            (preSum[i] - preSum[j]) mod 1 == k ⟺ preSum[i] - k == preSum[j]
            我们使用 map 记录前缀结果和出现的次数

            因为这里我们只需要要求 和为 k，而不需要求 mod k 的结果，因此无需关系 sum 是否为 负数 而进行特殊处理（负数处理见 974. 和可被 K 整除的子数组）
        */
        int len = nums.length;
        Map<Integer, Integer> map = new HashMap<>();
        //初始化 结果为 0 的个数为 1
        map.put(0, 1);

        int res = 0;

        int sum = 0;
        for(int num : nums){
            sum += num;
            res += map.getOrDefault(sum - k, 0);
            map.put(sum, map.getOrDefault(sum, 0) + 1);
        }
        return res;
    }
}