package io.github.notstirred.mawm.util;

public class LimitedFifoQueue<T> {
    private int tail = 0;
    private int head = 0;
    private int maxHead = 0;

    private final int max;
    private final SimpleArray<T> list;

    public LimitedFifoQueue(int _limit) {
        list = new SimpleArray<>(_limit);
        max = _limit-1;
    }

    public void push(T val) {
        list.set(head, val);
        incrementHead();
        maxHead = head;
        if(head == tail) // if they are equal head must have wrapped around fully, meaning tail should move forwards
            incrementTail();
    }

    public int getHead() {
        return head;
    }

    public boolean hasPrev() {
        return head != tail;
    }
    public boolean hasNext() { //Because maxHead can never be lower than head, and can wrap round to 0, this is the only case where there is no next
        return maxHead != head;
    }

    public T getPrev() {
        if(!hasPrev())
            throw new ArrayIndexOutOfBoundsException("Head moved before tail!");
        decrementHead();
        return list.get(head);
    }

    public T getNext() {
        T val = list.get(head);
        if(hasNext())
            incrementHead();
        else
            throw new ArrayIndexOutOfBoundsException("No value at index head");
        return val;
    }

    public void clear() {
        list.clear();
        head = 0;
        maxHead = 0;
        tail = 0;
    }

    private void incrementHead() {
        head++;
        head %= list.length;
    }

    private void decrementHead() {
        head--;
        if(head < 0)
            head += max + 1;
    }

    private void incrementTail() {
        tail++;
        tail %= max;
    }
}
