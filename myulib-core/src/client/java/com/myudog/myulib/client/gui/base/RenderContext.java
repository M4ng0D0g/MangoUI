package com.myudog.myulib.client.gui.base;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;

/**
 * RenderContext
 * 
 * 封裝了 GuiGraphicsExtractor，並提供座標系轉換與簡化的繪製介面。
 */
public record RenderContext(GuiGraphicsExtractor graphics, Font font, int mouseX, int mouseY, float partialTick) {
    
    public void fill(int x1, int y1, int x2, int y2, int color) {
        graphics.fill(x1, y1, x2, y2, color);
    }
    
    public void nextStratum() {
        graphics.nextStratum();
    }
}
