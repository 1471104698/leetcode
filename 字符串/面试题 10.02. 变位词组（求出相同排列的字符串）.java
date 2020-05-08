
编写一种方法，对字符串数组进行排序，将所有变位词组合在一起。变位词是指字母相同，但排列不同的字符串。

注意：本题相对原题稍作修改

示例:

输入: ["eat", "tea", "tan", "ate", "nat", "bat"],
输出:
[
  ["ate","eat","tea"],
  ["nat","tan"],
  ["bat"]
]
说明：

所有输入均为小写字母。
不考虑答案输出的顺序。


//思路①、不排序，直接获取 各个字母的个数组合构成字符串，作为 key
class Solution {
    public List<List<String>> groupAnagrams(String[] strs) {
        List<List<String>> res = new ArrayList<>();
        Map<String, Integer> map = new HashMap<>();
        for(String str : strs){
            int[] chs = new int[26];
            for(char ch : str.toCharArray()){
                chs[ch - 'a']++;
            }
            StringBuilder sb = new StringBuilder();
            for(int i = 0; i < 26; i++){
                sb.append((char)('a' + i)).append(chs[i]);
            }
            if(map.containsKey(sb.toString())){
                res.get(map.get(sb.toString())).add(str);
            }else{
                map.put(sb.toString(), res.size());
                res.add(new ArrayList<>(Arrays.asList(str)));
            }
        }
        return res;
    }

}

//思路②、获取 字符数组然后进行排序，作为 key
class Solution {
    public List<List<String>> groupAnagrams(String[] strs) {
        List<List<String>> res = new ArrayList<>();
        Map<String, Integer> map = new HashMap<>();
        for(String str : strs){
            char[] chs = str.toCharArray();
            Arrays.sort(chs);
            String key = new String(chs);
            if(map.containsKey(key)){
                res.get(map.get(key)).add(str);
            }else{
                map.put(key, res.size());
                res.add(new ArrayList<>(Arrays.asList(str)));
            }
        }
        return res;
    }

}