package com.myudog.myulib.api.core;

import java.util.Arrays;

@Deprecated
public class Index {

    private int[] shape;

    public Index(int... dimensions) {
        if (dimensions == null || dimensions.length == 0) {
            throw new IllegalArgumentException("維度數量 (T) 不能為空或 0！");
        }
        // 防禦性複製 (Defensive Copy)：防止外部陣列被修改，確保物件的絕對不可變性 (Immutable)
        this.shape = Arrays.copyOf(dimensions, dimensions.length);

    }

    private void assertValidShape(int... shape) {
        if (shape == null || shape.length == 0) {
            throw new IllegalArgumentException("維度數量 (T) 不能為空或 0！");
        }
        if (this.shape.length != shape.length) {
            throw new IllegalArgumentException("維度數量 不匹配！預期 " + this.shape.length + " 維，但提供了 " + shape.length + " 維。");
        }
    }

    public int[] getShape() {
        return Arrays.copyOf(shape, shape.length);
    }
    public int getDimensionCount() {
        return shape.length;
    }

    public void setShape(int... shape) {
        assertValidShape(shape);
        this.shape = Arrays.copyOf(shape, shape.length);
    }

    /**
     * 將目前索引與另一個索引相加 (模擬運算子重載的 + 號)
     * * @param other 另一個索引物件
     * @return 相加後產生的全新 Index 物件
     * @throws IllegalArgumentException 如果維度不一致，或相加後出現負數
     */
    public Index add(Index other) {
        assertValidShape(other.shape);

        int[] newShape = new int[this.shape.length];

        for (int i = 0; i < newShape.length; i++) {
            int result = this.shape[i] + other.shape[i];
            if (result < 0) {
                throw new IllegalArgumentException(String.format("索引相加後不得為負數！第 %d 維度相加結果為 %d", i, result));
            }
            newShape[i] = result;
        }

        return new Index(newShape);
    }

    /**
     * 將目前索引與另一個索引相減 (模擬運算子重載的 + 號)
     * * @param other 另一個索引物件
     * @return 相減後產生的全新 Index 物件
     * @throws IllegalArgumentException 如果維度不一致，或相減後出現負數
     */
    public Index subtract(Index other) {
        assertValidShape(other.shape);

        int[] newShape = new int[this.shape.length];

        for (int i = 0; i < newShape.length; i++) {
            int result = this.shape[i] - other.shape[i];
            if (result < 0) {
                throw new IllegalArgumentException(String.format("索引相減後不得為負數！第 %d 維度相減結果為 %d", i, result));
            }
            newShape[i] = result;
        }

        return new Index(newShape);
    }
}
