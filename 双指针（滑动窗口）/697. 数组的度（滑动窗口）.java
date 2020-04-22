给定一个非空且只包含非负数的整数数组 nums, 数组的度的定义是指数组里任一元素出现频数的最大值。

你的任务是找到与 nums 拥有相同大小的度的最短连续子数组，返回其长度。

示例 1:

输入: [1, 2, 2, 3, 1]
输出: 2
解释: 
输入数组的度是2，因为元素1和2的出现频数最大，均为2.
连续子数组里面拥有相同度的有如下所示:
[1, 2, 2, 3, 1], [1, 2, 2, 3], [2, 2, 3, 1], [1, 2, 2], [2, 2, 3], [2, 2]
最短连续子数组[2, 2]的长度为2，所以返回2.
示例 2:

输入: [1,2,2,3,1,4,2]
输出: 6
注意:

nums.length 在1到50,000区间范围内。
nums[i] 是一个在0到49,999范围内的整数。

class Solution {
    public int findShortestSubArray(int[] nums) {
        /*
            我们要找的是 包含最大出现频数的元素的连续子数组
            那么这意味着什么？意味着这个子数组最左边和最右边都是该元素
            比如  1, 2, 2, 3, 1, 2, 2, 1
            我们可以看出，出现频数最多的是 2，那么包含这个 2 的所有元素的最小连续子数组就是 2 最先出现的位置 到 2 最后出现的位置这段子数组

            因此，我们可以这么做：
            记录某个数出现的最先出现的位置，并且记录它出现的频数
            记录所有元素中的最大出现频数 maxCount，如果某个元素出现的次数比 maxCount 大，那么进行 maxCount 和 minLen 的更新
        */
        Map<Integer, Integer> leftMap = new HashMap<>();
        Map<Integer, Integer> timeMap = new HashMap<>();

        //记录元素出现的最大频数
        int maxCount = 0;
        //记录出现最大频数的元素的数组长度
        int minLen = Integer.MAX_VALUE;

        for(int i = 0; i < nums.length; i++){
            //更新左边界
            if(!leftMap.containsKey(nums[i])){
                leftMap.put(nums[i], i);
            }
            int time = timeMap.getOrDefault(nums[i], 0) + 1;
            //如果当前元素的出现次数和最大出现次数一致，那么更新长度
            if(maxCount == time){
                minLen = Math.min(minLen, i - leftMap.get(nums[i]) + 1);
            }
            //如果当前元素出现的次数比最大出现次数大，那么更新最大出现次数和长度
            if(maxCount < time){
                maxCount = time;
                minLen = i - leftMap.get(nums[i]) + 1;
            }
            timeMap.put(nums[i], time);
        }
        return minLen;
    }
}