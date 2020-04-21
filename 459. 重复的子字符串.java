给定一个非空的字符串，判断它是否可以由它的一个子串重复多次构成。给定的字符串只含有小写英文字母，并且长度不超过10000。

示例 1:

输入: "abab"

输出: True

解释: 可由子字符串 "ab" 重复两次构成。
示例 2:

输入: "aba"

输出: False
示例 3:

输入: "abcabcabcabc"

输出: True

解释: 可由子字符串 "abc" 重复四次构成。 (或者子字符串 "abcabc" 重复两次构成。)

class Solution {
    public boolean repeatedSubstringPattern(String s) {
        /*
        如果是重复的，那么我们选取出模式串 pattern 后，剩下的字符串按长度肯定都能与它进行匹配
        并且，如果是重复的，那么肯定能够分割出多个 模式串，即 len % i == 0
         */
        int len = s.length();
        for(int i = 1; i <= len / 2; i++){
            if(len % i == 0 && isOk(s.substring(i), s.substring(0, i))){
                return true;
            }
        }
        return false;
    }
    private boolean isOk(String s, String pattern){
        int len = pattern.length();
        if(s.equals(pattern)){
            return true;
        }
        for(int i = 0; i < s.length(); i++){ 
            //与模式串进行匹配， i % len 用于循环与 pattern 进行匹配
            if(s.charAt(i) != pattern.charAt(i % len)){
                return false;
            }
        }
        return true;
    }
}