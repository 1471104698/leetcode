
给你一个数组 orders，表示客户在餐厅中完成的订单，确切地说， orders[i]=[customerNamei,tableNumberi,foodItemi] ，
其中 customerNamei 是客户的姓名，tableNumberi 是客户所在餐桌的桌号，而 foodItemi 是客户点的餐品名称。

请你返回该餐厅的 点菜展示表 。在这张表中，表中第一行为标题，其第一列为餐桌桌号 “Table” ，
后面每一列都是按字母顺序排列的餐品名称。接下来每一行中的项则表示每张餐桌订购的相应餐品数量，
第一列应当填对应的桌号，后面依次填写下单的餐品数量。

注意：客户姓名不是点菜展示表的一部分。此外，表中的数据行应该按餐桌桌号升序排列。

 

示例 1：

输入：orders = [["David","3","Ceviche"],["Corina","10","Beef Burrito"],["David","3","Fried Chicken"],["Carla","5","Water"],["Carla","5","Ceviche"],["Rous","3","Ceviche"]]
输出：[["Table","Beef Burrito","Ceviche","Fried Chicken","Water"],["3","0","2","1","0"],["5","0","1","0","1"],["10","1","0","0","0"]] 
解释：
点菜展示表如下所示：
Table,Beef Burrito,Ceviche,Fried Chicken,Water
3    ,0           ,2      ,1            ,0
5    ,0           ,1      ,0            ,1
10   ,1           ,0      ,0            ,0
对于餐桌 3：David 点了 "Ceviche" 和 "Fried Chicken"，而 Rous 点了 "Ceviche"
而餐桌 5：Carla 点了 "Water" 和 "Ceviche"
餐桌 10：Corina 点了 "Beef Burrito" 

class Solution {
    public List<List<String>> displayTable(List<List<String>> orders) {
        List<List<String>> res = new ArrayList<>();
        /*
            我们边遍历边统计餐品名称，并自动进行排序

            使用 map 记录某个桌子点的菜品的数量
        */

        //记录菜品名称，使用 TreeSet，方便排序和去重
        Set<String> foods = new TreeSet<>();

        //记录餐桌号, 使用 Integer, 方便排序
        Set<Integer> tables = new TreeSet<>();

        //Map<String, Map<String, Integer>> ，外层 key 对应餐桌号，内层的 key - value 对应 菜品名称 - 份数
        Map<String, Map<String, Integer>> map = new HashMap<>();

        for(List<String> list : orders){
            //list = "David","3","Ceviche"

            //获取餐桌号
            String table = list.get(1);

            //添加餐桌号
            tables.add(Integer.parseInt(table));

            if(!map.containsKey(table)){
                map.put(table, new HashMap<>());
            }

            //获取餐桌号
            Map<String, Integer> tableMap = map.get(table);
            //获取菜品名称
            String food = list.get(2);
            //添加菜品数量 + 1
            tableMap.put(food, tableMap.getOrDefault(food, 0) + 1);
            foods.add(food);
        }
        
        //将存储 菜品名称 的 set 转换为 list
        List<String> foodList = new ArrayList<>();
        foodList.add("Table");
        foodList.addAll(foods);
        res.add(foodList);


        //遍历餐桌号
        for(int t : tables){
            String table = String.valueOf(t);
            //添加每一桌对应的情况：比如 最开始餐桌号，后面为菜单：3, 0, 2, 1, 0
            List<String> foodNumber = new ArrayList();

            //先添加餐桌号
            foodNumber.add(table);

            //获取餐桌号对应的菜单 map
            Map<String, Integer> tableMap = map.get(table);
            //foodList 第一个元素是 Table，不是菜品，不需要遍历
            for(int i = 1; i < foodList.size(); i++){
                foodNumber.add(String.valueOf(tableMap.getOrDefault(foodList.get(i), 0)));
            }
            res.add(foodNumber);
        }

        return res;
    }
}