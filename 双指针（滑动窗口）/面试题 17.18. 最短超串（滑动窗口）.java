假设你有两个数组，一个长一个短，短的元素均不相同。找到长数组中包含短数组所有的元素的最短子数组，其出现顺序无关紧要。

返回最短子数组的左端点和右端点，如有多个满足条件的子数组，返回左端点最小的一个。若不存在，返回空数组。

示例 1:

输入:
big = [7,5,9,0,2,1,3,5,7,9,1,1,5,8,8,9,7]
small = [1,5,9]
输出: [7,10]
示例 2:

输入:
big = [1,2,3]
small = [4]
输出: []
提示：

big.length <= 100000
1 <= small.length <= 100000

class Solution {
    public int[] shortestSeq(int[] big, int[] small) {
        /*
        滑动窗口

        1、使用滑动窗口的话，我们需要记录窗口内 small 数组每隔元素出现的次数，但是具体我们并不知道 small 数组的元素范围
            当然我们可以直接遍历一遍 small 数组，获取最大值 maxVal，然后开辟一个 大小为 maxVal 的数组 temp 记录对应元素出现的次数，但可能空间消耗太大
            因此我们使用 map 来记录 small 数组各个元素出现的次数
        2、我们需要知道 当前右边界 big[right] 是否是属于 small 数组内的元素，
            我们总不能每次遍历 big 的一个元素，就去遍历一遍 small 数组判断是否存在于 small 数组中
            因此，我们可以使用一个 set 先存储 small 数组的内容，后续直接 O(1) 查找判断
        3、我们使用 vaild 记录滑动窗口内已经查找到的 small 元素的个数（不重复的），
            当 valid == small.length，表示滑动窗口内全部包含 small 数组的元素，那么我们从左边界 left 开始收缩
            边收缩边记录满足条件的 start 和 minLen

        */

        int len = small.length;
        //记录 small 数组某个索引位置在滑动窗口出现内出现的次数
        Map<Integer, Integer> map = new HashMap<>();
        Set<Integer> set = new HashSet<>();
        for(int num : small){
            set.add(num);
        }

        //滑动窗口内已经找到的 small 数组的元素个数
        int valid = 0;

        //滑动窗口的范围
        int left = 0;
        int right = 0;

        //满足条件的窗口左边界 和 长度
        int start = 0;
        int minLen = Integer.MAX_VALUE;

        while (right < big.length) {
            
            //当前 big[right] 存在于 small 数组中
            if (set.contains(big[right])) {
                int time = map.getOrDefault(big[right], 0);
                //如果滑动窗口内不包含 big[right]，即这是第一次找到 big[right]，那么将 valid ++
                if (time == 0) {
                    valid++;
                }
                //元素出现次数 + 1
                map.put(big[right], time + 1);
            }

            right++;

            //当 valid == len ,即滑动窗口内包含 small 全部元素，开始左边界的收缩
            while (valid == len) {

                if (minLen > right - left) {
                    start = left;
                    minLen = right - left;
                }

                int time = map.getOrDefault(big[left], 0);
                //这里需要判断次数是否为 0，因为不存在于 small 数组中的，我们上面不会添加它的次数，默认为 0
                if (time != 0) {
                    if (time == 1) {
                        valid--;
                    }
                    map.put(big[left], time - 1);
                }
                left++;
            }
        }
        return minLen == Integer.MAX_VALUE ? new int[0] : new int[]{start, start + minLen - 1};
    }
}