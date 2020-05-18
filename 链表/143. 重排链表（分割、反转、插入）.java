给定一个单链表 L：L0→L1→…→Ln-1→Ln ，
将其重新排列后变为： L0→Ln→L1→Ln-1→L2→Ln-2→…

你不能只是单纯的改变节点内部的值，而是需要实际的进行节点交换。

示例 1:

给定链表 1->2->3->4, 重新排列为 1->4->2->3.
示例 2:

给定链表 1->2->3->4->5, 重新排列为 1->5->2->4->3.


class Solution {
    public void reorderList(ListNode head) {

        /*
        1、快慢指针分出前后段
        2、反转后段链表
        3、将后端链表插入前段链表中

        1->2->3->4->5
        分割：1->2->3 和 4->5
        反转：1->2->3 和 5->4
        插入：1->5->2->4->3

        如果总长度为奇数，那么将多出的中间节点算到前半段中
        */
        if(head == null){
            return;
        }
        //1、快慢指针分割链表
        ListNode fast = head;
        ListNode slow = head;

        while(fast != null && fast.next != null){
            slow = slow.next;
            fast = fast.next.next;
        }
        fast = slow.next;
        slow.next = null;
        //2、反转后半段链表
        ListNode newHead = reverseNode(fast);

        //3、将后半段插入到前半段中
        while(newHead != null){
            ListNode temp = newHead.next;
            newHead.next = head.next;
            head.next = newHead;
            head = newHead.next;
            newHead = temp;
        }
    }
    private ListNode reverseNode(ListNode head){
        if(head == null || head.next == null){
            return head;
        }
        ListNode node = reverseNode(head.next);
        head.next.next = head;
        head.next = null;
        return node;
    }
}