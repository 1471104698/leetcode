
在字符串 s 中找出第一个只出现一次的字符。如果没有，返回一个单空格。

示例:

s = "abaccdeff"
返回 "b"

s = "" 
返回 " "

class Solution {
    public char firstUniqChar(String s) {
        int[] chs = new int[26];
        for(char ch : s.toCharArray()){
            chs[ch - 'a']++;
        }
        for(char ch : s.toCharArray()){
            if(chs[ch - 'a'] == 1){
                return ch;
            }
        }
        return ' ';
    }
}