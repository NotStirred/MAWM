package io.github.notstirred.mawm.util;

import java.util.ArrayList;
import java.util.List;

public class LimitedFifoQueue<T> {

    private int head;

    private final int limit;
    private final List<T> list;

    public LimitedFifoQueue(int _limit) {
        this(new ArrayList<>(_limit), _limit);
    }

    public LimitedFifoQueue(List<T> _list, int _limit) {
        list = _list;
        limit = _limit;
        head = list.size();
    }

    public void push(T val) {
        list.add(head, val);
        incrementHead();
    }

    public T pop() {
        decrementHead();
        return list.get(head);
    }
    public T peek() {
        return list.get(head);
    }

    public void clear() {
        list.clear();
        head = 0;
    }
    private void incrementHead() {
        head++;
        head %= limit;
    }

    private void decrementHead() {
        if(head > 0)
            head--;
    }
}
