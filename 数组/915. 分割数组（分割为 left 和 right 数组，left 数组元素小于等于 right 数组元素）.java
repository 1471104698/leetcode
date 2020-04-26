给定一个数组 A，将其划分为两个不相交（没有公共元素）的连续子数组 left 和 right， 使得：

left 中的每个元素都小于或等于 right 中的每个元素。
left 和 right 都是非空的。
left 要尽可能小。
在完成这样的分组后返回 left 的长度。可以保证存在这样的划分方法。

 

示例 1：

输入：[5,0,3,8,6]
输出：3
解释：left = [5,0,3]，right = [8,6]
示例 2：

输入：[1,1,1,0,6,12]
输出：4
解释：left = [1,1,1,0]，right = [6,12]
 

提示：

2 <= A.length <= 30000
0 <= A[i] <= 10^6
可以保证至少有一种方法能够按题目所描述的那样对 A 进行划分。


class Solution {
    public int partitionDisjoint(int[] A) {
        /*
        滑动窗口
        题意：left 的最大值必须 小于等于 right 的最小值
        比如 A = {3,3,5,4,0,8,6}
        我们可以看出来 left = {3,3,5,4,0}，通过滑动窗口怎么得到？
        如果中间没有那个 0 的出现，那么 left = {3}，就是这个 0 的出现，导致 left 多了这么多元素
        因为 原本 left = {3} ，最大值为 3，直到遇到 0，这个最大值比 0 大，不满足 条件，因此我们需要将 从头 到 该 0 位置的元素都包含进 left
        而当更新了 left 长度，我们就需要更新 left 数组的最大值，而这个最大值是 {3,3,5,4,0} 中的最大值
        但是我们已经遍历过去了，怎么知道其中的最大值？
        我们可以在遍历的过程中记录遇到的最大值 tempVal，后续直接将 left 数组的最大值 maxVal 换为 tempVal 即可

        */
        int len = A.length;

        //left 数组的右边界
        int end = 0;

        int right = 0;

        int maxVal = A[0];
        int tempVal = A[0];

        while(right < len){
            tempVal = Math.max(tempVal, A[right]);
            //0 3 5 4 8 6
            if(maxVal > A[right]){
                end = right;
                maxVal = tempVal;
            }
            right++;
        }
        return end + 1;
    }
}