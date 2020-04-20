给定一个布尔表达式和一个期望的布尔结果 result，布尔表达式由 0 (false)、1 (true)、& (AND)、 | (OR) 和 ^ (XOR) 符号组成。实现一个函数，算出有几种可使该表达式得出 result 值的括号方法。

输入: s = "1^0|0|1", result = 0
输出: 2
解释: 两种可能的括号方法是
1^(0|(0|1))
1^((0|0)|1)

输入: s = "0&0&0&1^1|0", result = 1
输出: 10


class Solution {
    public int countEval(String s, int result) {

        int len = s.length();
        if(len == 1){
            return s.charAt(0) - '0' == result ? 1 : 0;
        }
        int[][][] memo = new int[len][len][];
        int c = 0;
        char[] chs = s.toCharArray();
        //从分割符进行分割
        for(int i = 1; i < len; i += 2){
            int[] left = helper(chs, 0, i - 1, memo);
            int[] right = helper(chs, i + 1, len - 1, memo);
            int n1 = bool(0, 0, chs[i]);
            int n2 = bool(1, 1, chs[i]);
            int n3 = bool(0, 1, chs[i]);
            if(n1 == result){
                c += left[0] * right[0];
            }
            if(n2 == result){
                c += left[1] * right[1];
            }
            if(n3 == result){
                c += left[0] * right[1] + left[1] * right[0];
            }
        }
        return c;
    }
    /*
    最终 bool 运算只有两种结果 ，为 0 和 为 1
    那么我们直接统计左边结果为 0 的个数 和 结果为 1 的个数
    */
    private int[] helper(char[] chs, int l, int r, int[][][] memo){
        if(memo[l][r] != null){
            return memo[l][r];
        }
        int[] res = new int[2];

        memo[l][r] = res;

        if(l == r){
            if(chs[l] == '0'){
                res[0] = 1;
            }else{
                res[1] = 1;
            }
            return res;
        }
        for(int i = l + 1; i <= r; i += 2){
            int[] left = helper(chs, l, i - 1, memo);
            int[] right = helper(chs, i + 1, r, memo);
            int n1 = bool(0, 0, chs[i]);
            int n2 = bool(1, 1, chs[i]);
            int n3 = bool(0, 1, chs[i]);
            res[n1] += left[0] * right[0];
            res[n2] += left[1] * right[1];
            res[n3] += left[0] * right[1] + left[1] * right[0];
        }
        return res;
    }

    private int bool(int n1, int n2, char ch){
        switch(ch){
            case '&':
                return n1 & n2;
            case '|':
                return n1 | n2;
            case '^':
                return n1 ^ n2;
        }
        return n1 & n2;
    }
}