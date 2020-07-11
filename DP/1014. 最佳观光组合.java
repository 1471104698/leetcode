给定正整数数组 A，A[i] 表示第 i 个观光景点的评分，并且两个景点 i 和 j 之间的距离为 j - i。

一对景点（i < j）组成的观光组合的得分为（A[i] + A[j] + i - j）：景点的评分之和减去它们两者之间的距离。

返回一对观光景点能取得的最高分。

 

示例：

输入：[8,1,5,2,6]
输出：11
解释：i = 0, j = 2, A[i] + A[j] + i - j = 8 + 5 + 0 - 2 = 11
 

提示：

2 <= A.length <= 50000
1 <= A[i] <= 1000

class Solution {
    public int maxScoreSightseeingPair(int[] A) {
        /*
        A[i] + A[j] + i - j
        = 
        A[i] + i + A[j] - j
        在数组中，每个位置的 A[i] + i 和 A[j] - j 是固定的
        题目要求 A[i] + i 出现在  A[j] - j 之前，那么我们可以边移动边算出已知的 left = A[i] + i 和 遇到的 A[j] - j 的和的最大值
        并同时更新最大的 A[i] + i
        
        比如 
        A = 8,1,5,2,6
            0 1 2 3 4
        我们先让 left = A[0] + 0 = 8
        因为 j 只能出现在 i 之后，因此只能 j = 1,因此记录 res = left + A[j] - j = 8 + 1 - 1 = 8
        因为对于后面的 j = 2 位置，我们只能从已经遍历过的 [0, 1] 中选取 A[i] + i ，因此我们选取 [0, 1] 中最大的 A[i] + i（这就是更新 left）

        当 j = 2，获取最大的 res 后，同理，对于 j = 3 来说，我们需要从 [0, 2] 中等到最大的 A[i] + i,
        因此我们同样使用 left = Math.max(left, A[j] + j) 对 left 进行更新

        直到结束
        */
        int len = A.length;
        int left = A[0] + 0;

        int res = 0;
        for(int j = 1; j < len; j++){
            res = Math.max(res, left + A[j] - j);
            left = Math.max(left, A[j] + j);
        }
        return res;
    }
}