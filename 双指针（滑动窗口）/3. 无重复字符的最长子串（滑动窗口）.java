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
		注意：我们只能记录遍历过的字符的个数，如果遇到的当前字符个数存在 1 个，那么我们需要从 left 开始遍历，将遇到的多余的字符都减去，直到在前面的当前字符 ch 为 0
		我们不能使用记录字符索引位置的方法，然后直接将 left 跳过记录的索引位置的下一个位置，
		比如 abba，我们刚开始记录 a 位置为 0， b 位置为 1，再次遇到 b，我们会将 left 跳到 1 后面的字符，那么这时候， left = 2, right = 3，但是再次遇到 a，left 会跳到记录的 a 的位置的下一个位置，即 1，导致结果错误
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