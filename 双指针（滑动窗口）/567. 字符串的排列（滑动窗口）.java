给定两个字符串 s1 和 s2，写一个函数来判断 s2 是否包含 s1 的排列。
换句话说，第一个字符串的排列之一是第二个字符串的子串。

//字符串 s1 的任意排列 是 s2 的子串（注意是子串，不是子序列）

示例1:

输入: s1 = "ab" s2 = "eidbaooo"
输出: True
解释: s2 包含 s1 的排列之一 ("ba").
 

示例2:

输入: s1= "ab" s2 = "eidboaoo"
输出: False
 

注意：

输入的字符串只包含小写字母
两个字符串的长度都在 [1, 10,000] 之间

class Solution {
    public boolean checkInclusion(String s1, String s2) {
        /*
            滑动窗口

            s1 = "abb" s2 = "eidbbbbcbbbaooo"
        */
        int[] need = new int[26];
        int[] have = new int[26];
        for(char ch : s1.toCharArray()){
            need[ch - 'a']++;
        }

        int len1 = s1.length();
        int len2 = s2.length();

        int valid = 0;

        int left = 0;
        int right = 0;

        while(right < len2){
            int rch = s2.charAt(right) - 'a';
            right++;

            if(need[rch] != 0){
                have[rch]++;
                if(need[rch] >= have[rch]){
                    valid++;
                }
            }else{  //如果当前字符不在 s1 中，那么舍弃掉前面的所有字符，数据重置，注意将 left 指向 right，重新开始
                Arrays.fill(have, 0);
                valid = 0;
                left = right;
            }          

            //当滑动窗口某个字符数量超过所需数量，那么从 left 开始排除掉多余的字符
            while(need[rch] < have[rch]){
                int lch = s2.charAt(left) - 'a';
                have[lch]--;
                left++;
                if(need[lch] > have[lch]){
                    valid--;
                }
            }

            if(valid == len1){
                return true;
            }
        }
        return false;
    }
}