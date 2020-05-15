给定一个非空数组，数组中元素为 a0, a1, a2, … , an-1，其中 0 ≤ ai < 231 。

找到 ai 和aj 最大的异或 (XOR) 运算结果，其中0 ≤ i,  j < n 。

你能在O(n)的时间解决这个问题吗？

示例:

输入: [3, 10, 5, 25, 2, 8]

输出: 28

解释: 最大的结果是 5 ^ 25 = 28.

class Solution {
    public int findMaximumXOR(int[] nums) {
        /*
        异或结果要最大，那么尽可能的保证高位是 1 
        而 前面的 1 值比 后面的 1 值更大，比如 1111，第一个 1 的值为 8，它的值比后面 3 个 1 总和 7 还要大
        因此，我们选择 1000 也不选择 0111
		
		1、我们先将所有的元素按二进制插入到前缀树中，如下
			比如 3, 10
			3：0011
		   10：1010
	   （本来是插入 32 位，这里简单展示低 4 位）
		   root         
		  /   \
		 0     1         
		/     / 
	   0     0 
	    \	  \
		 1     1
		  \   /
		   1 0
		   ↑ ↑
		   3 10（最终节点，即该二进制路径的 val 值）
		   
		2、再次遍历数组，对每个元素都从 [31, 0] 号二进制开始，在前缀树中找能使异或结果更大的路径
		比如 二进制数 bit 为 0，那么我们找为 1 的路径，如果不为空，那么沿着 1 走，如果为 空，那么沿着 0 找（尽可能找能使当前二进制位 异或后 为 1 的路径，因为这样最终结果更大）
		当 32 位二进制遍历完成后，我们获取该路径的最终节点，进行异或，获取最大值
        */
        Trie trie = new Trie();
        for(int num : nums){
            trie.insert(num);
        }

        TrieNode root = trie.root;

        int maxVal = 0;

        for(int num : nums){
            TrieNode cur = root;
            for(int i = 31; i >= 0; i--){
                int bit = (num >>> i) & 1;
                if(bit == 0){
                    //找 1 的节点
                    if(cur.childern[1] != null){
                        cur = cur.childern[1];
                    }else{
                        cur = cur.childern[0];
                    }
                }else{
                    //找 0 的节点
                    if(cur.childern[0] != null){
                        cur = cur.childern[0];
                    }else{
                        cur = cur.childern[1];
                    }
                }
            }
            maxVal = Math.max(maxVal, num ^ cur.val);
        }
        return maxVal;
    }

    //前缀树模板
    class TrieNode{
        int val;
        TrieNode[] childern = new TrieNode[2];
        public TrieNode() {
        } 
    }
    class Trie {

        TrieNode root;

        public Trie() {
            root = new TrieNode();
        }
        
        //0 放左边， 1 放右边
        public void insert(int num) {
            TrieNode cur = root;
            for(int i = 31; i >= 0; i--){
                int bit = (num >>> i) & 1;
                if(bit == 1){
                    if(cur.childern[1] == null){
                        cur.childern[1] = new TrieNode();
                    }
                    cur = cur.childern[1];
                }else{
                    if(cur.childern[0] == null){
                        cur.childern[0] = new TrieNode();
                    }
                    cur = cur.childern[0];
                }
            }
            //最终将最后节点的值设置为 num
            cur.val = num;
        }
    }
}