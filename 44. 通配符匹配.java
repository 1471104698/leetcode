给定一个字符串 (s) 和一个字符模式 (p) ，实现一个支持 '?' 和 '*' 的通配符匹配。

'?' 可以匹配任何单个字符。
'*' 可以匹配任意字符串（包括空字符串）。
两个字符串完全匹配才算匹配成功。

说明:

s 可能为空，且只包含从 a-z 的小写字母。
p 可能为空，且只包含从 a-z 的小写字母，以及字符 ? 和 *。
示例 1:

输入:
s = "aa"
p = "a"
输出: false
解释: "a" 无法匹配 "aa" 整个字符串。
示例 2:

输入:
s = "aa"
p = "*"
输出: true
解释: '*' 可以匹配任意字符串。
示例 3:

输入:
s = "cb"
p = "?a"
输出: false
解释: '?' 可以匹配 'c', 但第二个 'a' 无法匹配 'b'。
示例 4:

输入:
s = "adceb"
p = "*a*b"
输出: true
解释: 第一个 '*' 可以匹配空字符串, 第二个 '*' 可以匹配字符串 "dce".
示例 5:

输入:
s = "acdcb"
p = "a*c?b"
输入: false

class Solution {
    public boolean isMatch(String s, String p) {
        /*
        动态规划
        dp[i][j] 表示 s 的前 i 个字符是否能被 p 的前 j 个字符匹配

        初始条件：
        dp[0][0] = true; 即都为空的时候可以匹配

        s 为空时, p 为空 或 只存在 * 的时候才能进行匹配
        */
        int len1 = s.length();
        int len2 = p.length();
        boolean[][] dp = new boolean[len1 + 1][len2 + 1];
        dp[0][0] = true;

        char[] ss = s.toCharArray();
        char[] ps = p.toCharArray();

        for(int i = 1; i <= len2; i++){
            //p 当前字符为 "*" 并且 dp[i - 1] 为 true 时才匹配
            dp[0][i] = ps[i - 1] == '*' && dp[0][i - 1];
        }

        for(int i = 1; i <= len1; i++){
            for(int j = 1; j <= len2; j++){
                if(ss[i - 1] == ps[j - 1] || ps[j - 1] == '?'){
                    dp[i][j] = dp[i - 1][j - 1];
                }else if(ps[j - 1] == '*'){
                    /*
                    当 ps[i] 为 * 时，那么可以不匹配当前字符，也可以匹配当前字符及以下字符
                    dp[i][j] = dp[i][j - 1] || dp[i - 1][j]
                    */
                    dp[i][j] = dp[i][j - 1] || dp[i - 1][j];
                }
            }
        }
        return dp[len1][len2];
    }
}