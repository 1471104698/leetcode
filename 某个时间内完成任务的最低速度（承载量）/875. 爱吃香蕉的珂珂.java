珂珂喜欢吃香蕉。这里有 N 堆香蕉，第 i 堆中有 piles[i] 根香蕉。警卫已经离开了，将在 H 小时后回来。

珂珂可以决定她吃香蕉的速度 K （单位：根/小时）。每个小时，她将会选择一堆香蕉，从中吃掉 K 根。如果这堆香蕉少于 K 根，她将吃掉这堆的所有香蕉，然后这一小时内不会再吃更多的香蕉。  

珂珂喜欢慢慢吃，但仍然想在警卫回来前吃掉所有的香蕉。

返回她可以在 H 小时内吃掉所有香蕉的最小速度 K（K 为整数）。

 

示例 1：

输入: piles = [3,6,7,11], H = 8
输出: 4
示例 2：

输入: piles = [30,11,23,4,20], H = 5
输出: 30
示例 3：

输入: piles = [30,11,23,4,20], H = 6
输出: 23
 

提示：

1 <= piles.length <= 10^4
piles.length <= H <= 10^9
1 <= piles[i] <= 10^9

class Solution {
    public int minEatingSpeed(int[] piles, int H) {
        /*
        同 1011. 在 D 天内送达包裹的能力

        注意：因为固定只能动某一堆的香蕉，因此 最大右边界为 香蕉堆的最大值
        */
        int maxVal = 0;
        for(int val : piles){
            maxVal = Math.max(maxVal, val);
        }
        int left = 1;
        int right = maxVal;
        //二分查找
        while(left < right){
            int mid = (left + right) >>> 1;
            if(isOk(piles, mid, H)){
                right = mid;
            }else{
                left = mid + 1;
            }
        }
        return left;
    }
    private boolean isOk(int[] piles, int K, int H){
        for(int val : piles){
            /*
            如果 val <= K 表示香蕉数不超过 K，能够一小时吃完
            否则需要分多个小时吃完，最少需要 val / K 个小时，比如 val = 7 , K = 3 ，那么最少需要 7 / 3 = 2 个小时，
            但因为还剩下一个香蕉，即 val % K != 0，因此还需要多一个小时
            */
            int time = val <= K ? 1 : (val / K + (val % K == 0 ? 0 : 1));
            H -= time;
        }
        //H == 0 表示刚好时间够用
        return H >= 0;
    }
}