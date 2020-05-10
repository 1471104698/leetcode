编写一个方法，找出两个数字a和b中最大的那一个。不得使用if-else或其他比较运算符。

示例：

输入： a = 1, b = 2
输出： 2

class Solution {
    public int maximum(int a, int b) {
        /*
        a * k + b * (k ^ 1)
        当 a > b， 那么 k 为 1，那么式子为 a * 1 + b * 0 = a
        当 a < b， 那么 k 为 0，那么式子变为 a * 0 + b * 1 = b

        这个 k 怎么获取？
        首先我们让 a - b
        如果结果是负数，那么二进制首位必定是 1，如果是正数，那么二进制首位必定是 0
        那么我们就可以使用 首位 来赋值 k
        */
        //防止溢出，使用 long 型，long 有 64 位，因此右移 63 位 获取首位二进制
        int k = (int)(((long)a - b) >>> 63);
        return a * (k ^ 1) + b * k;
    }
}