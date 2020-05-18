给你一个整数数组 nums ，请你找出数组中乘积最大的连续子数组（该子数组中至少包含一个数字），并返回该子数组所对应的乘积。

 

示例 1:

输入: [2,3,-2,4]
输出: 6
解释: 子数组 [2,3] 有最大乘积 6。
示例 2:

输入: [-2,0,-1]
输出: 0
解释: 结果不能为 2, 因为 [-2,-1] 不是子数组。

class Solution {
    public int maxProduct(int[] nums) {
        /*
            要求的是连续的子数组的最大乘积，
            这道题跟 最大子序和 有点类似，只不过 最大子序和 要求的不需要连续，
            但是在遇到会导致结果变小的情况下舍弃前面的值，即 if(sum < 0) sum = 0
            并同时记录下当前的最大值，即 res = Math.max(res, sum)

            这里也一样，遇到会让结果变小的，就直接舍弃掉前面的子数组，重新从当前位置 i 开始计算乘积

            注意：因为乘积过程中存在负数，那么意味着前面的负数结果遇到负数的时候可能翻身做地主变成很大的数
            因此，我们需要记录前面遍历过的最大值和最小值

            舍弃前面子数组的条件：
            1、当前面子数组的最大值 maxVal * nums[i] 比 nums[i] 还小的时候
                即存在 maxVal < 0, nums[i] > 0 之类的情况，那么乘积结果为比 maxVal 更小的负数，这种情况下就舍弃前面的乘积结果，直接从 nums[i] 开始计算
            2、当前面子数组的最小值 minVal * nums[i] 比 nums[i] 还大的时候
                即存在 minVal < 0, nums[i] < 0 之类的情况，那么乘积结果为比 minVal 更大的正数，这种情况下就舍弃前面的乘积结果，直接从 nums[i] 开始计算

            当 nums[i] 为负数的时候，我们将 maxVal 和 minVal 进行交换，因为这样才可能获取最大值
            比如 nums[i] = -1
            1、maxVal = 5, minVal = 1, 交换后  maxVal = 1, minVal = 5，乘积结果 maxVal = -1, minVal = -5
            2、maxVal = 5, minVal = -1, 交换后  maxVal = -1, minVal = 5，乘积结果 maxVal = 1, minVal = -5
            3、maxVal = -1, minVal = -5, 交换后  maxVal = -5, minVal = -1，乘积结果 maxVal = 5, minVal = -1
                                                                                                👆
                                                                 (本来乘积结果为 1，但是发现比 nums[i] 还大，因此舍去，从 nums[i] 开始计算乘积结果)
        */
        int res = Integer.MIN_VALUE;
        int minVal = 1;
        int maxVal = 1;
        for(int num : nums){
            if(num < 0){
                int temp = minVal;
                minVal = maxVal;
                maxVal = temp;
            }
            maxVal = Math.max(num, maxVal * num);
            minVal = Math.min(num, minVal * num);
            res = Math.max(res, maxVal);
        }
        return res;
    }
}