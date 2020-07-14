给你一个整数数组 arr 。请你将数组中的元素按照其二进制表示中数字 1 的数目升序排序。

如果存在多个数字二进制中 1 的数目相同，则必须将它们按照数值大小升序排列。

请你返回排序后的数组。

 

示例 1：

输入：arr = [0,1,2,3,4,5,6,7,8]
输出：[0,1,2,4,8,3,5,6,7]
解释：[0] 是唯一一个有 0 个 1 的数。
[1,2,4,8] 都有 1 个 1 。
[3,5,6] 有 2 个 1 。
[7] 有 3 个 1 。
按照 1 的个数排序得到的结果数组为 [0,1,2,4,8,3,5,6,7]

/*

思路①、二维数组排序
*/
class Solution {
    public int[] sortByBits(int[] arr) {
        /*
        
        二维数组：汉明质量 + 数值
        */
        int len = arr.length;
        int[][] temp = new int[len][2];

        for(int i = 0; i < len; i++){
            temp[i][0] = helper(arr[i]);
            temp[i][1] = arr[i];
        }

        Arrays.sort(temp, (a, b) -> a[0] == b[0] ? a[1] - b[1] : a[0] - b[0]);
        
        for(int i = 0; i < len; i++){
            arr[i] = temp[i][1];
        }
        return arr;
    }
    //求汉明质量
    private int helper(int num){
        int c = 0;
        while(num != 0){
            //消掉最后一个 1
            num &= num - 1;
            c++;
        }
        return c;
    }
}

/*
思路②、直接对数组排序，自定义排序规则，获取汉明质量进行比较

注意事项：Arrays.sort() 排序不能对 int[] ，因此我们只能转换为 Integer[] 数组

方法缺点：比较过程中，一个值可能需要多次计算 汉明质量导致重复计算
*/
class Solution {
    public int[] sortByBits(int[] arr) {
        int len = arr.length;

        Integer[] temp = new Integer[len];
        for(int i = 0; i < len; i++){
            temp[i] = arr[i];
        }
        Arrays.sort(temp, (a, b) -> {
            int abit = helper(a);
            int bbit = helper(b);
            return abit == bbit ? a - b : abit - bbit;
        });
        
        for(int i = 0; i < len; i++){
            arr[i] = temp[i];
        }
        return arr;
    }
    //求汉明质量
    private int helper(int num){
        int c = 0;
        while(num != 0){
            //消掉最后一个 1
            num &= num - 1;
            c++;
        }
        return c;
    }
}