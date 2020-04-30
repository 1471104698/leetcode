传送带上的包裹必须在 D 天内从一个港口运送到另一个港口。

传送带上的第 i 个包裹的重量为 weights[i]。每一天，我们都会按给出重量的顺序往传送带上装载包裹。我们装载的重量不会超过船的最大运载重量。

返回能在 D 天内将传送带上的所有包裹送达的船的最低运载能力。

 

示例 1：

输入：weights = [1,2,3,4,5,6,7,8,9,10], D = 5
输出：15
解释：
船舶最低载重 15 就能够在 5 天内送达所有包裹，如下所示：
第 1 天：1, 2, 3, 4, 5
第 2 天：6, 7
第 3 天：8
第 4 天：9
第 5 天：10

请注意，货物必须按照给定的顺序装运，因此使用载重能力为 14 的船舶并将包装分成 (2, 3, 4, 5), (1, 6, 7), (8), (9), (10) 是不允许的。 
示例 2：

输入：weights = [3,2,2,4,1,4], D = 3
输出：6
解释：
船舶最低载重 6 就能够在 3 天内送达所有包裹，如下所示：
第 1 天：3, 2
第 2 天：2, 4
第 3 天：1, 4

class Solution {
    public int shipWithinDays(int[] weights, int D) {
        /*
            要在 D 天内运完所有包裹，那么每天至少的承载量为 sum / D
            但是，因为一次至少运 1 个包裹，而这个包裹的重量可大可小，那么可能 weights[i] > sum / D
            假设包裹最大的重量为 maxWeight
            因此，最低承载量应该为 capacity = max(sum / D, maxWeight);

            最直接的方法，就是直接从 capacity 开始，每次 capacity++ 进行尝试，但这样效率很低
            因此我们可以使用二分查找， left = capacity， right = sum，即最低承载量为 capacity,最高的承载量为 包裹总量
            我们判断承载量 mid 是否能在 D 天内装完（无需刚好 D 天，只需要 [1, D] 即可），如果不能，表示承载量太小，则 left = mid + 1，否则 right = mid
            因为必定存在一个答案，因此当退出循环， left = right 时，就是答案
        */
        int len = weights.length;
        int sum = 0;
        int maxWeight = 0;
        for(int num : weights){
            sum += num;
            maxWeight = Math.max(maxWeight, num);
        }
        //最低承载量
        int left = Math.max(sum / D, maxWeight);
        //最高承载量
        int right = sum;
        while(left < right){
            int mid = (left + right) >>> 1;
            //容量为 mid 是否可行，如果不可行，表示 mid 过小
            if(isOk(weights, mid, D)){
                right = mid;
            }else{
                left = mid + 1;
            }
        }
        return left;
    }
    //判断以 capa 作为容量是否能够在 D 天内装完
    private boolean isOk(int[] weights, int capa, int D){
        int temp = 0;
        for(int val : weights){
            if(temp + val > capa){
                temp = 0;
                D--;
            }
            temp += val;
        }
        return D > 0;
    }
}