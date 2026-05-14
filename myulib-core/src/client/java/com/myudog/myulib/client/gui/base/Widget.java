package com.myudog.myulib.client.gui.base;

import java.util.ArrayList;
import java.util.List;

/**
 * Widget
 * 
 * 所有 UI 組件的基類。採用聲明式與組合模式。
 */
public abstract class Widget {
    protected int x, y;
    protected int width, height;
    protected int paddingLeft, paddingRight, paddingTop, paddingBottom;
    protected boolean visible = true;
    
    protected final List<Widget> children = new ArrayList<>();
    protected Widget parent;

    public void render(RenderContext ctx) {
        if (!visible) return;
        
        onRender(ctx);
        
        for (Widget child : children) {
            child.render(ctx);
        }
    }

    protected abstract void onRender(RenderContext ctx);

    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!visible) return false;

        // 從最上層（最後加入的子組件）開始檢查
        for (int i = children.size() - 1; i >= 0; i--) {
            if (children.get(i).mouseClicked(mouseX, mouseY, button)) {
                return true;
            }
        }

        if (isHovered(mouseX, mouseY)) {
            return onMouseClick(mouseX, mouseY, button);
        }
        return false;
    }

    protected boolean onMouseClick(double mouseX, double mouseY, int button) {
        return false;
    }

    public boolean isHovered(double mouseX, double mouseY) {
        return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
    }

    public void update(long deltaMillis) {
        onUpdate(deltaMillis);
        for (Widget child : children) {
            child.update(deltaMillis);
        }
    }

    protected void onUpdate(long deltaMillis) {}

    public void addChild(Widget child) {
        child.parent = this;
        children.add(child);
        onChildrenChanged();
    }

    protected void onChildrenChanged() {}

    // Getters and Setters
    public int getX() { return x; }
    @SuppressWarnings("unchecked")
    public <T extends Widget> T setX(int x) { this.x = x; return (T) this; }
    public int getY() { return y; }
    @SuppressWarnings("unchecked")
    public <T extends Widget> T setY(int y) { this.y = y; return (T) this; }
    public int getWidth() { return width; }
    @SuppressWarnings("unchecked")
    public <T extends Widget> T setWidth(int width) { this.width = width; return (T) this; }
    public int getHeight() { return height; }
    @SuppressWarnings("unchecked")
    public <T extends Widget> T setHeight(int height) { this.height = height; return (T) this; }
    public boolean isVisible() { return visible; }
    @SuppressWarnings("unchecked")
    public <T extends Widget> T setVisible(boolean visible) { this.visible = visible; return (T) this; }
}
