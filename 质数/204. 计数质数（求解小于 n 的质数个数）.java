统计所有小于非负整数 n 的质数的数量。

示例:

输入: 10
输出: 4
解释: 小于 10 的质数一共有 4 个, 它们是 2, 3, 5, 7 。

class Solution {
    public int countPrimes(int n) {
        boolean[] isPrime = new boolean[n + 1];
        Arrays.fill(isPrime, true);

        int c = 0;
        //质数的倍数肯定不是质数，注意：从 2 开始算起， 1 不是质数
        for(int i = 2; i < n; i++){
            if(isPrime[i]){
                c++;
                for(int j = i + i; j < n; j += i){
                    isPrime[j] = false;
                }
            }
        }
        return c;
    }
}