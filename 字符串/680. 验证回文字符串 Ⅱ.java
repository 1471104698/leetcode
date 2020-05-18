给定一个非空字符串 s，最多删除一个字符。判断是否能成为回文字符串。

示例 1:

输入: "aba"
输出: True
示例 2:

输入: "abca"
输出: True
解释: 你可以删除c字符。
注意:

字符串只包含从 a-z 的小写字母。字符串的最大长度是50000。

class Solution {
    public boolean validPalindrome(String s) {
        /*	
            我们从外逐渐向内扩展，
            比如 aa bb aa
                👆      👆
                我们分别从两边开始比较，如果相同，则向内缩减

            当出现不一样的字符时，那么校验删除其中一个字符是否能够构成回文串
        */
        int len = s.length();
        for(int i = 0, j = len - 1; i < j; i++, j--){
            if(s.charAt(i) != s.charAt(j)){
                /*
                最开始的做法是进行字符串拼接，删除掉 i 或 j 字符
                然后从 [0, len - 1] 再重新判断是否是回文串
                return isOk(s.substring(0, i) + s.substring(i + 1)) || isOk(s.substring(0, j) + s.substring(j + 1));

                这样做会造成重复计算，因为对于 [0, i - 1] 和 [j + 1, len - 1] 我们已经判断过了
                因此我们直接判断 [i + 1, j] 或 [i, j - 1] 这段区间内的字符串是否回文串即可
                */
                return isOk(s, i + 1, j) || isOk(s, i, j - 1);
            }
        }
        return true;
    }

    private boolean isOk(String str, int left, int right){
        while(left < right && str.charAt(left) == str.charAt(right)){
            left++;
            right--;
        }
        return left >= right;
    }
}