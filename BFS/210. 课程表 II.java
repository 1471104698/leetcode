现在你总共有 n 门课需要选，记为 0 到 n-1。

在选修某些课程之前需要一些先修课程。 例如，想要学习课程 0 ，你需要先完成课程 1 ，我们用一个匹配来表示他们: [0,1]

给定课程总量以及它们的先决条件，返回你为了学完所有课程所安排的学习顺序。

可能会有多个正确的顺序，你只要返回一种就可以了。如果不可能完成所有课程，返回一个空数组。

示例 1:

输入: 2, [[1,0]] 
输出: [0,1]
解释: 总共有 2 门课程。要学习课程 1，你需要先完成课程 0。因此，正确的课程顺序为 [0,1] 。
示例 2:

输入: 4, [[1,0],[2,0],[3,1],[3,2]]
输出: [0,1,2,3] or [0,2,1,3]
解释: 总共有 4 门课程。要学习课程 3，你应该先完成课程 1 和课程 2。并且课程 1 和课程 2 都应该排在课程 0 之后。
     因此，一个正确的课程顺序是 [0,1,2,3] 。另一个正确的排序是 [0,2,1,3] 。
说明:

输入的先决条件是由边缘列表表示的图形，而不是邻接矩阵。详情请参见图的表示法。
你可以假定输入的先决条件中没有重复的边。

class Solution {
    public int[] findOrder(int numCourses, int[][] prerequisites) {
/*
        我们先记录每个节点的入度，以及使用 map 记录每个节点所能到达的其他节点

        然后我们将入度为 0 的节点存储进队列中，将它和它所能到达的节点 next 的通路断开，即 next 的入度 -1，
        当减为 0 的时候，表示入度为 0，那么添加进队列中
        */

        List<Integer>[] lists = new List[numCourses];

        //存储每个节点的度
        int[] points = new int[numCourses];
        for(int[] p : prerequisites){
            points[p[0]]++;
            if(lists[p[1]] == null){
                lists[p[1]] = new ArrayList<>();
            }
            //记录某个点能够到达的其他的点
            lists[p[1]].add(p[0]);
        }

        Queue<Integer> queue = new LinkedList<>();

        //找到入度为 0 的点，入队
        for(int i = 0; i < numCourses; i++){
            if(points[i] == 0){
                queue.add(i);
            }
        }

        int[] res = new int[numCourses];
        int idx = 0;
        
        int visited = 0;
        while(!queue.isEmpty()){
            int size = queue.size();
            while(size-- > 0){
                int p = queue.poll();
                visited++;
                res[idx++] = p;
                List<Integer> list = lists[p];
                if(list == null){
                    continue;
                }
                for(int next : list){
                    points[next]--;
                    if(points[next] == 0){
                        queue.add(next);
                    }
                }
            }
        }
        return visited == numCourses ?  res : new int[0];
    }
}