给定两个字符串 A 和 B, 寻找重复叠加字符串A的最小次数，使得字符串B成为叠加后的字符串A的子串，如果不存在则返回 -1。

举个例子，A = "abcd"，B = "cdabcdab"。

答案为 3， 因为 A 重复叠加三遍后为 “abcdabcdabcd”，此时 B 是其子串；A 重复叠加两遍后为"abcdabcd"，B 并不是其子串。

注意:

 A 与 B 字符串的长度在1和10000区间范围内。

class Solution {
    public int repeatedStringMatch(String A, String B) {
        /*
        因为一个完整的 B 可能首部用到 A 的一部分，尾部用到 A 的一部分
        像这样A'[AA....AA]A' , [AA....AA] 内必然 <= B 的长度，故总长 <= 2*A+B

        对于 [AA....AA]，因为 B 首部和尾部用到了 首尾 A 的一部分，那么意味着 [AA....AA] 也是 B 的一部分
        那么意味着 [AA....AA] 的长度 < B 的长度

        那么最终整个字符串长度最长不超过 首尾两个 A 的长度 + 中间 [AA....AA] 换算成 B 的长度
        即 2 * lenA + lenB;
        */
        int lenA = A.length();
        int lenB = B.length();

        int maxLen = 2 * lenA + lenB;

        StringBuilder sb = new StringBuilder();

        while(sb.length() < maxLen){
            /*
			contains() 内部是判断 indexOf() > -1 机制实现的，同样是使用 indexOf()
			
			这里如果使用 sb.toString().lastIndexOf(B) != -1 会快 100 倍，不清楚原因
			*/
            if(sb.toString().contains(B)){
                return sb.length() / lenA;
            }
            sb.append(A);
        }
        return -1;
    }
}