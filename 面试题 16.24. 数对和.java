设计一个算法，找出数组中两数之和为指定值的所有整数对。一个数只能属于一个数对。

示例 1:

输入: nums = [5,6,5], target = 11
输出: [[5,6]]
示例 2:

输入: nums = [5,6,5,6], target = 11
输出: [[5,6],[5,6]]
提示：

nums.length <= 100000

//思路①、使用 map 存储对应元素出现次数，一次遍历，边存储边遍历，同时避免 num == diff 的问题。
class Solution {
    public List<List<Integer>> pairSums(int[] nums, int target) {
        //存储某个数出现的次数
        Map<Integer, Integer> map = new HashMap<>();
		
        List<List<Integer>> res = new ArrayList<>();

        for(int num : nums){
            int diff = target - num;
            int time = map.getOrDefault(diff, 0);
            if(time != 0){
                res.add(new ArrayList<>(Arrays.asList(num, diff)));
                map.put(diff, time - 1);
            }else{
                map.put(num, map.getOrDefault(num, 0) + 1);
            }
        }

        return res;
    }
}

//思路②、双指针
class Solution {
    public List<List<Integer>> pairSums(int[] nums, int target) {
        /*
        5 5 6 6
        双指针法：
        计算出 sum = nums[left] + nums[right] 的值，如果刚好等于 target ,那么直接添加
        因为这两个数都用过了，并且每个数只能用一次，因此直接 left++ 和 right--

        如果 sum > target，那么 right 指针左移，即 right--,
        比如 [1 2 3 4 5 6] target = 4
             ↑          ↑
        sum > target，因为 left 是可用的最小值了，因此如果 sum 过大，那么 right 一定不存在匹配的数
        */
        List<List<Integer>> res = new ArrayList<>();
        Arrays.sort(nums);
        int left = 0;
        int right = nums.length - 1;
        while(left < right){
            int sum = nums[left] + nums[right];
            if(sum == target){
                res.add(Arrays.asList(nums[left], nums[right]));
                left++;
                right--;
            }else if(sum < target){
                left++;
            }else{
                right--;
            }
        }
        return res;
    }
}