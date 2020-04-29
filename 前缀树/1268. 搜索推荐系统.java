给你一个产品数组 products 和一个字符串 searchWord ，products  数组中每个产品都是一个字符串。

请你设计一个推荐系统，在依次输入单词 searchWord 的每一个字母后，推荐 products 数组中前缀与 searchWord 相同的最多三个产品。如果前缀相同的可推荐产品超过三个，请按字典序返回最小的三个。

请你以二维列表的形式，返回在输入 searchWord 每个字母后相应的推荐产品的列表。

 

示例 1：

输入：products = ["mobile","mouse","moneypot","monitor","mousepad"], searchWord = "mouse"
输出：[
["mobile","moneypot","monitor"],
["mobile","moneypot","monitor"],
["mouse","mousepad"],
["mouse","mousepad"],
["mouse","mousepad"]
]
解释：按字典序排序后的产品列表是 ["mobile","moneypot","monitor","mouse","mousepad"]
输入 m 和 mo，由于所有产品的前缀都相同，所以系统返回字典序最小的三个产品 ["mobile","moneypot","monitor"]
输入 mou， mous 和 mouse 后系统都返回 ["mouse","mousepad"]

class Solution {
    public List<List<String>> suggestedProducts(String[] products, String searchWord) {
        /*
        使用字典树
        比如
        products = ["mobile","mouse","moneypot","monitor","mousepad"], searchWord = "mouse"

        我们将 products 每个字符串都插入到 Trie 中，插入过程中顺便进行记录，比如:
        插入 mobile ，那么首先创建 m 节点，然后记录 m 开头的前缀有一个字符串 mobile ，再创建 o 节点，然后记录 mo 开头的有一个字符串 mobile，以此类推
        插入 mouse ，前面有 m 节点了，获取，然后再记录 m 开头的有一个字符串 mouse，前面有 o 节点，然后记录 mo 开头的有一个字符串 mouse，以此类推
        到此， m 开头的已经记录了 两个字符串 mobile 和 mouse，最多记录 3 个，当全部插入完成后，遍历 searchWord 时，将对应 list 返回进行添加即可
        */
        
        //先将字符串按字典序进行排列
         Arrays.sort(products, (a, b) -> {
            int alen = a.length();
            int blen = b.length();
            for(int i = 0; i < alen && i < blen; i++){
                char ach = a.charAt(i);
                char bch = b.charAt(i);
                if(ach > bch){
                    return 1;
                }else if(ach < bch){
                    return -1;
                }
            }
            return alen - blen;
        });

        Trie trie = new Trie();
        for(String word : products){
            trie.insert(word);
        }
        TrieNode root = trie.root;
        List<List<String>> res = new ArrayList<>();
        for(int i = 0; i < searchWord.length(); i++){
            if(root.childern[searchWord.charAt(i) - 'a'] != null){
                root = root.childern[searchWord.charAt(i) - 'a'];
                res.add(new ArrayList<>(root.list));
            }else{
                while(i++ < searchWord.length()){
                    res.add(new ArrayList<>());
                }
                break;
            }
        }
        return res;
    }
    class TrieNode{
        int idx = 0;
        //每个节点都记录对应的字符串，只记录 3 个，因为已经排过序了，因此前 3 个肯定是最小的
        List<String> list;
        TrieNode[] childern = new TrieNode[26];
        public TrieNode() {
            list = new ArrayList<>();
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
                if(cur.idx != 3){
                    cur.list.add(word);
                    cur.idx++;
                }
            }
        }
    }
}