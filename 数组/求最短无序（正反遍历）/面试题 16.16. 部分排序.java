给定一个整数数组，编写一个函数，找出索引m和n，只要将索引区间[m,n]的元素排好序，整个数组就是有序的。注意：n-m尽量最小，也就是说，找出符合条件的最短序列。函数返回值为[m,n]，若不存在这样的m和n（例如整个数组是有序的），请返回[-1,-1]。

示例：

输入： [1,2,4,7,10,11,7,12,6,7,16,18,19]
输出： [3,9]
提示：

0 <= len(array) <= 1000000

class Solution {
    public int[] subSort(int[] array) {
        /*
        跟 581. 最短无序连续子数组 一样，求的是最短无序的子数组
        只需要正反两次遍历
        */
        int len = array.length;
        if(len < 3){
            return new int[]{-1, -1};
        }
        int high = -1;
        int low = -1;

        int min = Integer.MAX_VALUE;
        int max = Integer.MIN_VALUE;

        for(int i = 0; i < len; i++){
            //正序，当前值比前面的最大值还小，那么该位置需要进行调整
            if(array[i] < max){
                high = i;
            }
            if(array[len - i - 1] > min){
                low = len - i - 1;
            }
            max = Math.max(array[i], max); 
            min = Math.min(array[len - i - 1], min);
            
        }
        return new int[]{low, high};
    }
}