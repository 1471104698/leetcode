class Solution {
    public int search(int[] arr, int target) {
        return helper(arr, target, 0, arr.length - 1);
    }
    private int helper(int[] arr, int target, int left, int right){
        if(left > right){
            return -1;
        }
        //注意：这里不存在普遍二分查找的 while(left < right)
        int mid = (left + right) >>> 1;
        if(arr[mid] == target){
			//向左递归，查找更小值（这段看情况而定）
            left = helper(arr, target, left, mid - 1);
            if(left != -1 && left < mid){
                return left;
            }
            return mid;
        }else{
            //向左向右递归，得到结果存储到 left 和 right 中
            left = helper(arr, target, left, mid - 1);
            //如果左不为 -1，那么直接返回，不再向右递归
            if(left != -1){
                return left;
            }
            right = helper(arr, target, mid + 1, right);
            return right;
        }
    }
}