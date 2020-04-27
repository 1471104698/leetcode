给定一个二叉搜索树（Binary Search Tree），把它转换成为累加树（Greater Tree)，使得每个节点的值是原来的节点值加上所有大于它的节点值之和。

 

例如：

输入: 原始二叉搜索树:
              4
            /   \
           3     5
				  \
 				   7
				 /   \
			    6     8

输出: 转换为累加树:
              30
            /   \
           33    26
				  \
 				   15
				 /   \
			    21    8

/*
思路：遍历数的顺序为	右 -> 根 -> 左
	我们可以看出，按当前顺序遍历到的节点，它的 值 应该是 root.val 加上之前遍历的节点的值的总和
	因此我们使用 sum 记录之前遍历的节点的总和
		
*/
class Solution {
    int sum = 0;
    public TreeNode bstToGst(TreeNode root) {
        
        //右 -> 根 -> 左
        dfs(root);
        return root;
    }
    private TreeNode dfs(TreeNode root){
        if(root == null){
            return root;
        }
        dfs(root.right);
        sum += root.val;
        root.val = sum;
        dfs(root.left);
        return root;
    }
}