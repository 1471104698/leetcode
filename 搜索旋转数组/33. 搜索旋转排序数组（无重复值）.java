假设按照升序排序的数组在预先未知的某个点上进行了旋转。

( 例如，数组 [0,1,2,4,5,6,7] 可能变为 [4,5,6,7,0,1,2] )。

搜索一个给定的目标值，如果数组中存在这个目标值，则返回它的索引，否则返回 -1 。

你可以假设数组中不存在重复的元素。

你的算法时间复杂度必须是 O(log n) 级别。

示例 1:

输入: nums = [4,5,6,7,0,1,2], target = 0
输出: 4
示例 2:

输入: nums = [4,5,6,7,0,1,2], target = 3
输出: -1

class Solution {
    public int search(int[] nums, int target) {
        int left = 0;
        int right = nums.length - 1;
        while(left < right){
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
        return nums.length > 0 && nums[left] == target ? left : -1;
    }
}

//思路②、与 面试题 10.03. 搜索旋转数组（查找 target 最小索引位置）一样的方法，这是通用方法
class Solution {
    public int search(int[] arr, int target) {
        return helper(arr, target, 0, arr.length - 1);
    }
    private int helper(int[] arr, int target, int left, int right){
        if(left > right){
            return -1;
        }
        //注意：这里不存在普遍二分查找的 while(left < right)
        int mid = (left + right) >>> 1;
        if(arr[mid] == target){
            //向左递归，查找更小值
            return mid;
        }else{
            //向左向右递归，得到结果存储到 left 和 right 中
            left = helper(arr, target, left, mid - 1);
            //如果左不为 -1，那么直接返回，不再向右递归
            if(left != -1){
                return left;
            }
            right = helper(arr, target, mid + 1, right);
            return right;
        }
    }
}