人们会互相发送好友请求，现在给定一个包含有他们年龄的数组，ages[i] 表示第 i 个人的年龄。

当满足以下条件时，A 不能给 B（A、B不为同一人）发送好友请求：

age[B] <= 0.5 * age[A] + 7
age[B] > age[A]
age[B] > 100 && age[A] < 100
否则，A 可以给 B 发送好友请求。

注意如果 A 向 B 发出了请求，不等于 B 也一定会向 A 发出请求。而且，人们不会给自己发送好友请求。 

求总共会发出多少份好友请求?

 

示例 1:

输入: [16,16]
输出: 2
解释: 二人可以互发好友申请。
示例 2:

输入: [16,17,18]
输出: 2
解释: 好友请求可产生于 17 -> 16, 18 -> 17.

class Solution {
    public int numFriendRequests(int[] ages) {

        //最高 120 岁，那么我们统计某个岁数的人数
        int[] arr = new int[121];
        for(int num : ages){
            arr[num]++;
        }

        int c = 0;
        for(int i = 1; i <= 120; i++){
            //给同龄人发邮件
            if(!isNotOk(i, i)){
                c += arr[i] * (arr[i] - 1);
            }
            for(int j = i - 1; j >= 1; j--){
                if(arr[j] != 0 && !isNotOk(i, j)){
                    c += arr[i] * arr[j];
                }
            }
        }
        return c;
    }
    //判断 a 是否不能 发送给 b，如果不能，返回 true
    private boolean isNotOk(int a, int b){
        //这里不需要添加 b > 100 && a < 100，因为这条就一定是 b > a，而前面 b > a 就返回了
        return b <= 0.5 * a + 7 || b > a;
    }
}