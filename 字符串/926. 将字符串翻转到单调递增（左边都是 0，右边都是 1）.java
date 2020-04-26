如果一个由 '0' 和 '1' 组成的字符串，是以一些 '0'（可能没有 '0'）后面跟着一些 '1'（也可能没有 '1'）的形式组成的，那么该字符串是单调递增的。

我们给出一个由字符 '0' 和 '1' 组成的字符串 S，我们可以将任何 '0' 翻转为 '1' 或者将 '1' 翻转为 '0'。

返回使 S 单调递增的最小翻转次数。

 

示例 1：

输入："00110"
输出：1
解释：我们翻转最后一位得到 00111.
示例 2：

输入："010110"
输出：2
解释：我们翻转得到 011111，或者是 000111。
示例 3：

输入："00011000"
输出：2
解释：我们翻转得到 00000000。
 

提示：

1 <= S.length <= 20000
S 中只包含字符 '0' 和 '1'

class Solution {
    public int minFlipsMonoIncr(String S) {
        /*
        1、左边全部为 0 ，右边全部为 1
        2、全部为 0
        3、全部为 1
        将上述 3 种情况最少的翻转次数返回即可

        找到 某个位置 进行分割，左边全部为 0，右边全部为 1 的最小翻转次数
        比如 "00110"
               ↑
             分割点，变成 00 111，只需要翻转 1 次
             
        我们先统计 [0, i] 位置的 1 的个数，在进行分割
        dp[i] 表示 [0, i] 的 1 的个数

        (为了处理方便，因此我们将索引偏移 1，即 默认长度设置为 dp[len + 1]， dp[1] 表示 [0, 1) 位置的 1 的个数)
        我们先令 c = Math.min(dp[len], len - dp[len]); 即求出全部变为 0 和 全部变为 1 之间的最小翻转次数
        然后再从中间开始分割

        对于 c = Math.min(c, dp[i] + len - i - (dp[len] - dp[i]));
        dp[i] 表示[0, i] 的 1 的个数，在这里则是将 1 翻转成 0，即有多少个 1 就翻转多少次，即 dp[i] 次
        len - i - (dp[len] - dp[i]) ，其中 dp[len] - dp[i] 是 [i, len] 位置的 1 的个数，那么 0 的个数就是 [i, len] 的长度减去 1 的个数，就是 0 的个数
            那么就是将 0 转换为 1，即有多少个 0 就翻转多少次
        */

        int len = S.length();
        int[] dp = new int[len + 1];

        //dp[i] 表示 [0, i] 之间有多少个 1
        for(int i = 1; i <= len; i++){
            dp[i] = dp[i - 1];
            if(S.charAt(i - 1) == '1'){
                dp[i] += 1;
            }
        }
        int c = Math.min(dp[len], len - dp[len]);

        for(int i = len - 1; i >= 1; i--){
            c = Math.min(c, dp[i] + len - i - (dp[len] - dp[i]));
        }
        return c;
    }
}