给定三个字符串 s1, s2, s3, 验证 s3 是否是由 s1 和 s2 交错组成的。

示例 1:

输入: s1 = "aabcc", s2 = "dbbca", s3 = "aadbbcbcac"
输出: true
示例 2:

输入: s1 = "aabcc", s2 = "dbbca", s3 = "aadbbbaccc"
输出: false

class Solution {
    public boolean isInterleave(String s1, String s2, String s3) {
        /*
            dp[i][j] 表示 s1 的前 i 个字符 和 s2 的前 j 个字符能够组成 s3 的前 i + j 个字符
            在判断 s3 的第 i + j 个字符的时候，有两种选择，
            一种是判断 s1[i] == s3[i+j]，这个前提是 dp[i-1][j] 为 true，即 s1 的前 i - 1 个字符 和 s2 的前 j 个字符能够组成 s3 的前 i + j - 1 个字符
            一种是判断 s2[j] == s3[i+j]，这个前提是 dp[i][j-1] 为 true，即 s1 的前 i 个字符 和 s2 的前 j - 1 个字符能够组成 s3 的前 i + j - 1 个字符
        */
        int len1 = s1.length();
        int len2 = s2.length();
        if(len1 + len2 != s3.length()){
            return false;
        }
        if(len1 == 0){
            return s2.equals(s3);
        }
        if(len2 == 0){
            return s1.equals(s3);
        }
        boolean[][] dp = new boolean[len1+1][len2+1];
        dp[0][0] = true; //s1 的第 0 个字符和 s2 的第 0 个字符能够组成 s3 的第 0 个字符，显然，都是空串，因此为 true
        for(int i = 0; i <= len1; i++){
            for(int j = 0; j <= len2; j++){
                if(i == 0 && j == 0){
                    continue;   
                }else if(i == 0){
                    dp[i][j] = dp[i][j-1] && s2.charAt(j-1) == s3.charAt(i+j-1);
                }else if(j == 0){
                    dp[i][j] = dp[i-1][j] && s1.charAt(i-1) == s3.charAt(i+j-1);
                }else{
                    dp[i][j] = dp[i][j-1] && s2.charAt(j-1) == s3.charAt(i+j-1) || dp[i-1][j] && s1.charAt(i-1) == s3.charAt(i+j-1);
                }
            }
        }
        return dp[len1][len2];
    }
}