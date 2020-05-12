给你一个整数数组 arr 。

现需要从数组中取三个下标 i、j 和 k ，其中 (0 <= i < j <= k < arr.length) 。

a 和 b 定义如下：

a = arr[i] ^ arr[i + 1] ^ ... ^ arr[j - 1]
b = arr[j] ^ arr[j + 1] ^ ... ^ arr[k]
注意：^ 表示 按位异或 操作。

请返回能够令 a == b 成立的三元组 (i, j , k) 的数目。

 

示例 1：

输入：arr = [2,3,1,6,7]
输出：4
解释：满足题意的三元组分别是 (0,1,2), (0,2,2), (2,3,4) 以及 (2,4,4)
示例 2：

输入：arr = [1,1,1,1,1]
输出：10
示例 3：

输入：arr = [2,3]
输出：0

/*
思路①：暴力法：时间复杂度：O(n^3): 28 ms
	题目意思：i < j <= k
	即 i 是必定小于 j 的，而 j <= k ，即 j 和 k 可以重合
	
	我们先固定 i , i 从 0 开始遍历，因为必须留一个位置给 j 和 k ，因此终止条件 i < len - 1
	然后固定 j , j 开始只能是 i 的下一个位置，即 j = i + 1，终止条件为 j < len
	我们使用 n1 记录 [i, j - 1] 之间的异或值， 使用 n2 记录 [j, k] 之间的异或值
*/
class Solution {
    public int countTriplets(int[] arr) {
        //a == b -> a ^ b = 0
        int res = 0;
        int len = arr.length;
        for(int i = 0; i < len - 1; i++){
            int n1 = arr[i];
            for(int j = i + 1; j < len; j++){
                int n2 = 0;
                for(int k = j; k < len; k++){
                    n2 ^= arr[k];
                    if(n1 == n2){
                        res++;
                    }
                }   
                //每次结束前 j 会 j++ ，即舍弃当前值，那么这个舍弃的值也应该算入到 i, j - 1 中
                n1 ^= arr[j];
            }
        }
        return res;
    }
}


/*
思路②、使用 a == b -> a ^ b == 0 的性质，时间复杂度 O(n^2)： 1 ms
        a = arr[i] ^ arr[i + 1] ^ ... ^ arr[j - 1]
        b = arr[j] ^ arr[j + 1] ^ ... ^ arr[k]

        a ^ b = arr[i] ^ arr[i + 1] ^ ... ^ arr[j - 1] ^ arr[j] ^ arr[j + 1] ^ ... ^ arr[k]
		当 a == b 时，那么 a ^ b == 0
		1、当 [i, k] 这段区间元素的异或值为 0，那么任意一个元素分割的左半部分和右半部分都相等（左右两边都必须存在一个元素），
			元素个数为 k - i + 1， 则组合数为 k - i
		2、为什么 区间异或值为 0 那么任意分割都相同？
			当 [i, k] = {1, 1, 3, 3} ,
			{1, 1} 的时候异或值为 0，可以分割为 {1} 和 {1}
			{1, 1, 3} 异或值不为 0，跳过
			{1, 1, 3, 3} 异或值为 0，那么可以分割成 {1} 和 {1, 3, 3}、{1, 1} 和 {3, 3}、 {1, 1, 3} 和 {3}
			
			成立原因：存在这么一个定理： (a ^ b) ^ c = a ^ (b ^ c)
			对于区间 [i, k] 异或值为 0，那么我们选择任意一个中间值作为 j， 我们先求出 [j, k] 的异或值 r2
			那么对于 [i, j - 1] 的异或结果 r1， 存在 arr[i] ^ ... ^ arr[j - 1] ^ arr[j] ^ ... ^arr[k]
												   = (arr[i] ^ ... ^ arr[j - 1]) ^ (arr[j] ^ ... ^arr[k])
												   = r1 ^ r2 = 0
			即我们任取 一个 j ,由于 [i, k] 异或值为 0，而 [j, k] 是里面的一部分，因此 [i, j - 1] 异或结果 必须跟 [j, k] 异或结果相同，才能使最终结果相同
			
*/
class Solution {
    public int countTriplets(int[] arr) {

        int len = arr.length;
        int res = 0;
        for(int i = 0; i < len - 1; i++){
            int temp = arr[i];
            for(int k = i + 1; k < len; k++){
                temp ^= arr[k];
                if(temp == 0){
                    res += k - i;
                }
            }
        }
        return res;
    }
}

