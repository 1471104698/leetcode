给你一个字符串 s ，请你根据下面的算法重新构造字符串：

从 s 中选出 最小 的字符，将它 接在 结果字符串的后面。
从 s 剩余字符中选出 最小 的字符，且该字符比上一个添加的字符大，将它 接在 结果字符串后面。
重复步骤 2 ，直到你没法从 s 中选择字符。
从 s 中选出 最大 的字符，将它 接在 结果字符串的后面。
从 s 剩余字符中选出 最大 的字符，且该字符比上一个添加的字符小，将它 接在 结果字符串后面。
重复步骤 5 ，直到你没法从 s 中选择字符。
重复步骤 1 到 6 ，直到 s 中所有字符都已经被选过。
在任何一步中，如果最小或者最大字符不止一个 ，你可以选择其中任意一个，并将其添加到结果字符串。

请你返回将 s 中字符重新排序后的 结果字符串 。

 

示例 1：

输入：s = "aaaabbbbcccc"
输出："abccbaabccba"
解释：第一轮的步骤 1，2，3 后，结果字符串为 result = "abc"
第一轮的步骤 4，5，6 后，结果字符串为 result = "abccba"
第一轮结束，现在 s = "aabbcc" ，我们再次回到步骤 1
第二轮的步骤 1，2，3 后，结果字符串为 result = "abccbaabc"
第二轮的步骤 4，5，6 后，结果字符串为 result = "abccbaabccba"

class Solution {
    public String sortString(String s) {
		/*
			统计各个字符的字符串
			然后从 小到大将字符添加一遍，再从大到小将字符添加一遍
		*/
        int[] chs = new int[26];
        for(char ch : s.toCharArray()){
            chs[ch - 'a']++;
        }

        StringBuilder sb = new StringBuilder();
        int count = 0;
        while(count < s.length()){
            for(int i = 0; i < 26; i++){
                if(chs[i] != 0){
                    sb.append((char)('a' + i));
                    chs[i]--;
                    count++;
                }
            }
            for(int i = 25; i >= 0; i--){
                if(chs[i] != 0){
                    sb.append((char)('a' + i));
                    chs[i]--;
                    count++;
                }
            }
        }
        return sb.toString();
    }
}