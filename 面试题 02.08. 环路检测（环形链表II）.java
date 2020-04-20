给定一个有环链表，实现一个算法返回环路的开头节点。
有环链表的定义：在链表中某个节点的next元素指向在它前面出现过的节点，则表明该链表存在环路。


示例 1：

输入：head = [3,2,0,-4], pos = 1
输出：tail connects to node index 1
解释：链表中有一个环，其尾部连接到第二个节点。

示例 2：

输入：head = [1,2], pos = 0
输出：tail connects to node index 0
解释：链表中有一个环，其尾部连接到第一个节点。

示例 3：

输入：head = [1], pos = -1
输出：no cycle
解释：链表中没有环。

/*
先快慢指针走，第一次相遇

	x				y		O(slow 和 fast 相遇点)
----------------------------
			|				|
			|				|
		  z |				|	z
			|				|
			----------------
					z
（上面三个 z 都是属于 z 段）
我们可以看到，如果成环，那么 slow 和 fast 必定会在环中某点相遇，如图上 O 点
而这时，假设 slow 走过的距离是 x + y，那么 fast 走过的距离为 x + y + z + y
由于 fast 走的速度是 slow 的两倍，那么距离也是 slow 的两倍
即 2 * (x + y) = x + y + z + y
可以得到 x = z

由上图得 slow 再走 z 就能到达环入口，而这个 z 刚好就是 x
那么我们只需要让 fast 重新从 head 开始走，然后 slow 从 0 点开始走
由于 x = z，所以最终再次相遇是在 环形入口
				
*/
public class Solution {
    public ListNode detectCycle(ListNode head) {
        ListNode fast = head;
        ListNode slow = head;
        do{
            if(fast == null || fast.next == null){
                return null;
            }
            fast = fast.next.next;
            slow = slow.next;
        }while (slow != fast);
        fast = head;
        while(slow != fast){
            fast = fast.next;
            slow = slow.next;
        }
        return slow;
    }
}