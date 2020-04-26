给定字符串 s 和 t ，判断 s 是否为 t 的子序列。

你可以认为 s 和 t 中仅包含英文小写字母。字符串 t 可能会很长（长度 ~= 500,000），而 s 是个短字符串（长度 <=100）。

字符串的一个子序列是原始字符串删除一些（也可以不删除）字符而不改变剩余字符相对位置形成的新字符串。（例如，"ace"是"abcde"的一个子序列，而"aec"不是）。

示例 1:
s = "abc", t = "ahbgdc"

返回 true.

示例 2:
s = "axc", t = "ahbgdc"

返回 false.

后续挑战 :

如果有大量输入的 S，称作S1, S2, ... , Sk 其中 k >= 10亿，你需要依次检查它们是否为 T 的子序列。在这种情况下，你会怎样改变代码？(这道题就是 792 题)

/*
思路：
遍历短的 s 字符串
然后使用 indexOf() 从 t 中查找字符，如果不存在，那么返回 false ，如果存在，那么继续从 查找到的位置查找下一个字符
直到 s 字符串都被查找到
*/
class Solution {
    public boolean isSubsequence(String s, String t) {
        if(s.length() > t.length()){
            return false;
        }
        int i = 0;
        for(char ch : s.toCharArray()){
            i = t.indexOf(ch, i);
            if(i == -1){
                return false;
            }
            i++;
        }
        return true;
    }
}

