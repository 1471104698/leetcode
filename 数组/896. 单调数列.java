如果数组是单调递增或单调递减的，那么它是单调的。

如果对于所有 i <= j，A[i] <= A[j]，那么数组 A 是单调递增的。 如果对于所有 i <= j，A[i]> = A[j]，那么数组 A 是单调递减的。

当给定的数组 A 是单调数组时返回 true，否则返回 false。

 

示例 1：

输入：[1,2,2,3]
输出：true
示例 2：

输入：[6,5,4,4]
输出：true
示例 3：

输入：[1,3,2]
输出：false
示例 4：

输入：[1,2,4,5]
输出：true

class Solution {
    public boolean isMonotonic(int[] A) {
        int len = A.length;
        if(len < 3){
            return true;
        }
        /*
        判断是否出现过递增和递减，如果同时出现过，那么必定不是单调数列
        相邻两数相等的情况我们直接忽略，因为我们不知道到底应该属于递增还是递减的情况
        */
        boolean up = false;
        boolean down = false;
        for(int i = 1; i < len; i++){
            if(A[i - 1] > A[i]) down = true;
            if(A[i - 1] < A[i]) up = true;
            if(down && up) return false;
        }
        return true;
    }
}