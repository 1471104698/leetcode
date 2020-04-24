请你帮忙给从 1 到 n 的数设计排列方案，使得所有的「质数」都应该被放在「质数索引」（索引从 1 开始）上；你需要返回可能的方案总数。

让我们一起来回顾一下「质数」：质数一定是大于 1 的，并且不能用两个小于它的正整数的乘积来表示。

由于答案可能会很大，所以请你返回答案 模 mod 10^9 + 7 之后的结果即可。

 

示例 1：

输入：n = 5
输出：12
解释：举个例子，[1,2,5,4,3] 是一个有效的排列，但 [5,2,3,4,1] 不是，因为在第二种情况里质数 5 被错误地放在索引为 1 的位置上。
示例 2：

输入：n = 100
输出：682289015
 

提示：

1 <= n <= 100

class Solution {
    int mod = (int)Math.pow(10, 9) + 7;
    public int numPrimeArrangements(int n) {

        //计算 [1, n] 有多少个质数
        int num = countPrimes(n);

        /*
        因为质数只能放在质数的位置，非质数只能放在非质数的位置
        并且位置可以任意放置，那么相当于是 质数的排列 * 非质数的排列
        比如 n 的排列 = n * (n - 1) * (n - 2) * ... * 1，即阶乘
         */
        return getF(n - num, getF(num, 1));
    }
    //计算阶乘
    private int getF(int num, long sum){
        while(num > 1){
            sum *= num--;
            sum %= mod;
        }
        return (int)sum;
    }

    public int countPrimes(int n) {
        boolean[] isPrime = new boolean[n + 1];
        Arrays.fill(isPrime, true);

        int c = 0;
        //质数的倍数肯定不是质数，注意：从 2 开始算起， 1 不是质数
        for(int i = 2; i <= n; i++){
            if(isPrime[i]){
                c++;
                for(int j = i + i; j <= n; j += i){
                    isPrime[j] = false;
                }
            }
        }
        return c;
    }
}