
给定 n 个整数，找出平均数最大且长度为 k 的连续子数组，并输出该最大平均数。

示例 1:

输入: [1,12,-5,-6,50,3], k = 4
输出: 12.75
解释: 最大平均数 (12-5-6+50)/4 = 51/4 = 12.75
 

注意:

1 <= k <= n <= 30,000。
所给数据范围 [-10,000，10,000]。

class Solution {
    public double findMaxAverage(int[] nums, int k) {
        //使用滑动窗口，求出长度为 k 的连续子数组的最大和，最后使用 sum / k 即可
        int len = nums.length;
        
        int sum = 0;
        for(int i = 0; i < k; i++){
            sum += nums[i];
        }
        int max = sum;

        for(int i = k; i < len; i++){
            //sum 值需要实时更新
            sum = sum - nums[i - k] + nums[i];
            max = Math.max(max, sum);
        }
        return (double)max / k;
    }
}