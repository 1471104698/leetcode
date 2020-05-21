给定一个包含非负数的数组和一个目标整数 k，编写一个函数来判断该数组是否含有连续的子数组，其大小至少为 2，总和为 k 的倍数，即总和为 n*k，其中 n 也是一个整数。

示例 1:

输入: [23,2,4,6,7], k = 6
输出: True
解释: [2,4] 是一个大小为 2 的子数组，并且和为 6。
示例 2:

输入: [23,2,6,4,7], k = 6
输出: True
解释: [23,2,6,4,7]是大小为 5 的子数组，并且和为 42。
说明:

数组的长度不会超过10,000。
你可以认为所有数字总和在 32 位有符号整数范围内。

class Solution {
    public boolean checkSubarraySum(int[] nums, int k) {
        /*
            (preSum[i] - preSum[j]) mod k == 0  ⟺ preSum[i] mod k == preSum[j] mod k
            题目说了元素是非负数，那么就不需要进行负数处理，（负数处理详见 974. 和可被 K 整除的子数组（前缀和 - 负数同余））

            题目要求的是是否存在这么一个连续子数组和为 k 的倍数，那么我们无需记录 sum 出现的次数，只需要记录对应的 索引

            特殊情况：
                k = 0，如果直接 % 0 会错误，因此我们需要单独判断是否存在两个连续的 0
        */

        int len = nums.length;
        if(k == 0){
            for(int i = 0; i < len - 1; i++){
                if(nums[i] == nums[i + 1] && nums[i] == 0){
                    return true;
                }
            }
            return false;
        }

        //存储 sum 和 对应的索引
        Map<Integer, Integer> map = new HashMap<>();

        //初始化：存储 0 和 索引 -1
        map.put(0, -1);

        int sum = 0;

        for(int i = 0; i < len; i++){
            sum += nums[i];
            sum %= k;
            if(map.containsKey(sum)){
                if(i - map.get(sum) > 1){
                    return true;
                }
            }else{
                map.put(sum, i);
            }
        }
        return false;
    }
}