给你一个链表，每 k 个节点一组进行翻转，请你返回翻转后的链表。

k 是一个正整数，它的值小于或等于链表的长度。

如果节点总数不是 k 的整数倍，那么请将最后剩余的节点保持原有顺序。

 

示例：

给你这个链表：1->2->3->4->5

当 k = 2 时，应当返回: 2->1->4->3->5

当 k = 3 时，应当返回: 3->2->1->4->5

 

说明：

你的算法只能使用常数的额外空间。
你不能只是单纯的改变节点内部的值，而是需要实际进行节点交换。

/**
 * Definition for singly-linked list.
 * public class ListNode {
 *     int val;
 *     ListNode next;
 *     ListNode(int x) { val = x; }
 * }
 */
class Solution {
    public ListNode reverseKGroup(ListNode head, int k) {
        /*  
            我们一段一段递归翻转
            比如链表 1->2->3->4->5, k = 2
            我们先找到 1->2，然后将 1->2 进行翻转，然后递归翻转 3->4->5

            我们将指针 a 指向要翻转链表的头节点，将 b 指向要翻转的链表的尾节点
            然后将链表段 [a, b] 进行翻转
        */
        if(head == null || head.next == null){
            return head;
        }

        ListNode a = head;
        ListNode b = head;
        for(int i = 0; i < k - 1; i++){
            //如果中途节点为 null，表示不足 k 个，那么直接按原顺序返回
            if(b.next == null){
                return head;
            }
            b = b.next;
        }

        /*
        next 是要翻转链表段的下一个节点
        比如链表 1->2->3->4->5, k = 2
        那么要翻转的就是 1 -> 2, 而这个 next 就是 3
        */
        ListNode next = b.next;
    
        reverseNode(a, next);
        //经过翻转， b 变成头节点， a 变为尾节点，因此 直接使用 a 连接后面的段即可
        a.next = reverseKGroup(next, k);
        return b;
    }

    //翻转链表模板
    private ListNode reverseNode(ListNode head, ListNode tail){
        if(head == tail || head.next == tail){
            return head;
        }
        ListNode node = reverseNode(head.next, tail);
        head.next.next = head;
        head.next = null;
        return node;
    }
}