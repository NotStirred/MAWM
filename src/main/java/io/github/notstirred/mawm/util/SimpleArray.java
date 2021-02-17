package io.github.notstirred.mawm.util;

import java.util.Arrays;

public class SimpleArray<T> {
    private final Object[] internalArray;
    public final int length;

    public SimpleArray(int _length) {
        internalArray = new Object [_length];
        length = _length;
    }

    T get(int idx) {
        //noinspection unchecked
        return (T) internalArray[idx];
    }

    void set(int idx, T t) {
        internalArray[idx] = t;
    }

    int size() {
        return length;
    }

    void clear() {
        Arrays.fill(internalArray, null);
    }

    @Override
    public String toString() {
        return Arrays.toString(internalArray);
    }
}
