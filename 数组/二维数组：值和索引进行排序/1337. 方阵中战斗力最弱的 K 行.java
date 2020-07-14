给你一个大小为 m * n 的方阵 mat，方阵由若干军人和平民组成，分别用 1 和 0 表示。

请你返回方阵中战斗力最弱的 k 行的索引，按从最弱到最强排序。

如果第 i 行的军人数量少于第 j 行，或者两行军人数量相同但 i 小于 j，那么我们认为第 i 行的战斗力比第 j 行弱。

军人 总是 排在一行中的靠前位置，也就是说 1 总是出现在 0 之前。

 

示例 1：

输入：mat = 
[[1,1,0,0,0],
 [1,1,1,1,0],
 [1,0,0,0,0],
 [1,1,0,0,0],
 [1,1,1,1,1]], 
k = 3
输出：[2,0,3]
解释：
每行中的军人数目：
行 0 -> 2 
行 1 -> 4 
行 2 -> 1 
行 3 -> 2 
行 4 -> 5 
从最弱到最强对这些行排序后得到 [2,0,3,1,4]

class Solution {
    public int[] kWeakestRows(int[][] mat, int k) {
        /*
            二维数组
        */
        int len = mat.length;
        int[][] temp = new int[len][2];
        for(int i = 0; i < len; i++){
            int sum = 0;
            for(int val : mat[i]){
                if(val == 1){
                    sum++;
                }
            }
            temp[i][0] = sum;
            temp[i][1] = i;
        }
        Arrays.sort(temp, (a, b) -> a[0] == b[0] ? a[1] - b[1] : a[0] - b[0]);

        int[] arr = new int[k];
        for(int i = 0; i < k; i++){
            arr[i] = temp[i][1];
        }
        return arr;
    }
}