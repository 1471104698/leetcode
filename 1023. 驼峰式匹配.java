如果我们可以将小写字母插入模式串 pattern 得到待查询项 query，那么待查询项与给定模式串匹配。
/*
（我们可以在任何位置插入每个字符，也可以插入 0 个字符）
这句话什么意思？ 比如 pattern = "FoBaT" ，那么我们可以在任何位置插入任何小写字符，
即匹配串和模式串大写字母必须相同，小写字母匹配串可以比模式串多，但模式串的小写字母需要保证相对顺序
*/

给定待查询列表 queries，和模式串 pattern，返回由布尔值组成的答案列表 answer。只有在待查项 queries[i] 与模式串 pattern 匹配时， answer[i] 才为 true，否则为 false。

 

示例 1：

输入：queries = ["FooBar","FooBarTest","FootBall","FrameBuffer","ForceFeedBack"], pattern = "FB"
输出：[true,false,true,true,false]
示例：
"FooBar" 可以这样生成："F" + "oo" + "B" + "ar"。
"FootBall" 可以这样生成："F" + "oot" + "B" + "all".
"FrameBuffer" 可以这样生成："F" + "rame" + "B" + "uffer".
示例 2：

输入：queries = ["FooBar","FooBarTest","FootBall","FrameBuffer","ForceFeedBack"], pattern = "FoBa"
输出：[true,false,true,false,false]
解释：
"FooBar" 可以这样生成："Fo" + "o" + "Ba" + "r".
"FootBall" 可以这样生成："Fo" + "ot" + "Ba" + "ll".
示例 3：

输出：queries = ["FooBar","FooBarTest","FootBall","FrameBuffer","ForceFeedBack"], pattern = "FoBaT"
输入：[false,true,false,false,false]
解释： 
"FooBarTest" 可以这样生成："Fo" + "o" + "Ba" + "r" + "T" + "est".
 

提示：

1 <= queries.length <= 100
1 <= queries[i].length <= 100
1 <= pattern.length <= 100
所有字符串都仅由大写和小写英文字母组成。

/*
思路①、
先统计模式串的大写字母个数
再统计匹配串的大写字母个数，如果两者个数不相同，那么必定不能匹配
如果相同，那么遍历 匹配串每个字符，逐一与 模式串每个字符进行匹配
如果模式串所有字符都能匹配到，那么匹配成功
*/
class Solution {
    public List<Boolean> camelMatch(String[] queries, String pattern) {
        List<Boolean> res = new ArrayList<>();

        //获取 pattern 字符串存在多少个大写字母
        int count = 0;
        for(int i = 0; i < pattern.length(); i++){
            char ch = pattern.charAt(i);
            if(ch >= 'A' && ch <= 'Z'){
                count++;
            }
        }

        for(String str : queries){
            //获取当前字符串的大写字母个数
            int c = 0;
            for(char ch : str.toCharArray()){
                if(ch >= 'A' && ch <= 'Z'){
                    c++;
                }
            }
            //如果两个字符串的大写字母个数不同，那么必定不能匹配
            if(c != count){
                res.add(false);
                continue;
            }
            //将 str 和 pattern 逐字符进行匹配
            int i = 0;
            for(char ch : str.toCharArray()){
                if(ch == pattern.charAt(i)){
                    i++;
                }
                if(i == pattern.length()){
                    break;
                }
            }
            //如果和 pattern 完全匹配，那么添加 true
            if(i == pattern.length()){
                res.add(true);
            }else{
                res.add(false);
            }
        }
        return res;
    }
}