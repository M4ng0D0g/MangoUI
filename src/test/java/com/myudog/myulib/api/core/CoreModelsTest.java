package com.myudog.myulib.api.core;
import org.junit.jupiter.api.Test;
import java.util.Arrays;
import java.util.List;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
final class CoreModelsTest {
    @Test
    void sizeSupportsArithmeticAndReportsHelpfulFailures() {
        int[] dimensions = {2, 3};
        Size size = new Size(dimensions);
        dimensions[0] = 99;
        assertEquals(2, size.getShape()[0], "Constructor should defensively copy the input array");
        assertEquals(2, size.getDimensionCount(), "Size should report the declared dimension count");
        assertEquals(List.of(2, 3), toList(size.getShape()), "Size should preserve the original shape");
        Size expanded = size.add(new Size(1, 2));
        assertEquals(List.of(3, 5), toList(expanded.getShape()), "Addition should be applied per dimension");
        Size reduced = expanded.substract(new Size(1, 1));
        assertEquals(List.of(2, 4), toList(reduced.getShape()), "Subtraction should be applied per dimension");
        Size doubled = reduced.multiply(2);
        assertEquals(List.of(4, 8), toList(doubled.getShape()), "Multiplication should scale each dimension");
        assertTrue(doubled.enclose(new Size(3, 7)), "A larger size should enclose a smaller one");
        assertFalse(doubled.enclose(new Size(5, 1)), "A smaller second dimension should break enclosure");
        IllegalArgumentException mismatch = assertThrows(
                IllegalArgumentException.class,
                () -> size.add(new Size(1, 2, 3)),
                "Adding different dimension counts should fail"
        );
        assertTrue(mismatch.getMessage().contains("維度數量 不匹配"),
                "Mismatch exception should explain the dimension-count problem");
        IllegalArgumentException negative = assertThrows(
                IllegalArgumentException.class,
                () -> new Size(1, 1).substract(new Size(2, 0)),
                "Subtracting past zero should fail"
        );
        assertTrue(negative.getMessage().contains("尺寸相減後不得為負數"),
                "Negative subtraction should mention the non-negative constraint");
        IllegalArgumentException badMultiplier = assertThrows(
                IllegalArgumentException.class,
                () -> size.multiply(-1),
                "Negative multipliers should fail"
        );
        assertEquals("乘數不得為負數！", badMultiplier.getMessage(),
                "Negative multiplier exception message should be exact");
    }
    @Test
    void gridRetainsTheDeclaredSize() {
        Size size = new Size(2, 2);
        Grid<String> grid = new Grid<>(size);
        assertEquals(size, grid.getSize(), "Grid should retain the size it was created with");
    }
    private static List<Integer> toList(int[] values) {
        return Arrays.stream(values).boxed().toList();
    }
}
