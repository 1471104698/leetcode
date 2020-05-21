有一个正整数数组 arr，现给你一个对应的查询数组 queries，其中 queries[i] = [Li, Ri]。

对于每个查询 i，请你计算从 Li 到 Ri 的 XOR 值（即 arr[Li] xor arr[Li+1] xor ... xor arr[Ri]）作为本次查询的结果。

并返回一个包含给定查询 queries 所有结果的数组。

 

示例 1：

输入：arr = [1,3,4,8], queries = [[0,1],[1,2],[0,3],[3,3]]
输出：[2,7,14,8] 
解释：
数组中元素的二进制表示形式是：
1 = 0001 
3 = 0011 
4 = 0100 
8 = 1000 
查询的 XOR 值为：
[0,1] = 1 xor 3 = 2 
[1,2] = 3 xor 4 = 7 
[0,3] = 1 xor 3 xor 4 xor 8 = 14 
[3,3] = 8
示例 2：

输入：arr = [4,8,2,10], queries = [[2,3],[1,3],[0,0],[0,3]]
输出：[8,0,4,4]

class Solution {
    public int[] xorQueries(int[] arr, int[][] queries) {
        /*
        前缀异或和

        假设 求 [1, 2]，那么对于 [0, 2] 来说就是多异或了 [0, 0] 这个结果
        根据 两个相同值异或结果为 0，那么我们可以再异或一次 [0, 0] 就将 [0, 0] 给抵消掉了
        */
        int len = arr.length;
        int[] sum = new int[len + 1];

        for(int i = 0; i < len; i++){
            sum[i + 1] = sum[i] ^ arr[i];
        }

        int qlen = queries.length;
        int[] res = new int[qlen];
        
        for(int i = 0; i < qlen; i++){
            //queries[i] = [1, 2]
            res[i] = sum[queries[i][1] + 1] ^ sum[queries[i][0]];
        }
        return res;
    }
}