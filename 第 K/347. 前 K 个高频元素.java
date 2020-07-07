给定一个非空的整数数组，返回其中出现频率前 k 高的元素。

 

示例 1:

输入: nums = [1,1,1,2,2,3], k = 2
输出: [1,2]
示例 2:

输入: nums = [1], k = 1
输出: [1]
 

提示：

你可以假设给定的 k 总是合理的，且 1 ≤ k ≤ 数组中不相同的元素的个数。
你的算法的时间复杂度必须优于 O(n log n) , n 是数组的大小。
题目数据保证答案唯一，换句话说，数组中前 k 个高频元素的集合是唯一的。
你可以按任意顺序返回答案。

class Solution {
    public int[] topKFrequent(int[] nums, int k) {
        /*
        大顶堆
        使用 map 统计元素出现个数
        然后使用优先队列进行排序
        */
        Map<Integer, Integer> map = new HashMap<>();
        for(int num : nums){
            map.put(num, map.getOrDefault(num, 0) + 1);
        }
																						//降序排序，大顶堆，堆顶元素为最大值
        PriorityQueue<Map.Entry<Integer, Integer>> minHeap = new PriorityQueue<>((a, b) -> b.getValue() - .getValue());
		
        minHeap.addAll(map.entrySet());
        int[] res = new int[k];
        int i = 0;
        while(i < k){
            res[i] = minHeap.poll().getKey();
            i++;
        }
        return res;
    }
}