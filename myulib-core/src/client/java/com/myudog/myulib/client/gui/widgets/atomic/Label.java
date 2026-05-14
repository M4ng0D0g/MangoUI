package com.myudog.myulib.client.gui.widgets.atomic;

import com.myudog.myulib.client.gui.base.RenderContext;
import com.myudog.myulib.client.gui.base.Widget;
import net.minecraft.network.chat.Component;

/**
 * Label
 * 
 * 文本組件。
 */
public class Label extends Widget {
    private Component text = Component.empty();
    private int color = 0xFFFFFFFF;
    private boolean shadow = true;
    private boolean centered = false;

    @Override
    protected void onRender(RenderContext ctx) {
        int drawX = x;
        if (centered) {
            drawX = x + (width - ctx.font().width(text)) / 2;
        }
        ctx.graphics().text(ctx.font(), text, drawX, y, color, shadow);
    }

    public Label setText(Component text) {
        this.text = text;
        return this;
    }

    public Label setColor(int color) {
        this.color = color;
        return this;
    }

    public Label setShadow(boolean shadow) {
        this.shadow = shadow;
        return this;
    }

    public Label setCentered(boolean centered) {
        this.centered = centered;
        return this;
    }

    public Label centered(boolean centered) {
        return setCentered(centered);
    }
}
