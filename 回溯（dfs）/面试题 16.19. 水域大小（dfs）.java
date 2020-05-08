你有一个用于表示一片土地的整数矩阵land，该矩阵中每个点的值代表对应地点的海拔高度。若值为0则表示水域。由垂直、水平或对角连接的水域为池塘。池塘的大小是指相连接的水域的个数。编写一个方法来计算矩阵中所有池塘的大小，返回值需要从小到大排序。

示例：

输入：
[
  [0,2,1,0],
  [0,1,0,1],
  [1,1,0,1],
  [0,1,0,1]
]
输出： [1,2,4]
提示：

0 < len(land) <= 1000
0 < len(land[i]) <= 1000

class Solution {
    public int[] pondSizes(int[][] land) {
        int rlen = land.length;
        int llen = land[0].length;
        List<Integer> res = new ArrayList<>();
        //查找水域，然后从该位置开始向四周蔓延
        for (int i = 0; i < rlen; i++) {
            for (int j = 0; j < llen; j++) {
                if (land[i][j] == 0) {
                    res.add(dfs(land, i, j));
                }
            }
        }
        int[] arr = new int[res.size()];
        for (int i = 0; i < res.size(); i++) {
            arr[i] = res.get(i);
        }
        Arrays.sort(arr);
        return arr;
    }

    private int dfs(int[][] land, int i, int j) {
        int rlen = land.length;
        int llen = land[0].length;
        //越界或不为水域，返回 0
        if (i < 0 || i == rlen || j < 0 || j == llen || land[i][j] != 0) {
            return 0;
        }
        int count = 1;
        //将遍历过的水域置为陆地，防止重复遍历
        land[i][j] = 1;
        //向 8 个方向遍历
        int[][] pos = {{1, 0}, {1, 1}, {1, -1}, {-1, 0}, {-1, -1}, {-1, 1}, {0, 1}, {0, -1}};
        for (int[] p : pos) {
            count += dfs(land, i + p[0], j + p[1]);
        }
        return count;
    }
}