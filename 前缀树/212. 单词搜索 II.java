给定一个二维网格 board 和一个字典中的单词列表 words，找出所有同时在二维网格和字典中出现的单词。

单词必须按照字母顺序，通过相邻的单元格内的字母构成，其中“相邻”单元格是那些水平相邻或垂直相邻的单元格。同一个单元格内的字母在一个单词中不允许被重复使用。

示例:

输入: 
words = ["oath","pea","eat","rain"] and board =
[
  ['o','a','a','n'],
  ['e','t','a','e'],
  ['i','h','k','r'],
  ['i','f','l','v']
]

输出: ["eat","oath"]
说明:
你可以假设所有输入都由小写字母 a-z 组成。

提示:
你需要优化回溯算法以通过更大数据量的测试。你能否早点停止回溯？
如果当前单词不存在于所有单词的前缀中，则可以立即停止回溯。
什么样的数据结构可以有效地执行这样的操作？散列表是否可行？为什么？ 前缀树如何？
如果你想学习如何实现一个基本的前缀树，请先查看这个问题： 实现Trie（前缀树）。

/*
思路：
将 words 全部插入到前缀树中，然后遍历 board ，以某个字符作为根，一直往下找，如果 cur.end = true ,那么进行添加

注意：当 某个节点 cur.end = true，添加完单词后。需要设置为 false，避免后面其他字符遍历到该位置重复添加
而且不能直接 return ,需要继续往下 dfs，比如 我们找到 aaa 时，添加完成，但不能直接 return, 因为可能还存在另一个单词 aaab，因此我们需要继续 dfs
*/
class Solution {
    List<String> res;
    public List<String> findWords(char[][] board, String[] words) {
        res = new ArrayList<>();

        int rlen = board.length;
        int llen = board[0].length;
        
        Trie trie = new Trie();
        TrieNode root = trie.root;
        
        //将所有 word 插入到前缀树中
        for(String str : words){
            trie.insert(str);
        }

        for(int i = 0; i < rlen; i++){
            for(int j = 0; j < llen; j++){
                dfs(board, i, j, root);
            }
        }
        return res;
    }
    //回溯
    private void dfs(char[][] board, int i, int j, TrieNode cur){
        //越界 或 访问过了
        if(i < 0 || i == board.length || j < 0 || j == board[0].length || board[i][j] == '#'){
            return;
        }

        cur = cur.childern[board[i][j] - 'a'];

        if(cur == null){
            return;
        }

        if(cur.end){
            res.add(cur.val);
            //置为 false，防止重复添加到该单词
            cur.end = false;
        }
        
        char temp = board[i][j];
        board[i][j] = '#';
        
        int[][] pos = {{1, 0}, {0, 1}, {-1, 0}, {0, -1}};
        boolean flag = false;
        for(int k = 0; k < 4; k++){
            dfs(board, i + pos[k][0], j + pos[k][1], cur);
        }
        board[i][j] = temp;
    }

    //前缀树节点
    class TrieNode{
        //该节点存储的字符串值
        String val;
        boolean end = false;
        TrieNode[] childern;
        public TrieNode(){
            childern = new TrieNode[26];
        }
    }

    //实现前缀树
    class Trie{
        TrieNode root;
        public Trie(){
            root = new TrieNode();
        }

        public void insert(String str){
            TrieNode cur = root;
            for(char ch : str.toCharArray()){
                int num = ch - 'a';
                if(cur.childern[num] == null){
                    cur.childern[num] = new TrieNode();
                }
                cur = cur.childern[num];
            }
            cur.end = true;
            cur.val = str;
        }
    }
}