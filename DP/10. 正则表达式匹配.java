给你一个字符串 s 和一个字符规律 p，请你来实现一个支持 '.' 和 '*' 的正则表达式匹配。

'.' 匹配任意单个字符
'*' 匹配零个或多个前面的那一个元素
所谓匹配，是要涵盖 整个 字符串 s的，而不是部分字符串。

说明:

s 可能为空，且只包含从 a-z 的小写字母。
p 可能为空，且只包含从 a-z 的小写字母，以及字符 . 和 *。
示例 1:

输入:
s = "aa"
p = "a"
输出: false
解释: "a" 无法匹配 "aa" 整个字符串。
示例 2:

输入:
s = "aa"
p = "a*"
输出: true
解释: 因为 '*' 代表可以匹配零个或多个前面的那一个元素, 在这里前面的元素就是 'a'。因此，字符串 "aa" 可被视为 'a' 重复了一次。
示例 3:

输入:
s = "ab"
p = ".*"
输出: true
解释: ".*" 表示可匹配零个或多个（'*'）任意字符（'.'）。
示例 4:

输入:
s = "aab"
p = "c*a*b"
输出: true
解释: 因为 '*' 表示零个或多个，这里 'c' 为 0 个, 'a' 被重复一次。因此可以匹配字符串 "aab"。
示例 5:

输入:
s = "mississippi"
p = "mis*is*p*."
输出: false

class Solution {
    public boolean isMatch(String s, String p) {
        /*
        动态规划
        dp[i][j] 表示 s 的前 i 个字符是否能被 p 的前 j 个字符匹配

        初始条件：
        dp[0][0] = true; 即都为空的时候可以匹配
        
        因为 * 表示前面字符出现的个数，因此不能单独出现，即不存在 p = "*"

        当 s 为空时，p 只有为空 或 在索引为奇数的位置出现 * 时才能匹配，并且此时 * 匹配为 0 个
        */
        
        int len1 = s.length();
        int len2 = p.length();
        boolean[][] dp = new boolean[len1 + 1][len2 + 1];

        char[] ss = s.toCharArray();
        char[] ps = p.toCharArray();
        dp[0][0] = true;
        for(int i = 2; i <= len2; i += 2){
            //p 的当前位置为 "*" 并且前两个位置必须为 true
            dp[0][i] = ps[i - 1] == '*' && dp[0][i - 2];
        }

        for(int i = 1; i <= len1; i++){
            for(int j = 1; j <= len2; j++){
                //如果 p 的当前字符为 "." 或 跟 s 当前字符相同，那么看上一个
                if(ps[j - 1] == '.' || ps[j - 1] == ss[i - 1]){
                    dp[i][j] = dp[i - 1][j - 1];
                }else if(ps[j - 1] == '*'){
                    /*
                    ps[i] == "*"，那么存在两种情况
                    1、ps[i - 1] == '.'，那么可以选择舍弃或复制
                    dp[i][j] = dp[i][j - 2] || dp[i - 1][j]

                    2、ps[i - 1] 跟 ss[i] 相同，那么可以选择舍弃或复制
                    dp[i][j] = dp[i][j - 2] || dp[i - 1][j]

                    3、ps[i - 1] != ss[i]，那么只能舍弃
                    dp[i][j] = dp[i][j - 2]
                    */
                    if(ps[j - 2] == ss[i - 1] || ps[j - 2] == '.'){
                        dp[i][j] = dp[i - 1][j] || dp[i][j - 2];
                    }else{
                        dp[i][j] = dp[i][j - 2];
                    }
                }
            }
        }
        return dp[len1][len2];
    }
}