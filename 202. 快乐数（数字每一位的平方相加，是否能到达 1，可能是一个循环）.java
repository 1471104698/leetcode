编写一个算法来判断一个数 n 是不是快乐数。

「快乐数」定义为：对于一个正整数，每一次将该数替换为它每个位置上的数字的平方和，然后重复这个过程直到这个数变为 1，也可能是 无限循环 但始终变不到 1。如果 可以变为  1，那么这个数就是快乐数。

如果 n 是快乐数就返回 True ；不是，则返回 False 。

 

示例：

输入：19
输出：true
解释：
12 + 92 = 82
82 + 22 = 68
62 + 82 = 100
12 + 02 + 02 = 1

//思路①、如果不能为 1，那么肯定会进入无限循环，那么会重复某些值，使用 set 记录得到的值，如果重复出现表示不能变成 1
class Solution {
    public boolean isHappy(int n) {
        Set<Integer> set = new HashSet<>();
        while(!set.contains(n)){
            set.add(n);
            int temp = 0;
            while(n != 0){
                temp += (int)Math.pow(n % 10, 2);
                n /= 10;
            }
            if(temp == 1){
                return true;
            }
            n = temp;
        }
        return false;
    }
}

//思路②、既然是循环，那么循环问题就可以使用快慢指针
class Solution {
    public boolean isHappy(int n) {
        int slow = n;
        int fast = n;
        do{
            slow = getNext(slow);
            fast = getNext(getNext(fast));
            if(fast == 1){
                return true;
            }
        }while(slow != fast);
        return false;
    }
    private int getNext(int n){
        int  temp = 0;
        while(n != 0){
            temp += (int)Math.pow(n % 10, 2);
            n /= 10;
        }
        return temp;
    }
}