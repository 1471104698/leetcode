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