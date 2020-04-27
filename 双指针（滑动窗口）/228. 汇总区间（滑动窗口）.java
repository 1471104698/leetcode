给定一个无重复元素的有序整数数组，返回数组区间范围的汇总。

示例 1:

输入: [0,1,2,4,5,7]
输出: ["0->2","4->5","7"]
解释: 0,1,2 可组成一个连续的区间; 4,5 可组成一个连续的区间。
示例 2:

输入: [0,2,3,4,6,8,9]
输出: ["0","2->4","6","8->9"]
解释: 2,3,4 可组成一个连续的区间; 8,9 可组成一个连续的区间。

class Solution {
    public List<String> summaryRanges(int[] nums) {
        /*
        滑动窗口
        如果 nums[right] != nums[right - 1] + 1，那么我们就添加 [left, right - 1]，然后重新将 left 指向 right 开始新的滑动窗口

        注意：字符串拼接使用 StringBuilder ，不要直接拼接 res.add(nums[left] + "->" + nums[right - 1])
            使用 sb 速度： 0ms， 字符串拼接： 10ms
        */
        List<String> res = new ArrayList<>();

        int len = nums.length;
        if(len == 0){
            return res;
        }

        int left = 0;
        int right = 1;

        while(right < len){
            if(nums[right] != nums[right - 1] + 1){
                //只有一个值
                if(left == right - 1){
                    res.add(String.valueOf(nums[left]));
                }else {
                    StringBuilder sb = new StringBuilder();
                    sb.append(nums[left]).append("->").append(nums[right - 1]);
                    res.add(sb.toString());
                }
                left = right;
            }
            right++;
        }
        /*
        处理最后的值
        */
        if(left == len - 1){
            res.add(String.valueOf(nums[left]));
        }else{
            StringBuilder sb = new StringBuilder();
            sb.append(nums[left]).append("->").append(nums[right - 1]);
            res.add(sb.toString());
        }
        return res;
    }
}