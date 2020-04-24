给你一个由一些多米诺骨牌组成的列表 dominoes。

如果其中某一张多米诺骨牌可以通过旋转 0 度或 180 度得到另一张多米诺骨牌，我们就认为这两张牌是等价的。

形式上，dominoes[i] = [a, b] 和 dominoes[j] = [c, d] 等价的前提是 a==c 且 b==d，或是 a==d 且 b==c。

在 0 <= i < j < dominoes.length 的前提下，找出满足 dominoes[i] 和 dominoes[j] 等价的骨牌对 (i, j) 的数量。

 

示例：

输入：dominoes = [[1,2],[2,1],[3,4],[5,6]]
输出：1
 

提示：

1 <= dominoes.length <= 40000
1 <= dominoes[i][j] <= 9


class Solution {
    public int numEquivDominoPairs(int[][] dominoes) {

        /*
        因为题目说了，每个元素在 [0, 9]，因此可以使用 i * 10 + j 作为索引
        将一维数组排序，然后统计等价的数组个数
		
        最后对每个等价数组进行排列组合的统计： n * (n - 1) / 2
		比如 a b c d 任意组合，有
			b	   	   c		   d
		a	c	和 b   d	和 c		6 种
			d
        */
        int[] cp = new int[100];
        for(int[] arr : dominoes){
            Arrays.sort(arr);
            cp[arr[0] * 10 + arr[1]]++;
        }
        int count = 0;
        for(int num : cp){
            if(num != 0){
                count += num * (num - 1) / 2;
            }
        }
        return count;
    }
}