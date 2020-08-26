# [vivo笔试：手机屏幕解锁模式](<https://www.nowcoder.com/profile/302235906/wrongset/320914194?page=1&offset=0&tags=>)

*![image.png](https://pic.leetcode-cn.com/352eaa1987d855b138ca747ebe938ceeb5ff28d284729f5acd669007151de895-image.png)*

## 题意描述

现有一个 3x3 规格的 Android 智能手机锁屏程序和两个正整数 m 和 n ，请计算出使用最少m 个键和最多 n个键可以解锁该屏幕的所有有效模式总数。

即 给定 两个正整数 m 和 n，要求我们求出 满足 图形锁屏点的个数 大于等于 m ，小于等于 n 的图形方案数

### **注意：**

- 1、即使图形相同，但是只要点的连续顺序不同，也算作一个方案

- 2、我们不能越过某个点去连接下一个点

  ```java
  (0,0) (0,1) (0,2)
  (1,0) (1,1) (1,2)
  (2,0) (2,1) (2,2)
  当我们从 (0,0) 出发时，如果要连接 (2,2) 点，那么不能直接越过 (1,1)点去连接 (2,2) 点
  即如果我们要连接 (0,0) 和 (2,2)，那么路径上的 (1,1) 也会被连接
  
  但是，如果从 (0,0) 点出发，即使 (1,1) 点已经连接了，我们也可以直接越过 (1,1) 点，再去连接 (2,2) 点
  ```

- 3、我们可以斜着连接

  ```java
  (0,0) (0,1) (0,2)
  (1,0) (1,1) (1,2)
  (2,0) (2,1) (2,2)
  比如上面的 (0,0) 点，我们可以斜着不碰到其他的点去连接 (2,1) 点，这也是一种连接方式
  ```

综上，每个点除了对应的 8 个方向，即上下左右和对角线 4 个方向，我们可以还存在 斜着连接的 8 个方向

总共存在 16 个方向



## 示例

```java
输入：m,n
代表允许解锁的最少m个键和最多n个键

输出：
满足m和n个键数的所有有效模式的总数
```

![img](https://uploadfiles.nowcoder.com/images/20200219/317905_1582122504146_33DA1E7D457581C46D48133FD48253F3)



## 方法一：dfs

### 实现思路

**图形密码即使图案相同，但是顺序不同的话也算作一种**

dfs，以每个位置作为起点，然后走 [n, m] 个点
当走到无路可走的时候，如果走的点小于 m，那么该路径不满足要求
当已经走的点 > n 的时候，那么表示接下来的路径也不满足要求，直接返回

当我们发现周围 几个点都已经走过了，如果是普通的 dfs，这时候已经返回了
但是，我们可以越过已经访问过的点，继续往下一个点走，因此我们需要判断
8 个方向第 2 个点是否已经访问过了，如果没有，那么可以继续访问

**注意：只有我们发现某个方向上的点访问过了才可以越过该点**
**如果该方向上的第一个点还没有访问，那么我们不能直接越过**



### 实现代码

```java
import java.util.*;

public class Solution {
    /**
     * 实现方案
     * @param m int整型 最少m个键
     * @param n int整型 最多n个键
     * @return int整型
     */
    public int solution (int m, int n) {
        //范围处理
        if(m > n){
            return 0;
        }
        m = Math.max(0, m);
        n = Math.min(9, n);
        if(n == 0){
            return 0;
        }
        // write code here
        res = 0;
        visited = new boolean[3][3];
        for(int i = 0; i < 3; i++){
            for(int j = 0; j < 3; j++){
                dfs(i, j, m, n, 1);
            }
        }
        return res;
    }
    static int res;
    static boolean[][] visited;
    int[][] pos = {
            {1, 0},{1, 1},{1, -1}, {-1, 0}, {-1, -1}, {-1, 1}, {0, 1}, {0, -1},
            {1, 2}, {2, 1}, {-1, 2}, {-1, -2}, {-2, -1}, {-2, 1}, {1, -2}, {2, -1}
    };
    private void dfs(int i, int j, int m, int n, int p){
        if(p >= m){
            res++;
        }
        //当访问个数大于等于 n 个，那么已经足够了，无需继续访问
        if(p >= n){
            return;
        }
        visited[i][j] = true;
        //8 个方向走一遍
        for(int[] po : pos){
            int x = i + po[0];
            int y = j + po[1];
            if(!isEvil(x, y)){
                if(!visited[x][y]){
                    dfs(x, y, m, n, p + 1);
                }else{
                    //越过同方向上的点
                    int xx = x + po[0];
                    int yy = y + po[1];
                    if(!isEvil(xx, yy) && !visited[xx][yy]){
                        //越过 (x, y) 点，访问下一个点
                        dfs(xx, yy, m, n, p + 1);
                    }
                }
            }
        }
        visited[i][j] = false;
    }
    private boolean isEvil(int i, int j){
        return i < 0 || i >= 3 || j < 0 || j >= 3;
    }
}
```

