给定一个 m x n 的矩阵，如果一个元素为 0，
则将其所在行和列的所有元素都设为 0。请使用原地算法。

示例 1:
输入: 
[
  [1,1,1],
  [1,0,1],
  [1,1,1]
]
输出: 
[
  [1,0,1],
  [0,0,0],
  [1,0,1]
]

示例 2:
输入: 
[
  [0,1,2,0],
  [3,4,5,2],
  [1,3,1,5]
]
输出: 
[
  [0,0,0,0],
  [0,4,5,0],
  [0,3,1,0]
]
进阶:

一个直接的解决方案是使用  O(mn) 的额外空间，但这并不是一个好的解决方案。
一个简单的改进方案是使用 O(m + n) 的额外空间，但这仍然不是最好的解决方案。
你能想出一个常数空间的解决方案吗？


//思路①、使用腐烂橘子的做法，队列存储，但不满足 O(1) 空间

//思路②、原地算法
class Solution {
    public void setZeroes(int[][] matrix) {
        int rlen = matrix.length;
        int llen = matrix[0].length;


        //判断第一行第一列是否置 0
        boolean row = false;
        boolean col = false;
        for(int i = 0; i < rlen; i++){
            for(int j = 0; j < llen; j++){
                //将所在行头和列头标为 2
                if(matrix[i][j] == 0){
                    matrix[i][0] = 0;
                    matrix[0][j] = 0;
                    if(i == 0){
                        row = true;
                    }
                    if(j == 0){
                        col = true;
                    }
                }
            }
        }
        //第一行先别动
        for(int i = 1; i < rlen; i++){
            if(matrix[i][0] == 0){
                //将整行置 0 
                for(int j = 1; j < llen; j++){
                    matrix[i][j] = 0;
                }
            }
        }
        //第一列先别动
        for(int j = 1; j < llen; j++){
            if(matrix[0][j] == 0){
                //将整列置 0 
                for(int i = 1; i < rlen; i++){
                    matrix[i][j] = 0;
                }
            }
        }
        //如果第一行需要置 0
        if(row){
            for(int j = 0; j < llen; j++){
                matrix[0][j] = 0;
            }
        }
        //如果第一列需要置 0
        if(col){
            for(int i = 0; i < rlen; i++){
                matrix[i][0] = 0;
            }
        }
    }
}