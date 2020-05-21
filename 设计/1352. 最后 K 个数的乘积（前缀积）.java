请你实现一个「数字乘积类」ProductOfNumbers，要求支持下述两种方法：

1. add(int num)

将数字 num 添加到当前数字列表的最后面。
2. getProduct(int k)

返回当前数字列表中，最后 k 个数字的乘积。
你可以假设当前列表中始终 至少 包含 k 个数字。
题目数据保证：任何时候，任一连续数字序列的乘积都在 32-bit 整数范围内，不会溢出。


示例：

输入：
["ProductOfNumbers","add","add","add","add","add","getProduct","getProduct","getProduct","add","getProduct"]
[[],[3],[0],[2],[5],[4],[2],[3],[4],[8],[2]]

输出：
[null,null,null,null,null,null,20,40,0,null,32]

解释：
ProductOfNumbers productOfNumbers = new ProductOfNumbers();
productOfNumbers.add(3);        // [3]
productOfNumbers.add(0);        // [3,0]
productOfNumbers.add(2);        // [3,0,2]
productOfNumbers.add(5);        // [3,0,2,5]
productOfNumbers.add(4);        // [3,0,2,5,4]
productOfNumbers.getProduct(2); // 返回 20 。最后 2 个数字的乘积是 5 * 4 = 20
productOfNumbers.getProduct(3); // 返回 40 。最后 3 个数字的乘积是 2 * 5 * 4 = 40
productOfNumbers.getProduct(4); // 返回  0 。最后 4 个数字的乘积是 0 * 2 * 5 * 4 = 0
productOfNumbers.add(8);        // [3,0,2,5,4,8]
productOfNumbers.getProduct(2); // 返回 32 。最后 2 个数字的乘积是 4 * 8 = 32 
 

提示：

add 和 getProduct 两种操作加起来总共不会超过 40000 次。
0 <= num <= 100
1 <= k <= 40000


class ProductOfNumbers {

    /*
    前缀乘积：前缀和思想
    比如 3,2,0,2,5,4
	首先集合添加一个元素 1，方便处理遇到的第一个元素可以直接 计算乘积，无需判空
    遇到 0，那么将前面的乘积都清空，重新开始计算（需要重新 add(1)）
    前面计算结果集合为 {1, 3, 6}，然后遇到 0，清空，变成 {1}
    后面的结算结果为 {1, 2, 10, 40}，如果 k 大于等于 集合元素个数（注意，除去 1 本来只有 3 个，如果 k = 4 ，那么就超过这 3 个），表示需要包含之前的 0 元素，那么乘积结果必定为 0，因此直接返回 0
    */
    List<Integer> product;
    int count;
    public ProductOfNumbers() {
        product = new ArrayList<>();
        product.add(1);
        count = 1;
    }
    
    public void add(int num) {
        if(num == 0){
            product.clear();
            product.add(1);
            count = 1;
        }else{
            product.add(product.get(count - 1) * num);
            count++;
        }
    }
    
    public int getProduct(int k) {
        if(k >= count){
            return 0;
        }
        return product.get(count - 1) / product.get(count - k - 1);
    }
}
