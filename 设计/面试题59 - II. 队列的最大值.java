请定义一个队列并实现函数 max_value 得到队列里的最大值，要求函数max_value、push_back 和 pop_front 的均摊时间复杂度都是O(1)。

若队列为空，pop_front 和 max_value 需要返回 -1

示例 1：

输入: 
["MaxQueue","push_back","push_back","max_value","pop_front","max_value"]
[[],[1],[2],[],[],[]]
输出: [null,null,null,2,1,2]
示例 2：

输入: 
["MaxQueue","pop_front","max_value"]
[[],[],[]]
输出: [null,-1,-1]
 

限制：

1 <= push_back,pop_front,max_value的总操作数 <= 10000
1 <= value <= 10^5


    /*
    等下，我们使用一个队列正常的存放数据，
    然后使用维护一个单调栈，栈顶为最大值
    再加一个辅助栈

    元素插入顺序：1   3   5   5   2   6   1   2

    （栈的元素最左边为栈底，最右边为栈顶，队列也一样）
    插入 1：
    queue = {1}
    stack = {1}
    插入 3
    queue = {3, 1}
    stack = {3}
    (
        这里为什么 stack 是 3？之前的 1 哪去了？
        因为 1 比 3 先插入，因此最先 poll() 的必定会是 1，而在 poll 3 之前，无论 1 是否存在，最大值都会是 3，因此 1 的存在无关紧要
        这里我们就可以知道，只要后面进来一个比之前存储元素都要大的，那么前面的小元素都无关紧要了，清空栈，只入栈这个最大元素
    )
    插入 5
    queue = {5, 3, 1}
    stack = {5}
    插入 5
    queue = {5，5, 3, 1}
    stack = {5，5}
    (
        插入跟栈顶元素一样大的值，这个就不必多说什么了吧，直接入栈即可
    )
    插入 2
    queue = {2, 5, 5, 3, 1}
    stack = {2, 5, 5}
    (
        这里为什么 2 又存在了？ 而且是在栈底？
        因为 5 比 2 先插入，因此最先 poll() 的一定会是 5，那么当 poll 5 的时候，前面的 1 和 3 也都已经 poll() 了
        那么队列剩下的就是 5 后面插入元素的最大值，即 2，因此我们需要留下 2
    )
    插入 6
    queue = {6, 2, 5, 5, 3, 1}
    stack = {6}
    插入 1
    queue = {1, 6, 2, 5, 5, 3, 1}
    stack = {1, 6}
    插入 2
    queue = {2, 1, 6, 2, 5, 5, 3, 1}
    stack = {2, 6}
    (
        这里为什么是 2 在底部？ 1 哪去了？
        6 比 后面的 1 和 2 都先插入，因此在 poll 6 之前， 6 是最大的
        而 1 比 2 先插入，因此在 poll 2 之前，前面的 1 是否存在无关紧要，当 poll 6 后，最大值就会是 6 后面插入元素的最大值。即 2
        这里我们就知道，我们当插入元素比 栈顶元素 小的时候，我们需要清空掉 栈内所有比 插入元素小的值，然后入栈
        （这里做法就是 面试题 03.05. 栈排序 的做法）
    )
    */
	
	
class MaxQueue {


    Queue<Integer> queue;
    Deque<Integer> stack;
    Deque<Integer> helper_stack;
    public MaxQueue() {
        queue = new LinkedList<>();
        stack = new LinkedList<>();
        helper_stack = new LinkedList<>();
    }
    
    public int max_value() {
        if(queue.isEmpty()){
            return -1;
        }
        return stack.peek();
    }
    
    public void push_back(int value) {
        queue.add(value);
        //栈为空，直接入栈
        if(stack.isEmpty()){
            stack.push(value);
        }else{
            if(stack.peek() < value){   //当栈顶元素比当前元素小，那么清空栈，然后将 value 入栈
                stack.clear();
                stack.push(value);
            }else if(stack.peek() == value){    //当栈顶元素等于当前元素，那么直接入栈
                stack.push(value);
            }else{
                while(!stack.isEmpty() && stack.peek() >= value){   //当栈顶元素大于当前元素，将大于等于 value 的值压入辅助栈
                    helper_stack.push(stack.pop());
                }
                //压入插入元素
                helper_stack.push(value);
                //清空 stack 
                stack.clear();
                //重新将辅助栈的值压回 stack
                while(!helper_stack.isEmpty()){
                    stack.push(helper_stack.pop());
                }
            }
        }
    }
    
    public int pop_front() {
        if(queue.isEmpty()){
            return -1;
        }
        int temp = queue.poll();
        if(stack.peek() == temp){
            stack.pop();
        }
        return temp;
    }
}