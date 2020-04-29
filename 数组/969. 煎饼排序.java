给定数组 A，我们可以对其进行煎饼翻转：我们选择一些正整数 k <= A.length，然后反转 A 的前 k 个元素的顺序。我们要执行零次或多次煎饼翻转（按顺序一次接一次地进行）以完成对数组 A 的排序。

返回能使 A 排序的煎饼翻转操作所对应的 k 值序列。任何将数组排序且翻转次数在 10 * A.length 范围内的有效答案都将被判断为正确。

 

示例 1：

输入：[3,2,4,1]
输出：[4,2,4,3]
解释：
我们执行 4 次煎饼翻转，k 值分别为 4，2，4，和 3。
初始状态 A = [3, 2, 4, 1]
第一次翻转后 (k=4): A = [1, 4, 2, 3]
第二次翻转后 (k=2): A = [4, 1, 2, 3]
第三次翻转后 (k=4): A = [3, 2, 1, 4]
第四次翻转后 (k=3): A = [1, 2, 3, 4]，此时已完成排序。 
示例 2：

输入：[1,2,3]
输出：[]
解释：
输入已经排序，因此不需要翻转任何内容。
请注意，其他可能的答案，如[3，3]，也将被接受。


class Solution {
    public List<Integer> pancakeSort(int[] A) {
        /*
            开始让 end = A.length - 1，即最后一个元素的索引位置
            每次 从 [0, end] 中找到最大值的位置 maxIdx ，然后将 [0, maxIdx] 进行翻转，然后将 [0, end] 进行翻转，这样最大值就到了最后了
            然后 end--，即每次都能够缩短一个长度，直到只剩下一个值，即 end == 0 为止
        */
        
        List<Integer> res = new ArrayList<>();
        //end == 0 表示剩下一个元素，那么无需进行翻转
        for(int end = A.length - 1; end >= 1; end--){
            //查找最大值
            int maxIdx = findMax(A, end);
            if(maxIdx == end){
                continue;
            }
            reverse(A, 0, maxIdx);
            reverse(A, 0, end);
            res.add(maxIdx + 1);
            res.add(end + 1);
        }
        return res;
    }
    //查找最大值的位置
    private int findMax(int[] A, int end){
        int max = 0;
        for(int i = 1; i <= end; i++){
            if(A[max] < A[i]){
                max = i;
            }
        }
        return max;
    }

    //将 [start, end] 进行翻转
    private void reverse(int[] A, int start, int end){
        while(start < end){
            int temp = A[start];
            A[start++] = A[end];
            A[end--] = temp;
        }
    }
}