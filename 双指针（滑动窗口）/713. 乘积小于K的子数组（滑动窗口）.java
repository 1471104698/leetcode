给定一个正整数数组 nums。

找出该数组内乘积小于 k 的连续的子数组的个数。

示例 1:

输入: nums = [10,5,2,6], k = 100
输出: 8
解释: 8个乘积小于100的子数组分别为: [10], [5], [2], [6], [10,5], [5,2], [2,6], [5,2,6]。
需要注意的是 [10,5,2] 并不是乘积小于100的子数组。
说明:

0 < nums.length <= 50000
0 < nums[i] < 1000
0 <= k < 10^6


class Solution {
    public int numSubarrayProductLessThanK(int[] nums, int k) {
        /*
        这里使用滑动窗口，具体思路自己懂的

        一个长度为 n 数组，它的连续子数组个数为 1 + 2 + 3 + ... + n = n * (n - 1) / 2
        但下面为什么我们写成 c += right - left ？ 而不是 n * (n - 1) / 2
        因为有点子数组会发生重叠
        nums = [10,5,2,6], k = 100
        比如 [10, 5] 和 [5, 2, 6] 其中 [5] 会发生重叠

        而 c += right - left 的思路是什么？
        首先滑动窗口只包含 10，子数组：[10],那么 c += 1 - 0 = 1，
        然后添加 5，子数组：[10] [5] [10, 5]，由于前面添加了 [10]，因此我们只需要添加 [5] 和 [10, 5] 两个
        而这刚好是什么？刚好是以 5 结尾的连续子数组的个数，即 right - left
        */
        int len = nums.length;

        int c = 0;

        int left = 0;
        int right = 0;

        int sum = 1;
        while(right < len){
            sum *= nums[right];
            right++;

            while(left < right && sum >= k){
                sum /= nums[left];
                left++;
            }
            c += right - left;
        }
        //1 2 3 4       5
        return c;
    }
}