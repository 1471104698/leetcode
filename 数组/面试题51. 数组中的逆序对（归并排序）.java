
在数组中的两个数字，如果前面一个数字大于后面的数字，则这两个数字组成一个逆序对。输入一个数组，求出这个数组中的逆序对的总数。

 

示例 1:

输入: [7,5,6,4]
输出: 5

class Solution {
    public int reversePairs(int[] nums) {
        return mergeSort(nums);
    }
    private int mergeSort(int[] arr){
        return mergeSort(arr, 0, arr.length - 1);
    }

    //有点像快排，分割为只有一个元素时停止分割，然后进行排序
    private int mergeSort(int[] arr, int l, int r) {
        int count = 0;
        if (l < r) {
            int mid = (l + r) >>> 1;
            count += mergeSort(arr, l, mid);
            count += mergeSort(arr, mid + 1, r);
            if(arr[mid] > arr[mid + 1]){
            	count += merge(arr, l, mid, r);
			}
        }
        return count;
    }

    /*
    分割成两个数组进行合并，跟合并两个有序链表一个样
    m 是属于左边的
     */
    private int merge(int[] arr, int l, int m, int r) {
        int len1 = m - l + 1;
        int len2 = r - m;

        int[] left = new int[len1];
        int[] right = new int[len2];

        System.arraycopy(arr, l, left, 0, len1);
        System.arraycopy(arr, m + 1, right, 0, len2);

        int count = 0;
        /*
        合并两个有序数组
        因为两个数组都是有序的，因此我们从后往前合并，将大的数值先进行填充
        如果 left 先填充完，那么我们将 right 剩余的元素填充到 merge 中
        如果 right 先填充完，那么就无需再管 left 剩下元素，因为 left 截取的是 arr 的前半段元素，
            因此 left 剩下的元素是 arr 前半段的元素，因此是否填充都一样
        */
        int idx1 = len1 - 1;
        int idx2 = len2 - 1;
        while(idx1 >= 0 && idx2 >= 0){
            // arr[r--] = left[idx1] < right[idx2] ? right[idx2--] : left[idx1--];
            if(left[idx1] > right[idx2]){
                count += idx2 + 1;
                arr[r--] = left[idx1--];
            }else{
                arr[r--] = right[idx2--];
            }
        }
        System.arraycopy(right, 0, arr, l, idx2 + 1);
        return count;
    }
}