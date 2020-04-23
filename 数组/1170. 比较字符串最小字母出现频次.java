我们来定义一个函数 f(s)，其中传入参数 s 是一个非空字符串；该函数的功能是统计 s  中（按字典序比较）最小字母的出现频次。

例如，若 s = "dcce"，那么 f(s) = 2，因为最小的字母是 "c"，它出现了 2 次。

现在，给你两个字符串数组待查表 queries 和词汇表 words，请你返回一个整数数组 answer 作为答案，其中每个 answer[i] 是满足 f(queries[i]) < f(W) 的词的数目，W 是词汇表 words 中的词。

 

示例 1：

输入：queries = ["cbd"], words = ["zaaaz"]
输出：[1]
解释：查询 f("cbd") = 1，而 f("zaaaz") = 3 所以 f("cbd") < f("zaaaz")。
示例 2：

输入：queries = ["bbb","cc"], words = ["a","aa","aaa","aaaa"]
输出：[1,2]
解释：第一个查询 f("bbb") < f("aaaa")，第二个查询 f("aaa") 和 f("aaaa") 都 > f("cc")。
 

提示：

1 <= queries.length <= 2000
1 <= words.length <= 2000
1 <= queries[i].length, words[i].length <= 10
queries[i][j], words[i][j] 都是小写英文字母


class Solution {
    public int[] numSmallerByFrequency(String[] queries, String[] words) {
        /*
        思路：
        先求出 queries 数组 和 words 数组各自的最小字母出现频次
        结果存储在 q 数组 和 w 数组中
        然后进行遍历，找出 w 数组 中 比 q[i] 大的元素个数

        这里可以进行一下优化：（先看代码，理解思想）
        我们可以对 w 数组进行排序，然后当遇到 w[j] > q[i] 时直接 break,因为后面必定都是比 q[i] 长的，因此比 q[i] 大的元素个数为 wlen - j
        */
        int qlen = queries.length;
        int wlen = words.length;

        int[] q = new int[qlen];
        int[] w = new int[wlen];

        for(int i = 0; i < qlen; i++){
            q[i] = helper(queries[i]);
        }
        for(int i = 0; i < wlen; i++){
            w[i] = helper(words[i]);
        }
        Arrays.sort(w);
        int[] res = new int[qlen];
        for(int i = 0; i < qlen; i++){
            int j = 0;
            for(; j < wlen; j++){
                if(w[j] > q[i]){
                    break;
                }
            }
            res[i] = wlen - j;
        }
        return res;
    }
    /*
    得到某个字符串最小字母的频次
    统计每个字符出现的次数，然后遍历 chs，最先不是 0 就是最小字母的频次，直接返回即可
    */
    private int helper(String str){
        int[] chs = new int[26];
        for(char ch : str.toCharArray()){
            chs[ch - 'a']++;
        }
        for(int num : chs){
            if(num != 0){
                return num;
            }
        }
        return 0;
    }
}