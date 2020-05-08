假设你正在读取一串整数。每隔一段时间，你希望能找出数字 x 的秩(小于或等于 x 的值的个数)。请实现数据结构和算法来支持这些操作，也就是说：

实现 track(int x) 方法，每读入一个数字都会调用该方法；

实现 getRankOfNumber(int x) 方法，返回小于或等于 x 的值的个数。

注意：本题相对原题稍作改动

示例:

输入:
["StreamRank", "getRankOfNumber", "track", "getRankOfNumber"]
[[], [1], [0], [0]]
输出:
[null,0,null,1]
提示：

x <= 50000
track 和 getRankOfNumber 方法的调用次数均不超过 2000 次

//思路①、使用 map 记录某个值出现的次数，获取秩的时候遍历 [0, val]，当然，我们默认最小值为 0，如果存在负数，那么就不可靠
class StreamRank {
    /*
    需要每次插入 一个数 就进行一次调整
    */
    Map<Integer, Integer> map;
    public StreamRank() {
        map = new HashMap<>();
    }
    
    public void track(int x) {
        map.put(x, map.getOrDefault(x, 0) + 1);
    }
    
    public int getRankOfNumber(int x) {
        int sum = 0;
        for(int i = 0; i <= x; i++){
            sum += map.getOrDefault(i, 0);
        }
        return sum;
    }
}

//思路②、实现二叉搜索树
class StreamRank {
    /*
    实现二叉搜索树
    我们将小于等于 this.val 的值插在 this 的左子树，然后，顺便 leftSize++
    这意味着 this 节点的 leftSize 记录的是它左子树的节点个数
    */
    class TreeNode{
        TreeNode left;
        TreeNode right;
        int val;
        int leftSize = 1;   //本身秩为 1 
        public TreeNode(int val){
            this.val = val;
        }
        public void insert(int val){
            //同值也插在 left
            if(this.val >= val){
                this.leftSize++;
                if(this.left == null){
                    this.left = new TreeNode(val);
                }else{
                    this.left.insert(val);
                }
            }else{
                if(this.right == null){
                    this.right = new TreeNode(val);
                }else{
                    this.right.insert(val);
                }
            }
        }

        public int find(int val){
            if(this.val > val){
                if(this.left == null){
                    return 0;
                }
                return this.left.find(val);
            }else{
                if(this.right == null){
                    return this.leftSize;
                }
                return this.leftSize + this.right.find(val);
            }
        }
    }
    class BST{
        TreeNode root;
        public BST(){}

        public void insert(int val){
            if(root == null){
                root = new TreeNode(val);
            }else{
                root.insert(val);
            }
        }
        public int find(int val){
            if(root == null){
                return 0;
            }
            return root.find(val);
        }
    }

    BST bst;
    public StreamRank() {
        bst = new BST();
    }
    
    public void track(int x) {
        bst.insert(x);
    }
    
    public int getRankOfNumber(int x) {
        return bst.find(x);
    }
}