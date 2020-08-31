你现在手里有一份大小为 N x N 的「地图」（网格） grid，上面的每个「区域」（单元格）都用 0 和 1 标记好了。其中 0 代表海洋，1 代表陆地，请你找出一个海洋区域，这个海洋区域到离它最近的陆地区域的距离是最大的。

我们这里说的距离是「曼哈顿距离」（ Manhattan Distance）：(x0, y0) 和 (x1, y1) 这两个区域之间的距离是 |x0 - x1| + |y0 - y1| 。

如果我们的地图上只有陆地或者海洋，请返回 -1。

 

示例 1：

1	0	1
0	0	0
1	0	1

输入：[[1,0,1],[0,0,0],[1,0,1]]
输出：2
解释： 
海洋区域 (1, 1) 和所有陆地区域之间的距离都达到最大，最大距离为 2。


示例 2：

1	0	0
0	0	0
0	0	0

输入：[[1,0,0],[0,0,0],[0,0,0]]
输出：4
解释： 
海洋区域 (2, 2) 和所有陆地区域之间的距离都达到最大，最大距离为 4。
 

提示：
1 <= grid.length == grid[0].length <= 100
grid[i][j] 不是 0 就是 1


class Solution {
    public int maxDistance(int[][] grid) {
        int len = grid.length;
        //记录所有陆地位置
        Queue<int[]> queue = new LinkedList<>();
        for(int i = 0 ; i < len; i++){
            for(int j = 0; j < len; j++){
                if(grid[i][j] == 1){
                    queue.add(new int[]{i, j});
                }
            }
        }
        //如果没有陆地或没有海洋，那么返回 -1
        if(queue.size() == 0 || queue.size() == len * len){
            return -1;
        }
        //四个方向扩展
        int[][] pos = {{0, 1}, {0, -1}, {1, 0}, {-1, 0}};
        //记录 BFS 层数
        int n = 0;
        while(!queue.isEmpty()){
            int size = queue.size();
            while(size-- > 0){
                int[] p = queue.poll();
                int x = p[0];
                int y = p[1];
                //遍历陆地 4 个方向
                for(int k = 0; k < 4; k++){
                    //如果陆地是边界那么就不用向外扩展了
                    int xx = x + pos[k][0];
                    int yy = y + pos[k][1];
                    if(xx >= 0 && xx < len && yy >= 0 && yy < len && grid[xx][yy] == 0){
                        queue.add(new int[]{xx, yy});
                        grid[xx][yy] = 1;
                    }
                }
            }
            n++;
        }
        return n - 1;
    }
}