几张卡牌 排成一行，每张卡牌都有一个对应的点数。点数由整数数组 cardPoints 给出。

每次行动，你可以从行的开头或者末尾拿一张卡牌，最终你必须正好拿 k 张卡牌。

你的点数就是你拿到手中的所有卡牌的点数之和。

给你一个整数数组 cardPoints 和整数 k，请你返回可以获得的最大点数。

 

示例 1：

输入：cardPoints = [1,2,3,4,5,6,1], k = 3
输出：12
解释：第一次行动，不管拿哪张牌，你的点数总是 1 。但是，先拿最右边的卡牌将会最大化你的可获得点数。最优策略是拿右边的三张牌，最终点数为 1 + 6 + 5 = 12 。
示例 2：

输入：cardPoints = [2,2,2], k = 2
输出：4
解释：无论你拿起哪两张卡牌，可获得的点数总是 4 。
示例 3：

输入：cardPoints = [9,7,7,9,7,7,9], k = 7
输出：55
解释：你必须拿起所有卡牌，可以获得的点数为所有卡牌的点数之和

class Solution {
    public int maxScore(int[] cardPoints, int k) {
        /*
            滑动窗口
            先求出数组的总和 sum
            然后删除 n - k 长度的连续子数组

            cardPoints = [1,2,3,4,5,6,1], k = 3
            数组长度 len = 7，那么我们需要删除 7 - 3 = 4 长度的子数组

            我们可以选择删除 [1,2,3,4] 或 [2,3,4,5] 或 [3,4,5,6] 或 [4,5,6,1]
            我们只有判断删除哪个子数组后能得到最大的点数即可
        */
        int len = cardPoints.length;

        int sum = 0;
        for(int num : cardPoints){
            sum += num;
        }

        //需要删除的子数组长度
        int deleteLen = len - k;

        //最大点数
        int maxScore = 0;

        //滑动窗口区间
        int left = 0;
        int right = 0;

        //滑动窗口元素总和
        int tempSum = 0;

        while(right < len){
            tempSum += cardPoints[right];
            right++;

            if(right - left > deleteLen){
                tempSum -= cardPoints[left++];
            }
            if(right - left == deleteLen){
                maxScore = Math.max(maxScore, sum - tempSum);
            }
        }
        return maxScore;
    }
}