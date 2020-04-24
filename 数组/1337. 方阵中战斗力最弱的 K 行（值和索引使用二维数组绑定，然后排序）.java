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
            使用一个二维数组 list
            list[i][0] 记录 mat 第 i 行 的战斗力
            list[i][1] 记录 mat 第 i 行的索引 i
            然后我们对 list 进行排序，对战斗力进行升序排序，如果战斗力相同，那么对索引进行升序排序
            然后我们获取前 k 个即可

            使用 二维记录索引是为了方便对战斗力排序后还能准确的获取到属于哪一行
        */
        int len = mat.length;
        int[][] list = new int[len][2];
        for(int i = 0; i < len; i++){
            list[i][0] = getCount(mat[i]);
            list[i][1] = i;
        }
        Arrays.sort(list, (a, b) -> a[0] == b[0] ? a[1] - b[1] : a[0] - b[0]);
        
        int[] res = new int[k];
        for(int i = 0; i < k; i++){
            res[i] = list[i][1];
        }
        return res;
    }
    private int getCount(int[] arr){
        int c = 0;
        for(int num : arr){
            if(num == 0){
                break;
            }
            c++;
        }
        return c;
    }
}