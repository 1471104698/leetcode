最初在一个记事本上只有一个字符 'A'。你每次可以对这个记事本进行两种操作：

Copy All (复制全部) : 你可以复制这个记事本中的所有字符(部分的复制是不允许的)。
Paste (粘贴) : 你可以粘贴你上一次复制的字符。
给定一个数字 n 。你需要使用最少的操作次数，在记事本中打印出恰好 n 个 'A'。输出能够打印出 n 个 'A' 的最少操作次数。

示例 1:

输入: 3
输出: 3
解释:
最初, 我们只有一个字符 'A'。
第 1 步, 我们使用 Copy All 操作。
第 2 步, 我们使用 Paste 操作来获得 'AA'。
第 3 步, 我们使用 Paste 操作来获得 'AAA'。

class Solution {
    public int minSteps(int n) {
        /*
        当 n 为质数，比如 2 3 5 7 ，那么只能一个个进行 P，先 C ，再进行 n - 1 次 P，加上原本的 A，总共 n 个 A，刚好 进行 n 次操作，即 dp[n] = n
        当 n 为非质数，则分解它的因子，得到最小的步数
        
        n == 8
        2 * 4：CP CPPP 6 步 
        4 * 2：CPCP CP 6 步

        n == 12
        2 * 6：CP CPPCP  7 步
        6 * 2：CPPCP CP  7 步
        3 * 4：CPP CPCP  7 步
        4 * 3：CPCP PPP  7 步

        n == 16
        2 * 8：CP CPCPPP 8 步
        4 * 4：CPCP CPCP 8 步

        我们可以看出，我们只需要找到其中一组分解因子即可

        状态转移方程：dp[i] = Math.min(dp[i], dp[j] + dp[i / j])
        初始条件：dp[1] = 0

        n = 12, dp[j] = 6， dp[i / j] = 2
        先算出得到 6 个 A 需要多少步，然后需要 2 个这样的 6 个 A，那么相当于把前面的这 6 个 A 当作一个 A，需要得到 2 个
        */
        
        int[] dp = new int[n + 1];

        for(int i = 2; i <= n; i++){
            dp[i] = i;
            for(int j = 2; j <= Math.sqrt(i); j++){
                //找到一组分解因子
                if(i % j == 0){
                    dp[i] = dp[j] + dp[i / j];
                    break;
                }
            }
        }
        return dp[n];
    }
}