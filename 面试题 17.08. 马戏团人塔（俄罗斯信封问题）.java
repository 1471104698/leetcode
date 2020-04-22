有个马戏团正在设计叠罗汉的表演节目，一个人要站在另一人的肩膀上。出于实际和美观的考虑，在上面的人要比下面的人矮一点且轻一点。已知马戏团每个人的身高和体重，请编写代码计算叠罗汉最多能叠几个人。

示例：

输入：height = [65,70,56,75,60,68] weight = [100,150,90,190,95,110]
输出：6
解释：从上往下数，叠罗汉最多能叠 6 层：(56,90), (60,95), (65,100), (68,110), (70,150), (75,190)
提示：

height.length == weight.length <= 10000


class Solution {
    public int bestSeqAtIndex(int[] height, int[] weight) {
        /*
        一看就跟俄罗斯信封一样的
        按身高升序，再按体重降序，然后根据体重求最长上升子序列

        为什么需要按体重降序？
        如果升序，那么就是这样的，[1,2] [2,3],[2,5],[4,4],[4,6]，求得的最长上升子序列为：[1,2] [2,3],[2,5],[4,4],[4,6]，升高一样的会重复取值
        如果降序，那么就是这样的，[1,2] [2,5],[2,3],[4,6],[4,4]，求得的最长上升子序列为：[1,2] [2,5] [4,6]，每次取的是某个升高的最大值
            因为我们按体重降序，因此同身高的排在前面的一定是体重最重的，那么我们选取了最重的，后面比较轻的必定没有机会被选取，那么就避免了同身高的重复选取

        */
        
        //因为身高和身高分成了两个数组，这样不方便排序，因为身高体重对应的索引会打乱，因此我们合并为一个二维数组
        int len = height.length;
		//这里需要注意的是，第二维只需要 2 个长度，而不是 len 长度（之前设为了 [len][len]，导致了内存溢出）
        int[][] mat = new int[len][2];
        for(int i = 0; i < len; i++){
            mat[i] = new int[]{height[i], weight[i]};
        }
        Arrays.sort(mat, (a, b) -> a[0] == b[0] ? b[1] - a[1] : a[0] - b[0]);

        return LIT(mat);
    }
    private int LIT(int[][] nums){
        int len = nums.length;
        int[] top = new int[len];

        int piles = 0;

        for(int[] poke : nums){
            int left = 0;
            int right = piles;
            while(left < right){
                int mid = (left + right) >>> 1;
                if(top[mid] < poke[1]){
                    left = mid + 1;
                }else{
                    right = mid;
                }
            }
            if(left == piles){
                piles++;
            }

            top[left] = poke[1];
        }
        return piles; 
    }
}