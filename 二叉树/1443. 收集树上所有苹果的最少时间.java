给你一棵有 n 个节点的无向树，节点编号为 0 到 n-1 ，它们中有一些节点有苹果。通过树上的一条边，需要花费 1 秒钟。你从 节点 0 出发，请你返回最少需要多少秒，可以收集到所有苹果，并回到节点 0 。

无向树的边由 edges 给出，其中 edges[i] = [fromi, toi] ，表示有一条边连接 from 和 toi 。除此以外，还有一个布尔数组 hasApple ，其中 hasApple[i] = true 代表节点 i 有一个苹果，否则，节点 i 没有苹果。

 

示例 1：
//（图片地址）https://assets.leetcode-cn.com/aliyun-lc-upload/uploads/2020/05/10/min_time_collect_apple_1.png
输入：n = 7, edges = [[0,1],[0,2],[1,4],[1,5],[2,3],[2,6]], hasApple = [false,false,true,false,true,true,false]
输出：8 


解释：上图展示了给定的树，其中红色节点表示有苹果。一个能收集到所有苹果的最优方案由绿色箭头表示。

        /*
        edges.length == n-1 意味着 对于任意一个节点 node, 其他节点中只有一个节点 能够到达这个 node 节点
        即只存在一条路径，只存在一个父节点，即是标准的树状的，而不是四通八达的图形
        
        1、我们可以求苹果到根节点的距离 res，然后 res * 2（因为先从根节点到苹果，再从苹果返回根节点，两倍距离）
        2、我们记录已经遍历过的点，比如我们 1 4 有苹果，那么我们已经遍历过 1 了，即记录了 1 到 0 的距离
            那么我们遍历到 4 的时候，我们无需计算 4 到 0 的距离，只需要计算 4 到 1 的距离即可
            即 对于任意一个苹果节点，我们不一定需要记录它到根节点的距离，只需要记录到遍历过的节点的距离即可，防止重复边的添加
            0           
          /   \
         1     2
        / \   
       3   4
       
       过程：记录每个节点的父节点，然后找到苹果的位置，从该位置开始往根节点的位置跑

            0           
          /   \
         1     2
        / \   / \
       3   4 5   6

       hasApple = {1, 3, 6}
       根据题意，编号节点是 [0, n - 1]
       我们从 1 开始遍历到 n - 1，找到有苹果的节点，然后开始往根节点走
       过程如下：
       1、遍历到 1， 1 上有苹果，并且 visited[1] = false，即 1 未访问， 因此路径 + 1,并且设置为已访问 然后获取它的父节点 0，发现 visited[0] = true，因此终止
       2、遍历到 2，2 上没苹果，跳过
       3、遍历到 3， 3 上有苹果，并且 visited[3] = false, 因此路径 + 1，然后获取它的父节点 1，发现 visited[1] = true，因此终止
       4、遍历到 4，4 上没苹果，跳过
       5、遍历到 5，发现 5 上没苹果，跳过
       6、遍历到 6，发现有苹果，并且 visited[6] = false， 因此路径 + 1，然后获取父节点 2，发现 visited[2] = false，因此路径 + 1，
                然后获取 2 的父节点，发现 visited[0] = true,因此终止
        至此，所有节点遍历完毕，由于是路程是双向的，因此 return res * 2
        */
		
class Solution {
    public int minTime(int n, int[][] edges, List<Boolean> hasApple) {

        //记录 i 号节点的父节点编号, [3, 5] 表示 5 的父节点是 3， 即 edg[0] 是 edg[1] 的父节点
        int[] fatherIdx = new int[n];
        for(int[] edg : edges){
            fatherIdx[edg[1]] = edg[0];
        }
        
        //记录遍历过的节点
        boolean[] visited = new boolean[n];
        //根节点设置为 true
        visited[0] = true;
        
        int res = 0;
        for(int i = 1; i < n; i++){
            //从前面的开始找苹果的位置
            if(hasApple.get(i)){
                int temp = i;
                //如果当前节点没有访问过，那么
                while(!visited[temp]){
                    //设置为已访问
                    visited[temp] = true;
                    //获取父节点
                    temp = fatherIdx[temp];
                    res++;
                }
            }
        }
        return res * 2;
    }
}
