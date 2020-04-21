你有一个单词列表 words 和一个模式  pattern，你想知道 words 中的哪些单词与模式匹配。
//注意：word 和 pattern 字符一一进行匹配

示例：
输入：words = ["abc","deq","mee","aqq","dkd","ccc"], pattern = "abb"
输出：["mee","aqq"]
解释：
"mee" 与模式匹配，因为存在排列 {a -> m, b -> e, ...}。
"ccc" 与模式不匹配，因为 pattern 字符串中 a 和 b 同时匹配了 c
因为 a 和 b 映射到同一个字母。
 

提示：

1 <= words.length <= 50
1 <= pattern.length = words[i].length <= 20	//每个 word 长度跟 pattern 长度一样，即每个字符分别进行对应


下面两种方法，同种思路

//思路①、使用 map 存储 pattern 某个字母对应的字符
class Solution {
    public List<String> findAndReplacePattern(String[] words, String pattern) {
        List<String> res = new ArrayList<>();
    
        int len = pattern.length();
        char[] ps = pattern.toCharArray();
        for(String word : words){
            Map<Character, Character> map = new HashMap<>();
            int i = 0;
            for(; i < len; i++){
                char ch = word.charAt(i);
                if(!map.containsKey(ps[i])){
                    //如果 value 值存在，那么意味着 word 当前字符已经被别的 pattern 的字符匹配了
                    if(map.containsValue(ch)){
                        break;
                    }
                    map.put(ps[i], ch);
                }else if(map.get(ps[i]) != ch){
                    break;
                }
            }
            if(i == word.length()){
                res.add(word);
            }
        }
        return res;
    }
}

//思路②、使用两个数组代替 map
class Solution {
    public List<String> findAndReplacePattern(String[] words, String pattern) {
        List<String> res = new ArrayList<>();
        /*
		不使用 map 了，直接使用两个 26 长度的数组存储 word 和 pattern 的对应情况
		w 存储 word 某个字符是否已经被对应了
		p 存储 pattern 某个字符对应的 word 的某个字符
		
		如果 p[pNum] == -1 ，那么表示 pattern 当前字符还没有对应，但是如果 w[wNum] == true，表示 word 当前字符已经被别的 pattern 字符对应了，那么 break;
		否则添加 p[pNum] = wNum，即 设置 pNum 和 wNum 的对应关系，并设置 wNum 已经被对应了
		
		如果 p[pNum != -1 ，表示已经对应了某个 word 字符，那么判断对应的字符是否与 word 当前字符相同
		*/

        int len = pattern.length();
        for(String word : words){
            boolean[] w = new boolean[26];
            int[] p = new int[26];
            Arrays.fill(p, -1);
            int i = 0;
            for(; i < len; i++){
                int wNum = word.charAt(i) - 'a';
                int pNum = pattern.charAt(i) - 'a';
                if(p[pNum] == -1){
                    if(w[wNum]){
                        break;
                    }
                    p[pNum] = wNum;
                    w[wNum] = true;
                }else if(p[pNum] != wNum){
                    break;
                }
            }
            if(i == len){
                res.add(word);
            }
        }
        return res;
    }
}
