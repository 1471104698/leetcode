让我们一起来玩扫雷游戏！

给定一个代表游戏板的二维字符矩阵。 'M' 代表一个未挖出的地雷，'E' 代表一个未挖出的空方块，'B' 代表没有相邻（上，下，左，右，和所有4个对角线）地雷的已挖出的空白方块，数字（'1' 到 '8'）表示有多少地雷与这块已挖出的方块相邻，'X' 则表示一个已挖出的地雷。

现在给出在所有未挖出的方块中（'M'或者'E'）的下一个点击位置（行和列索引），根据以下规则，返回相应位置被点击后对应的面板：

如果一个地雷（'M'）被挖出，游戏就结束了- 把它改为 'X'。
如果一个没有相邻地雷的空方块（'E'）被挖出，修改它为（'B'），并且所有和其相邻的方块都应该被递归地揭露。
如果一个至少与一个地雷相邻的空方块（'E'）被挖出，修改它为数字（'1'到'8'），表示相邻地雷的数量。
如果在此次点击中，若无更多方块可被揭露，则返回面板。
 

示例 1：
输入: 

[['E', 'E', 'E', 'E', 'E'],
 ['E', 'E', 'M', 'E', 'E'],
 ['E', 'E', 'E', 'E', 'E'],
 ['E', 'E', 'E', 'E', 'E']]

Click : [3,0]

输出: 

[['B', '1', 'E', '1', 'B'],
 ['B', '1', 'M', '1', 'B'],
 ['B', '1', '1', '1', 'B'],
 ['B', 'B', 'B', 'B', 'B']]
 
示例 2：
输入: 

[['B', '1', 'E', '1', 'B'],
 ['B', '1', 'M', '1', 'B'],
 ['B', '1', '1', '1', 'B'],
 ['B', 'B', 'B', 'B', 'B']]

Click : [1,2]

输出: 

[['B', '1', 'E', '1', 'B'],
 ['B', '1', 'X', '1', 'B'],
 ['B', '1', '1', '1', 'B'],
 ['B', 'B', 'B', 'B', 'B']]


class Solution {
    public char[][] updateBoard(char[][] board, int[] click) {
        /*
        题意是什么意思？
        我们从 click 位置开始，如果 click 是雷，那么直接 click 位置修改为 X 然后结束游戏 return

        每次我们计算的当前位置只能是 E，即还未挖出，如果不是 E（即 B、数字、M） 那么直接返回
        不然的话，我们遍历周围 8 个方向，获取雷的数量 c
        如果 c 不为 0，那么将当前位置设置为 c，然后直接返回，不再递归 8 个方向
        如果 c 为 0，那么我们将当前位置设置为 B，然后递归 8 个方向
        */
        int x = click[0];
        int y = click[1];
        if(board[x][y] == 'M'){
            board[x][y] = 'X';
            return board;
        }
        dfs(board, x, y);
        return board;
    }

    int[][] pos = {{1,0},{-1,0},{0,1},{0,-1},{1,1},{1,-1},{-1,1},{-1,-1}};

    private void dfs(char[][] board, int x, int y){
        int rlen = board.length;
        int llen = board[0].length;
    
        if(isTransboundary(x, y, rlen, llen) || board[x][y] != 'E'){
            return;
        }
        //获取周边雷的数量
        int c = 0;
        for(int[] p : pos){
            int xx = x + p[0];
            int yy = y + p[1];
            if(!isTransboundary(xx, yy, rlen, llen) && board[xx][yy] == 'M'){
                c++;
            }
        }
        //周边没有雷，那么才递归 8 个方向，如果周边存在雷，那么返回，不递归
        if(c == 0){
            board[x][y] = 'B';
            //递归 8 个方向
            for(int[] p : pos){
                dfs(board, x + p[0], y + p[1]);
            }
        }else{
            //将该位置设置为雷的数量
            board[x][y] = (char)('0' + c);
        }
    }

    //判断是否越界，越界返回 true
    private boolean isTransboundary(int x, int y, int rlen, int llen){
        return x < 0 || x == rlen || y < 0 || y == llen;
    }
}