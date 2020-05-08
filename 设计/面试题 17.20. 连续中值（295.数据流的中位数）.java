
随机产生数字并传递给一个方法。你能否完成这个方法，在每次产生新值时，寻找当前所有值的中间值（中位数）并保存。

中位数是有序列表中间的数。如果列表长度是偶数，中位数则是中间两个数的平均值。

例如，

[2,3,4] 的中位数是 3

[2,3] 的中位数是 (2 + 3) / 2 = 2.5

设计一个支持以下两种操作的数据结构：

void addNum(int num) - 从数据流中添加一个整数到数据结构中。
double findMedian() - 返回目前所有元素的中位数。
示例：

addNum(1)
addNum(2)
findMedian() -> 1.5
addNum(3) 
findMedian() -> 2

    /*  
        中位数是将数组平分为左右两半
        并且左边的数必定小于等于右边的数
        即左数组的最大值 小于等于 右数组的最小值

        由于我们关心的只是中位数，并不关心其他的元素，即无需对其他元素进行排序等

        比如某个有序数组    {1，2，3，4，5，6，7}
        我们可以分为两部分 {1，2，3，4} 和 {5，6，7}

        比如某个有序数组    {1，2，3，4，5，6，7，8}
        我们可以分为两部分 {1，2，3，4} 和 {5，6，7，8}

        我们可以看出
        当元素个数为奇数个时，中位数为前半段的最大值
        当元素个数为偶数个时，中位数为前半段的最大值 和 后半段的最小值之和 * 0.5

        因此，我们只需要最值，而能够很快得到最值的数据结构就是：堆

        大顶堆存放前半段小值，堆顶为小值的最大值
        小顶堆存放后半段大值，堆顶为大值的最小值
        两个堆的元素个数差不超过 1，
        即 大顶堆元素个数 - 小顶堆元素个数 <= 1， 即始终保持左数组 元素个数 大于等于 右数组元素个数 且不超过 1

        当元素个数为 偶数 时，返回 两个堆顶元素之和 / 2，当元素个数为 奇数 时，返回大顶堆堆顶元素
    */
	
class MedianFinder {

    PriorityQueue<Integer> minHeap;
    PriorityQueue<Integer> maxHeap;
    public MedianFinder() {
        minHeap = new PriorityQueue<>();
        maxHeap = new PriorityQueue<>(Collections.reverseOrder());
    }
    
    public void addNum(int num) {
        if(maxHeap.size() == 0){
            maxHeap.add(num);
        }else if(maxHeap.size() == minHeap.size()){
            /*
            两个堆元素个数相同：
            什么时候我们可以直接添加到 maxHeap？
            当
            num >= maxHeap.peek() && num <= minHeap.peek()
            或
            num <= maxHeap.peek() && num <= minHeap.peek()

            综上，只要 num <= minHeap.peek() 就可以直接添加到 maxHeap

            而如果 num > minHeap.peek()，那么我们需要挤出 minHeap 的最小值，再将 num 添加到 minHeap
			(
			想想这是为什么？ 
			提示：
			插入顺序 ：1 2 3，此时 maxHeap = 1, minHeap = 2，num = 3  插入后 👉 maxHeap = 1 2, minHeap = 3
			插入顺序 ：3 2 1，此时 maxHeap = 2, maxHeap = 3，num = 1  插入后 👉 maxHeap = 1 2, minHeap = 3
			插入顺序： 1 3 2，此时 maxHeap = 1, maxHeap = 3，num = 2  插入后 👉 maxHeap = 1 2, minHeap = 3
			)
            */
            if(minHeap.peek() < num){
                maxHeap.add(minHeap.poll());
                minHeap.add(num);
            }else{
                maxHeap.add(num);
            }
        }else { 
            /*
            maxHeap 元素个数 比 minHeap 元素个数 多 1
            什么时候我们可以直接添加到 minHeap？
            当 num >= maxHeap.peek() 时就可以直接添加到 minHeap

            如果 num < maxHeap.peek()，那么我们就需要挤出 maxHeap 的最大值，再将 num 添加到 maxHeap
            */
            if(maxHeap.peek() > num){
                minHeap.add(maxHeap.poll());
                maxHeap.add(num);
            }else{
                minHeap.add(num);
            }
        }
    }
    
    public double findMedian() {
        int sum = minHeap.size() + maxHeap.size();
        if((sum & 1) == 0){
            return (minHeap.peek() + maxHeap.peek()) * 0.5;
        }else{
            return maxHeap.peek();
        }
    }
}