稀疏数组搜索。有个排好序的字符串数组，其中散布着一些空字符串，编写一种方法，找出给定字符串的位置。

示例1:

 输入: words = ["at", "", "", "", "ball", "", "", "car", "", "","dad", "", ""], s = "ta"
 输出：-1
 说明: 不存在返回-1。
示例2:

 输入：words = ["at", "", "", "", "ball", "", "", "car", "", "","dad", "", ""], s = "ball"
 输出：4
提示:

words的长度在[1, 1000000]之间

class Solution {
    public int findString(String[] words, String s) {
        int left = 0;
        int right = words.length - 1;
        while(left <= right){
            //去除空串
            while("".equals(words[left]) && left < right) left++;
            while("".equals(words[right]) && left < right) right--;

            int mid = (left + right) >>> 1;

            /*
            当 mid 位置字符串为空串时，我们直接移动 mid ，向 right 靠拢（或向 left 靠拢）
            而无需去判断 left 、right 位置的字符串 和 s 的大小关系进而来移动 left 和 right
            */
            while("".equals(words[mid]) && mid < right) mid++;

            int res = words[mid].compareTo(s);

            if(res < 0){
                left = mid + 1;
            }else if(res > 0){
                right = mid - 1;
            }else{
                return mid;
            }
        }
        return -1;
    }
}