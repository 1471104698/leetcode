假设按照升序排序的数组在预先未知的某个点上进行了旋转。

( 例如，数组 [0,0,1,2,2,5,6] 可能变为 [2,5,6,0,0,1,2] )。

编写一个函数来判断给定的目标值是否存在于数组中。若存在返回 true，否则返回 false。

示例 1:

输入: nums = [2,5,6,0,0,1,2], target = 0
输出: true
示例 2:

输入: nums = [2,5,6,0,0,1,2], target = 3
输出: false
进阶:

这是 搜索旋转排序数组 的延伸题目，本题中的 nums  可能包含重复元素。
这会影响到程序的时间复杂度吗？会有怎样的影响，为什么？

class Solution {
    public boolean search(int[] nums, int target) {
        int len = nums.length;

        int left = 0;
        int right = len - 1;
        while(left < right){
            //去除左重复
            while(left < right && nums[left] == nums[left + 1]) left++;
            //去除右重复
            while(left < right && nums[right] == nums[right - 1]) right--;

            int mid = (left + right) >>> 1;
            //左边有序,注意，这里必须是 nums[left] <= nums[mid]， 如果写成 nums[left] < nums[mid] 会死循环
            if(nums[left] <= nums[mid]){
                //target 在左边有序部分内
                if(target >= nums[left] && target <= nums[mid]){
                    right = mid;
                }else{
                    left = mid + 1;
                }
            }else{
                //target 在右边有序部分内
                if(target >= nums[mid] && target <= nums[right]){
                    left = mid;
                }else{
                    right = mid - 1;
                }
            }
        }
        //这里由于去重，所以 left 可能会越界，比如 [1,1] ,target = 0，最终 left = 2, 所以需要需要判断是否越界
        return len > 0 && left < len && nums[left] == target;
    }
}