给定一个长度为偶数的整数数组 A，只有对 A 进行重组后可以满足 “对于每个 0 <= i < len(A) / 2，都有 A[2 * i + 1] = 2 * A[2 * i]” 时，返回 true；否则，返回 false。

 

示例 1：

输入：[3,1,3,6]
输出：false
示例 2：

输入：[2,1,2,6]
输出：false
示例 3：

输入：[4,-2,2,-4]
输出：true
解释：我们可以用 [-2,-4] 和 [2,4] 这两组组成 [-2,-4,2,4] 或是 [2,4,-2,-4]
示例 4：

输入：[1,2,4,16,8,4]
输出：false
 

提示：

0 <= A.length <= 30000
A.length 为偶数
-100000 <= A[i] <= 100000

class Solution {
    Map<Integer, Integer> map;
    public boolean canReorderDoubled(int[] A) {
        /*
        将数组升序排序，那么元素排序如下
        -8 -7 -6 -5 -4 -3 -2 -1 0 1 2 3 4 5 6 7 8
        对于负数，我们只求 * 2
        对于正数，我们只求 / 2
        因为比如 存在 -8，那么它 *2 不能消掉，那么只能 /2 消去，因此需要留着，等到遍历到 -4 的时候再消去，即我们是留着后面再消去
        比如 1，那么它 /2 不能消掉，那么只能 * 2 消去，因此需要留着，等到遍历到 2 的时候再消去，即我们是留着后面再消去
         */
        Arrays.sort(A);
        //记录某个数字
        map = new HashMap<>();
        for(int num : A){
            if(num < 0){
                int big = num * 2;
                if(getTime(big) != 0){
                    put(big, getTime(big) - 1);
                }else{
                    put(num, getTime(num) + 1);
                }
            }else if(num > 0 && (num & 1) == 0){	//大于 0 并且还是偶数
                int small = num / 2;
                if(getTime(small) != 0){
                    put(small, getTime(small) - 1);
                }else{
                    put(num, getTime(num) + 1);
                }
            }else{
                map.put(num, getTime(num) + 1);
            }
        }
        for(Map.Entry<Integer, Integer>  entry : map.entrySet()){
            if(entry.getKey() == 0){
                if((entry.getValue() & 1) != 0){
                    return false;
                }
            }else if(entry.getValue() != 0){
                return false;
            }
        }
        return true;
    }
    private int getTime(int num){
        return map.getOrDefault(num, 0);
    }
    private boolean isExist(int num){
        return map.containsKey(num);
    }
    private void put(int num, int time){
        map.put(num, time);
    }
}