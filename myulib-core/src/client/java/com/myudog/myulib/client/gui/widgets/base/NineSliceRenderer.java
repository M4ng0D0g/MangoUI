package com.myudog.myulib.client.gui.widgets.base;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;

/**
 * NineSliceRenderer
 * 
 * 提供高品質的九宮格（Nine-Slice）貼圖渲染功能。
 * 將貼圖分為 9 個區域：4 個角落（固定大小）、4 條邊（拉伸）、1 個中心（拉伸）。
 */
public final class NineSliceRenderer {

    private NineSliceRenderer() {}

    /**
     * 繪製九宮格貼圖。
     *
     * @param graphics       渲染器
     * @param texture        貼圖標記符
     * @param x              目標 X 座標
     * @param y              目標 Y 座標
     * @param width          目標寬度
     * @param height         目標高度
     * @param cornerSize     角落大小（貼圖像素）
     * @param textureSize    貼圖總大小（寬高相等時使用，否則需拆分）
     * @param color          疊加顏色 (ARGB)
     */
    public static void draw(GuiGraphicsExtractor graphics, Identifier texture, int x, int y, int width, int height, int cornerSize, int textureSize, int color) {
        draw(graphics, texture, x, y, width, height, cornerSize, cornerSize, textureSize, textureSize, color);
    }

    public static void draw(GuiGraphicsExtractor graphics, Identifier texture, int x, int y, int width, int height, int cornerWidth, int cornerHeight, int texWidth, int texHeight, int color) {
        // 角落大小
        int cw = cornerWidth;
        int ch = cornerHeight;
        
        // 中間區域大小 (貼圖)
        int midTexW = texWidth - 2 * cw;
        int midTexH = texHeight - 2 * ch;
        
        // 中間區域大小 (目標)
        int midW = width - 2 * cw;
        int midH = height - 2 * ch;

        // --- 角落 ---
        // Top-Left
        graphics.blit(RenderPipelines.GUI_TEXTURED, texture, x, y, 0, 0, cw, ch, texWidth, texHeight, color);
        // Top-Right
        graphics.blit(RenderPipelines.GUI_TEXTURED, texture, x + width - cw, y, texWidth - cw, 0, cw, ch, texWidth, texHeight, color);
        // Bottom-Left
        graphics.blit(RenderPipelines.GUI_TEXTURED, texture, x, y + height - ch, 0, texHeight - ch, cw, ch, texWidth, texHeight, color);
        // Bottom-Right
        graphics.blit(RenderPipelines.GUI_TEXTURED, texture, x + width - cw, y + height - ch, texWidth - cw, texHeight - ch, cw, ch, texWidth, texHeight, color);

        // --- 邊緣 (平鋪或拉伸，此處使用拉伸) ---
        if (midW > 0) {
            // Top
            graphics.blit(RenderPipelines.GUI_TEXTURED, texture, x + cw, y, midW, ch, cw, 0, midTexW, ch, texWidth, texHeight, color);
            // Bottom
            graphics.blit(RenderPipelines.GUI_TEXTURED, texture, x + cw, y + height - ch, midW, ch, cw, texHeight - ch, midTexW, ch, texWidth, texHeight, color);
        }
        
        if (midH > 0) {
            // Left
            graphics.blit(RenderPipelines.GUI_TEXTURED, texture, x, y + ch, cw, midH, 0, ch, cw, midTexH, texWidth, texHeight, color);
            // Right
            graphics.blit(RenderPipelines.GUI_TEXTURED, texture, x + width - cw, y + ch, cw, midH, texWidth - cw, ch, cw, midTexH, texWidth, texHeight, color);
        }

        // --- 中心 ---
        if (midW > 0 && midH > 0) {
            graphics.blit(RenderPipelines.GUI_TEXTURED, texture, x + cw, y + ch, midW, midH, cw, ch, midTexW, midTexH, texWidth, texHeight, color);
        }
    }
}
