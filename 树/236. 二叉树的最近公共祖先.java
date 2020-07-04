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
		有这么一棵树
			    3            
			  /   \
			 5     1         
			/ \   / \
		   6   2 0   4  
		约定：当 root == null || root == p || root == q 的时候就直接返回，不再继续往下遍历
		理由如下：
		当 p = 5, q = 6 时，p 和 q 的父子关系，那么当我们遍历过去的时候，
			对于节点 3，它的左子树肯定最先遇到 p = 5，直接返回 p，而右子树肯定返回 null，这时候一边为 null， 一边不为空，意味着另一个节点 q 肯定是 p 的子节点，那么 LCA 就是 p = 5
		当 p = 5, q = 4 时， p 和 q 不是父子关系，那么对于节点 3，它的左子树返回 P, 右子树返回 q, 两边都不为空，意味着节点 3 就是它们的 LCA
		
		综上
        如果左右两边各得到 p 和 q，那么最近公共祖先就是当前节点
        如果左右两边任意一边为空，那么最近公共祖先就是不为空的那个节点
		
		提前结束遍历条件：
		但左子树返回结果不为空时，并且不是 p 或者 q，意味着返回的就是 p 和 q 的 LCA，那么我们直接返回这个节点，而不需要再遍历右子树
		比如上面的树，p = 6, q = 2，它们的 LCA 是 5，对于节点 3 来说，它的左子树返回的结果是 5，而这个 5 不是 p 也不是 q,那么就是 p 和 q 的 LCA，那么就不用再继续遍历右子树了
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