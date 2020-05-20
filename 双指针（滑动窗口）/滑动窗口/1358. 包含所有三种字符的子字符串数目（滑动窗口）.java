
给你一个字符串 s ，它只包含三种字符 a, b 和 c 。

请你返回 a，b 和 c 都 至少 出现过一次的子字符串数目。

 

示例 1：

输入：s = "abcabc"
输出：10
解释：包含 a，b 和 c 各至少一次的子字符串为 "abc", "abca", "abcab", "abcabc", "bca", "bcab", "bcabc", "cab", "cabc" 和 "abc" (相同字符串算多次)。
示例 2：

输入：s = "aaacb"
输出：3
解释：包含 a，b 和 c 各至少一次的子字符串为 "aaacb", "aacb" 和 "acb" 。
示例 3：

输入：s = "abc"
输出：1
 

提示：

3 <= s.length <= 5 x 10^4
s 只包含字符 a，b 和 c 。

class Solution {
    public int numberOfSubstrings(String s) {
        /*
        标准滑动窗口

        s = "a b c a b c"
             👆  👆
             l   r
        当 滑动窗口范围内 a b c 都至少出现一次，那么可以跟后面紧接着的子字符串进行拼接
        比如 {a b c}、{a b c a} 、{a b c a b}、{a b c a b c}，即 可以组成的子字符串个数为 len - r

        同理
        s = "a b c a b c"
               👆  👆
               l   r
        {b c a}、{b c a b}、{b c a b c} 可组成 3 个，仍然是 len - r
        */
        int[] have = new int[3];

        int len = s.length();

        char[] chs = s.toCharArray();

        //字符串数目
        int count = 0;

        /*
        滑动窗口出现了 a b c 几种字符
        当 valid = 3 表示 a b c 全部出现了
        当 valid = 2 表示 a b c 只出现了其中两种
        */
        int vaild = 0;

        int left = 0;
        int right = 0;
        
        while(right < len){
            int i = chs[right] - 'a';

            if(i < 3){
                if(have[i] == 0){
                    vaild++;
                }
                have[i]++;
            }
            while(vaild == 3){
                count += len - right;
                i = chs[left] - 'a';
                if(i < 3){
                    have[i]--;
                    if(have[i] == 0){
                        vaild--;
                    }
                }
                left++;
            }
            right++;
        }
        return count;
    }
}