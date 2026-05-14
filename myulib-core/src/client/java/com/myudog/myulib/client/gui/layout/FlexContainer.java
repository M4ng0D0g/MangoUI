package com.myudog.myulib.client.gui.layout;

import com.myudog.myulib.client.gui.base.RenderContext;
import com.myudog.myulib.client.gui.base.Widget;

/**
 * FlexContainer
 * 
 * 提供自動佈局功能的容器。支援垂直 (Column) 或水平 (Row) 排列。
 */
public class FlexContainer extends Widget {
    public enum Direction { VERTICAL, HORIZONTAL }

    private Direction direction = Direction.VERTICAL;
    private int gap = 2;

    public FlexContainer(Direction direction) {
        this.direction = direction;
    }

    @Override
    protected void onRender(RenderContext ctx) {
        // 佈局通常在 render 前或 update 中完成，這裡我們在 render 時檢查並排版
        layout();
    }

    private void layout() {
        int currentX = x + paddingLeft;
        int currentY = y + paddingTop;

        for (Widget child : children) {
            child.setX(currentX);
            child.setY(currentY);

            if (direction == Direction.VERTICAL) {
                currentY += child.getHeight() + gap;
            } else {
                currentX += child.getWidth() + gap;
            }
        }
    }

    public FlexContainer setGap(int gap) {
        this.gap = gap;
        return this;
    }
}
