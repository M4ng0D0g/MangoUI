//package com.myudog.myulib.client.api.render;
//
//import com.myudog.myulib.api.hologram.HologramDefinition;
//import com.myudog.myulib.api.hologram.HologramFeature;
//import com.myudog.myulib.api.hologram.HologramStyle;
//import com.myudog.myulib.client.api.hologram.HologramClientManager;
//import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
//import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
//import net.minecraft.client.render.*;
//import net.minecraft.client.util.math.MatrixStack;
//import net.minecraft.util.math.Vec3d;
//import net.minecraft.world.phys.AABB;
//import org.joml.Matrix4f;
//
//import java.util.List;
//
///**
// * 核心全息投影渲染器
// * 負責將伺服器同步的區域數據渲染為 3D 框線、面或角落特效
// */
//public class HologramRenderer {
//
//    public static void register() {
//        // 在所有實體渲染完成後進行渲染，確保全息投影具備正確的透明遮擋關係
//        WorldRenderEvents.AFTER_ENTITIES.register(HologramRenderer::onRender);
//    }
//
//    private static void onRender(WorldRenderContext context) {
//        List<HologramDefinition> holograms = HologramClientManager.INSTANCE.getActiveHolograms();
//        if (holograms.isEmpty()) return;
//
//        MatrixStack matrices = context.matrixStack();
//        Vec3d cameraPos = context.camera().getPos();
//        VertexConsumerProvider consumers = context.consumers();
//
//        matrices.push();
//        // 🌟 絕對座標轉換：將渲染系統的「相機相對座標」平移回「世界絕對座標」
//        matrices.translate(-cameraPos.x, -cameraPos.y, -cameraPos.z);
//        Matrix4f posMatrix = matrices.peek().getPositionMatrix();
//
//        for (HologramDefinition holo : holograms) {
//            renderSingle(matrices, posMatrix, consumers, holo);
//        }
//        matrices.pop();
//    }
//
//    private static void renderSingle(MatrixStack ms, Matrix4f mat, VertexConsumerProvider vcp, HologramDefinition holo) {
//        AABB b = holo.bounds();
//        HologramStyle style = holo.style();
//
//        // 解析 ARGB 顏色
//        int color = style.color();
//        float a = ((color >> 24) & 0xFF) / 255f;
//        float r = ((color >> 16) & 0xFF) / 255f;
//        float g = ((color >> 8) & 0xFF) / 255f;
//        float bCol = (color & 0xFF) / 255f;
//
//        // 1. 渲染角落 (Corners) - 模仿結構方塊風格
//        if (style.isEnabled(HologramFeature.CORNERS)) {
//            VertexConsumer lines = vcp.getBuffer(RenderLayer.getLines());
//            drawCorners(mat, lines, b, r, g, bCol, 1.0f); // 角落設為不透明以增強辨識度
//        }
//
//        // 2. 渲染完整框線 (Lines)
//        if (style.isEnabled(HologramFeature.LINES)) {
//            VertexConsumer lines = vcp.getBuffer(RenderLayer.getLines());
//            WorldRenderer.drawBox(ms, lines, b.minX, b.minY, b.minZ, b.maxX, b.maxY, b.maxZ, r, g, bCol, a);
//        }
//
//        // 3. 渲染半透明面 (Faces)
//        if (style.isEnabled(HologramFeature.FACES)) {
//            VertexConsumer translucent = vcp.getBuffer(RenderLayer.getTranslucent());
//            drawFaces(mat, translucent, b, r, g, bCol, a * 0.4f); // 面通常較淡
//        }
//    }
//
//    /**
//     * 繪製 8 個角落的 L 型結構線
//     */
//    private static void drawCorners(Matrix4f mat, VertexConsumer b, AABB box, float r, float g, float bl, float a) {
//        float l = 0.3f; // L 型線段長度 (方塊單位)
//
//        float minX = (float) box.minX;
//        float minY = (float) box.minY;
//        float minZ = (float) box.minZ;
//        float maxX = (float) box.maxX;
//        float maxY = (float) box.maxY;
//        float maxZ = (float) box.maxZ;
//
//        // 下層 4 個角落
//        drawCorner(mat, b, minX, minY, minZ, l, l, l, r, g, bl, a); // 0,0,0
//        drawCorner(mat, b, maxX, minY, minZ, -l, l, l, r, g, bl, a); // 1,0,0
//        drawCorner(mat, b, minX, minY, maxZ, l, l, -l, r, g, bl, a); // 0,0,1
//        drawCorner(mat, b, maxX, minY, maxZ, -l, l, -l, r, g, bl, a); // 1,0,1
//
//        // 上層 4 個角落
//        drawCorner(mat, b, minX, maxY, minZ, l, -l, l, r, g, bl, a); // 0,1,0
//        drawCorner(mat, b, maxX, maxY, minZ, -l, -l, l, r, g, bl, a); // 1,1,0
//        drawCorner(mat, b, minX, maxY, maxZ, l, -l, -l, r, g, bl, a); // 0,1,1
//        drawCorner(mat, b, maxX, maxY, maxZ, -l, -l, -l, r, g, bl, a); // 1,1,1
//    }
//
//    private static void drawCorner(Matrix4f mat, VertexConsumer b, float x, float y, float z, float dx, float dy, float dz, float r, float g, float bl, float a) {
//        // X 軸向線段
//        line(mat, b, x, y, z, x + dx, y, z, r, g, bl, a);
//        // Y 軸向線段
//        line(mat, b, x, y, z, x, y + dy, z, r, g, bl, a);
//        // Z 軸向線段
//        line(mat, b, x, y, z, x, y, z + dz, r, g, bl, a);
//    }
//
//    private static void line(Matrix4f mat, VertexConsumer b, float x1, float y1, float z1, float x2, float y2, float z2, float r, float g, float bl, float a) {
//        b.vertex(mat, x1, y1, z1).color(r, g, bl, a).normal(0, 0, 0).next();
//        b.vertex(mat, x2, y2, z2).color(r, g, bl, a).normal(0, 0, 0).next();
//    }
//
//    /**
//     * 繪製 AABB 的六個半透明面
//     */
//    private static void drawFaces(Matrix4f mat, VertexConsumer b, AABB box, float r, float g, float bl, float a) {
//        float x1 = (float) box.minX;
//        float y1 = (float) box.minY;
//        float z1 = (float) box.minZ;
//        float x2 = (float) box.maxX;
//        float y2 = (float) box.maxY;
//        float z2 = (float) box.maxZ;
//
//        // 底面 (Bottom)
//        b.vertex(mat, x1, y1, z1).color(r, g, bl, a).next();
//        b.vertex(mat, x2, y1, z1).color(r, g, bl, a).next();
//        b.vertex(mat, x2, y1, z2).color(r, g, bl, a).next();
//        b.vertex(mat, x1, y1, z2).color(r, g, bl, a).next();
//
//        // 頂面 (Top)
//        b.vertex(mat, x1, y2, z1).color(r, g, bl, a).next();
//        b.vertex(mat, x1, y2, z2).color(r, g, bl, a).next();
//        b.vertex(mat, x2, y2, z2).color(r, g, bl, a).next();
//        b.vertex(mat, x2, y2, z1).color(r, g, bl, a).next();
//
//        // 北面 (North, -Z)
//        b.vertex(mat, x1, y1, z1).color(r, g, bl, a).next();
//        b.vertex(mat, x1, y2, z1).color(r, g, bl, a).next();
//        b.vertex(mat, x2, y2, z1).color(r, g, bl, a).next();
//        b.vertex(mat, x2, y1, z1).color(r, g, bl, a).next();
//
//        // 南面 (South, +Z)
//        b.vertex(mat, x1, y1, z2).color(r, g, bl, a).next();
//        b.vertex(mat, x2, y1, z2).color(r, g, bl, a).next();
//        b.vertex(mat, x2, y2, z2).color(r, g, bl, a).next();
//        b.vertex(mat, x1, y2, z2).color(r, g, bl, a).next();
//
//        // 西面 (West, -X)
//        b.vertex(mat, x1, y1, z1).color(r, g, bl, a).next();
//        b.vertex(mat, x1, y1, z2).color(r, g, bl, a).next();
//        b.vertex(mat, x1, y2, z2).color(r, g, bl, a).next();
//        b.vertex(mat, x1, y2, z1).color(r, g, bl, a).next();
//
//        // 東面 (East, +X)
//        b.vertex(mat, x2, y1, z1).color(r, g, bl, a).next();
//        b.vertex(mat, x2, y2, z1).color(r, g, bl, a).next();
//        b.vertex(mat, x2, y2, z2).color(r, g, bl, a).next();
//        b.vertex(mat, x2, y1, z2).color(r, g, bl, a).next();
//    }
//}