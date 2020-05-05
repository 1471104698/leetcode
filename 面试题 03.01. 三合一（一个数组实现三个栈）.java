三合一。描述如何只用一个数组来实现三个栈。

你应该实现push(stackNum, value)、pop(stackNum)、isEmpty(stackNum)、peek(stackNum)方法。stackNum表示栈下标，value表示压入的值。

构造函数会传入一个stackSize参数，代表每个栈的大小。

示例1:

 输入：
["TripleInOne", "push", "push", "pop", "pop", "pop", "isEmpty"]
[[1], [0, 1], [0, 2], [0], [0], [0], [0]]
 输出：
[null, null, null, 1, -1, -1, true]
说明：当栈为空时`pop, peek`返回-1，当栈满时`push`不压入元素。
示例2:

 输入：
["TripleInOne", "push", "push", "push", "pop", "pop", "pop", "peek"]
[[2], [0, 1], [0, 2], [0, 3], [0], [0], [0], [0]]
 输出：
[null, null, null, null, 2, 1, -1, -1]

/*
思路：
用一个数组表示 3 个栈，每个栈的大小为 stackSize，即数组大小为 stackSize * 3

我们将 下标 0、1，2 作为 一、二、三 号栈的起始下标，即当前可插入的下标，然后某个栈每插入一个元素，那么就将可插入下标 + 3，这样 连续的 3 个下标 就是被 三个栈 等分
*/
class TripleInOne {
    /*
    栈空间分配
    一号栈：0， 0 + 3， 3 + 3 ... stackSize - 3
    二号栈：1， 1 + 3， 4 + 3 ... stackSize - 2
    三号栈：2， 2 + 3， 5 + 3 ... stackSize - 1
    */
    int[] arr;
    int stackSize;
    int cur_num = 0;
    //记录 3 个栈每个栈可以插入的下标，本质上使用 3 个指针，这里直接简化使用一个大小为 3 的数组
    int[] indexs;
    public TripleInOne(int stackSize) {
        arr = new int[stackSize * 3];
        this.stackSize = stackSize;
        indexs = new int[]{0, 1, 2};
    }
    //stackNum 表示的是 3 个栈中某个选择某个栈的下标
    public void push(int stackNum, int value) {
        //当前栈的可插入下标超过了界限，那么表示已经满了，不能再进行插入了
        if(indexs[stackNum] >= stackSize * 3){
            return;
        }
        arr[indexs[stackNum]] = value;
        indexs[stackNum] += 3;
    }
    
    public int pop(int stackNum) {
        if(isEmpty(stackNum)){
            return -1;
        }
        indexs[stackNum] -= 3;
        return arr[indexs[stackNum]];
    }
    
    public int peek(int stackNum) {
        if(isEmpty(stackNum)){
            return -1;
        }
        return arr[indexs[stackNum] - 3];
    }
    
    public boolean isEmpty(int stackNum) {
        //当前栈的可插入下标 < 3，即为 0、1、2 时，表示还没有元素入栈，即为空
        return indexs[stackNum] < 3;
    }
}
