栈排序。 编写程序，对栈进行排序使最小元素位于栈顶。最多只能使用一个其他的临时栈存放数据，但不得将元素复制到别的数据结构（如数组）中。该栈支持如下操作：push、pop、peek 和 isEmpty。当栈为空时，peek 返回 -1。

示例1:

 输入：
["SortedStack", "push", "push", "peek", "pop", "peek"]
[[], [1], [2], [], [], []]
 输出：
[null,null,null,1,null,2]
示例2:

 输入： 
["SortedStack", "pop", "pop", "push", "pop", "isEmpty"]
[[], [], [], [1], [], []]
 输出：
[null,null,null,null,null,true]
说明:

栈中的元素数目在[0, 5000]范围内。

class SortedStack {

    /*
    栈排序：
    使用一个 stack 正常存放数据
    使用一个 helper_stack 作为辅助栈

    在 push 时将 stack 维护成单调递增栈，即最上面是最小值，时间复杂度 O(n)
    pop() 和 peek() 为 O(1)
    */
    Deque<Integer> stack;
    Deque<Integer> helper_stack;
    public SortedStack() {
        stack = new LinkedList<>();
        helper_stack = new LinkedList<>();
    }
    
    //2 3 4 1
    public void push(int val) {
        while(!stack.isEmpty() && stack.peek() < val){
            helper_stack.push(stack.pop());
        }
        stack.push(val);
        while(!helper_stack.isEmpty()){
            stack.push(helper_stack.pop());
        }
    }
    
    public void pop() {
        if(stack.isEmpty()){
            return;
        }
        stack.pop();
    }
    
    public int peek() {
        if(stack.isEmpty()){
            return -1;
        }
        return stack.peek();
    }
    
    public boolean isEmpty() {
        return stack.isEmpty();
    }
}
