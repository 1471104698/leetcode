请你实现一个类 UndergroundSystem ，它支持以下 3 种方法：

1. checkIn(int id, string stationName, int t)

编号为 id 的乘客在 t 时刻进入地铁站 stationName 。
一个乘客在同一时间只能在一个地铁站进入或者离开。
2. checkOut(int id, string stationName, int t)

编号为 id 的乘客在 t 时刻离开地铁站 stationName 。
3. getAverageTime(string startStation, string endStation) 

返回从地铁站 startStation 到地铁站 endStation 的平均花费时间。
平均时间计算的行程包括当前为止所有从 startStation 直接到达 endStation 的行程。
调用 getAverageTime 时，询问的路线至少包含一趟行程。
你可以假设所有对 checkIn 和 checkOut 的调用都是符合逻辑的。	//注意：是合理的，即一个 id 入站后 必须伴随着一次出站
也就是说，如果一个顾客在 t1 时刻到达某个地铁站，那么他离开的时间 t2 一定满足 t2 > t1 。所有的事件都按时间顺序给出。

 

示例：

输入：
["UndergroundSystem","checkIn","checkIn","checkIn","checkOut","checkOut","checkOut","getAverageTime","getAverageTime","checkIn","getAverageTime","checkOut","getAverageTime"]
[[],[45,"Leyton",3],[32,"Paradise",8],[27,"Leyton",10],[45,"Waterloo",15],[27,"Waterloo",20],
[32,"Cambridge",22],["Paradise","Cambridge"],["Leyton","Waterloo"],[10,"Leyton",24],
["Leyton","Waterloo"],[10,"Waterloo",38],["Leyton","Waterloo"]]

输出：
[null,null,null,null,null,null,null,14.0,11.0,null,11.0,null,12.0]

解释：
UndergroundSystem undergroundSystem = new UndergroundSystem();
undergroundSystem.checkIn(45, "Leyton", 3);
undergroundSystem.checkIn(32, "Paradise", 8);
undergroundSystem.checkIn(27, "Leyton", 10);
undergroundSystem.checkOut(45, "Waterloo", 15);
undergroundSystem.checkOut(27, "Waterloo", 20);
undergroundSystem.checkOut(32, "Cambridge", 22);
undergroundSystem.getAverageTime("Paradise", "Cambridge");       // 返回 14.0。从 "Paradise"（时刻 8）到 "Cambridge"(时刻 22)的行程只有一趟
undergroundSystem.getAverageTime("Leyton", "Waterloo");          // 返回 11.0。总共有 2 躺从 "Leyton" 到 "Waterloo" 的行程，编号为 id=45 的乘客出发于 time=3 到达于 time=15，
																				//编号为 id=27 的乘客于 time=10 出发于 time=20 到达。所以平均时间为 ( (15-3) + (20-10) ) / 2 = 11.0
undergroundSystem.checkIn(10, "Leyton", 24);
undergroundSystem.getAverageTime("Leyton", "Waterloo");          // 返回 11.0
undergroundSystem.checkOut(10, "Waterloo", 38);
undergroundSystem.getAverageTime("Leyton", "Waterloo");          // 返回 12.0
 

提示：

总共最多有 20000 次操作。
1 <= id, t <= 10^6
所有的字符串包含大写字母，小写字母和数字。
1 <= stationName.length <= 10
与标准答案误差在 10^-5 以内的结果都视为正确结果。

class UndergroundSystem {
    /*	
		记录某个用户 id 入站 的 站名 和 时间
		
        在某个用户出站的时候，获取它入站的时间，并计算总的时间存储起来
    */
    class Node<T, V>{
        T v1;
        V v2;
        public Node(T v1, V v2){
            this.v1 = v1;
            this.v2 = v2;
        }
    }
    //存储 用户 id 对应 入站的 地点和时间
    Map<Integer, Node<Integer, String>> map;
    //存储 入站 + 出战 和 总时间 + 次数，后续直接使用 总时间 / 次数即可
    Map<String, Node<Integer, Integer>> time;
    public UndergroundSystem() {
        map = new HashMap<>();
        time = new HashMap<>();
    }
    
    public void checkIn(int id, String stationName, int t) {
        map.put(id, new Node(t, stationName));
    }
    
    public void checkOut(int id, String stationName, int t) {
        //获取用户入站节点
        Node<Integer, String> node = map.get(id);
        
        //将 入站 和 出战 作为 key
        String key = node.v2 + stationName;

        if(!time.containsKey(key)){
            time.put(key, new Node(0, 0));
        }
        Node<Integer, Integer> timeNode = time.get(key);
        timeNode.v1 += t - node.v1;
        timeNode.v2++;
    }
    
    public double getAverageTime(String startStation, String endStation) {
        Node<Integer, Integer> timeNode = time.get(startStation + endStation);
        return (double)timeNode.v1 / timeNode.v2;
    }
}
