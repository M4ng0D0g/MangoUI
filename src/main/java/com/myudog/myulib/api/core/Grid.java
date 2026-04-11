package com.myudog.myulib.api.core;

import java.util.ArrayList;
import java.util.Collections;

public class Grid<T> {

    private final ArrayList<T> array;
    private final Size size;
    private final int stride;

    public Grid(Size size) {
        assert size != null : "Size cannot be null";
        assert size.getDimensionCount() == 2 :  "Dimension count must be 2";

        int[] shape = size.getShape();
        this.array = new ArrayList<>(shape[0] * shape[1]);
        this.size = size;
        this.stride = shape[1];
    }

    public Size getSize() { return size; }

    public T get(int... index) {
        assert index != null : "Index cannot be null";
        assert index.length == 2 :  "Index count must be 2";
        assert this.size.enclose(index) : "Index out of bounds: " + index[0] + ", " + index[1];

        return array.get(index[0] * stride + index[1]);
    }

    public void set(int[] index, T value) {
        assert index != null : "Index cannot be null";
        assert index.length == 2 :  "Index count must be 2";
        assert this.size.enclose(index) : "Index out of bounds: " + index[0] + ", " + index[1];

        array.set(index[0] * stride + index[1], value);
    }

    public void clear() {
        Collections.fill(this.array, null);
    }
}
