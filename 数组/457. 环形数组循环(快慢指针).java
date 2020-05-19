给定一个含有正整数和负整数的环形数组 nums。 如果某个索引中的数 k 为正数，则向前移动 k 个索引。相反，如果是负数 (-k)，
则向后移动 k 个索引。因为数组是环形的，所以可以假设最后一个元素的下一个元素是第一个元素，而第一个元素的前一个元素是最后一个元素。

确定 nums 中是否存在循环（或周期）。循环必须在相同的索引处开始和结束并且循环长度 > 1。
此外，一个循环中的所有运动都必须沿着同一方向进行。换句话说，一个循环中不能同时包括向前的运动和向后的运动。
 

示例 1：

输入：[2,-1,1,2,2]
输出：true
解释：存在循环，按索引 0 -> 2 -> 3 -> 0 。循环长度为 3 。
示例 2：

输入：[-1,2]
输出：false
解释：按索引 1 -> 1 -> 1 ... 的运动无法构成循环，因为循环的长度为 1 。根据定义，循环的长度必须大于 1 。
示例 3:

输入：[-2,1,-1,-2,-2]
输出：false
解释：按索引 1 -> 2 -> 1 -> ... 的运动无法构成循环，因为按索引 1 -> 2 的运动是向前的运动，而按索引 2 -> 1 的运动是向后的运动。一个循环中的所有运动都必须沿着同一方向进行。


class Solution {
    public boolean circularArrayLoop(int[] nums) {
        /*
        2,-1,1,2,2
        0  1 2 3 4

        判断环就使用快慢指针：
        从 i 位置开始走，slow 走 一步， fast 走两步，如果成环，那么最后必定存在 fasy == slow ，因为一直在数组上走，也就那几个值

        1、如果不成环，那么跟 链表走到 null 不一样的是，遇到反向运动
        2、成环，但是循环长度为 1，即 slow 和 fast 一直指向当前位置
        3、nums[i] 为 0
        这 3 条作为终止条件
        */
        int len = nums.length;
        if(len < 2){
            return false;
        }
        for(int i = 0; i < len; i++){
            if(nums[i] == 0){
                continue;
            }
            int preS = -1;
            int preF = -1;
            int slow = i;
            int fast = i;
            //判断是正向还是反向
            boolean flag = nums[i] > 0;
            while(true){
                preS = slow;
                slow = nums[slow] + slow;
                slow = getIndex(len ,slow);
                if(preS == slow || nums[preS] * nums[slow] < 0 || nums[slow] == 0){
                    break;
                }

                preF = fast;
                fast = nums[fast] + fast;
                fast = getIndex(len, fast);
                if(preF == fast || nums[preF] * nums[fast] < 0 || nums[fast] == 0){
                    break;
                }

                preF = fast;
                fast = nums[fast] + fast;
                fast = getIndex(len, fast);
                if(preF == fast || nums[preF] * nums[fast] < 0 || nums[fast] == 0){
                    break;
                }

                if(slow == fast){
                    return true;
                }
            }
        }
        return false;
    }
	/*
		如果 idx = -1，表示我们需要跳到数组尾，那么就是 idx = len - 1，即 idx = -1 + len
		那么负数我们可以通过 + len 转换对应的位置
		
		对于正数，我们可以直接 % len 跳到对应的位置
		
		综上，直接 (idx + len) % len 即可
		
		但这里为什么是 + 100 * len ？ 因为负数可能很少，比如 len = 5 ，而 idx = -100，那么 + len 后显然还是负数，因此我们需要加上 len * 100
	*/
    private int getIndex(int len, int idx){
		//return (idx % len + len) % len;  负数取同余
        return (idx + 1000 * len) % len;
    }
}
