在老式手机上，用户通过数字键盘输入，手机将提供与这些数字相匹配的单词列表。每个数字映射到0至4个字母。给定一个数字序列，实现一个算法来返回匹配单词的列表。你会得到一张含有有效单词的列表。映射如下图所示：

示例 1:

输入: num = "8733", words = ["tree", "used"]
输出: ["tree", "used"]
示例 2:

输入: num = "2", words = ["a", "b", "c", "d"]
输出: ["a", "b", "c"]
提示：

num.length <= 1000
words.length <= 500
words[i].length == num.length
num中不会出现 0, 1 这两个数字

class Solution {
    List<String> res;
    public List<String> getValidT9Words(String num, String[] words) {
        res = new ArrayList<>();
        int len = num.length();
        Trie trie = new Trie();
        for(String word : words){
            trie.insert(word);
        }
        String[] map = {"", "", "abc", "def", "ghi", "jkl", "mno", "pqrs", "tuv", "wxyz"};
        TrieNode cur = trie.root;
        dfs(num.toCharArray(), 0, cur, map);
        return res;
    }
    private void dfs(char[] chs, int i, TrieNode cur, String[] map){
        if(i == chs.length){
            return;
        }
        for(char ch : map[chs[i] - '0'].toCharArray()){
            TrieNode temp = cur.childern[ch - 'a'];
            if(temp == null){
                continue;
            }else{
                //我们不需要管长度，只需要管是否到头，因为 num 和 word 长度是一致的，只要 前缀树节点到头，那么表示 num 也到头了
                if(temp.end){
                    res.add(temp.val);
                }else{
                    dfs(chs, i + 1, temp, map);
                }
            }
        }
    }

    class TrieNode{
        String val;
        TrieNode[] childern = new TrieNode[26];
        //记录当前节点是否是某个单词的结尾
        boolean end = false;
        public TrieNode() {
        } 
        public TrieNode(String val) {
            this.val = val;
        }   
    }
    class Trie {
        TrieNode root;

        public Trie() {
            root = new TrieNode();
        }
        
        public void insert(String word) {
            TrieNode cur = root;
            for(int i = 0; i < word.length(); i++){
                int ch = word.charAt(i) - 'a';
                if(cur.childern[ch] == null){
                    cur.childern[ch] = new TrieNode();
                }
                cur = cur.childern[ch];
            }
            cur.end = true;
            cur.val = word;
        }
    
    }
}