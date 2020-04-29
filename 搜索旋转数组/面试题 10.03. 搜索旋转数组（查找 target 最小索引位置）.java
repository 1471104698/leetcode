搜索旋转数组。给定一个排序后的数组，包含n个整数，但这个数组已被旋转过很多次了，次数不详。请编写代码找出数组中的某个元素，假设数组元素原先是按升序排列的。若有多个相同元素，返回索引值最小的一个。

示例1:

 输入: arr = [15, 16, 19, 20, 25, 1, 3, 4, 5, 7, 10, 14], target = 5
 输出: 8（元素5在该数组中的索引）
示例2:

 输入：arr = [15, 16, 19, 20, 25, 1, 3, 4, 5, 7, 10, 14], target = 11
 输出：-1 （没有找到）
提示:

arr 长度范围在[1, 1000000]之间

/*
        使用递归
        if(arr[mid] == target) 即找到了，那么向左递归，判断是否存在更小的索引位置
        else 向左向右递归查找，取代了 left++ 和 right-- 的方式（有点像快排的递归步骤）
        
*/
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
            left = helper(arr, target, left, mid - 1);
            if(left != -1 && left < mid){
                return left;
            }
            return mid;
        }
        //向左向右递归，得到结果存储到 left 和 right 中
        left = helper(arr, target, left, mid - 1);
        right = helper(arr, target, mid + 1, right);
        
        return left != -1 ? left : right;
    }
}