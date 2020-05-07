动物收容所。有家动物收容所只收容狗与猫，且严格遵守“先进先出”的原则。在收养该收容所的动物时，收养人只能收养所有动物中“最老”（由其进入收容所的时间长短而定）的动物，或者可以挑选猫或狗（同时必须收养此类动物中“最老”的）。换言之，收养人不能自由挑选想收养的对象。请创建适用于这个系统的数据结构，实现各种操作方法，比如enqueue、dequeueAny、dequeueDog和dequeueCat。允许使用Java内置的LinkedList数据结构。

enqueue方法有一个animal参数，animal[0]代表动物编号，animal[1]代表动物种类，其中 0 代表猫，1 代表狗。

dequeue*方法返回一个列表[动物编号, 动物种类]，若没有可以收养的动物，则返回[-1,-1]。

示例1:

 输入：
["AnimalShelf", "enqueue", "enqueue", "dequeueCat", "dequeueDog", "dequeueAny"]
[[], [[0, 0]], [[1, 0]], [], [], []]
 输出：
[null,null,null,[0,0],[-1,-1],[1,0]]
示例2:

 输入：
["AnimalShelf", "enqueue", "enqueue", "enqueue", "dequeueDog", "dequeueCat", "dequeueAny"]
[[], [[0, 0]], [[1, 0]], [[2, 1]], [], [], []]
 输出：
[null,null,null,null,[2,1],[0,0],[1,0]]

//思路①、我的做法
class AnimalShelf {
    /*
    注意：编号是递增的
    dequeueAny 返回的是 猫 和 狗 中最先进入的一个
    dequeueDog 返回的是 狗 中最先进入的一个
    dequeueCat 返回的是 猫 中最先进入的一个

    我们使用 map 记录每个动物进入的全局编号作为时间
    */
    Map<int[], Integer> map;
    int number = 0;
    List<int[]> cats;
    List<int[]> dogs;
    public AnimalShelf() {
        map = new HashMap<>();
        cats = new LinkedList<>();
        dogs = new LinkedList<>();
    }
    
    public void enqueue(int[] animal) {
        if(animal[1] == 0){
            cats.add(animal);
        }else{
            dogs.add(animal);
        }
        map.put(animal, number++);
    }
    
    public int[] dequeueAny() {
        if(cats.size() == 0 && dogs.size() == 0){
            return new int[]{-1, -1};
        }
        if(cats.size() == 0){
            return dogs.remove(0);
        }
        if(dogs.size() == 0){
            return cats.remove(0);
        }
        return map.get(cats.get(0)) > map.get(dogs.get(0)) ? dogs.remove(0) : cats.remove(0);
    }
    
    public int[] dequeueDog() {
        if(dogs.size() == 0){
            return new int[]{-1, -1};
        }
        return dogs.remove(0);
    }
    
    public int[] dequeueCat() {
        if(cats.size() == 0){
            return new int[]{-1, -1};
        }
        return cats.remove(0);
    }
}

//思路②、
class AnimalShelf {
    /*
    注意：编号是递增的
    dequeueAny 返回的是 猫 和 狗 中最先进入的一个
    dequeueDog 返回的是 狗 中最先进入的一个
    dequeueCat 返回的是 猫 中最先进入的一个

    我们使用 一个 list 记录 猫 和 狗的添加顺序，
    */
    List<int[]> res;
    int[] error = new int[]{-1,-1};
    public AnimalShelf() {
        res = new LinkedList<>();
    }
    
    public void enqueue(int[] animal) {
        res.add(animal);
    }
    
    public int[] dequeueAny() {
        if(res.size() == 0){
            return error;
        }
        return res.remove(0);
    }
    
    public int[] dequeueDog() {
        for(int i = 0; i < res.size(); i++){
            if(res.get(i)[1] == 1){
                return res.remove(i);
            }
        }
        return error;
    }
    
    public int[] dequeueCat() {
        for(int i = 0; i < res.size(); i++){
            if(res.get(i)[1] == 0){
                return res.remove(i);
            }
        }
        return error;
    }
}
