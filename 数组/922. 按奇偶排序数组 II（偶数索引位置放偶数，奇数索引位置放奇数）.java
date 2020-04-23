给定一个非负整数数组 A， A 中一半整数是奇数，一半整数是偶数。

对数组进行排序，以便当 A[i] 为奇数时，i 也是奇数；当 A[i] 为偶数时， i 也是偶数。

你可以返回任何满足上述条件的数组作为答案。

 

示例：

输入：[4,2,5,7]
输出：[4,5,2,7]
解释：[4,7,2,5]，[2,5,4,7]，[2,7,4,5] 也会被接受。
 

提示：

2 <= A.length <= 20000
A.length % 2 == 0
0 <= A[i] <= 1000


class Solution {
    public int[] sortArrayByParityII(int[] A) {
        //双指针，在偶数位置找奇数，在奇数位置找偶数，然后进行交换
        int len = A.length;
        int left = 0;
        int right = 1;

        while(left < len && right < len){
            //偶数位置找奇数
            while(left < len && A[left] % 2 == 0){
                left += 2;
            }
            //奇数位置找偶数
            while(right < len && A[right] % 2 != 0){
                right += 2;
            }
            if(left < len && right < len){
                int temp = A[left];
                A[left] = A[right];
                A[right] = temp;
            }
        }
        return A;
    }
}