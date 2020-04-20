给定一个数组，将数组中的元素向右移动 k 个位置，其中 k 是非负数。

示例 1:

输入: [1,2,3,4,5,6,7] 和 k = 3
输出: [5,6,7,1,2,3,4]
解释:
向右旋转 1 步: [7,1,2,3,4,5,6]
向右旋转 2 步: [6,7,1,2,3,4,5]
向右旋转 3 步: [5,6,7,1,2,3,4]
示例 2:

输入: [-1,-100,3,99] 和 k = 2
输出: [3,99,-1,-100]
解释: 
向右旋转 1 步: [99,-1,-100,3]
向右旋转 2 步: [3,99,-1,-100]
说明:

尽可能想出更多的解决方案，至少有三种不同的方法可以解决这个问题。
要求使用空间复杂度为 O(1) 的 原地 算法。

//思路①、暴力解法，移动 k 次：因为每个元素移动 k 次，所以 时间复杂度 O(k * n)，空间复杂度：O(1)
class Solution {
    public void rotate(int[] nums, int k) {
        /*
        暴力解法
        所有元素每次只移动 1 个位置，将数组移动 k 次
        每次只记录下最后一个元素，然后移动完成将最后一个元素放在 nums[0] 位置
        */

        int len = nums.length;
        k %= len;
        for(int i = 0; i < k; i++){
            int last = nums[len - 1];
            for(int j = len - 1; j >= 1; j--){
                nums[j] = nums[j - 1];
            }
            nums[0] = last;
        }
    }
}


//思路②、翻转数组：时间复杂度 O(n)，空间复杂度：O(1)
class Solution {
    public void rotate(int[] nums, int k) {
        /*
        len = 7
        0 1 2 3 4 5 6  k = 2
        5 6 0 1 2 3 4    

        翻转：
        第一次翻转：[6 5 4 3 2 1 0] 翻转范围：[0, len - 1]
        第二次翻转：[5 6 4 3 2 1 0] 翻转范围：[0, k - 1]
        第三次翻转：[5 6 0 1 2 3 4] 翻转范围：[k, len - 1]

        i 跟 (i + 2) % len 的位置进行交换 
        */
        if(k == 0){
            return;
        }
        int len = nums.length;

        k %= len;

        reverse(nums, 0, len - 1);
        reverse(nums, 0, k - 1);
        reverse(nums, k, len - 1);
    }
    private void reverse(int[] nums, int start, int end){
        while(start < end){
            int temp = nums[start];
            nums[start++] = nums[end];
            nums[end--] = temp;
        }
    }
}