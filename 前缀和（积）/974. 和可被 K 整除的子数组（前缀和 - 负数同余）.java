给定一个整数数组 A，返回其中元素之和可被 K 整除的（连续、非空）子数组的数目。

 

示例：

输入：A = [4,5,0,-2,-3,1], K = 5
输出：7
解释：

class Solution {
    public int subarraysDivByK(int[] A, int K) {
        /*
        (preSum[i] - preSum[j]) mod k == 0  ⟺ preSum[i] mod k == preSum[j] mod k


        如果 sum 为正数，那么直接 mod k 即可
        如果 sum 为负数，那么需要转换为转换为正数，
            比如 sum = -1,根据同余定理： 如果 k = 6, 那么 -1 的余数为 5， 即 (-1 = 5) mod 6，则可转换为 sum += k = -1 + 6 = 5
            比如 sum = -7， 我们可以先 mod k, 再 + k, 即 sum = sum mod k + k = (-7 mod 6) + 6 = -1 + 6 = 5
        综上，我们只需要 
        1、sum += num;
        2、(sum % K + K) % K
        */
		
		//这里我们可以使用 map，但题目说了 -10000 <= A[i] <= 10000，2 <= K <= 10000， 那么最终 sum 的范围为 [0, 10000]，因为负数也会转换为整数
		//因此我们只需要开 10001 大小的数组存放数据即可
        int[] arr = new int[10001];

        arr[0] = 1;

        int res = 0;

        int sum = 0;
        for(int num : A){
            sum += num;
            sum = (sum % K + K) % K;
            res += arr[sum];
           arr[sum]++;
        }
        return res;
    }   
}