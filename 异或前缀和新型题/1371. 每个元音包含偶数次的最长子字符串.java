给你一个字符串 s ，请你返回满足以下条件的最长子字符串的长度：每个元音字母，即 'a'，'e'，'i'，'o'，'u' ，在子字符串中都恰好出现了偶数次。

 

示例 1：

输入：s = "eleetminicoworoep"
输出：13
解释：最长子字符串是 "leetminicowor" ，它包含 e，i，o 各 2 个，以及 0 个 a，u 。
示例 2：

输入：s = "leetcodeisgreat"
输出：5
解释：最长子字符串是 "leetc" ，其中包含 2 个 e 。
示例 3：

输入：s = "bcbcbc"
输出：6
解释：这个示例中，字符串 "bcbcbc" 本身就是最长的，因为所有的元音 a，e，i，o，u 都出现了 0 次。


        /*
        第一印象是 滑动窗口，然而滑动窗口不行，滑不出来
        
        新的方法思路：
        'a'，'e'，'i'，'o'，'u'  5 个元音字母，我们使用 0 和 1 来记录每个元音字母的奇偶状态
        0 表示 出现偶数个，1 表示出现奇数个
        按这种情况表示，总共存在 2^5 = 32 种状态
        比如 0000 ... 0001 0010
                        a eiou （a 和 o 出现奇数次，e、i 和 u 出现偶数次）

        我们使用一个 int 型变量的 后 5 位二进制数来记录 5 个元音字母的状态
        前提：无论有多少个字母，只要是偶数个，那么异或结果为 0， 只要是奇数个，那么异或结果为 1

        假设 [0, i] 状态为 state, 那么如果 [0, j] 的状态也为 state
        那么意味着 [0, i) ^ (i, j] = [0, j] 👉 state ^ [i, j] = state，
        即 (i, j] 异或结果为 0，即 (i, j] 这段的元音字母出现现次全部都是偶数，因此异或结果才能为 0

        上面做法相当于前缀和，不过是异或前缀和
        要求最长，因此我们使用一个 32 大小的 int 数组 记录每种状态最先出现的位置
        */
		
class Solution {
    public int findTheLongestSubstring(String s) {


        int len = s.length();
        //32 位状态索引位置
        int[] pre = new int[32];
        Arrays.fill(pre, -2);
        //状态 0 位置设置位 -1，基本是前缀和的标配
        pre[0] = -1;
        
        //最长子字符串
        int maxLen = 0;
        //异或结果
        int xres = 0;
        for(int i = 0; i < len; i++){
            switch(s.charAt(i)){
                case 'a':
                    xres ^= 1 << 4; break;
                case 'e':
                    xres ^= 1 << 3; break;
                case 'i':
                    xres ^= 1 << 2; break;
                case 'o':
                    xres ^= 1 << 1; break;
                case 'u':
                    xres ^= 1 << 0; break;
                default: break;
            }
            if(pre[xres] == -2){
                pre[xres] = i;
            }
            maxLen = Math.max(maxLen, i - pre[xres]);
        }
        return maxLen;
    }
}