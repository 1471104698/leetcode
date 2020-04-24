珠玑妙算游戏（the game of master mind）的玩法如下。

计算机有4个槽，每个槽放一个球，颜色可能是红色（R）、黄色（Y）、绿色（G）或蓝色（B）。
例如，计算机可能有RGGB 4种（槽1为红色，槽2、3为绿色，槽4为蓝色）。作为用户，你试图猜出颜色组合。
打个比方，你可能会猜YRGB。要是猜对某个槽的颜色，则算一次“猜中”；要是只猜对颜色但槽位猜错了，则算一次“伪猜中”。注意，“猜中”不能算入“伪猜中”。

给定一种颜色组合solution和一个猜测guess，编写一个方法，返回猜中和伪猜中的次数answer，其中answer[0]为猜中的次数，answer[1]为伪猜中的次数。

示例：

输入： solution="RGBY",guess="GGRR"
输出： [1,1]
解释： 猜中1次，伪猜中1次。
提示：

len(solution) = len(guess) = 4
solution和guess仅包含"R","G","B","Y"这4种字符


class Solution {
    public int[] masterMind(String solution, String guess) {
        /*
        题目意思：
        solution="RGGB",guess="YRGB"
                  0123         0123
        我们可以看出 两个字符串 2 和 3 位置都是相同，对应得上，因此猜中次数为 2
        0 和 1 位置对应不上，因此我们看伪猜中次数， solution 出现了 1 次 R 和 1 次 G，guess 出现了 1 次 Y 和 1 次 R
        都存在 1 个 R，因此伪猜中次数为 1，因此返回 [2, 1]

        使用两个数组 gue 和 fact 记录 猜测 和 实际颜色 的个数
        如果 i 位置时，两个字符串的字符都相同，那么直接猜中次数 + 1
        如果不相同，那么记录对应的字符到数组
        */
        int[] gue = new int[4];
        int[] fact = new int[4];

        char[] ss = solution.toCharArray();
        char[] gs = guess.toCharArray();

        int len = solution.length();

        int[] res = new int[2];
        for(int i = 0; i < len; i++){
            if(ss[i] == gs[i]){
                res[0]++;
            }else{
                gue[getIndex(gs[i])]++;
                fact[getIndex(ss[i])]++;
            }
        }
        for(int i = 0; i < 4; i++){
            res[1] += Math.min(gue[i], fact[i]);
        }
        return res;
    }
    private int getIndex(char ch){
        switch(ch){
            case 'R': return 0;
            case 'Y': return 1;
            case 'G': return 2;
            default: return 3;
        }
    }
}