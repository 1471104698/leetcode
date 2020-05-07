设计和构建一个“最近最少使用”缓存，该缓存会删除最近最少使用的项目。缓存应该从键映射到值(允许你插入和检索特定键对应的值)，并在初始化时指定最大容量。当缓存被填满时，它应该删除最近最少使用的项目。

它应该支持以下操作： 获取数据 get 和 写入数据 put 。

获取数据 get(key) - 如果密钥 (key) 存在于缓存中，则获取密钥的值（总是正数），否则返回 -1。
写入数据 put(key, value) - 如果密钥不存在，则写入其数据值。当缓存容量达到上限时，它应该在写入新数据之前删除最近最少使用的数据值，从而为新的数据值留出空间。

示例:

LRUCache cache = new LRUCache( 2 /* 缓存容量 */ );

cache.put(1, 1);
cache.put(2, 2);
cache.get(1);       // 返回  1
cache.put(3, 3);    // 该操作会使得密钥 2 作废
cache.get(2);       // 返回 -1 (未找到)
cache.put(4, 4);    // 该操作会使得密钥 1 作废
cache.get(1);       // 返回 -1 (未找到)
cache.get(3);       // 返回  3
cache.get(4);       // 返回  4

class LRUCache {
    class Node{
        int val;
        int key;    //为什么需要 key ？用于 map 的移除
        Node pre;
        Node next;
        public Node(int val, int key){
            this.val = val;
            this.key = key;
        }
    }
    int count;
    int capacity;
    Node head;
    Node tail;
    //key 和 Node 进行映射
    Map<Integer, Node> map;
    public LRUCache(int capacity) {
        map = new HashMap<>(capacity);
        count = 0;
        this.capacity = capacity;
        head = null;
        tail = null;
    }

    public int get(int key) {
        if(!map.containsKey(key)){
            return -1;
        }
        Node node = map.get(key);
        //将该节点移至最前端
        oldNodeMoveToHead(node);
        return node.val;
    }

    public void put(int key, int value) {
        Node node = map.getOrDefault(key, new Node(value, key));
        if(!map.containsKey(key)){
            count++;
            map.put(key, node);
        }
        //可能是同一个 key，更新为不同的值
        node.val = value;
        newNodeMoveToHead(node);
    }

    private void newNodeMoveToHead(Node node){
        //如果是第一个节点，那么直接设置
        if(head == null){
            head = tail = node;
            return;
        }
        //如果是新加的节点
        if(node.pre == null && node.next == null){
            head.pre = node;
            node.next = head;
            head = node;
        }
        //超出容量了（新加了一个节点导致超出容量） ，那么需要移除掉最后一个
        if(count > capacity){
            Node temp = tail;
            tail = tail.pre;
            tail.next = null;
            //当从链表中移除掉一个后，需要将 count-- 防止一直超出容量
            count--;
            //将移除的节点从 map 中移除，防止 get 还能获取到
            map.remove(temp.key);
        }else{
            //node 为已有节点，进行已有节点的移动
            oldNodeMoveToHead(node);
        }
    }

    private void oldNodeMoveToHead(Node node){
        //只有这一个节点
        if(count == 1){
            return;
        }
        //是头节点
        if(head == node){
            return;
        }
        //是尾节点
        if(tail == node){
            tail = node.pre;
            tail.next = null;
            node.pre = null;
            head.pre = node;
            node.next = head;
            head = node;
            return;
        }
        //是中间节点
        node.pre.next = node.next;
        node.next.pre = node.pre;
        head.pre = node;
        node.next = head;
        node.pre = null;
        head = node;
    }
}