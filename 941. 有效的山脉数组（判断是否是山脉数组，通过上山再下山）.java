给定一个整数数组 A，如果它是有效的山脉数组就返回 true，否则返回 false。

让我们回顾一下，如果 A 满足下述条件，那么它是一个山脉数组：

A.length >= 3
在 0 < i < A.length - 1 条件下，存在 i 使得：
A[0] < A[1] < ... A[i-1] < A[i]
A[i] > A[i+1] > ... > A[A.length - 1]

示例 1：

输入：[2,1]
输出：false
示例 2：

输入：[3,5,5]
输出：false
示例 3：

输入：[0,3,2,1]
输出：true
 

提示：

0 <= A.length <= 10000
0 <= A[i] <= 10000 

/*
思路：正序遍历，看作上山，得到山顶元素
然后再继续遍历，看作下山，最后判断是否到达山脚，如果不能，那么不是山脉数组

特殊情况：
1、nums = {1,2,3,4,5,6} ，最后 i == len ，表示只能上山不能下山
2、nums = {6,5,4,3,2,1}，  i == 1，上不了山
*/
class Solution {
    public boolean validMountainArray(int[] A) {
        int len = A.length;
        
        int i = 1;
        //先上山
        for(; i < len; i++){
            if(A[i - 1] >= A[i]){
                break;
            }
        }
        if(i == 1 || i == len){
            return false;
        }
        //再下山
        for(; i < len; i++){
            if(A[i - 1] <= A[i]){
                break;
            }
        }
        //下到山脚就是赢
        return i == len;
    }
}