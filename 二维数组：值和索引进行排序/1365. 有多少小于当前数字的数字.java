
给你一个数组 nums，对于其中每个元素 nums[i]，请你统计数组中比它小的所有数字的数目。

换而言之，对于每个 nums[i] 你必须计算出有效的 j 的数量，其中 j 满足 j != i 且 nums[j] < nums[i] 。

以数组形式返回答案。

 

示例 1：

输入：nums = [8,1,2,2,3]
输出：[4,0,1,1,3]
解释： 
对于 nums[0]=8 存在四个比它小的数字：（1，2，2 和 3）。 
对于 nums[1]=1 不存在比它小的数字。
对于 nums[2]=2 存在一个比它小的数字：（1）。 
对于 nums[3]=2 存在一个比它小的数字：（1）。 
对于 nums[4]=3 存在三个比它小的数字：（1，2 和 2）。
示例 2：

输入：nums = [6,5,4,8]
输出：[2,1,0,3]
示例 3：

输入：nums = [7,7,7,7]
输出：[0,0,0,0]

class Solution {
    public int[] smallerNumbersThanCurrent(int[] nums) {
        /*
            数组排序
        */
        int len = nums.length;
        int[][] temp = new int[len][2];

        for(int i = 0; i < len; i++){
            temp[i][0] = nums[i];
            temp[i][1] = i;
        }

        Arrays.sort(temp, (a, b) -> a[0] == b[0] ? a[1] - b[1] : a[0] - b[0]);

        int[] res = new int[len];
        /*
        排在第一位的必定是最小的，没有比它更小的值，因此直接忽略、从 i = 1 开始算起
        */
        for(int i = 1; i < len; i++){
            /*
            当 当前值跟上一个值不同时，那么比 当前值小的元素个数有 i 个
            当 当前值跟上一个值相同时，那么比 当前值小的元素跟上一个值相同
            */
            if(temp[i][0] != temp[i - 1][0]){
                res[temp[i][1]] = i;
            }else{
                res[temp[i][1]] = res[temp[i - 1][1]];
            }
        }
        return res;
    }
}