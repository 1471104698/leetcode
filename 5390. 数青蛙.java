给你一个字符串 croakOfFrogs，它表示不同青蛙发出的蛙鸣声（字符串 "croak" ）的组合。由于同一时间可以有多只青蛙呱呱作响，所以 croakOfFrogs 中会混合多个 “croak” 。请你返回模拟字符串中所有蛙鸣所需不同青蛙的最少数目。
注意：要想发出蛙鸣 "croak"，青蛙必须 依序 输出 ‘c’, ’r’, ’o’, ’a’, ’k’ 这 5 个字母。如果没有输出全部五个字母，那么它就不会发出声音。
如果字符串 croakOfFrogs 不是由若干有效的 "croak" 字符混合而成，请返回 -1 。


输入：croakOfFrogs = "croakcroak"
输出：1 
解释：一只青蛙 “呱呱” 两次

输入：croakOfFrogs = "crcoakroak"
输出：2 
解释：最少需要两只青蛙，“呱呱” 声用黑体标注
第一只青蛙 "crcoakroak"
第二只青蛙 "crcoakroak"

输入：croakOfFrogs = "croakcrook"
输出：-1
解释：给出的字符串不是 "croak" 的有效组合。

class Solution {
    public int minNumberOfFrogs(String croakOfFrogs) {
        /*
        直接记录 c r o a k 字符出现的个数
        因为 c 最先出现， k 最后出现，因此 一个 c 和 k 能够构成 croak
        如果遍历过程中 c 和 k 出现的次数不相等，那么表示至少 c - k 只青蛙
        比如 “crcoakroak”
                ↑
        遍历到该位置时，因为 c 出现了 2 次，而 k 出现了一次，因此至少需要 2 只青蛙

        遍历过程中，必须保证 c 出现次数不低于 r ，r 出现次数不低于 o ，o 出现次数不低于 a, a 出现次数不低于 k
        */
        int minCount = 1;
        int c = 0, r = 0, o = 0, a = 0, k = 0;
        for(char ch : croakOfFrogs.toCharArray()){
            if(ch == 'c') c++;
            else if(ch == 'r') r++;
            else if(ch == 'o') o++;
            else if(ch == 'a') a++;
            else k++;
            minCount = Math.max(minCount, c - k);
            //保证次数出现的相对顺序
            if(c < r || r < o || o < a || a < k){
                return -1;
            }
        }
        //最终各个字符数量必须相等
        if(c == a && c == r && c == o && c == a && c == k){
            return minCount;
        }else{
            return -1;
        }
    }
}