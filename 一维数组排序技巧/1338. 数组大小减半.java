给你一个整数数组 arr。你可以从中选出一个整数集合，并删除这些整数在数组中的每次出现。

返回 至少 能删除数组中的一半整数的整数集合的最小大小。

 

示例 1：

输入：arr = [3,3,3,3,5,5,5,2,2,7]
输出：2
解释：选择 {3,7} 使得结果数组为 [5,5,5,2,2]、长度为 5（原数组长度的一半）。
大小为 2 的可行集合有 {3,5},{3,2},{5,2}。
选择 {2,7} 是不可行的，它的结果数组为 [3,3,3,3,5,5,5]，新数组长度大于原数组的二分之一。
示例 2：

输入：arr = [7,7,7,7,7,7]
输出：1
解释：我们只能选择集合 {7}，结果数组为空。
示例 3：

输入：arr = [1,9]
输出：1
示例 4：

输入：arr = [1000,1000,3,7]
输出：1

提示：

1 <= arr.length <= 10^5
arr.length 为偶数
1 <= arr[i] <= 10^5

//思路①、直接 [0, 100000] 全部排序
class Solution {
    public int minSetSize(int[] arr) {
        /*
        选出一个要删除的整数集合，然后删除这些整数集合内的元素，让原本整个数组长度减少一半
        要求这个整数集合最小
        比如 整数集合为 {1, 2} ,那么就是删除原本数组中所有的 1 和 2
        */
        //统计元素出现个数
        Integer[] temp = new Integer[100001];
        Arrays.fill(temp, 0);
        for(int val : arr){
            temp[val]++;
        }
        Arrays.sort(temp, (a, b) -> b - a);
        int len = arr.length;
        int deleteSum = len / 2;

        for(int i = 0; i < len; i++){
            deleteSum -= temp[i];
            if(deleteSum <= 0){
                return i + 1;
            }
        }
        return -1;
    }
}

//思路②、记录最大值 max，排序范围 [0, max]（此处有新的 api）
class Solution {
    public int minSetSize(int[] arr) {
        /*
        选出一个要删除的整数集合，然后删除这些整数集合内的元素，让原本整个数组长度减少一半
        要求这个整数集合最小
        比如 整数集合为 {1, 2} ,那么就是删除原本数组中所有的 1 和 2
        */
        //统计元素出现个数
        int max = 0;
        int[] temp = new int[100001];
        for(int val : arr){
            temp[val]++;
            max = Math.max(max, val);
        }
        //新的 api，对于 int[] 数组排序，可以选择排序范围
        Arrays.sort(temp, 0, max);

        int len = arr.length;
        int deleteSum = len / 2;
        
        //因为最大值只到 max，且我们只排序 [0, max] 部分，因此我们只需要遍历 [0, max] 这个范围，而不需要遍历 [0, 100000]
        for(int i = max; i >= 0; i--){
            deleteSum -= temp[i];
            if(deleteSum <= 0){
                return max - i + 1;
            }
        }
        return -1;
    }
}