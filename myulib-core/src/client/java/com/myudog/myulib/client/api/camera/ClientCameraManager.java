package com.myudog.myulib.client.api.camera;

import com.myudog.myulib.api.core.animation.Easing;
import com.myudog.myulib.api.core.camera.CameraActionPayload;
import com.myudog.myulib.api.core.camera.CameraModifier;
import com.myudog.myulib.api.core.camera.CameraTrackingTarget;
import com.myudog.myulib.api.core.camera.CameraTransform;
import com.myudog.myulib.api.core.camera.CameraPerspective;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.phys.Vec3;

import java.util.Iterator;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 客戶端相機總管。
 * 負責管理相機修改器 (Modifiers)、追蹤目標以及視角狀態控制。
 */
public final class ClientCameraManager {

    public static final ClientCameraManager INSTANCE = new ClientCameraManager();

    // 儲存進行中的相機效果（如震動、補間位移）
    private final CopyOnWriteArrayList<CameraModifier> modifiers = new CopyOnWriteArrayList<>();

    // 當前相機追蹤的目標資料
    private CameraTrackingTarget trackingTarget = null;

    // 當前視角狀態與 F5 權限
    private CameraPerspective currentPerspective = CameraPerspective.FREE;
    private boolean allowF5 = true;

    // 特殊模式：偏移跟隨
    private boolean controlOffsetMode = false;

    private ClientCameraManager() {}

    // --- 狀態存取 (Getters & Setters) ---

    public CameraTrackingTarget getTrackingTarget() { return trackingTarget; }
    public void setTrackingTarget(CameraTrackingTarget target) { this.trackingTarget = target; }

    public CameraPerspective getCurrentPerspective() { return currentPerspective; }
    public boolean canUseF5() { return allowF5; }

    public void setPerspectiveState(CameraPerspective perspective, boolean allowF5) {
        this.currentPerspective = perspective;
        this.allowF5 = allowF5;
    }

    public boolean isControlOffsetMode() { return controlOffsetMode; }
    public void setControlOffsetMode(boolean mode) { this.controlOffsetMode = mode; }

    // --- 核心功能方法 ---

    public void addModifier(CameraModifier modifier) {
        if (modifier != null) modifiers.add(modifier);
    }

    public void clearModifiers() {
        modifiers.clear();
    }

    /** 觸發相機震動效果 */
    public void shake(float intensity, long durationMillis) {
        addModifier(new ShakeModifier(intensity, durationMillis));
    }

    /** 觸發攝影機移動至目標 */
    public void moveTo(Vec3 destination, long durationMillis, Easing easing) {
        if (destination == null) return;
        // 使用目前相機位置作為起點
        Vec3 start = Minecraft.getInstance().gameRenderer.getMainCamera().position();
        addModifier(new PathAnimationModifier(start, CameraTrackingTarget.of(destination), durationMillis, easing));
    }

    // --- 網路封包處理 ---

    /** 處理來自伺服器的相機動作封包 */
    public void applyPayload(CameraActionPayload payload) {
        if (payload == null) return;

        switch (payload.action()) {
            case MOVE_TO -> {
                CameraTrackingTarget target = null;
                if (payload.targetStaticPos() != null) {
                    target = CameraTrackingTarget.of(payload.targetStaticPos());
                } else if (payload.targetEntityId() != null) {
                    target = CameraTrackingTarget.ofEntityId(payload.targetEntityId());
                }

                if (target != null) {
                    target = target.withOffset(payload.offset());
                    if (payload.lookAtStaticPos() != null) {
                        target = target.lookAt(payload.lookAtStaticPos());
                    } else if (payload.lookAtEntityId() != null) {
                        target = target.lookAtEntityId(payload.lookAtEntityId());
                    }
                    this.trackingTarget = target;
                    this.setPerspectiveState(CameraPerspective.CUSTOM_OFFSET, false);
                }
            }
            case SHAKE -> shake(payload.intensity(), payload.durationMillis());
            case RESET -> {
                this.trackingTarget = null;
                this.setPerspectiveState(CameraPerspective.FREE, true);
                clearModifiers();
            }
        }
    }

    // --- 渲染層每幀呼叫邏輯 ---

    /**
     * 應用所有進行中的修改器並更新相機狀態。
     * 由 CameraMixin 的 TAIL 注入點呼叫。
     */
    public void applyAll(Camera camera, float tickDelta) {
        if (camera == null || (modifiers.isEmpty() && trackingTarget == null)) return;

        long now = System.currentTimeMillis();
        // 建立轉換快照
        CameraTransform transform = CameraTransform.of(
                CameraCompat.getPosition(camera),
                CameraCompat.getYaw(camera),
                CameraCompat.getPitch(camera)
        );

        // 1. 執行所有修改器運算（如 Shake, PathAnimation）
        for (CameraModifier modifier : modifiers) {
            modifier.apply(transform, tickDelta, now);
        }

        // 2. 將運算後的結果寫回相機物件（透過 Compat 或 Accessor）
        CameraCompat.setPosition(camera, transform.position());
        CameraCompat.setRotation(camera, transform.yaw(), transform.pitch());

        // 3. 清理已過期的修改器
        modifiers.removeIf(modifier -> modifier.isFinished(now));
    }

    /** 計算左後方偏移座標 (供特殊模式使用) */
    public Vec3 calculateLeftRearOffset(Entity focusedEntity, float tickDelta) {
        if (focusedEntity == null) return Vec3.ZERO;
        float yaw = focusedEntity.getViewYRot(tickDelta);
        float f = yaw * ((float)Math.PI / 180F);
        double rearX = Math.sin(f) * 2.0;
        double rearZ = -Math.cos(f) * 2.0;
        float fLeft = (yaw - 90.0F) * ((float)Math.PI / 180F);
        double leftX = Math.sin(fLeft) * 1.5;
        double leftZ = -Math.cos(fLeft) * 1.5;
        return new Vec3(rearX + leftX, 1.5, rearZ + leftZ);
    }

    /** 簡單的座標檢測 */
    public Vec3 clipCameraToSpace(BlockGetter level, Vec3 cameraPos, Vec3 offset) {
        // 此處目前為基礎實作，可進一步整合射線檢測防穿牆
        return cameraPos.add(offset);
    }
}