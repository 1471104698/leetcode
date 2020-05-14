你这个学期必须选修 numCourse 门课程，记为 0 到 numCourse-1 。

在选修某些课程之前需要一些先修课程。 例如，想要学习课程 0 ，你需要先完成课程 1 ，我们用一个匹配来表示他们：[0,1]

给定课程总量以及它们的先决条件，请你判断是否可能完成所有课程的学习？

 

示例 1:

输入: 2, [[1,0]] 
输出: true
解释: 总共有 2 门课程。学习课程 1 之前，你需要完成课程 0。所以这是可能的。
示例 2:

输入: 2, [[1,0],[0,1]]
输出: false
解释: 总共有 2 门课程。学习课程 1 之前，你需要先完成​课程 0；并且学习课程 0 之前，你还应先完成课程 1。这是不可能的。
 

提示：

输入的先决条件是由 边缘列表 表示的图形，而不是 邻接矩阵 。详情请参见图的表示法。
你可以假定输入的先决条件中没有重复的边。
1 <= numCourses <= 10^5

class Solution {
    public boolean canFinish(int numCourses, int[][] prerequisites) {
        /*
        我们先记录每个节点的入度，以及使用 map 记录每个节点所能到达的其他节点

        然后我们将入度为 0 的节点存储进队列中，将它和它所能到达的节点 next 的通路断开，即 next 的入度 -1，
        当减为 0 的时候，表示入度为 0，那么添加进队列中
        */
		
		//这里本应使用 map ,但因为节点值已经确定了是在 [0, numCourses - 1] ，那么我们可以将该节点值作为下标值
        List<Integer>[] lists = new List[numCourses];

        //存储每个节点的度
        int[] points = new int[numCourses];
        for(int[] p : prerequisites){
            points[p[1]]++;
            if(lists[p[0]] == null){
                lists[p[0]] = new ArrayList<>();
            }
            //记录某个点能够到达的其他的点
            lists[p[0]].add(p[1]);
        }

        Queue<Integer> queue = new LinkedList<>();

        //找到入度为 0 的点，入队
        for(int i = 0; i < numCourses; i++){
            if(points[i] == 0){
                queue.add(i);
            }
        }

        //记录访问过的节点个数
        int visited = 0;

        while(!queue.isEmpty()){
            int size = queue.size();
            while(size-- > 0){
                int p = queue.poll();
                visited++;
				 /*
                当 list 为空，表示该节点没有通向任何一个节点，即自己就是道路的末尾
                比如用例：2, [[1,0]] ， 0 没有通向任何节点
                */
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
        return visited == numCourses;
    }
}