示例 1:
输入: "42"
输出: 42

示例 2:
输入: "   -42"
输出: -42
解释: 第一个非空白字符为 '-', 它是一个负号。
     我们尽可能将负号与后面所有连续出现的数字组合起来，最后得到 -42 。

示例 3:
输入: "4193 with words"
输出: 4193
解释: 转换截止于数字 '3' ，因为它的下一个字符不为数字。

示例 4:
输入: "words and 987"
输出: 0
解释: 第一个非空字符是 'w', 但它不是数字或正、负号。
     因此无法执行有效的转换。

class Solution {
    public int myAtoi(String str) {
        str = str.trim();
        if(str.length() == 0){
            return 0;
        }

        //默认符号为 正，因为如果第一个字符是数字的话，那么也是正数
        int sign = 1;
        //判断是否需要跳过第一个字符
        int index = 0;
        char ch = str.charAt(0);
        if(ch == '+'){
            index++;
        }else if(ch == '-'){
            sign = -1;
            index++;
        }else if(!isNumber(ch - '0')){
            return 0;
        }

        int res = 0;
        for(int i = index; i < str.length(); i++){
            int num = str.charAt(i) - '0';
            if(!isNumber(num)){
                return res;
            }
            if(sign == 1 && (res == Integer.MAX_VALUE / 10 && num > 7 || res > Integer.MAX_VALUE / 10)){
                return Integer.MAX_VALUE;
            }
            if(sign == -1 && (res == Integer.MIN_VALUE / 10 && num > 8 || res < Integer.MIN_VALUE / 10)){
                return Integer.MIN_VALUE;
            }
            res = res * 10 + sign * num;
        }
        return res;
    }
    private boolean isNumber(int num){
        return num >= 0 && num <= 9;
    }
}
