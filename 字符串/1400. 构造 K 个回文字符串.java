
给你一个字符串 s 和一个整数 k 。请你用 s 字符串中 所有字符 构造 k 个非空 回文串 。

如果你可以用 s 中所有字符构造 k 个回文字符串，那么请你返回 True ，否则返回 False 。

 

示例 1：

输入：s = "annabelle", k = 2
输出：true
解释：可以用 s 中所有字符构造 2 个回文字符串。
一些可行的构造方案包括："anna" + "elble"，"anbna" + "elle"，"anellena" + "b"
示例 2：

输入：s = "leetcode", k = 3
输出：false
解释：无法用 s 中所有字符构造 3 个回文串。
示例 3：

输入：s = "true", k = 4
输出：true
解释：唯一可行的方案是让 s 中每个字符单独构成一个字符串。
示例 4：

输入：s = "yzyzyzyzyzyzyzy", k = 2
输出：true
解释：你只需要将所有的 z 放在一个字符串中，所有的 y 放在另一个字符串中。那么两个字符串都是回文串。

class Solution {
    public boolean canConstruct(String s, int k) {
        /*
        构造 k 个回文串
        1、判断字符串长度是否小于 k ，如果是则直接返回 false
        2、统计各个字符的个数
        3、如果为奇数字符的个数超过 k 个，那么直接返回 false，否则返回 true
			原理：如果最多只存在 k 个奇数字符，那么将这 k 个奇数字符平均分配给 k 个回文串，一个回文串一个，然后将偶数字符随机分配即可
					但如果超过 k 个，那么怎么也构不成 k 个回文串
        */
        int len = s.length();
        if(len < k){
            return false;
        }
        int[] chs = new int[26];
        for(char ch : s.toCharArray()){
            chs[ch - 'a']++;
        }
        //奇数字符的个数
        int count = 0;
        for(int i = 0; i < 26; i++){
            if((chs[i] & 1) != 0){
                count++;
            }
        }
        return count <= k;
    }
}