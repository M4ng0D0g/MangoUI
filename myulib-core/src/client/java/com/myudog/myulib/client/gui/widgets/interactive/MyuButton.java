package com.myudog.myulib.client.gui.widgets.interactive;

import com.myudog.myulib.client.gui.base.RenderContext;
import com.myudog.myulib.client.gui.widgets.atomic.Box;
import com.myudog.myulib.client.gui.widgets.atomic.Label;
import net.minecraft.network.chat.Component;

/**
 * MyuButton
 * 
 * 具備懸停效果與點擊回調的按鈕。
 */
public class MyuButton extends Box {
    private final Label label = new Label();
    private Runnable onClick;
    
    public MyuButton(Component text, Runnable onClick) {
        this.onClick = onClick;
        this.label.setText(text);
        this.label.setCentered(true);
        this.addChild(label);
        
        // 預設樣式
        this.setBackgroundColor(0xFF444444);
        this.setWidth(100);
        this.setHeight(20);
    }

    @Override
    protected void onRender(RenderContext ctx) {
        // 簡單的懸停變色邏輯
        if (isHovered(ctx.mouseX(), ctx.mouseY())) {
            this.setBackgroundColor(0xFF666666);
        } else {
            this.setBackgroundColor(0xFF444444);
        }
        
        // 更新 Label 位置使其置中
        label.setX(x);
        label.setY(y + (height - 8) / 2); // 8 是文字大概高度
        label.setWidth(width);
        label.setHeight(height);
        
        super.onRender(ctx);
    }

    @Override
    protected boolean onMouseClick(double mouseX, double mouseY, int button) {
        if (onClick != null && button == 0) {
            onClick.run();
            return true;
        }
        return false;
    }
}
