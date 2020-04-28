给你一个整数数组 nums 和一个正整数 k，请你判断是否可以把这个数组划分成一些由 k 个连续数字组成的集合。
如果可以，请返回 True；否则，返回 False。

 

示例 1：

输入：nums = [1,2,3,3,4,4,5,6], k = 4
输出：true
解释：数组可以分成 [1,2,3,4] 和 [3,4,5,6]。
示例 2：

输入：nums = [3,2,1,2,3,4,3,4,5,9,10,11], k = 3
输出：true
解释：数组可以分成 [1,2,3] , [2,3,4] , [3,4,5] 和 [9,10,11]。
示例 3：

输入：nums = [3,3,2,2,1,1], k = 3
输出：true
示例 4：

输入：nums = [1,2,3,4], k = 3
输出：false
解释：数组不能分成几个大小为 3 的子数组。

class Solution {
    Map<Integer, Integer> map;
    public boolean isPossibleDivide(int[] nums, int k) {
        /*
        记录某个数字出现的次数，根据频率判断是否能够组成

        如果可能拆分数组中的最小数字为V，则数字V + 1，V + 2，... V + k-1也必须包含该数字。
        
        比如
        nums = [1,2,3,3,4,4,5,6], k = 4
        我们先遍历到 1，它的次数不为 0，那么表示它必须进行组合，那么需要找到 [2, 4] 进行组合，如果其中一个次数为 0，那么返回 false
        否则将它们的次数减一

        继续遍历，经过 上一轮的组合， 2 的次数为 0，跳过，然后 3 ，经过上一轮，它的次数为 1 ，那么需要找到 [4, 6] 进行组合，同理，如果其中任何一个次数为 0，那么直接返回 false
        否则次数减一
        */
        Arrays.sort(nums);

        int len = nums.length;

        if(len % k != 0){
            return false;
        }

        map = new HashMap<>();
		//记录每个数字出现的次数
        for(int num : nums){
           put(num, getTime(num) + 1);
        }
		
		//从最小值开始遍历，遇到次数不为 0 的，那么找到后面的值进行组合
        for (int num : nums) {
            if (getTime(num) != 0) {
                for (int j = num; j < num + k; j++) {
                    int time = getTime(j);
                    if (time == 0) {
                        return false;
                    }
                    put(j, time - 1);
                }
            }
        }
        return true;
    }
    private int getTime(int num){
        return map.getOrDefault(num, 0);
    }
    private void put(int num, int time){
        map.put(num, time);
    }
}