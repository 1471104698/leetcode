给定字符串 S 和单词字典 words, 求 words[i] 中是 S 的子序列的单词个数。

示例:
输入: 
S = "abcde"
words = ["a", "bb", "acd", "ace"]
输出: 3
解释: 有三个是 S 的子序列的单词: "a", "acd", "ace"。
注意:

所有在words和 S 里的单词都只由小写字母组成。
S 的长度在 [1, 50000]。
words 的长度在 [1, 5000]。
words[i]的长度在[1, 50]。

//思路①、暴力法，有多少个 word 就需要遍历多少次 S
class Solution {
    public int numMatchingSubseq(String S, String[] words) {
        int c = 0;
        for(String word : words){
            int len = word.length();
            int i = 0;
            int j = 0;
            for(; j < len; j++){
                i = S.indexOf(word.charAt(j), i);
                if(i == -1){
                    break;
                }
                i++;
            }
            if(j == len){
                c++;
            }
        }
        return c;
    }
}

/*
思路②、记录 S 每个字符出现的位置，然后使用二分查找比上一个匹配字符所在位置更大的索引位置
比如 S = "babcd" word = "abc"
	S 最开始匹配的是 a ，索引位置为 1，那么后续匹配 b 的时候就需要找比 a 索引位置较大的位置。即 2，而不能是 0

*/

class Solution {
    public int numMatchingSubseq(String S, String[] words) {
        //
        List<Integer>[] indexs = new ArrayList[26];

        for(int i = 0; i < S.length(); i++){
            char ch = S.charAt(i);
            if(indexs[ch - 'a'] == null){
                indexs[ch - 'a'] = new ArrayList<>();
            }
            indexs[ch - 'a'].add(i);
        }
        int c = 0;
        for(String word : words){
            int len = word.length();
            //word 的索引
            int i = 0;
            //S 的索引
            int j = 0;
			//遍历 word
            for(; i < len; i++){
				//查找 
                List<Integer> list = indexs[word.charAt(i) - 'a'];
                j = getIndex(list, j);
                if(j == -1){
                    break;
                }
                j++;
            }
            if(i == len){
                c++;
            }
        }
        return c;
    }

    //二分查找 比 index 较大的索引
    private int getIndex(List<Integer> list, int index){
        if(list == null){
            return -1;
        }
        int left = 0;
        int right = list.size() - 1;
        while(left < right){
            int mid = (left + right) >>> 1;
            if(list.get(mid) < index){
                left = mid + 1;
            }else{
                right = mid;
            }
        }
        int idx = list.get(left);
        return idx >= index ? idx : -1;
    }
}