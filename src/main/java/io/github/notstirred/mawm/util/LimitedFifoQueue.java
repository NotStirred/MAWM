package io.github.notstirred.mawm.util;

import java.util.ArrayList;
import java.util.List;

public class LimitedFifoQueue<T> {
    private int tail = 0;
    private int head;
    private int maxHead;

    private final int limit;
    private final List<T> list;

    public LimitedFifoQueue(int _limit) {
        this(new ArrayList<>(_limit), _limit);
    }

    public LimitedFifoQueue(List<T> _list, int _limit) {
        list = _list;
        limit = _limit;
        head = list.size();
        maxHead = head;
    }

    public void push(T val) {
        list.add(head, val);
        incrementHead();
        maxHead = head;
        tail = Math.max(0, maxHead-limit);
    }

    public int getHead() {
        return head;
    }

    public boolean hasPrev() {
        return head > tail;
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
        head %= limit;
    }

    private void decrementHead() {
        head--;
    }
}
