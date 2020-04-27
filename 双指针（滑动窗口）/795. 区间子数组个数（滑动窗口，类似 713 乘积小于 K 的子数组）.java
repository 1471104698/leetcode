给定一个元素都是正整数的数组A ，正整数 L 以及 R (L <= R)。

求连续、非空且其中最大元素满足大于等于L 小于等于R的子数组个数。

例如 :
输入: 
A = [2, 1, 4, 3]
L = 2
R = 3
输出: 3
解释: 满足条件的子数组: [2], [2, 1], [3].
注意:

L, R  和 A[i] 都是整数，范围在 [0, 10^9]。
数组 A 的长度范围在[1, 50000]。

class Solution {
    public int numSubarrayBoundedMax(int[] A, int L, int R) {
        /*
        滑动窗口
        */
        int len = A.length;

        int c = 0;

        int left = 0;
        int right = 0;

        int max = 0;
        //记录连续小于 L 的元素个数
        int minCount = 0;
        while(right < len){
            max = Math.max(max, A[right]);
            right++;

            if(max > R){
                max = 0;
                left = right;
                minCount = 0;
            }else if(max >= L){
                if(A[right - 1] < L){
                    minCount++;
                }else{
                    minCount = 0;
                }
                //以 right 结尾的满足条件的连续子数组个数
                c += right - left - minCount;
            }
        }
        return c;
    }
}