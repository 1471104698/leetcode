给定一个由整数组成的非空数组所表示的非负整数，在该数的基础上加一。

最高位数字存放在数组的首位， 数组中每个元素只存储单个数字。

你可以假设除了整数 0 之外，这个整数不会以零开头。

示例 1:

输入: [1,2,3]
输出: [1,2,4]
解释: 输入数组表示数字 123。
示例 2:

输入: [4,3,2,1]
输出: [4,3,2,2]
解释: 输入数组表示数字 4321。

class Solution {
    public int[] plusOne(int[] digits) {
        int len = digits.length;

        for(int i = len - 1; i >= 0; i--){
            digits[i] += 1;
            if(digits[i] / 10 == 0){
                return digits;
            }
            digits[i] %= 10;
        }
        /*
        对于普通的数 + 1，如果不是 999 之类的数，比如 998，那么在上面已经返回了，因此到这里的必定是 999 之类的数
        因此，最终进位必定是首位为 1，其余为 0
        */
        digits = new int[len + 1];
        digits[0] = 1;
        return digits;
    }
}