你将获得 K 个鸡蛋，并可以使用一栋从 1 到 N  共有 N 层楼的建筑。

每个蛋的功能都是一样的，如果一个蛋碎了，你就不能再把它掉下去。

你知道存在楼层 F ，满足 0 <= F <= N 任何从高于 F 的楼层落下的鸡蛋都会碎，从 F 楼层或比它低的楼层落下的鸡蛋都不会破。

每次移动，你可以取一个鸡蛋（如果你有完整的鸡蛋）并把它从任一楼层 X 扔下（满足 1 <= X <= N）。

你的目标是确切地知道 F 的值是多少。

无论 F 的初始值如何，你确定 F 的值的最小移动次数是多少？

 

示例 1：
输入：K = 1, N = 2
输出：2
解释：
鸡蛋从 1 楼掉落。如果它碎了，我们肯定知道 F = 0 。
否则，鸡蛋从 2 楼掉落。如果它碎了，我们肯定知道 F = 1 。
如果它没碎，那么我们肯定知道 F = 2 。
因此，在最坏的情况下我们需要移动 2 次以确定 F 是多少。

示例 2：
输入：K = 2, N = 6
输出：3

//思路①、递归
class Solution {
    Integer[][] memo = null;
    public int superEggDrop(int K, int N) {
        if(memo == null){
            memo = new Integer[K + 1][N + 1];
        }
        if(memo[K][N] != null){
            return memo[K][N];
        }
        /*
        我们需要考虑的是最坏的情况

        只有一个鸡蛋，那么每层楼都需要试一遍，扔的次数为 N
        只有一层楼，那么无论有多少个鸡蛋，都只需要扔一次
		*/
        //先递归
        if(K == 1){
            return N;
        }
        if(N == 1){
            return 1;
        }
        int count = N;
        //我们遍历每个楼层，从某个楼层往下扔，求得当前扔的使用的最少的扔鸡蛋次数，每次有不同的结果：鸡蛋碎与不碎
        for(int i = 1; i <= N; i++){
            /*
            Math.min(count,xx) 求的是从不同楼层扔鸡蛋的最优最少的次数
            Math.max(superEggDrop(K - 1, i), superEggDrop(K, N - i)) 求的是鸡蛋 碎 与 不碎 的最坏的扔鸡蛋次数

            鸡蛋碎了，还剩下 K - 1 个蛋，并且一定不在 [1, i] 楼，因此我们递归 [i + 1, N] 楼，即剩下 N - i 楼（因为 i 楼已经确定了，没必要再去扔）
            鸡蛋没碎，还剩下 K 个蛋，并且一定不在 [i + 1, N] 楼，因此我们递归 [1, i - 1] 楼，即 剩下 i - 1 楼（因为 i 楼已经确定了，没必要再去扔）
            */
            count = Math.min(count, Math.max(superEggDrop(K - 1, i - 1), superEggDrop(K, N - i)) + 1);
        }
        memo[K][N] = count;
        return count;
    }
}

//思路②、动规
class Solution {
    public int superEggDrop(int K, int F) {
        /*
        使用 dp 
        我们需要考虑的是最坏的情况

        只有一个鸡蛋，那么每层楼都需要试一遍，扔的次数为 N
        只有一层楼，那么无论有多少个鸡蛋，都只需要扔一次
        K       1 2 3 4 5 6 7
        F   1   1 1 1 1 1 1 1
            2   2 2 2 2 2 2 2 
            3   3 
            4   4   
            5   5
            6   6
            7   7
        */
        if(K == 1){
            return F;
        }
        if(F == 1){
            return 1;
        }
        int[][] dp = new int[K + 1][F + 1];

        for(int i = 0; i <= K; i++){
            Arrays.fill(dp[i], Integer.MAX_VALUE);
        }        
        //初始化
        //鸡蛋只有 1 个的情况
        for(int i = 1; i <= F; i++){
            dp[1][i] = i;
        }
        //楼层只有 1 层 或 0 层的情况
        for(int i = 1; i <= K; i++){
            dp[i][1] = 1;
            dp[i][0] = 0;
        }

        //从两个鸡蛋遍历起，对应从每层楼扔的情况
        for(int i = 2; i <= K; i++){
            //共有 j 层楼
            for(int j = 2; j <= F; j++){
                //从 j 层楼的任意一层扔，判断情况
                for(int k = 1; k <= j; k++){
                    //每次都有碎与不碎的情况
                    dp[i][j] = Math.min(dp[i][j], Math.max(dp[i - 1][k - 1], dp[i][j - k]) + 1);
                }
            }
        }
        return dp[K][F];
    }
}

//思路③、递归 + 二分
class Solution {
    Integer[][] memo;
    public int superEggDrop(int K, int N) {
        /*
            回溯 二分

            在楼层 i 扔鸡蛋，有碎和不碎的情况
            如果碎了，鸡蛋数 - 1, F 在 [0, i - 1]，继续往下 dfs
            如果没碎，鸡蛋数不变，F 在 [i, N]，继续往上 dfs，因为我们已经知道 i 楼的情况了，因此我们只要遍历 [i + 1, N] 的情况就可以确定 i 是不是 F，因此不用再遍历 i

            两种情况我们取最坏的情况，然后再在当前拥有 K 个鸡蛋，N 个楼层进行所有的楼层的尝试中选取最好的结果
            dp[k][i] = min(dp[k][i], max(dp[k - 1][j - 1], dp[k][N - i]) + 1);

            我们可以进行二分，递归剪枝
        */
        if(memo == null){
            memo = new Integer[K + 1][N + 1];
        }
        if(memo[K][N] != null){
            return memo[K][N];
        }
        if(K == 1){
            return N;
        }
        if(N == 1){
            return 1;
        }
        if(N == 0){
            return 0;
        }

        int res = Integer.MAX_VALUE;
        int left = 1;
        int right = N;

        while(left <= right){
            int mid = (left + right) >>> 1;
            //蛋碎
            int broken = superEggDrop(K - 1, mid - 1);
            //蛋没碎，因为 left == right 还继续递归，因此 mid = left = right，那么 N - mid 可能就是为 0，需要在上面判断 N == 0 的情况
            int non_broken = superEggDrop(K, N - mid);

            res = Math.min(res, Math.max(broken, non_broken) + 1);

            //因为楼层增加，必定会造成扔蛋次数的增多，蛋碎的扔蛋次数 大于 蛋没碎 的扔蛋次数，表示所选楼层太高，需要降低
            if(broken > non_broken){
                right = mid - 1;
            }else{
                left = mid + 1;
            }
        }
        memo[K][N] = res;
        return res;
    }
}