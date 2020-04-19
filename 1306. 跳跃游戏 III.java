/*
这里有一个非负整数数组 arr，你最开始位于该数组的起始下标 start 处。当你位于下标 i 处时，你可以跳到 i + arr[i] 或者 i - arr[i]。

请你判断自己是否能够跳到对应元素值为 0 的 任意 下标处。

注意，不管是什么情况下，你都无法跳到数组之外。


输入：arr = [4,2,3,0,3,1,2], start = 5
输出：true
解释：
到达值为 0 的下标 3 有以下可能方案： 
下标 5 -> 下标 4 -> 下标 1 -> 下标 3 
下标 5 -> 下标 6 -> 下标 4 -> 下标 1 -> 下标 3 

输入：arr = [4,2,3,0,3,1,2], start = 0
输出：true 
解释：
到达值为 0 的下标 3 有以下可能方案： 
下标 0 -> 下标 4 -> 下标 1 -> 下标 3

输入：arr = [3,0,2,1,2], start = 2
输出：false
解释：无法到达值为 0 的下标 1 处。 
*/

class Solution {
    public boolean canReach(int[] arr, int start) {
        /*
        回溯，向左右两边跳，如果跳回原地，那么表示不行
        */
        return dfs(arr, start, new boolean[arr.length]);
    }
    private boolean dfs(int[] arr, int i, boolean[] visited){
        if(i < 0 || i >= arr.length || visited[i]){
            return false;
        }
        if(arr[i] == 0){
            return true;
        }
        visited[i] = true;
        boolean flag = dfs(arr, i + arr[i], visited) || dfs(arr, i - arr[i], visited);
        visited[i] = false;
        return flag;
    }
}

