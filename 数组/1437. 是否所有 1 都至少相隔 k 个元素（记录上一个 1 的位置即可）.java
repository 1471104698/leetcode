给你一个由若干 0 和 1 组成的数组 nums 以及整数 k。如果所有 1 都至少相隔 k 个元素，则返回 True ；否则，返回 False 。

 

示例 1：



输入：nums = [1,0,0,0,1,0,0,1], k = 2
输出：true
解释：每个 1 都至少相隔 2 个元素。


示例 2：

输入：nums = [1,0,0,1,0,1], k = 2
输出：false
解释：第二个 1 和第三个 1 之间只隔了 1 个元素。
示例 3：

输入：nums = [1,1,1,1,1], k = 0
输出：true


class Solution {
    public boolean kLengthApart(int[] nums, int k) {
        /*
            我们只需要记录上一个 1 的位置即可
        */
        int lastIdx = -1;
        int len = nums.length;
        for(int i = 0; i < len; i++){
            if(nums[i] == 1){
                if(lastIdx != -1 && i - lastIdx < k + 1){
                    return false;
                }
                lastIdx = i;
            }
        }
        return true;
    }
}