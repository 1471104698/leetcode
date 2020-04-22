给定一个字符串，请你找出其中不含有重复字符的 最长子串 的长度。

示例 1:

输入: "abcabcbb"
输出: 3 
解释: 因为无重复字符的最长子串是 "abc"，所以其长度为 3。
示例 2:

输入: "bbbbb"
输出: 1
解释: 因为无重复字符的最长子串是 "b"，所以其长度为 1。
示例 3:

输入: "pwwkew"
输出: 3
解释: 因为无重复字符的最长子串是 "wke"，所以其长度为 3。
     请注意，你的答案必须是 子串 的长度，"pwke" 是一个子序列，不是子串。

class Solution {
    public int lengthOfLongestSubstring(String s) {
        /*
        滑动窗口
        */
        int[] have = new int[127];

        int left = 0;
        int right = 0;

        int maxLen = 0;

        while(right < s.length()){
            char rch = s.charAt(right);
            right++;

            have[rch]++;

            //如果字符个数超过 1 ，那么从 left 开始进行缩减
            while(have[rch] > 1){
                have[s.charAt(left)]--;
                left++;
            }
            maxLen = Math.max(maxLen, right - left);
        }
        return maxLen;
    }
}