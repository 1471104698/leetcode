合并 k 个排序链表，返回合并后的排序链表。请分析和描述算法的复杂度。

示例:

输入:
[
  1->4->5,
  1->3->4,
  2->6
]
输出: 1->1->2->3->4->4->5->6


class Solution {
    public ListNode mergeKLists(ListNode[] lists) {
        /*
        我们进行首尾合并，最终只剩下一条链表
        比如 0  1   2   3   4 
        0 和 4 合并存储到 0 中， 1 和 3 合并存储到 1 中， 2 不合并
        然后剩下 0  1   2
        然后 0 和 2 合并存储到 0 中，1 不合并
        然后剩下 0  1
        然后 0 和 1 合并存储到 0 中
        最终只剩下 0，然后我们将 lists[0] 直接返回

        有以下 6 条链表
        0   1   2   3   4   5
        0 和 5 合并到 0
        1 和 4 合并到 1
        2 和 3 合并到 2
        综上：边界条件为 i < len / 2
        经过 1 轮后，长度变成 3，即 len / 2 或 (len + 1) / 2

        有以下 5 条链表
        0   1   2   3   4
        0 和 4 合并到 0
        1 和 3 合并到 1
        到 2 不进行合并
        综上，：边界条件为 i < len / 2
        经过 1 轮后，长度变成 3，即 len / 2 + 1 或 (len + 1) / 2

        那么我们可以得知，for 循环边界条件为 i < len / 2，长度减半条件为 (len + 1) / 2
        */
        int len = lists.length;
        if(len == 0){
            return null;
        }
        //当长度大于 1 的时候，那么继续归并
        while(len > 1){
            for(int i = 0; i < len / 2; i++){
                lists[i] = merge(lists[i], lists[len - i - 1]);
            }
            len = (len + 1) / 2;
        }
        return lists[0];
    }
    //合并两条有序链表
    private ListNode merge(ListNode l1, ListNode l2){
        ListNode dummy = new ListNode(-1);
        ListNode pre = dummy;
        while(l1 != null && l2 != null){
            if(l1.val > l2.val){
                pre.next = l2;
                l2 = l2.next;
            }else{
                pre.next = l1;
                l1 = l1.next;
            }
            pre = pre.next;
        }
        pre.next = l1 == null ? l2 : l1;
        return dummy.next;
    }
}