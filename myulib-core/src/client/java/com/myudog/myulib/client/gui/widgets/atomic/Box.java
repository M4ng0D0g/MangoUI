package com.myudog.myulib.client.gui.widgets.atomic;

import com.myudog.myulib.client.gui.base.RenderContext;
import com.myudog.myulib.client.gui.base.Widget;
import com.myudog.myulib.client.gui.widgets.base.NineSliceRenderer;
import net.minecraft.resources.Identifier;

/**
 * Box
 * 
 * 基礎容器組件，支援背景色或九宮格貼圖。
 */
public class Box extends Widget {
    private int backgroundColor = 0x00000000;
    private Identifier texture;
    private int cornerSize = 4;
    private int textureSize = 16;

    @Override
    protected void onRender(RenderContext ctx) {
        if (backgroundColor != 0) {
            ctx.fill(x, y, x + width, y + height, backgroundColor);
        }
        
        if (texture != null) {
            NineSliceRenderer.draw(ctx.graphics(), texture, x, y, width, height, cornerSize, textureSize, 0xFFFFFFFF);
        }
    }

    public Box setBackgroundColor(int backgroundColor) {
        this.backgroundColor = backgroundColor;
        return this;
    }

    public Box setTexture(Identifier texture, int cornerSize, int textureSize) {
        this.texture = texture;
        this.cornerSize = cornerSize;
        this.textureSize = textureSize;
        return this;
    }
}
