你有一套活字字模 tiles，其中每个字模上都刻有一个字母 tiles[i]。返回你可以印出的非空字母序列的数目。

注意：本题中，每个活字字模只能使用一次。

 

示例 1：

输入："AAB"
输出：8
解释：可能的序列为 "A", "B", "AA", "AB", "BA", "AAB", "ABA", "BAA"。
示例 2：

输入："AAABBC"
输出：188
 

提示：

1 <= tiles.length <= 7
tiles 由大写英文字母组成

class Solution {
    int count = 0;
    public int numTilePossibilities(String tiles) {
        //求组合排列数
        char[] chs = tiles.toCharArray();
        Arrays.sort(chs);

        dfs(chs, 0, new boolean[chs.length]);
        return count;
    }

    //全排列
    private void dfs(char[] chs, int n, boolean[] visited){
        int len = chs.length;
        //n 表示添加了 n 个，如果全部添加完成，那么次数 + 1
        if(n == len){
            return;
        }

        for(int i = 0; i < len; i++){
            if(!visited[i]){
                if(i > 0 && chs[i] == chs[i - 1] && !visited[i - 1]){
                    continue;
                }
				/*
				边遍历递归，边计算组合数，每次新添加一个元素，都是一种新的组合方法
				
				比如 AAB，那么我们第一个添加的是 第一个 A，递归过程中就将 后面的能跟 A 组合的都遍历过了，并且每添加一个元素 count++ ，即组合数 + 1
				那么对于第二个 A，我们无需第一个就去添加它了，因为前面的 A 已经将所有组合情况都组合过了，达到去重的效果
				*/
                count++;
                visited[i] = true;
                dfs(chs, n + 1, visited);
                visited[i] = false;
            }
        }
    }
}