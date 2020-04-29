（这是一个 交互式问题 ）
给你一个 山脉数组 mountainArr，请你返回能够使得 mountainArr.get(index) 等于 target 最小 的下标 index 值。

如果不存在这样的下标 index，就请返回 -1。

何为山脉数组？如果数组 A 是一个山脉数组的话，那它满足如下条件：
首先，A.length >= 3
其次，在 0 < i < A.length - 1 条件下，存在 i 使得：
A[0] < A[1] < ... A[i-1] < A[i]
A[i] > A[i+1] > ... > A[A.length - 1]
 
你将 不能直接访问该山脉数组，必须通过 MountainArray 接口来获取数据：
MountainArray.get(k) - 会返回数组中索引为k 的元素（下标从 0 开始）
MountainArray.length() - 会返回该数组的长度
 

注意：
对 MountainArray.get 发起超过 100 次调用的提交将被视为错误答案。此外，任何试图规避判题系统的解决方案都将会导致比赛资格被取消。


 

示例 1：

输入：array = [1,2,3,4,5,3,1], target = 3
输出：2
解释：3 在数组中出现了两次，下标分别为 2 和 5，我们返回最小的下标 2。
示例 2：

输入：array = [0,1,2,4,2,1], target = 3
输出：-1
解释：3 在数组中没有出现，返回 -1。
 
class Solution {
    public int findInMountainArray(int target, MountainArray mountainArr) {
        /*
        使用二分查找
        先使用二分查找找出山顶索引
        山顶左边为升序，右边为降序，那么使用左边进行二分，再右边进行二分
        */
        int len = mountainArr.length();

        int left = 0;
        int right = len - 1;
        //获取山顶索引
        int mid = getHighIndex(mountainArr, left, right);
        //二分查找左边升序部分，如果不为 -1，那么直接返回
        left = getLeft(mountainArr, left, mid, target);
        if(left != -1){
            return left;
        }else{
            //二分查找右边降序部分
            return getRight(mountainArr, mid + 1, right, target);
        }
    }
    //二分查找山顶索引位置
    private int getHighIndex(MountainArray mountainArr, int left, int right){
        while(left < right){
            int mid = (left + right) >>> 1;
            if(mountainArr.get(mid) < mountainArr.get(mid + 1)){
                left = mid + 1;
            }else{
                right = mid;
            }
        }
        return left;
    }
    //左边升序，查找 target
    private int getLeft(MountainArray mountainArr, int left, int right, int target){
        //使用 left <= right，内部直接判断 mid 位置是否为 target,而不用在退出循环后判断再多调用一次 get(left) 来判断是否是 target 
        while(left <= right){
            int mid = (left + right) >>> 1;
            int midNum = mountainArr.get(mid);
            if(midNum == target){
                return mid;
            }
            if(midNum < target){
                left = mid + 1;
            }else{
                right = mid - 1;
            }
        }
        return -1;
    }
    //右边降序，查找 target
    private int getRight(MountainArray mountainArr, int left, int right, int target){
        while(left <= right){
            int mid = (left + right) >>> 1;
            int midNum = mountainArr.get(mid);
            if(midNum == target){
                return mid;
            }
            if(midNum < target){
                right = mid - 1;
            }else{
                left = mid + 1;
            }
        }
        return -1;
    }
}

