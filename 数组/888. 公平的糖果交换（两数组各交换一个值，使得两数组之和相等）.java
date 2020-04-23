爱丽丝和鲍勃有不同大小的糖果棒：A[i] 是爱丽丝拥有的第 i 块糖的大小，B[j] 是鲍勃拥有的第 j 块糖的大小。
因为他们是朋友，所以他们想交换一个糖果棒，这样交换后，他们都有相同的糖果总量。（一个人拥有的糖果总量是他们拥有的糖果棒大小的总和。）
返回一个整数数组 ans，其中 ans[0] 是爱丽丝必须交换的糖果棒的大小，ans[1] 是 Bob 必须交换的糖果棒的大小。
如果有多个答案，你可以返回其中任何一个。保证答案存在。
 

示例 1：

输入：A = [1,1], B = [2,2]
输出：[1,2]
示例 2：

输入：A = [1,2], B = [2,3]
输出：[1,2]

class Solution {
    public int[] fairCandySwap(int[] A, int[] B) {
        /*
        假设 A 的糖果总数为 sumA, B 的糖果总数为 sumB
        要想 A 和 B 糖果总数相同，A 需要交换的糖果数为 a， B 需要交换的糖果数为 b
        那么有以下等式
        sumA - a + b = sumB - b + a
        a - b = (sumA - sumB) / 2
        */

        //存储 b 的元素，方便后续 O(1) 查找
        Set<Integer> set = new HashSet<>();
        
        int sumA = 0;
        int sumB = 0;

        for(int a : A){
            sumA += a;
        }
        for(int b : B){
            sumB += b;
            set.add(b);
        }
        //如果两者总和不能平分，那么答案不存在
        if(((sumA - sumB) & 1) != 0){
            return new int[0];
        }

        //diff = a - b
        int diff = (sumA - sumB) / 2;
        
        for(int a : A){
            int b = a - diff;
            if(set.contains(b)){
                return new int[]{a, b};
            }
        }
        return new int[0];
    }
}