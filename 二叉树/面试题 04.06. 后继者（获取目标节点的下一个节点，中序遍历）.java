设计一个算法，找出二叉搜索树中指定节点的“下一个”节点（也即中序后继）。

如果指定节点没有对应的“下一个”节点，则返回null。

示例 1:

输入: root = [2,1,3], p = 1

  2
 / \
1   3

输出: 2
示例 2:

输入: root = [5,3,6,2,4,null,null,1], p = 6

      5
     / \
    3   6
   / \
  2   4
 /   
1

输出: null

/**
 * Definition for a binary tree node.
 * public class TreeNode {
 *     int val;
 *     TreeNode left;
 *     TreeNode right;
 *     TreeNode(int x) { val = x; }
 * }
 */
class Solution {
    boolean flag = false;
    TreeNode next;
    public TreeNode inorderSuccessor(TreeNode root, TreeNode p) {
        //直接中序遍历
        dfs(root, p);
        return next;
    }
    private void dfs(TreeNode root, TreeNode p){
        if(root == null){
            return;
        }
        dfs(root.left, p);
        if(next != null){
            return;
        }
        /*
        注意 flag 和 root == p 的判断顺序不能换
        如果换成如下代码：
        if(root == p){
            flag = true;
        }
        if(flag){
            next = root;
        }
        那么在判断完 root == p 设置了 flag = true 后，下一步会 if(flag) 会成立，直接将 root 赋值给 next
        */
        if(flag){
            next = root;
        }
        if(root == p){
            flag = true;
        }

        dfs(root.right, p);
    }
}