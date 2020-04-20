给定一组单词words，编写一个程序，找出其中的最长单词，且该单词由这组单词中的其他单词组合而成。若有多个长度相同的结果，返回其中字典序最小的一项，若没有符合要求的单词则返回空字符串。

示例：

输入： ["cat","banana","dog","nana","walk","walker","dogwalker"]
输出： "dogwalker"
解释： "dogwalker"可由"dog"和"walker"组成。
提示：

0 <= len(words) <= 100
1 <= len(words[i]) <= 100

        /*
            将字符串按长度降序排列，即长度越长，则排在越前面
            如果长度相等，则按字典序升序排列，即如果字符串越小，则排在越前面

            那么，按这个顺序，我们从头开始遍历字符串，判断字符串是否能由其他单词组成

            这里使用 dp
        */
class Solution {
    public String longestWord(String[] words) {

        Arrays.sort(words, (a, b) -> (a.length() == b.length() ? 
		(a + b).compareTo(b + a) : b.length() - a.length()));

        Set<String> set = new HashSet<>(Arrays.asList(words));

        //判断一个字符是否能由其他字符串组成
        for (String word : words) {
            int len = word.length();
            //使用 dp 判断一个单词是否能由其他单词组成
            boolean[] dp = new boolean[len + 1];
            dp[0] = true;
            /*
            这里使用一个变量用来记录 i == len 的时候，
            判断是否是 j == 0 的情况，因为如果 j == 0，那么意味着 word.substring(0, len) 是它本身，即是在 set 中找到自己
            那么就不是由其他字符串构成的
            */
            int idx = -1;
            for (int i = 1; i <= len; i++) {
                for (int j = i - 1; j >= 0; j--) {
                    if (dp[j] && set.contains(word.substring(j, i))) {
                        dp[i] = true;
                        idx = j;
                        break;
                    }
                }
            }
            //如果 idx 为 0，表示是找到了自身，那么就不是其他字符串构成
            if(dp[len] && idx != 0){
                return word;
            }
        }
        return "";
    }
}