给定一个非负整数数组 A，返回一个数组，在该数组中， A 的所有偶数元素之后跟着所有奇数元素。

你可以返回满足此条件的任何数组作为答案。

 

示例：

输入：[3,1,2,4]
输出：[2,4,3,1]
输出 [4,2,3,1]，[2,4,1,3] 和 [4,2,1,3] 也会被接受。
 

提示：

1 <= A.length <= 5000
0 <= A[i] <= 5000

class Solution {
    public int[] sortArrayByParity(int[] A) {
         //双指针，左边找奇数，右边找偶数，然后进行交换（有点像快排）
        int left = 0;
        int right = A.length - 1;
        while(left < right){
            //左边找一个奇数
            while(left < right && A[left] % 2 == 0){
                left++;
            }
            //右边找一个偶数
            while(left < right && A[right] % 2 != 0){
                right--;
            }
            if(left < right){
                int temp = A[left];
                A[left] = A[right];
                A[right] = temp;
            }
        }
        return A;
    }
}