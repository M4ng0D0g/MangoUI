package com.myudog.myulib.client.api.camera;

import com.myudog.myulib.api.core.camera.CameraTrackingTarget;
import com.myudog.myulib.client.api.control.ClientControlManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * 負責處理鎖定目標的生命週期、遮擋檢測以及 UI 座標投影。
 */
public final class LockOnTargetTracker {
    private static int occlusionTime = 0;

    /** 🌟 檢查目前是否處於鎖定狀態 */
    public static boolean isLockedOn() {
        return ClientCameraManager.INSTANCE.getTrackingTarget() != null &&
                ClientControlManager.INSTANCE.isLockedOn();
    }

    /** 🌟 獲取當前鎖定的實體物件 */
    @Nullable
    public static Entity getCurrentTarget() {
        CameraTrackingTarget target = ClientCameraManager.INSTANCE.getTrackingTarget();
        if (target == null || target.getEntityId() == null) return null;

        Minecraft client = Minecraft.getInstance();
        return client.level != null ? client.level.getEntity(target.getEntityId()) : null;
    }

    /** * 🌟 計算目標實體在螢幕上的 2D 座標。
     * 用於在 GuiMixin 中繪製鎖定框。
     */
    public static Vec2 getLockedTargetScreenPos(float tickDelta) {
        Minecraft client = Minecraft.getInstance();
        Entity target = getCurrentTarget();

        if (target == null || client.level == null) {
            // 若無目標，回傳螢幕正中央
            return new Vec2(client.getWindow().getGuiScaledWidth() / 2f, client.getWindow().getGuiScaledHeight() / 2f);
        }

        // 取得實體中心點的世界座標
        Vec3 targetWorldPos = target.getPosition(tickDelta).add(0, target.getBbHeight() / 2.0, 0);

        // 🌟 核心：世界座標轉螢幕座標投影 (Project World to Screen)
        return projectWorldToScreen(targetWorldPos, tickDelta);
    }

    /** * 🌟 獲取虛擬準星 (滑鼠操作) 的座標。
     * 這直接對接 ClientControlManager 中累加的 dx/dy。
     */
    public static Vec2 getVirtualCrosshairPos() {
        Minecraft client = Minecraft.getInstance();
        // 這裡回傳由 ClientControlManager 管理的虛擬位置，
        // 基礎位置通常是螢幕中央，加上玩家移動滑鼠產生的偏移。
        float centerX = client.getWindow().getGuiScaledWidth() / 2f;
        float centerY = client.getWindow().getGuiScaledHeight() / 2f;

        // 假設 ClientControlManager 已經實作了 getVirtualOffset
        // 這裡暫時回傳中央，需視具體滑鼠攔截邏輯調整
        return new Vec2(centerX, centerY);
    }

    public static void incrementOcclusionTime() {
        occlusionTime++;
    }

    public static int getOcclusionTime() {
        return occlusionTime;
    }

    public static void resetOcclusionTime() {
        occlusionTime = 0;
    }

    public static void unlock() {
        ClientCameraManager.INSTANCE.setTrackingTarget(null);
        ClientControlManager.INSTANCE.setLockedOn(false);
        resetOcclusionTime();
    }

    /** * 輔助方法：將世界座標轉換為 GUI 螢幕縮放後的 2D 座標。
     */
    private static Vec2 projectWorldToScreen(Vec3 worldPos, float tickDelta) {
        Minecraft client = Minecraft.getInstance();
        Vec3 cameraPos = client.gameRenderer.getMainCamera().position();

        // 1. 計算相對向量
        Vec3 relative = worldPos.subtract(cameraPos);

        // 2. 利用相機的 Yaw/Pitch 進行逆矩陣變換 (此處為簡化邏輯)
        // 在正式開發中，應使用矩陣 (Matrix4f) 運算以獲得精確座標
        // 暫時回傳螢幕中心，待投影矩陣工具類別完成
        return new Vec2(client.getWindow().getGuiScaledWidth() / 2f, client.getWindow().getGuiScaledHeight() / 2f);
    }
}