硬币。给定数量不限的硬币，币值为25分、10分、5分和1分，编写代码计算n分有几种表示法。(结果可能会很大，你需要将结果模上1000000007)

示例1:
 输入: n = 5
 输出：2
 解释: 有两种方式可以凑成总金额:
5=5
5=1+1+1+1+1

示例2:
 输入: n = 10
 输出：4
 解释: 有四种方式可以凑成总金额:
10=10
10=5+5
10=5+1+1+1+1+1
10=1+1+1+1+1+1+1+1+1+1

class Solution {
    public int waysToChange(int n) {
        /*
        完全背包问题，求组合数

            0   1   2   3   4   5   6
        1   0   1   1   1   1   1   1
        5   0   1   1   1   1   2   2
        */
        int[] coins = {1, 5, 10, 25};
        int[] dp = new int[n + 1];

        /*
        dp[0] 初始化为 1，为了刚好选择 coin 能够凑成 amount，则其方法数为 1
        比如 amoutn = 1，然后选择了 coin = 1，那么就是剩下的 0 元，那么组成 1 元的方法数为 1
        */
        dp[0] = 1;
        for(int coin : coins){
            for(int i = coin; i <= n; i++){
                dp[i] += dp[i - coin];
                dp[i] %= 1000000007;
            }
        }
        return dp[n];
    }
}

求最少硬币组成 

示例1:
 输入: n = 3
 输出：3
 解释: 至少需要 3 枚 1 分的硬币

示例2:
 输入: n = 5
 输出：1
 解释: 只需要 1 枚 5 分的硬币就可以:
 
 class Solution {
    public int waysToChange(int n) {
        完全背包问题，求组成的最少硬币数

            0   1   2   3   4   5   6
        1   0   1   2   3   4   5   6
        5   0   1   2   3   4   1   2

        int[] coins = {1, 5, 10, 25};
        int[] dp = new int[n + 1];

        for(int coin : coins){
            for(int i = coin; i <= n; i++){
                dp[i] = dp[i] == 0 ? dp[i - coin] + 1 : Math.min(dp[i], dp[i - coin] + 1);
            }
        }
        return dp[n];
    }
}
