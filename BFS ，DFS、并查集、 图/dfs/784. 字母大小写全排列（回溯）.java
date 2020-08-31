
给定一个字符串S，通过将字符串S中的每个字母转变大小写，我们可以获得一个新的字符串。返回所有可能得到的字符串集合。

示例:
输入: S = "a1b2"
输出: ["a1b2", "a1B2", "A1b2", "A1B2"]

输入: S = "3z4"
输出: ["3z4", "3Z4"]

输入: S = "12345"
输出: ["12345"]
注意：

S 的长度不超过12。
S 仅由数字和字母组成。

class Solution {
    List<String> res = new ArrayList<>();
    int temp = 'A' - 'a';
    public List<String> letterCasePermutation(String S) {
        dfs(S.toCharArray(), 0);
        return res;
    }
    private void dfs(char[] chs, int i){
        if(i == chs.length){
            res.add(new String(chs));
            return;
        }
        //如果当前字符是数字，那么直接进行下一轮 dfs
        if(Character.isDigit(chs[i])){
            dfs(chs, i + 1);
            return;
        }
        //记录当前字符
        char t = chs[i];

        //按原本字符进行 dfs
        dfs(chs, i + 1);
        
        //如果是大写那么转小写，如果是小写那么转大写，然后进行 dfs
        if(t >= 'a' && t <= 'z'){
            chs[i] += temp;
        }else{
            chs[i] -= temp;
        }
        dfs(chs, i + 1);
        //将字符改回去
        chs[i] = t;
    }
}