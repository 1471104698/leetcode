给定两个整数数组，请交换一对数值（每个数组中取一个数值），使得两个数组所有元素的和相等。

返回一个数组，第一个元素是第一个数组中要交换的元素，第二个元素是第二个数组中要交换的元素。若有多个答案，返回任意一个均可。若无满足条件的数值，返回空数组。

示例:

输入: array1 = [4, 1, 2, 1, 1, 2], array2 = [3, 6, 3, 3]
输出: [1, 3]
示例:

输入: array1 = [1, 2, 3], array2 = [4, 5, 6]
输出: []

class Solution {
    public int[] findSwapValues(int[] A, int[] B) {
        /*
        A = [4, 1, 2, 1, 1, 2]     11
        B = [3, 6, 3, 3]          15

        数组 A 交换 a ，数组 B 交换 b
        那么最终必须 sumA - a + b = sumB - b + a
        a - b = (sumA - sumB) / 2
        */
        int sumA = 0;
        int sumB = 0;
        //存储 数组 B 的元素
        Set<Integer> set = new HashSet<>();

        for(int num : A){
            sumA += num;
        }
        for(int num : B){
            sumB += num;
            set.add(num);
        }
        //两数组总和不是偶数，那么必不能相等
        if((sumA + sumB) % 2 != 0){
            return new int[0];
        }
        //从数组 A 和 数组 B 中寻找 a,b 两个元素满足 a - b = target
        int target = (sumA - sumB) / 2;

        for(int a : A){
            int b = a - target;
            if(set.contains(b)){
                return new int[]{a, b};
            }
        }
        return new int[0];
    }
}