节点间通路。给定有向图，设计一个算法，找出两个节点之间是否存在一条路径。

示例1:

 输入：n = 3, graph = [[0, 1], [0, 2], [1, 2], [1, 2]], start = 0, target = 2
 输出：true
示例2:

 输入：n = 5, graph = [[0, 1], [0, 2], [0, 4], [0, 4], [0, 1], [1, 3], [1, 4], [1, 3], [2, 3], [3, 4]], start = 0, target = 4
 输出 true
提示：

节点数量n在[0, 1e5]范围内。
节点编号大于等于 0 小于 n。
图中可能存在自环和平行边。

class Solution {
    public boolean findWhetherExistsPath(int n, int[][] graph, int start, int target) {
        /*
        BFS
        使用 BFS，那么就需要队列，这相当于是求路径
		遍历的过程中我们肯定需要知道某个点它所能到达哪些点，我们总不能在队列遍历过程中对于每个点都去遍历一遍数组吧
		因此，我们可以先做预处理，提前先将每个点所能到达的点存储起来，后续直接获取即可
        
		queue 存储的是下一次循环需要看的点，比如最开始我们存储 start，那么我们就需要得到 start 下一步所能到达的点
		然后将这些点存储到 queue 中，下一次循环就遍历这些点，继续获取它们下一步所能到达的点
		
		当遍历过程中出现了 target，表示能够到达目的地
		
		怎么防止对某些点的重复访问呢？
		方法一：建立一个 set 存储遍历过的点 或 建立一个 boolean 数组存储访问过的点
		方法二：我们预处理是将每个点下一步所能到达的点给存储到 list 中，而我们能获取这个 list 如果为空，
				表示它不能通往任何其他节点，那么同样的，也可以当作是该点已经访问过了
				因此，我们访问过一个节点，就将该节点的 list 置空，下次访问到它的时候，获取的 list 为空，自然就跳过了
        */
        Map<Integer, List<Integer>> map = new HashMap<>();
        for(int[] p : graph){
            if(!map.containsKey(p[0])){
                map.put(p[0], new ArrayList<>());
            }
            map.get(p[0]).add(p[1]);
        }

        //可以先将
        Queue<Integer> queue = new LinkedList<>();

        queue.add(start);

        while(!queue.isEmpty()){
            int size = queue.size();
            while(size-- > 0){
                int st = queue.poll();
                List<Integer> list = map.get(st);
                //当 list == null ，表示当前 st 不能通往其他任何一个节点 或 之前已经访问过了
                if(list == null){
                    continue;
                }
                for(int num : list){
                    if(num == target){
                        return true;
                    }
                    queue.add(num);
                }
                map.put(st, null);
            }
        }
        return false;
    }
}