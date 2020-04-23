有两种特殊字符。第一种字符可以用一比特0来表示。第二种字符可以用两比特(10 或 11)来表示。

现给一个由若干比特组成的字符串。问最后一个字符是否必定为一个一比特字符。给定的字符串总是由0结束。

示例 1:

输入: 
bits = [1, 0, 0]
输出: True
解释: 
唯一的编码方式是一个两比特字符和一个一比特字符。所以最后一个字符是一比特字符。
示例 2:

输入: 
bits = [1, 1, 1, 0]
输出: False
解释: 
唯一的编码方式是两比特字符和两比特字符。所以最后一个字符不是一比特字符。
注意:

1 <= len(bits) <= 1000.
bits[i] 总是0 或 1.

class Solution {
    public boolean isOneBitCharacter(int[] bits) {
        int len = bits.length;
        if(bits[len - 1] != 0){
            return false;
        }
        //遇到 1，那么跟必须跟后面一位结合，遇到 0，那么只能单独解码
        for(int i = 0; i < len; i++){
            //遇到 1，跳过下一位
            if(bits[i] == 1){
                i++;
                continue;
            }
            //因为我们上面判断了最后一位必定是 0，因此如果能够到达最后一位，那么表示最后一位没有被跳过，即 上一位被用了 或者 是 0
            if(i == len - 1){
                return true;
            }
        }
        return false;
    }
}