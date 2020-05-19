房间中有 n 枚灯泡，编号从 1 到 n，自左向右排成一排。最初，所有的灯都是关着的。

在 k  时刻（ k 的取值范围是 0 到 n - 1），我们打开 light[k] 这个灯。

灯的颜色要想 变成蓝色 就必须同时满足下面两个条件：

灯处于打开状态。
排在它之前（左侧）的所有灯也都处于打开状态。
请返回能够让 所有开着的 灯都 变成蓝色 的时刻 数目 。

输入：light = [2,1,3,5,4]
输出：3
解释：所有开着的灯都变蓝的时刻分别是 1，2 和 4 。

class Solution {
    public int numTimesAllBlue(int[] light) {
        /*
        全部开着的灯为蓝色需要满足以下要求： 亮着的最远的灯刚好等于亮灯的个数
        */
        //最远的灯的位置
        int maxVal = -1;
        //灯亮个数
        int count = 0;
        //变蓝时刻个数
        int res = 0;
        for(int val : light){
            count++;
            maxVal = Math.max(maxVal, val);
            if(maxVal == count){
                res++;
            }
        }
        return res;
    }
}