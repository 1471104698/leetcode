师父给徒弟一排固定好的竹签，竹签是下端对齐的，让徒弟帮忙上色，每根竹签的宽度为1
徒弟手上有一把宽度为1的刷子，每次可以选择横向或纵向刷，求最少刷几次可以把所有竹签上色。
竹签的数量范围为[1,1e5]，长度范围[1,1e9]
例如：
输入：arr = [2,2,1,2,2]
输出：3
解释：横向刷最下面一排，然后分别横向刷[0,1]和[3,4]

class Solution{
	private int te(int[] arr){
        /*
            尽可能的横着刷
            找到最短的木板 min， 刷 min 次，然后分成 m 个区间进行递归
            比如 arr = {2,2,1,2,2}，最短的木板为 1，那么刷 1 次，变成 {1,1,0,1,1}，分成 {1,1} 和 {1,1} 两个区间，进行递归
			
			当 arr = {2,2,1,4,5}，如果横刷的话，结果为 6，如果直接竖刷，那么结果为 5
			因此，我们从横刷和竖刷的结果取最小值
         */
        return Math.min(arr.length, count(arr, 0, arr.length - 1));
    }
    private int count(int[] arr, int left, int right){
        if(left > right){
            return 0;
        }
        if(left == right){
            return 1;
        }
        int res = findMin(arr, left, right);

        for(int i = left; i <= right; i++){
            arr[i] -= res;
        }
        //找到为 0 的位置，进行递归刷
        int l = left;
        for(int i = left; i <= right; i++){
            if(arr[i] == 0){
                res += count(arr, l, i - 1);
                l = i + 1;
            }
        }
        return res + count(arr, l, right);
    }

    private int findMin(int[] arr, int left, int right){
        int min = arr[left];
        for(int i = left + 1; i <= right; i++){
            min = Math.min(min, arr[i]);
        }
        return min;
    }
}