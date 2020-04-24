你的任务是计算 ab 对 1337 取模，a 是一个正整数，b 是一个非常大的正整数且会以数组形式给出。

示例 1:

输入: a = 2, b = [3]
输出: 8
示例 2:

输入: a = 2, b = [1,0]
输出: 1024

class Solution {
    int mod = 1337;
    public int superPow(int a, int[] b) {
        /*
       a ^ (1,5,6,4) 我们可以转换为 a ^ 4 * (a ^ 1 5 6) ^ 10
        */
        a %= mod;
        return superPow(a, b, b.length - 1);
    }
    private int superPow(int a, int[] b, int idx){
        if(idx == -1){
            return 1;
        }
        //取出最后一位数
        int num = b[idx--];
        int p1 = quickPow(a, num);
        int p2 = quickPow(superPow(a, b, idx), 10);
        return p1 * p2 % mod;
    }
    private int quickPow(int a, int x){
        if(x == 0){
            return 1;
        }
        if(x == 1){
            return a;
        }
        int res = quickPow(a, x / 2);
        res *= res;
        res %= mod;
        if((x & 1) != 0){
            res *= a;
            res %= mod;
        }
        return res;
    }
}
