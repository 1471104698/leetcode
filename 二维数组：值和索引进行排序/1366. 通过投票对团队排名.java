现在有一个特殊的排名系统，依据参赛团队在投票人心中的次序进行排名，每个投票者都需要按从高到低的顺序对参与排名的所有团队进行排位。

排名规则如下：

参赛团队的排名次序依照其所获「排位第一」的票的多少决定。如果存在多个团队并列的情况，将继续考虑其「排位第二」的票的数量。以此类推，直到不再存在并列的情况。
如果在考虑完所有投票情况后仍然出现并列现象，则根据团队字母的字母顺序进行排名。
给你一个字符串数组 votes 代表全体投票者给出的排位情况，请你根据上述排名规则对所有参赛团队进行排名。

请你返回能表示按排名系统 排序后 的所有团队排名的字符串。

 

示例 1：

输入：votes = ["ABC","ACB","ABC","ACB","ACB"]
输出："ACB"
解释：A 队获得五票「排位第一」，没有其他队获得「排位第一」，所以 A 队排名第一。
B 队获得两票「排位第二」，三票「排位第三」。
C 队获得三票「排位第二」，两票「排位第三」。
由于 C 队「排位第二」的票数较多，所以 C 队排第二，B 队排第三。
示例 2：

输入：votes = ["WXYZ","XYZW"]
输出："XWYZ"
解释：X 队在并列僵局打破后成为排名第一的团队。X 队和 W 队的「排位第一」票数一样，但是 X 队有一票「排位第二」，而 W 没有获得「排位第二」。 
示例 3：

输入：votes = ["ZMNAGUEDSJYLBOPHRQICWFXTVK"]
输出："ZMNAGUEDSJYLBOPHRQICWFXTVK"
解释：只有一个投票者，所以排名完全按照他的意愿。

class Solution {
    public String rankTeams(String[] votes) {
        /*
        使用二维数组， temp[i][j] 表示第 i 队 第 j 名的票数
        
        为了方便排序后处理，我们将 temp[i][0] 作为该名参赛者的 id，比如 'A' 的 id 为 0， 'B' 的 id 为 1
        然后 temp[i][1] - temp[i][26] 作为对应名次票数

        思路：我们先统计各个 id 对应名次的票数
        然后根据名次票数、 id 大小进行排序
        最后直接统计，过程如下代码注释
        */
        int len = votes.length;

        if(len == 1){
            return votes[0];
        }

        //总共多少个名次， 例如 ："ABC" ，3 位参赛者则有 3 个名次
        int size = votes[0].length();
        
        int[][] temp = new int[26][size + 1];

        //将 0 号位置作为 id
        for(int i = 0; i < 26; i++){
            temp[i][0] = i;
        }

        //记录各个 id 对应的名次票数
        for(String str : votes){
            for(int i = 0; i < size; i++){
                temp[str.charAt(i) - 'A'][i + 1]++;
            }
        }

        /*
        自定义排序：按 各 名次 票数进行排序，前面 名次 票数多的排前面
        如果所有名次票数都一样，那么根据 id 排序，小的排前面
        比如假设 'A' 和 'C' 所有名次票数都一样，但是 'A' 的 id 为 0， 'C' 的 id 为 2，因此 'A' 排在 'C' 的前面
        */
        Arrays.sort(temp, (a, b) -> {
            for(int i = 1; i < a.length; i++){
                if(a[i] < b[i]){
                    return 1;
                }else if(a[i] > b[i]){
                    return -1;
                }
            }
            return a[0] - b[0];
        });
        
        /*
        我们不需要去判断 26 个 id 哪个没有参赛，
        因为排序过后， 票数越少的越排在后面，那么没有参赛的 id 票数都为 0，肯定都是排在最后
        我们只需要使用变量 visited 记录遍历了多少个 id ，当 id == size（参赛人数） 时表示所有参赛 id 遍历完毕
        */
        StringBuilder sb = new StringBuilder();
        for(int i = 0, visited = 0; visited < size; i++, visited++){
            sb.append((char)(temp[i][0] + 'A'));
        }
        return sb.toString();
    }
}