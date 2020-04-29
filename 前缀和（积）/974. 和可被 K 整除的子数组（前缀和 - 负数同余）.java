给定一个整数数组 A，返回其中元素之和可被 K 整除的（连续、非空）子数组的数目。

 

示例：

输入：A = [4,5,0,-2,-3,1], K = 5
输出：7
解释：
有 7 个子数组满足其元素之和可被 K = 5 整除：
[4, 5, 0, -2, -3, 1], [5], [5, 0], [5, 0, -2, -3], [0], [0, -2, -3], [-2, -3]
 

提示：

1 <= A.length <= 30000
-10000 <= A[i] <= 10000
2 <= K <= 10000

class Solution {
    public int subarraysDivByK(int[] A, int K) {
        /*
        (preSum[i] − preSum[j]) mod k == 0 ⟺ preSum[i] mod k == preSum[j] mod k

        我们可以使用 map 记录 preSum[i] 的个数

        4,5,0,-2,-3,1
      0 4 9 9   
        */

        Map<Integer, Integer> map = new HashMap<>();
        map.put(0, 1);
        int c = 0;
        int sum = 0;
        for(int num : A){
            /* 
            这里不直接用 sum[i] % K，是因为 sum[i] 会有负数，根据同余定理：
            比如 len = 6 ，
            num = -2 那么根据同余 它的余数为 4
            相当于 num % len + len = -2 % 6 + 6 = -2 + 6 = 4
            */
            //负数同余，这里先取 % 是因为可能 num = -10000，而 K = 10,所以需要先取余缩减到 -[0, K]
            num = num % K + K;
            sum = (sum + num) % K;
            int time = map.getOrDefault(sum, 0);
            c += time;
            map.put(sum, time + 1);
        }
        return c;
    }
}