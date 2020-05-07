设计链表的实现。您可以选择使用单链表或双链表。单链表中的节点应该具有两个属性：val 和 next。val 是当前节点的值，next 是指向下一个节点的指针/引用。如果要使用双向链表，则还需要一个属性 prev 以指示链表中的上一个节点。假设链表中的所有节点都是 0-index 的。

在链表类中实现这些功能：

get(index)：获取链表中第 index 个节点的值。如果索引无效，则返回-1。
addAtHead(val)：在链表的第一个元素之前添加一个值为 val 的节点。插入后，新节点将成为链表的第一个节点。
addAtTail(val)：将值为 val 的节点追加到链表的最后一个元素。
addAtIndex(index,val)：在链表中的第 index 个节点之前添加值为 val  的节点。如果 index 等于链表的长度，则该节点将附加到链表的末尾。//如果 index 大于链表长度，则不会插入节点。如果index小于0，则在头部插入节点。
deleteAtIndex(index)：如果索引 index 有效，则删除链表中的第 index 个节点。
 

示例：

MyLinkedList linkedList = new MyLinkedList();
linkedList.addAtHead(1);
linkedList.addAtTail(3);
linkedList.addAtIndex(1,2);   //链表变为1-> 2-> 3
linkedList.get(1);            //返回2
linkedList.deleteAtIndex(1);  //现在链表是1-> 3
linkedList.get(1);            //返回3
 

提示：

所有val值都在 [1, 1000] 之内。
操作次数将在  [1, 1000] 之内。
请不要使用内置的 LinkedList 库。

class MyLinkedList {
    /*
    注意：
    对于插入：注意都要 count++
    插入头 和 插入尾没什么可讲的
    中间插入：如果 index <= 0，那么就是插入头，如果 index == count ，那么就是插入尾
            对于中间插入，比如 链表 1 -> 3， index = 1，那么我们就需要得到 0 号节点，即节点值为 1 的节点
            然后利用 0 号节点插入即可

    查找节点：直接从 虚拟节点 head 开始找，如果查找的索引范围不是 [0, count - 1]，那么表示越界

    删除节点：比如 链表 1 -> 2 -> 3，删除索引 1，那么我们只需要找到 节点 2，然后将前后节点相连即可
            注意 count--
    */
    class Node{
        int val;
        Node pre;
        Node next;
        public Node(int val){
            this.val = val;
        }
    }
    Node head;
    int count = 0;
    public MyLinkedList() {
        head = new Node(-1);
    }

    public int get(int index) {
        Node node = findIndex(index);
        if(node == null){
            return -1;
        }
        return node.val;
    }

    public void addAtHead(int val) {
        Node node = new Node(val);
        if(count != 0){
            head.next.pre = node;
            node.next = head.next;
        }
        head.next = node;
        node.pre = head;
        count++;
    }

    public void addAtTail(int val) {
        if(count == 0){
            addAtHead(val);
            return;
        }
        Node node = new Node(val);
        Node targetNode = findIndex(count - 1);

        targetNode.next = node;
        node.pre = targetNode;
        count++;
    }

    public void addAtIndex(int index, int val) {
        //当 index <= 0 那么就是插在开头
        if(index <= 0){
            addAtHead(val);
            return;
        }
        //当 index == count,那么就是插在尾部
        if(index == count){
            addAtTail(val);
            return;
        }
        if(!isExist(index)){
            return;
        }
        Node node = new Node(val);
        Node targetNode = findIndex(index - 1);
        targetNode.next.pre = node;
        node.next = targetNode.next;
        targetNode.next = node;
        node.pre = targetNode;
        count++;
    }

    public void deleteAtIndex(int index) {
        if(!isExist(index)){
            return;
        }
        Node targetNode = findIndex(index);
        if(targetNode.next != null){
            targetNode.next.pre = targetNode.pre;
        }
        targetNode.pre.next = targetNode.next;
        count--;
    }

    //查找某个位置的节点, index：索引位置，范围 [0, count - 1]
    private Node findIndex(int index){
        if(!isExist(index)){
            return null;
        }
        Node cur = head;
        while(index-- >= 0){
            cur = cur.next;
        }
        return cur;
    }

    private boolean isExist(int index){
        return index < count;
    }
}

