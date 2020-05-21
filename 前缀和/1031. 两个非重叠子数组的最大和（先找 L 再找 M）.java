给出非负整数数组 A ，返回两个非重叠（连续）子数组中元素的最大和，子数组的长度分别为 L 和 M。（这里需要澄清的是，长为 L 的子数组可以出现在长为 M 的子数组之前或之后。）

从形式上看，返回最大的 V，而 V = (A[i] + A[i+1] + ... + A[i+L-1]) + (A[j] + A[j+1] + ... + A[j+M-1]) 并满足下列条件之一：

 

0 <= i < i + L - 1 < j < j + M - 1 < A.length, 或
0 <= j < j + M - 1 < i < i + L - 1 < A.length.
 

示例 1：

输入：A = [0,6,5,2,2,5,1,9,4], L = 1, M = 2
输出：20
解释：子数组的一种选择中，[9] 长度为 1，[6,5] 长度为 2。
示例 2：

输入：A = [3,8,1,3,2,1,8,9,0], L = 3, M = 2
输出：29
解释：子数组的一种选择中，[3,8,1] 长度为 3，[8,9] 长度为 2。

class Solution {
    public int maxSumTwoNoOverlap(int[] A, int L, int M) {
        /*
        求前缀和，然后我们先固定 L ，然后从另外的数组段中找 M
        */
        int len = A.length;
        int[] sum = new int[len + 1];
        for(int i = 0; i < len; i++){
            sum[i + 1] = sum[i] + A[i];
        }

        int maxSum = 0;
        //我们将 (i - L, i] 当作 L，那么我们将其余的数组段 [0, i - L] 和 [i + 1, len] 中找 M
        for(int i = L; i <= len; i++){

            //M 的最大和
            int tempSum = 0;
            //j 从 M 开始算起，将 [j - M, j] 当作 M
            for(int j = M; j < i - L; j++){
                tempSum = Math.max(tempSum, sum[j] - sum[j - M]);
            }
            for(int j = i + M; j <= len; j++){
                tempSum = Math.max(tempSum, sum[j] - sum[j - M]);
            }
            maxSum = Math.max(maxSum, tempSum + sum[i] - sum[i - L]);
        }
        return maxSum;
    }
}