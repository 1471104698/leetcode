在歌曲列表中，第 i 首歌曲的持续时间为 time[i] 秒。

返回其总持续时间（以秒为单位）可被 60 整除的歌曲对的数量。形式上，我们希望索引的数字 i 和 j 满足  i < j 且有 (time[i] + time[j]) % 60 == 0。

 

示例 1：

输入：[30,20,150,100,40]
输出：3
解释：这三对的总持续时间可被 60 整数：
(time[0] = 30, time[2] = 150): 总持续时间 180
(time[1] = 20, time[3] = 100): 总持续时间 120
(time[1] = 20, time[4] = 40): 总持续时间 60
示例 2：

输入：[60,60,60]
输出：3
解释：所有三对的总持续时间都是 120，可以被 60 整数。
 

提示：

1 <= time.length <= 60000
1 <= time[i] <= 500

class Solution {
    public int numPairsDivisibleBy60(int[] time) {
        /*
        (a + b) % 60 = 0
        => a % 60 + b % 60 = 60
        比如 20 和 100， (a + b) % 60 = 120 % 60 = 0
        可转换成 20 % 60 + 100 % 60 = 20 + 40 = 60

        使用一个数组 记录 % 60 各个结果的个数
        */
        int count = 0;
        int[] temp =  new int[60];
        for(int t : time){
            int mod = t % 60;
            // % 60 是因为可能 t = 60, mod = 60 % 60 = 0,然后 (60 - mod) = 60，因此需要 % 60，直接存放到 0 号 位置
            count += temp[(60 - mod) % 60];
            temp[mod]++;
        }
        return count;
    }
}