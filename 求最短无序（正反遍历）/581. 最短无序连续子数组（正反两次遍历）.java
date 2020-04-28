
给定一个整数数组，你需要寻找一个连续的子数组，如果对这个子数组进行升序排序，那么整个数组都会变为升序排序。

你找到的子数组应是最短的，请输出它的长度。

示例 1:

输入: [2, 6, 4, 8, 10, 9, 15]
输出: 5
解释: 你只需要对 [6, 4, 8, 10, 9] 进行升序排序，那么整个表都会变为升序排序。
说明 :

输入的数组长度范围在 [1, 10,000]。
输入的数组可能包含重复元素 ，所以升序的意思是<=。

class Solution {
    public int findUnsortedSubarray(int[] nums) {
        /*
        正序遍历，查找不满足升序的位置，记录我们遇到的最大值，如果 nums[i] < max，那么 i 位置需要排序，将最后的 i 赋值为 high
        反序遍历，查找不满足降序的位置，记录我们遇到的最小值，如果 nums[i] > min，那么 i 位置需要排序，将最好的 i 赋值为 low

        最终结果 high - low + 1

        过程分析：
        nums = {2,2,2,1,2,2,1,2,2,2,1}

        2,2,2,1,2,2,1,2,2,2,1
            ↑               ↑
            i               j
        我们一般情况下进行正序遍历，会发现 [i, j] 这段需要进行调整，但是前面两个 2 我们会给忽略掉
        因为我们遍历前 3 个 2 的时候没什么问题，直到遇到 1 ，才发现 第 3 个 2 需要进行调整，但是无法得知 第 3 个 2 前面的情况
        我们添加一次反序遍历，查找不满足降序的位置，这样就能通过反序查找出正序遍历漏掉的值
        */

        int len = nums.length;

        int high = -1;
        int low = len;

        int min = Integer.MAX_VALUE;
        int max = Integer.MIN_VALUE;
        for(int i = 0; i < len; i++){
            if(nums[i] < max){
                high = i;
            }
            if(nums[len - i - 1] > min){
                low = len - i - 1;
            }
            max = Math.max(nums[i], max);
            min = Math.min(nums[len - i - 1], min);
        }
        return high == -1 ? 0 : high - low + 1;
    }
}