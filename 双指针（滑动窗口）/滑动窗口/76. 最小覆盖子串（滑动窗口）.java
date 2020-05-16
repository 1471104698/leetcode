给你一个字符串 S、一个字符串 T，请在字符串 S 里面找出：包含 T 所有字母的最小子串。

示例：

输入: S = "ADOBECODEBAANC", T = "AABC"
输出: "BAANC"

输入: S = "a", T = "aa"
输出: ""

说明：
如果 S 中不存这样的子串，则返回空字符串 ""。
如果 S 中存在这样的子串，我们保证它是唯一的答案。

        /*
            我们使用 need 存储还需要 t 的字符某个字符个数
            使用 have 存储 t 的某个字符在窗口中的个数
            使用 valid 记录已经找到的 t 中字符的个数，比如 t = "aabbbc" ，那么最终 valid = 6

            为了不麻烦，直接使用 两个数组代替 map


            过程描述：
            s = "ababacabc", t = "abc"
            need = {1, 1, 1}, valid = 0
            首先我们窗口 left = 0, right = 0，由于窗口范围是 [left, right)，那么表示窗口现在没有任何值
            记录 rch = ss[right]，即是将 right 位置的值加入到窗口中，然后 right++，将窗口右边界右移
            判断 rch 是否是 t 需要的字符，如果需要，那么 have[rch]++，表示滑动窗口内多了 t 需要的这么个字符
            （这过程可能会出现滑动窗口内多出了多余的字符个数，比如 t 只需要 1 个 a，但是滑动窗口添加了 2 个 a，这些多余的字符留到后面再进行缩减）
            再判断 have 字符个数是否超过 t 字符个数，如果没有，那么 valid++，表示又匹配了 t 的一个字符

            当 valid == tlen 时，表示滑动窗口内已经包含了 t 的所有字符，那么我们从左边窗口进行缩减
            s = "ababacabc", t = "abc"
                       ↑
            我们可以发现，当 right++ 后 到该位置时，前面的窗口已经包含了 t 的所有字符，但是存在多余的字符，因此我们需要进行缩减
            我们从 left 开始，我们发现，“ababac” 这个字符串中， 前面的 "aba" 是冗余的，那么我们可以直接刷掉它们，并且每刷掉一个字符，就更新一下最小子串长度
            我们一直刷，直到发现 have 中的某个字符个数比 need 少，那么 vaild-- ，然后停止，表示当前窗口内部不包含 t 的全部字符，需要重新 right 往后移动，找新的字符进行填充

        */
class Solution {
    public String minWindow(String s, String t) {
        int[] need = new int[127];
        int[] have = new int[127];

        char[] ss = s.toCharArray();
        char[] ts = t.toCharArray();

        //存储所需 t 字符个数
        for(char ch : ts){
            need[ch]++;
        }

        int slen = s.length();
        int tlen = t.length();

        //滑动窗口，范围为 [left, right)
        int left = 0;
        int right = 0;

        //记录滑动窗口包含的 t 字符个数
        int valid = 0;

        //记录最小子串的开始索引
        int start = 0;
        //记录最小子串的长度
        int minLen = Integer.MAX_VALUE;
        /*
        上面的都是变量定义，下面才是主代码
        */
        while(right < slen){
            
            //记录当前右边字符
            char rch = ss[right];
            //右窗口右移
            right++;

            if(need[rch] != 0){
                have[rch]++;
                //滑动窗口该字符个数没有达到 need
                if(need[rch] >= have[rch]){
                    valid++;
                }
            }
            //如果滑动窗口已经全部包含 t，那么从 left 进行优化缩减
            while(valid == tlen){
                int tempLen = right - left;
                if(tempLen < minLen){
                    start = left;
                    minLen = tempLen;
                }

                //左边的字符
                char lch = ss[left]; 
                left++;

                if(need[lch] != 0){
                    have[lch]--;
                    if(need[lch] > have[lch]){
                        valid--;
                    }
                }
            }
        }
        return minLen == Integer.MAX_VALUE ? "" : s.substring(start, start + minLen);
    }
}