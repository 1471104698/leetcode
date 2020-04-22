给定一个二叉树和一个目标和，找到所有从根节点到叶子节点路径总和等于给定目标和的路径。

说明: 叶子节点是指没有子节点的节点。

示例:
给定如下二叉树，以及目标和 sum = 22，

              5
             / \
            4   8
           /   / \
          11  13  4
         /  \    / \
        7    2  5   1
返回:

[
   [5,4,11,2],
   [5,8,4,5]
]


class Solution {
    //这里要求的是根节点到叶子节点
    List<List<Integer>> res;
    public List<List<Integer>> pathSum(TreeNode root, int sum) {
        res = new ArrayList<>();
        dfs(root, sum, new ArrayList<>());
        return res;
    }
    //频繁使用插入和删除，因此使用 链表
    private void dfs(TreeNode root, int sum, List<Integer> list){
        if(root == null){
            //注意：不能在最后为 null 的时候添加，否则，如果上一个节点就是叶子节点并且满足 sum == 0，那么因为它左右节点为 null
            //那么会进入这里两个 null，重复添加两次
            // if(sum == 0){
            //     res.add(new ArrayList<>(list));
            // }
            return;
        }
        list.add(root.val);
        sum -= root.val;
        //如果左右节点有一个不为空，那么可以进行 dfs
        if(root.left != null || root.right != null){
            dfs(root.left, sum, list);
            dfs(root.right, sum, list);
        }else if(sum == 0){
            res.add(new ArrayList<>(list));
        }
        list.remove(list.size() - 1);
    }
}