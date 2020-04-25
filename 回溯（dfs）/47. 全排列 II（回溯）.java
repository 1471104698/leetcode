给定一个可包含重复数字的序列，返回所有不重复的全排列。

示例:

输入: [1,1,2]
输出:
[
  [1,1,2],
  [1,2,1],
  [2,1,1]
]

class Solution {
    public List<List<Integer>> permuteUnique(int[] nums) {
        Arrays.sort(nums);
        // {1，1，1，2}{1，1，2，1}{1，2，1，1}{2，1，1，1}
        List<List<Integer>> res = new ArrayList<>();
        helper(new ArrayList<>(), nums, 0, new boolean[nums.length], res);
        return res;
    }
    private void helper(List<Integer> list, int[] nums, int index, boolean[] use, List<List<Integer>> res){
        if(index == nums.length){
            res.add(new ArrayList<>(list));
            return;
        }
        for(int i = 0; i < nums.length; i++){
            if(!use[i]){
                //如果
                if(i > 0 && nums[i-1] == nums[i] && !use[i-1]){
                    continue;
                }
                list.add(nums[i]);
                use[i] = true;
                helper(list, nums, index+1, use, res);
                use[i] = false;
                list.remove(index);
            }
        }
    }
}