 n 名士兵站成一排。每个士兵都有一个 独一无二 的评分 rating 。

每 3 个士兵可以组成一个作战单位，分组规则如下：

从队伍中选出下标分别为 i、j、k 的 3 名士兵，他们的评分分别为 rating[i]、rating[j]、rating[k]
作战单位需满足： rating[i] < rating[j] < rating[k] 或者 rating[i] > rating[j] > rating[k] ，其中  0 <= i < j < k < n
请你返回按上述条件可以组建的作战单位数量。每个士兵都可以是多个作战单位的一部分。


示例 1：

输入：rating = [2,5,3,4,1]
输出：3
解释：我们可以组建三个作战单位 (2,3,4)、(5,4,1)、(5,3,1) 。
示例 2：

输入：rating = [2,1,3]
输出：0
解释：根据题目条件，我们无法组建作战单位。
示例 3：

输入：rating = [1,2,3,4]
输出：4

//简单点说就是找任意 3 个元素的 升序 和 降序组合个数
class Solution {
    public int numTeams(int[] rating) {
        /*
        找多少个升序组合和多少个降序组合

        up 记录从左到右升序， down 记录从左到右降序
        up 和 down 记录 [0, i - 1] 有多少个 比 i 小的 和 比 i 大的

        规定 j < i，我们固定 i 和 j，然后根据 rating[j] 和 rating[i] 的关系进行处理
        如果 rating[j] > rating[i]，那么就相当于是降序，那么我们找 j 前面有多少个比 j 大的元素，这些元素每一个都能够跟 j 和 i 构成组合，即 c += down[j]
        如果 rating[j] < rating[i]，那么就相当于是升序，那么我们找 j 前面有多少个比 j 小的元素，这些元素每一个都能够跟 j 和 i 构成组合，即 c += up[j]
        如果 rating[j] == rating[i]，直接忽略
        */
        int len = rating.length;
        int[] up = new int[len];
        int[] down = new int[len];

        int c = 0;
        for(int i = 0; i < len; i++){
            for(int j = i - 1; j >= 0; j--){                
                //比它大，找降序
                if(rating[j] > rating[i]){
                    c += down[j];
                    down[i]++;  
                }else if(rating[j] < rating[i]){
                    c += up[j];
                    up[i]++;
                }
            }
        }
        return c;
    }
}