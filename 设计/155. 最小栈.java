设计一个支持 push ，pop ，top 操作，并能在常数时间内检索到最小元素的栈。

push(x) —— 将元素 x 推入栈中。
pop() —— 删除栈顶的元素。
top() —— 获取栈顶元素。
getMin() —— 检索栈中的最小元素。
 

示例:

输入：
["MinStack","push","push","push","getMin","pop","top","getMin"]
[[],[-2],[0],[-3],[],[],[],[]]

输出：
[null,null,null,null,-3,null,0,-2]

解释：
MinStack minStack = new MinStack();
minStack.push(-2);
minStack.push(0);
minStack.push(-3);
minStack.getMin();   --> 返回 -3.
minStack.pop();
minStack.top();      --> 返回 0.
minStack.getMin();   --> 返回 -2.
 

提示：

pop、top 和 getMin 操作总是在 非空栈 上调用。


class MinStack {

    /*
    维护一个存储元素值的栈 和 一个单调辅助栈

    插入顺序：1 2 3 4 5 6， 1 是在栈底，6 是在栈顶，因此弹出顺序是 6 5 4 ...，1 是最慢弹出的，因此前面的值无关紧要，最小值都是 1，因此我们存储 1 即可
            helper_stack = {1}
    插入顺序：6 5 4 3 2 1， 1 是在栈顶，6 是在栈底，1 是最先弹出的，当 1 弹出后，最小值变成 2，因此我们需要这些值都进行存储
            helper_stack = {6, 5, 4, 3, 2, 1}

    插入顺序：2 3 1， 首先插入 2，然后 3 比 2 大，并且 3 会比 2 先弹出，因此无论 3 是否存在，最小值都是 2，因此忽略 3，
                    再插入 1，由于 1 比 2 小，并且在 1 弹出之前最小值是 1，因此需要添加 1
            helper_stack = {2, 1}
    */
    Deque<Integer> stack;
    Deque<Integer> helper_stack;

    public MinStack() {
        stack = new LinkedList<>();
        helper_stack = new LinkedList<>();
    }
    
    public void push(int x) {
        stack.push(x);
        if(helper_stack.isEmpty() || helper_stack.peek() >= x){
            helper_stack.push(x);
        }
    }
    
    public void pop() {
        int temp = stack.pop();
        if(temp == helper_stack.peek()){
            helper_stack.pop();
        }
    }
    
    public int top() {
        return stack.peek();
    }
    
    public int getMin() {
        return helper_stack.peek();
    }
}
