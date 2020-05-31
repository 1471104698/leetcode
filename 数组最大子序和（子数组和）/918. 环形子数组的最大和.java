
给定一个由整数数组 A 表示的环形数组 C，求 C 的非空子数组的最大可能和。

在此处，环形数组意味着数组的末端将会与开头相连呈环状。（形式上，当0 <= i < A.length 时 C[i] = A[i]，而当 i >= 0 时 C[i+A.length] = C[i]）

此外，子数组最多只能包含固定缓冲区 A 中的每个元素一次。（形式上，对于子数组 C[i], C[i+1], ..., C[j]，不存在 i <= k1, k2 <= j 其中 k1 % A.length = k2 % A.length）

 

示例 1：

输入：[1,-2,3,-2]
输出：3
解释：从子数组 [3] 得到最大和 3
示例 2：

输入：[5,-3,5]
输出：10
解释：从子数组 [5,5] 得到最大和 5 + 5 = 10
示例 3：

输入：[3,-1,2,-1]
输出：4
解释：从子数组 [2,-1,3] 得到最大和 2 + (-1) + 3 = 4


class Solution {
    public int maxSubarraySumCircular(int[] A) {
        /*
        求最大和，莫非就两种情况：
        1、不用看作环形数组，就直接在原数组中求得
        2、看作环形数组，即 A[0] 和 A[len - 1] 都必须选中

        如果是情况 1，那么直接使用 最大子序和 的求法即可
        如果是情况 2，因为 A[0] 和 A[len - 1] 都必须选中，那么剩下的就是判断 A[2] ... A[len - 2] 删除中间哪段连续序列
            我们可以发现，我们要求的就是 A[2] ... A[len - 2] 之间最小子序和，将它删除，这其实是 最大子序和 的一次变形
        */
        int len = A.length;

        //情况 1
        int res1 = Integer.MIN_VALUE;
        int sum = 0;
        for(int num : A){
            if(sum < 0){
                sum = 0;
            }
            sum += num;
            res1 = Math.max(res1, sum);
        }
        //当只有 1 个 或 2 个元素，那么无需当作环形数组
        if(len <= 2){
            return res1;
        }

        //情况 2
        int res2 = Integer.MAX_VALUE;
        sum = 0;
        //记录 [1, len - 2] 的总和
        int tempSum = 0;
        for(int i = 1; i < len - 1; i++){
            if(sum > 0){
                sum = 0;
            }
            tempSum += A[i];
            sum += A[i];
            res2 = Math.min(res2, sum);
        }

        return Math.max(res1, tempSum - res2 + A[0] + A[len - 1]);
    }
}