实现 pow(x, n) ，即计算 x 的 n 次幂函数。

示例 1:

输入: 2.00000, 10
输出: 1024.00000
示例 2:

输入: 2.10000, 3
输出: 9.26100
示例 3:

输入: 2.00000, -2
输出: 0.25000
解释: 2-2 = 1/22 = 1/4 = 0.25
说明:

-100.0 < x < 100.0
n 是 32 位有符号整数，其数值范围是 [−231, 231 − 1] 。

class Solution {
    public double myPow(double x, int n) {
        /*
        1、任何数的 0 次方都是 1
        2、由 1 得，我们可以从 0 次方重新往上乘，乘到 n 次为止，因此使用递归，一直 n / 2 直到 n = 0，返回 0
        3、由于 递归下去的是 n / 2，因此需要将返回的结果再乘以自身，即 res *= res
        4、如果 n 为偶数的情况，比如 n = 10，那么 n / 2 下去就是 5，返回的结果 res 直接乘以自身即可，但是对于奇数，比如 n = 9，那么 n / 2 下去就是 4，返回乘以自身，还缺少 1 次乘积，因此我们需要再将结果乘以 x
        */
        double res = helper(x, n);
        return n > 0 ? res : 1 / res;
    }
    private double helper(double x, int n){
        if(n == 0){
            return 1;
        }
        double res = helper(x, n / 2);
        res *= res;
        if(n % 2 != 0){
            res *= x;
        }
        return res;
    }
}