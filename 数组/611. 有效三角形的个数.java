给定一个包含非负整数的数组，你的任务是统计其中可以组成三角形三条边的三元组个数。

示例 1:

输入: [2,2,3,4]
输出: 3
解释:
有效的组合是: 
2,3,4 (使用第一个 2)
2,3,4 (使用第二个 2)
2,2,3
注意:

数组长度不超过1000。
数组里整数的范围为 [0, 1000]。

class Solution {
    public int triangleNumber(int[] nums) {
        /*
        因为三条边中任意两边之和大于第三边，因此我们只要求较小的两条边大于较大的边即可

        我们先将数组排序，然后从后面取第三边，再从前面求两边
        */
        Arrays.sort(nums);
        int len = nums.length;
        int c = 0;
        for(int i = len - 1; i >= 2; i--){
            int left = 0;
            int right = i - 1;
            while(left < right){
                //只要 left 和 right 两边符合条件，那么表示 [left, right - 1] 这几条边 和 right 都满足条件，因此组合数为 right - left
                if(nums[left] + nums[right] > nums[i]){
                    c += right - left;
                    right--;
                }else{
                    left++;
                }
            }
        }
        return c;
    }
}