给你一个待查数组 queries ，数组中的元素为 1 到 m 之间的正整数。 请你根据以下规则处理所有待查项 queries[i]（从 i=0 到 i=queries.length-1）：

一开始，排列 P=[1,2,3,...,m]。
对于当前的 i ，请你找出待查项 queries[i] 在排列 P 中的位置（下标从 0 开始），然后将其从原位置移动到排列 P 的起始位置（即下标为 0 处）。注意， queries[i] 在 P 中的位置就是 queries[i] 的查询结果。
请你以数组形式返回待查数组  queries 的查询结果。

 

示例 1：

输入：queries = [3,1,2,1], m = 5
输出：[2,1,2,1] 
解释：待查数组 queries 处理如下：
对于 i=0: queries[i]=3, P=[1,2,3,4,5], 3 在 P 中的位置是 2，接着我们把 3 移动到 P 的起始位置，得到 P=[3,1,2,4,5] 。
对于 i=1: queries[i]=1, P=[3,1,2,4,5], 1 在 P 中的位置是 1，接着我们把 1 移动到 P 的起始位置，得到 P=[1,3,2,4,5] 。 
对于 i=2: queries[i]=2, P=[1,3,2,4,5], 2 在 P 中的位置是 2，接着我们把 2 移动到 P 的起始位置，得到 P=[2,1,3,4,5] 。
对于 i=3: queries[i]=1, P=[2,1,3,4,5], 1 在 P 中的位置是 1，接着我们把 1 移动到 P 的起始位置，得到 P=[1,2,3,4,5] 。 
因此，返回的结果数组为 [2,1,2,1] 。  


/*  

    每次查询到某个值就将值移动到头部，这相当于 LRU
    当查询到某个值的时候，我们从头开始遍历直到找到该节点，记录遍历过多少个节点作为索引位置
    然后我们将节点移到前面当作第一个节点
*/

class Solution {
    public int[] processQueries(int[] queries, int m) {
        LRU lru = new LRU(m);

        int len = queries.length;
        int[] res = new int[len];
        for(int i = 0; i < len; i++){
            res[i] = lru.getIdx(queries[i]);
        }
        return res;
    }
    class LRU {
        class Node {
            Node pre;
            Node next;
            int val;
            public Node(int val){
                this.val = val;
            }
        }
        //头节点（dummy 节点）
        Node head;

        // val 值 和 节点一一对应，方便通过 val 值直接获取 node 节点
        Map<Integer, Node> map = new HashMap<>();

        public LRU(int m){
            head = new Node(-1);
            //插入 [1, m] 节点
            Node cur = head;
            for(int i = 1; i <= m; i++){
                Node node = new Node(i);
                //相连
                node.pre = cur;
                cur.next = node;
                cur = node;
                //将 val 值 和 对应节点进行绑定，方便我们通过 val 值获取节点
                map.put(i, node);
            }
        }

        public int getIdx(int val){
            //从 map 中获取对应节点
            Node node = map.get(val);
            //获取节点对应的索引位置
            int idx = getIdx(node);
            //移到前面作为首节点
            removeToHead(node);
            return idx;
        }
        
        private int getIdx(Node node){
            //因为我们要求的是索引位置，因此 idx 从 0 开始，并且从 head.next 开始遍历
            int idx = 0;
            for(Node cur = head.next; cur != node; cur = cur.next){
                idx++;
            }
            return idx;
        }

        //将节点移动到首部
        public void removeToHead(Node node){
            //如果 node 有后继节点，那么将后继节点和 前驱节点相连
            if(node.next != null){
                node.next.pre = node.pre;
            }
            node.pre.next = node.next;
            //如果 head 有后继节点，那么将后继节点跟 node 相连
            if(head.next != null){
                head.next.pre = node;
            }
            node.next = head.next;
            node.pre = head;
            head.next = node;
        }
    }
}