给定一个字符串 s 和一个非空字符串 p，找到 s 中所有是 p 的字母异位词的子串，返回这些子串的起始索引。
字符串只包含小写英文字母，并且字符串 s 和 p 的长度都不超过 20100。

说明：

字母异位词指字母相同，但排列不同的字符串。
不考虑答案输出的顺序。

示例 1:
输入:
s: "cbaebabacd" p: "abc"

输出:
[0, 6]

解释:
起始索引等于 0 的子串是 "cba", 它是 "abc" 的字母异位词。
起始索引等于 6 的子串是 "bac", 它是 "abc" 的字母异位词。

 示例 2:
输入:
s: "abab" p: "ab"

输出:
[0, 1, 2]

解释:
起始索引等于 0 的子串是 "ab", 它是 "ab" 的字母异位词。
起始索引等于 1 的子串是 "ba", 它是 "ab" 的字母异位词。
起始索引等于 2 的子串是 "ab", 它是 "ab" 的字母异位词。

class Solution {
    public List<Integer> findAnagrams(String s, String p) {
        /*
        滑动窗口
        */
        List<Integer> res = new ArrayList<>();

        int slen = s.length();
        int plen = p.length();

        int[] need = new int[26];
        int[] have = new int[26];

        for(char ch : p.toCharArray()){
            need[ch - 'a']++;
        }
        
        int left = 0;
        int right = 0;

        int valid = 0;

        while(right < slen){
            int rch = s.charAt(right) - 'a';
            right++;

            if(need[rch] != 0){
                have[rch]++;
                if(need[rch] >= have[rch]){
                    valid++;
                }
            }else{
                Arrays.fill(have, 0);
                valid = 0;
                left = right;
            }

            while(need[rch] < have[rch]){
                int lch = s.charAt(left) - 'a';
                left++;
                have[lch]--;
                if(need[lch] > have[lch]){
                    valid--;
                }
            }
			//找到无需返回，直接添加，然后继续找
            if(valid == plen){
                res.add(left);
            }
        }

        return res;
    }
}