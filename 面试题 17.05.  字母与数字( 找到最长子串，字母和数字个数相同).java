给定一个放有字符和数字的数组，找到最长的子数组，且包含的字符和数字的个数相同。

返回该子数组，若存在多个最长子数组，返回左端点最小的。若不存在这样的数组，返回一个空数组。

示例 1:

输入: ["A","1","B","C","D","2","3","4","E","5","F","G","6","7","H","I","J","K","L","M"]

输出: ["A","1","B","C","D","2","3","4","E","5","F","G","6","7"]
示例 2:

输入: ["A","A"]

输出: []
提示：

array.length <= 100000

/*
        "A","1","B","C","D","2","3","4","E","5","F","G","6","7","H","I","J","K","L","M"
    a    1   1   2   3   4   4   4   4   5   5   6   7   7   7   8   9   10
    b    0   1   1   1   1   2   3   4   4   5   5   5   6   7   7   7
preSum   1   0   1   2   3   2   1   0   1   0   1   2   1   0   1   2
         ↑       ↑
         i       j
        比如前缀和为 1 ，i 和 j 位置前缀和都为 1，那么 (i, j] 这段字母和数字个数相同
        因此，我们记录最左边的 1 ，即固定该位置，然后后面每遍历到新的前缀和为 1 的位置 ，就获取长度
        其他 前缀和值 类似

        为什么我们初始化 dp 长度为  len * 2 + 1？
        因为当全部只有数字的时候，那么 前缀和为 -len， 当全部为字母的时候，前缀和为 len
        因为下标没有 负数，因此向右偏移 len 长度

        特殊的，如果前缀和为 0，那么意味着 [0, i] 整个子数组都满足条件，那么长度为 i + 1，
        那么我们可以初始化 dp[0] = -1，这样 i - (-1) = i + 1，因为向右偏移 len 长度，因此 dp[len] = -1
*/

class Solution {
    public String[] findLongestSubarray(String[] array) {

        int len = array.length;

        //dp[i] 记录 i 位置 [0, i] 的字母和数字对应情况
        int[] dp = new int[len * 2 + 1];
        //初始化为 -2，表示没有固定好第一个位置
        Arrays.fill(dp, -2);
        //初始化 0 位置
        dp[len] = -1;

        //字母个数
        int a = 0;
        //数字个数
        int b = 0;

        //起始索引
        int start = 0;
        //结束索引
        int end = 0;
        //满足条件的最长子数组长度
        int maxLen = 0;
		
        for(int i = 1; i <= len; i++){
            if(Character.isDigit(array[i - 1].charAt(0))){
                b++;
            }else{
                a++;
            }

            int diff = a - b + len;
            if(dp[diff] == -2){
                dp[diff] = i;
            }else{
                int tempLen = i - dp[diff];
                if(maxLen < tempLen){
                    start = dp[diff];
                    end = i;
                    maxLen = tempLen;
                }
            }
        }
        /*
        Arrays.copyOfRange(T[] arr, int start, int end) 截取 arr 数组的 [start, end] 作为一个新数组返回
        这里为什么是 Math.max(start, 0) ？ 因为如果最长子数组是前缀和为 0 产生的，那么 start = -1
        */
        return Arrays.copyOfRange(array, Math.max(start, 0), end);
    }
}