
这里有 n 个航班，它们分别从 1 到 n 进行编号。

我们这儿有一份航班预订表，表中第 i 条预订记录 bookings[i] = [i, j, k] 意味着我们在从 i 到 j 的每个航班上预订了 k 个座位。

请你返回一个长度为 n 的数组 answer，按航班编号顺序返回每个航班上预订的座位数。

 

示例：

输入：bookings = [[1,2,10],[2,3,20],[2,5,25]], n = 5
输出：[10,55,45,25,25]
 

提示：

1 <= bookings.length <= 20000
1 <= bookings[i][0] <= bookings[i][1] <= n <= 20000
1 <= bookings[i][2] <= 10000

class Solution {
    public int[] corpFlightBookings(int[][] bookings, int n) {
        /*
        我们统计 初始站 和 终点站 上车 和 下车 的人数
        只要将某一站下车人数算好了，那么前一站的人数对于当前栈都是上车人数

        比如 [1,2,10],[2,3,20]
        第一站上车人数是 10，经过第 2 站，在第 3 站下车
        第二站上车人数是 20，经过第 3 站，在第 4 站下车
        那么我们可以将某一站的下车人数提前算好，即将 第 3 站下车人数为 10，那么第三站人数 -10 ，这里第 4 站是越界的，因此不管了
        当前站的人数，就是上一站的总人数减去该站下车的人数即可，因为我们提前将某一沾下车人数算好了，因此我们最后直接加上上一站的人数即可
        */
        int[] counts = new int[n];
        for(int i = 0; i < bookings.length; i++){
            int[] temp = bookings[i];
            counts[temp[0] - 1] += temp[2];
            //因为 temp[1] 表示最终坐到 temp[1] 站，而下车是在 temp[1] + 1 站下车
            if(temp[1] < n){
                counts[temp[1]] -= temp[2];
            }
        }
        for(int i = 1; i < n; i++){
            counts[i] += counts[i - 1];
        }
        return counts;
    }
}