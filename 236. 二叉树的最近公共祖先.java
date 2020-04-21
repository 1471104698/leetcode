给定一个二叉树, 找到该树中两个指定节点的最近公共祖先。

百度百科中最近公共祖先的定义为：“对于有根树 T 的两个结点 p、q，最近公共祖先表示为一个结点 x，满足 x 是 p、q 的祖先且 x 的深度尽可能大（一个节点也可以是它自己的祖先）。”

例如，给定如下二叉树:  root = [3,5,1,6,2,0,8,null,null,7,4]
	    3            
	  /   \
	 5     1         
	/ \   / \
   6   2 0   4     
	  / \
	 7   4
  
示例 1:
输入: root = [3,5,1,6,2,0,8,null,null,7,4], p = 5, q = 1
输出: 3
解释: 节点 5 和节点 1 的最近公共祖先是节点 3。

示例 2:
输入: root = [3,5,1,6,2,0,8,null,null,7,4], p = 5, q = 4
输出: 5
解释: 节点 5 和节点 4 的最近公共祖先是节点 5。因为根据定义最近公共祖先节点可以为节点本身。


class Solution {
    public TreeNode lowestCommonAncestor(TreeNode root, TreeNode p, TreeNode q) {
        /*
        如果左右两边各得到 p 和 q，那么最近公共祖先就是当前节点
        如果左右两边任意一边为空，那么最近公共祖先就是不为空的那个节点
        */
        if(root == null || root == q || root == p){
            return root;
        }
        TreeNode left = lowestCommonAncestor(root.left, p, q);
        //提前结束，不再遍历 right
        if(left != null && left != p && left != q){
            return left;
        }
        TreeNode right = lowestCommonAncestor(root.right, p, q);
        if(left != null && right != null){
            return root;
        }
        return left == null ? right : left;
    }
}