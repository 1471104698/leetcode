师父给徒弟一排固定好的竹签，竹签是下端对齐的，让徒弟帮忙上色，每根竹签的宽度为1
徒弟手上有一把宽度为1的刷子，每次可以选择横向或纵向刷，求最少刷几次可以把所有竹签上色。
竹签的数量范围为[1,1e5]，长度范围[1,1e9]
例如：
输入：arr = [2,2,1,2,2]
输出：3
解释：横向刷最下面一排，然后分别横向刷[0,1]和[3,4]

class Solution{
	private int minBrushTime(int[] arr){
        /*
            尽可能的横着刷
            找到最短的木板 min， 刷 min 次，然后分成 m 个区间进行递归
            比如 arr = {2,2,1,2,2}，最短的木板为 1，那么刷 1 次，变成 {1,1,0,1,1}，分成 {1,1} 和 {1,1} 两个区间，进行递归
			
			注意：
			arr = {2,2,1,4,5}
			一、如果全部进行竖刷：次数为 5
			二、如果每次寻找最小值，然后进行横刷，那么有以下情况：
				2	2	1	4	5
			横刷 1 次
				1	1	0	3	4
			横刷一次	  横刷 3 次
			   (0	0)	0  (0	1)	
						  横刷 1 次
							0	0
			次数为 6
			
			（我们可以看出，在 横刷 1 次后，右边竹子剩下 3 和 4，而这两根如果使用竖刷的话，次数会更少）
			三、递归过程中判断使用横刷还是竖刷，那么有以下情况：
				2	2	1	4	5
			横刷 1 次
				1	1	0	3	4	
			横刷一次	  竖刷 2 次
			   (0	0)	0  (0	0)	
			次数为 4
			
         */
        return Math.min(arr.length, count(arr, 0, arr.length - 1));
    }
	/*
	统计刷竹子的次数
	*/
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
        return Math.min(right - left + 1, res + count(arr, l, right));
    }

    /*
	寻找最小的竹子长度
	*/
    private int findMin(int[] arr, int left, int right){
        int min = arr[left];
        for(int i = left + 1; i <= right; i++){
            min = Math.min(min, arr[i]);
        }
        return min;
    }
}