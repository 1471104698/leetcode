给定一个二叉树，它的每个结点都存放着一个整数值。

找出路径和等于给定数值的路径总数。

路径不需要从根节点开始，也不需要在叶子节点结束，但是路径方向必须是向下的（只能从父节点到子节点）。

二叉树不超过1000个节点，且节点数值范围是 [-1000000,1000000] 的整数。

示例：

root = [10,5,-3,3,2,null,11,3,-2,null,1], sum = 8

      10
     /  \
    5   -3
   / \    \
  3   2   11
 / \   \
3  -2   1

返回 3。和等于 8 的路径有:

1.  5 -> 3
2.  5 -> 2 -> 1
3.  -3 -> 11

//思路①、暴力，以当前节点为头 和 以左节点为头 和 以右节点为头
class Solution {
    public int pathSum(TreeNode root, int sum) {
        //每个节点有两种选择，继续承接上面的值，或者 重新计算值   
        if(root == null){
            return 0;
        }
        return dfs(root, 0, sum) + pathSum(root.left, sum) + pathSum(root.right, sum);
    }
    private int dfs(TreeNode root, int sum, int target){
        if(root == null){
            return 0;
        }
        sum += root.val;
        int count = sum == target ? 1 : 0;
        return count + dfs(root.left, sum, target) + dfs(root.right, sum, target);
    }
}

//思路②、前缀和
 /*
            使用前缀和
            思路，某个节点 node2 的前缀和 preSum2 到前面节点 node1 的前缀和 preSum1 的差为 sum，
            那么表示 node2 到 node1 之间节点的和为 sum
            (注意：node 节点的前缀和 包含当前 node 节点值)
            比如
			  10
			 /  \
			5   -3
		   / \    \
		  3   2   11
		 / \   \
		3  -2   1
        sum = 8 
        节点 10 的前缀和为 10
        节点 3 的前缀和为 18
        那么我们找 18 - 8 = 10 ，发现之前存在前缀和为 10 的路径和，刚好是 节点 10，那么节点 10 到 节点 3 之间的值即为 sum = 8
        */
class Solution {
    public int pathSum(TreeNode root, int sum) {
        Map<Integer, Integer> map = new HashMap<>();
        /*
		前缀和为 0 的路径数为 1，防止 根节点的值就是 sum 
		比如 sum = 10，而根节点值就是 10，那么它要求的前缀和就是 10
			  10
			 /  \
			5   -3
		*/
        map.put(0, 1);
        return dfs(root, sum, map, 0);
    }
    /*
    map 存储某个前缀和的个数，因为一条路径可能存在多个相同的前缀和，
		  10
		 /    
		1  
	   /
	 -1     
    比如 node 节点的前缀和为 10，而后面连续两个节点值为 1 和 -1，那么后面节点的前缀和也为 10
    */
    private int dfs(TreeNode root, int sum, Map<Integer, Integer> map, int preSum){
        if(root == null){
            return 0;
        }
        preSum += root.val;

        int count = map.getOrDefault(preSum - sum, 0);

        map.put(preSum, map.getOrDefault(preSum, 0) + 1);

        count += dfs(root.left, sum, map, preSum);
        count += dfs(root.right, sum, map, preSum);
        //回溯
        map.put(preSum, map.get(preSum) - 1);
        return count;
    }
}