
给定两个单词（beginWord 和 endWord）和一个字典 wordList，找出所有从 beginWord 到 endWord 的最短转换序列。转换需遵循如下规则：

每次转换只能改变一个字母。
转换过程中的中间单词必须是字典中的单词。
说明:

如果不存在这样的转换序列，返回一个空列表。
所有单词具有相同的长度。
所有单词只由小写字母组成。
字典中不存在重复的单词。
你可以假设 beginWord 和 endWord 是非空的，且二者不相同。

示例 1:
输入:
beginWord = "hit",
endWord = "cog",
wordList = ["hot","dot","dog","lot","log","cog"]
输出:
[
  ["hit","hot","dot","dog","cog"],
  ["hit","hot","lot","log","cog"]
]

示例 2:
输入:
beginWord = "hit"
endWord = "cog"
wordList = ["hot","dot","dog","lot","log"]
输出: []
解释: endWord "cog" 不在字典中，所以不存在符合要求的转换序列。


class Solution {
    public List<List<String>> findLadders(String beginWord, String endWord, List<String> wordList) {
        /*
        最短路径：使用 BFS
        单词接龙 I queue 存储的是每层的元素
        而这里要求的是路径，因此我们这里 queue 存储的是每层的 list 
        例子：
        beginWord = "hit",
        endWord = "cog",
        wordList = ["hot","dot","dog","lot","log","cog"]

        第一层：{hit}
        第二层：{hit, hot}
        第三层：{hit, hot, dot}, {hit, hot, lot}
        
        因此 seen 存储的是已经遍历过的 list
        */
        List<List<String>> res = new ArrayList<>();

        Set<String> wordSet = new HashSet<>(wordList);
        if(!wordSet.contains(endWord)){
            return res;
        }
        Queue<List<String>> queue = new LinkedList<>();
        queue.add(Arrays.asList(beginWord));

        Set<String> seen = new HashSet<>();

        //是否已经到达目标层，可以结束遍历
        boolean flag = false;
        while(!queue.isEmpty() && !flag){
            int size = queue.size();
            Set<String> set = new HashSet<>();
            while(size-- > 0){
                List<String> cur = queue.poll();
                //得到最后一个字符
                String str = cur.get(cur.size() - 1);
                char[] chs = str.toCharArray();
                //找到所有与它相差一个字符的字典
                for(int i = 0; i < chs.length; i++){
                    char temp = chs[i];
                    for(char ch = 'a'; ch <= 'z'; ch++){
                        if(ch == temp) continue;
                        chs[i] = ch;
                        String newStr = new String(chs);
                        List<String> tempList = new ArrayList<>(cur);
                        tempList.add(newStr);

                        if(!seen.contains(newStr) && wordSet.contains(newStr)){
                            if(newStr.equals(endWord)){
                                flag = true;
                                res.add(tempList);
                            }
                            set.add(newStr);
                            queue.add(tempList);
                        }
                    }
                    chs[i] = temp;
                }
            }
            seen.addAll(set);
        }
        return res;
    }
}