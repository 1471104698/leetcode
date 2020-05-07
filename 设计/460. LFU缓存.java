请你为 最不经常使用（LFU）缓存算法设计并实现数据结构。它应该支持以下操作：get 和 put。

get(key) - 如果键存在于缓存中，则获取键的值（总是正数），否则返回 -1。
put(key, value) - 如果键已存在，则变更其值；如果键不存在，请插入键值对。当缓存达到其容量时，则应该在插入新项之前，使最不经常使用的项无效。在此问题中，当存在平局（即两个或更多个键具有相同使用频率）时，应该去除最久未使用的键。
「项的使用次数」就是自插入该项以来对其调用 get 和 put 函数的次数之和。使用次数会在对应项被移除后置为 0 。

 

进阶：
你是否可以在 O(1) 时间复杂度内执行两项操作？

 

示例：

LFUCache cache = new LFUCache( 2 /* capacity (缓存容量) */ );

cache.put(1, 1);
cache.put(2, 2);
cache.get(1);       // 返回 1
cache.put(3, 3);    // 去除 key 2
cache.get(2);       // 返回 -1 (未找到key 2)
cache.get(3);       // 返回 3
cache.put(4, 4);    // 去除 key 1
cache.get(1);       // 返回 -1 (未找到 key 1)
cache.get(3);       // 返回 3
cache.get(4);       // 返回 4

class LFUCache {
    /*
        使用双向链表，最近使用可以设计成 LRU，然后使用 map 记录使用的次数
    */
    //记录最少的访问次数
    int minVisited = 1;

    //存储 访问次数 time 和 对应次数 LRU 链表
    Map<Integer, DoubleLinkedList> timeMap;

    //存储所有 key 和 节点 的映射
    Map<Integer, Node> nodeMap;

    int size = 0;

    //容量
    int capacity;

    public LFUCache(int capacity) {
        this.timeMap = new HashMap<>();
        this.nodeMap = new HashMap<>();
        this.capacity = capacity;
    }

    public int get(int key) {
        Node node = nodeMap.get(key);
        if(node == null){
            return -1;
        }
        update(node);
        return node.val;
    }
    
    private DoubleLinkedList getList(int time){
        if(!timeMap.containsKey(time)){
            timeMap.put(time, new DoubleLinkedList());
        }
        return timeMap.get(time);
    }

    public void put(int key, int value) {
        Node node;
        //如果不为空，那么将节点更新为新值，并移至表头
        if(nodeMap.containsKey(key)){
            node = nodeMap.get(key);
            node.val = value;
            update(node);
            return;
        }
        node = new Node(key, value);
        nodeMap.put(key, node);
        DoubleLinkedList list = getList(1);
        list.addNode(node);
        if(size >= capacity){
            DoubleLinkedList list2 = getList(minVisited);
            nodeMap.remove(list2.removeTail().key);
        }else{
            size++;
        }
        minVisited = 1;
    }

    private void update(Node node){
        //根据次数 time 获取所在链表，并移除 node 节点
        DoubleLinkedList oldList = timeMap.get(node.time);
        oldList.removeNode(node);

        //如果当前节点所在链表恰好是最小访问次数的链表，并且移除该节点后为空，那么更新最小访问次数
        if(oldList.count == 0 && node.time == minVisited){
            minVisited++;
        }
        //更新访问次数
        node.time++;

        //得到 新的访问次数的 链表
        DoubleLinkedList newList = getList(node.time);
        //链表添加新节点，并移至开头
        newList.addNode(node);
    }

    class Node{
        int time = 1;//访问次数
        int key;
        int val;
        Node pre;
        Node next;

        public Node(int key, int val){
            this.key = key;
            this.val = val;
        }
    }

    //双链表数据结构
    class DoubleLinkedList{
        //虚拟节点
        Node head;
        int count = 0;  //节点的个数
        public DoubleLinkedList(){
            head = new Node(-1, -1);
        }

        //添加节点：这里如果需要添加节点，那么表示是最新访问的，需要移到头节点
        public void addNode(Node node){
            moveToHead(node);
            count++;
        }

        //将某个节点移至头节点
        private void moveToHead(Node node){
            //如果已经是头节点了
            if(node == head.next){
                return;
            }
            //如果是旧节点，那么需要和前后断开，并将前后连接起来
            if(node.pre != null){
                //如果 count == 1，表示只有当前这个节点
                if(node.next != null){
                    node.pre.next = node.next;
                    node.next.pre = node.pre;
                }
            }

            //如果存在别的节点，那么将头指针和后面的节点断开，并将 node 和 后面的节点进行连接
            if(count != 0){
                head.next.pre = node;
                node.next = head.next;
            }
            node.pre = head;
            head.next = node;
        }

        //移除尾节点
        private Node removeTail(){
            count--;
            Node cur = head;
            while(cur.next != null){
                cur = cur.next;
            }
            cur.pre.next = null;
            cur.pre = null;
            return cur;
        }

        //移除某个节点
        public void removeNode(Node node){
            count--;
            if(node.next == null){ //是尾节点
                node.pre.next = null;
            }else{                  //不是尾节点
                node.pre.next = node.next;
                node.next.pre = node.pre;
            }
            node.pre = null;
            node.next = null;
        }
    }
}