这里有一幅服务器分布图，服务器的位置标识在 m * n 的整数矩阵网格 grid 中，1 表示单元格上有服务器，0 表示没有。

如果两台服务器位于同一行或者同一列，我们就认为它们之间可以进行通信。

请你统计并返回能够与至少一台其他服务器进行通信的服务器的数量。

输入：grid = [[1,1,0,0],[0,0,1,0],[0,0,1,0],[0,0,0,1]]
输出：4
解释：第一行的两台服务器互相通信，第三列的两台服务器互相通信，但右下角的服务器无法与其他服务器通信。

class Solution {
    public int countServers(int[][] grid) {
        /*
        计算每行每列的元素个数
        然后再次遍历数组，判断当前服务器所在行或列是否存在 2 台以上服务器，如果存在，那么当前服务器能够进行通信，c++
        */
        int rlen = grid.length;
        int llen = grid[0].length;
        
        int[] row = new int[rlen];
        int[] col = new int[llen];
        for(int i = 0; i < rlen; i++){
            for(int j = 0; j < llen; j++){
                if(grid[i][j] == 1){
                    row[i]++;
                    col[j]++;
                }
            }
        }
        int c = 0;
        for(int i = 0; i < rlen; i++){
            for(int j = 0; j < llen; j++){
                if(grid[i][j] == 1 && (row[i] > 1 || col[j] > 1)){
                    c++;
                }
            }
        }
        return c;
    }
}