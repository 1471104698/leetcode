对于非负整数 X 而言，X 的数组形式是每位数字按从左到右的顺序形成的数组。例如，如果 X = 1231，那么其数组形式为 [1,2,3,1]。

给定非负整数 X 的数组形式 A，返回整数 X+K 的数组形式。

 

示例 1：

输入：A = [1,2,0,0], K = 34
输出：[1,2,3,4]
解释：1200 + 34 = 1234
示例 2：

输入：A = [2,7,4], K = 181
输出：[4,5,5]
解释：274 + 181 = 455
示例 3：

输入：A = [2,1,5], K = 806
输出：[1,0,2,1]
解释：215 + 806 = 1021
示例 4：

输入：A = [9,9,9,9,9,9,9,9,9,9], K = 1
输出：[1,0,0,0,0,0,0,0,0,0,0]
解释：9999999999 + 1 = 10000000000


class Solution {
    public List<Integer> addToArrayForm(int[] A, int K) {
        LinkedList<Integer> res = new LinkedList<>();

        int cin = 0;
        for(int i = A.length - 1; i >= 0; i--, K /= 10){
            int sum = A[i] + K % 10 + cin;
            cin = sum / 10;
            res.addFirst(sum % 10);
        }
        //处理 A = {99}, K = 1 或者 A = {1}, K = 99 之类的情况
        while(K != 0 || cin != 0){
            int sum = K % 10 + cin;
            res.addFirst(sum % 10);
            cin = sum / 10;
            K /= 10;
        }

        return res;
    }
}