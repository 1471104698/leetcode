
给你一个整数数组 nums ，和一个表示限制的整数 limit，请你返回最长连续子数组的长度，该子数组中的任意两个元素之间的绝对差必须小于或者等于 limit 。

如果不存在满足条件的子数组，则返回 0 。

 

示例 1：

输入：nums = [8,2,4,7], limit = 4
输出：2 
解释：所有子数组如下：
[8] 最大绝对差 |8-8| = 0 <= 4.
[8,2] 最大绝对差 |8-2| = 6 > 4. 
[8,2,4] 最大绝对差 |8-2| = 6 > 4.
[8,2,4,7] 最大绝对差 |8-2| = 6 > 4.
[2] 最大绝对差 |2-2| = 0 <= 4.
[2,4] 最大绝对差 |2-4| = 2 <= 4.
[2,4,7] 最大绝对差 |2-7| = 5 > 4.
[4] 最大绝对差 |4-4| = 0 <= 4.
[4,7] 最大绝对差 |4-7| = 3 <= 4.
[7] 最大绝对差 |7-7| = 0 <= 4. 
因此，满足题意的最长子数组的长度为 2 。
示例 2：

输入：nums = [10,1,2,4,7,2], limit = 5
输出：4 
解释：满足题意的最长子数组是 [2,4,7,2]，其最大绝对差 |2-7| = 5 <= 5 。

class Solution {
    public int longestSubarray(int[] nums, int limit) {
        /*
            维护两个单调双端队列
            一个最大队（队首元素 peek() 为最大值），一个最小队
            目的是为了避免 重新 遍历数组 来获取 某个范围内的 最大值和最小值

            (
                我们之前维护单调值的是使用使用单调栈，那么判断数据就是使用栈顶元素
                这里使用队列，那么判断数据就是使用队尾元素（即最后入队的元素）
                
                比如最大队列，队头为最大值，那么后面加入的值 num 如果比 前面加入的值 还大，那么 最大值 就跟 前面的加入值 无关
                因为前面的值最先入队，也是最先出队的，它无论出不出队，最大值都是 后面入队的这个值 num
                同理，最小队也一样
            )
        */
        Deque<Integer> maxQ = new LinkedList<>();
        Deque<Integer> minQ = new LinkedList<>();

        int mlen = 0;

        int len = nums.length;
        for(int left = 0, right = 0; right < len; ){
            //维护最大队
            while(!maxQ.isEmpty() && nums[maxQ.peekLast()] < nums[right]){
                maxQ.pollLast();
            }
            maxQ.add(right);
            //维护最小队
            while(!minQ.isEmpty() && nums[minQ.peekLast()] > nums[right]){
                minQ.pollLast();
            }
            minQ.add(right);
            /*
                maxQ 的 peek() 是 [left, right] 中的最大值
                minQ 的 peek() 是 [left, right] 中的最小值

                nums = [8,2,4,7], limit = 4
                idx     0 1 2 3
                首先插入 8 和 2
                队列情况如下：
                maxQ = {1, 0}   （peek() == 0）
                minQ = {1}      （peek() == 1）

                滑动窗口区间：[0, 1]

                我们发现最大值和最小值的差 > 4，因此我们需要找到舍弃某些值，重新划定窗口区间
                因为最大值的索引位置为 0，最小值的索引位置为 1，因此我们舍弃掉 0 位置 及前面的值，将 left 指向 0 后面的一个值，即 1
            */
            /*
            这里不会出现队列为空的情况
            因为上面最大队 和 最小队都添加了 right，即滑动窗口右边界的值
            那么表示它们之中必定存在一个相同的索引位置，绝对值差为 0，不会超过 limit，因此不会 poll()
            */
            while(nums[maxQ.peek()] - nums[minQ.peek()] > limit){
                //哪个索引在前面就去除掉哪个，然后将 left 指向该位置的后一个位置，相当于去除掉前面所有值
                if(maxQ.peek() < minQ.peek()){
                    left = maxQ.poll() + 1;
                }else{
                    left = minQ.poll() + 1;
                }
            }
            right++;
            mlen = Math.max(mlen, right - left);
        }
        return mlen;
    }
}