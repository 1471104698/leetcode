    class TrieNode{
        String val;
        TrieNode[] childern = new TrieNode[26];
        记录当前节点是否是某个单词的结尾
        boolean end = false;
        public TrieNode() {
        } 
	}

    class Trie {

        TrieNode root;

        public Trie() {
            root = new TrieNode();
        }
        
        public void insert(String word) {
            TrieNode cur = root;
            for(int i = 0; i  word.length(); i++){
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