一个 「开心字符串」定义为：

仅包含小写字母 ['a', 'b', 'c'].
对所有在 1 到 s.length - 1 之间的 i ，满足 s[i] != s[i + 1] （字符串的下标从 1 开始）。
比方说，字符串 "abc"，"ac"，"b" 和 "abcbabcbcb" 都是开心字符串，但是 "aa"，"baa" 和 "ababbc" 都不是开心字符串。

给你两个整数 n 和 k ，你需要将长度为 n 的所有开心字符串按字典序排序。

请你返回排序后的第 k 个开心字符串，如果长度为 n 的开心字符串少于 k 个，那么请你返回 空字符串 。

示例 1：

输入：n = 1, k = 3
输出："c"
解释：列表 ["a", "b", "c"] 包含了所有长度为 1 的开心字符串。按照字典序排序后第三个字符串为 "c" 。
示例 2：

输入：n = 1, k = 4
输出：""
解释：长度为 1 的开心字符串只有 3 个。
示例 3：

输入：n = 3, k = 9
输出："cab"
解释：长度为 3 的开心字符串总共有 12 个 ["aba", "abc", "aca", "acb", "bab", "bac", "bca", "bcb", "cab", "cac", "cba", "cbc"] 。第 9 个字符串为 "cab"
示例 4：

输入：n = 2, k = 7
输出：""
示例 5：

输入：n = 10, k = 100
输出："abacbabacb"
 

提示：

1 <= n <= 10
1 <= k <= 100

/*
思路①、找到所有长度为 n 的开心字符串，然后进行排序，获取第 k 个
直接按 a、b、c 的顺序进行填充，记录上一个填充的是哪个字符（14 ms）
*/
class Solution {
    List<String> res;
    public String getHappyString(int n, int k) {
        /*
        递归生成长度为 n 的开心字符串，然后排序
        */
        res = new ArrayList<>();
        dfs(new StringBuilder(), n, -1);
        if(res.size() < k){
            return "";
        }
        return res.get(k - 1);
    }
    char[] chs = {'a', 'b', 'c'};
    private void dfs(StringBuilder sb, int n, int pos){
        if(n == 0){
            res.add(sb.toString());
            return;
        }
        int len = sb.length();
        for(int i = 0; i < 3; i++){
            if(pos == i){
                continue;
            }
            sb.append(chs[i]);
            dfs(sb, n - 1, i);
            sb.setLength(len);
        }
    }
}

/*
思路②、其实我们遍历的过程就是按照 a 、b、c 的顺序添加的，本身就是已经排好序的
		那么我们直接遍历到第 k 个返回即可
*/
class Solution {
    int k;
    public String getHappyString(int n, int k) {

        this.k = k;
        return dfs(new StringBuilder(), n, -1);
    }
    char[] chs = {'a', 'b', 'c'};
    private String dfs(StringBuilder sb, int n, int pos){
        String res = "";
		
		//当添加长度为 n 了，那么 k-- ，如果减为 0 ，表示是第 k 个字符串，那么直接返回即可
        if(n == 0){
            k--;
            if(k == 0){
                return sb.toString();
            }
            return res;
        }
        int len = sb.length();
        for(int i = 0; i < 3; i++){
            if(pos == i){
                continue;
            }
            sb.append(chs[i]);
            res = dfs(sb, n - 1, i);
            if(!"".equals(res)){
                break;
            }
            sb.setLength(len);
        }
        return res;
    }
}