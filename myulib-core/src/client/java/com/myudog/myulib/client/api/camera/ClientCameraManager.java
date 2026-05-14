package com.myudog.myulib.client.api.camera;

import com.myudog.myulib.api.core.animation.Easing;
import com.myudog.myulib.api.core.camera.*;
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

    // --- 新增：Fade 效果狀態 ---
    private int fadeR, fadeG, fadeB;
    private float fadeAlpha = 0.0f;
    private long fadeStartTime = 0;
    private float fadeInTime, fadeHoldTime, fadeOutTime;

    // --- 新增：FOV 效果狀態 ---
    private float targetFov = -1;
    private float startFov = -1;
    private long fovStartTime = 0;
    private long fovDuration = 0;
    private Easing fovEasing = Easing.LINEAR;

    private ClientCameraManager() {}

    // --- 狀態存取 (Getters & Setters) ---

    public CameraTrackingTarget getTrackingTarget() { return trackingTarget; }
    public void setTrackingTarget(CameraTrackingTarget target) { this.trackingTarget = target; }

    public CameraPerspective getCurrentPerspective() { return currentPerspective; }
    public boolean canUseF5() { return allowF5; }

    public float getFadeAlpha() { return fadeAlpha; }
    public int getFadeR() { return fadeR; }
    public int getFadeG() { return fadeG; }
    public int getFadeB() { return fadeB; }

    public void setPerspectiveState(CameraPerspective perspective, boolean allowF5) {
        this.currentPerspective = perspective;
        this.allowF5 = allowF5;
    }

    public boolean isControlOffsetMode() { return controlOffsetMode; }
    public void setControlOffsetMode(boolean mode) { this.controlOffsetMode = mode; }

    public void resetState() {
        this.trackingTarget = null;
        this.currentPerspective = CameraPerspective.FREE;
        this.allowF5 = true;
        this.controlOffsetMode = false;
        this.modifiers.clear();
        this.fadeAlpha = 0;
        this.targetFov = -1;
    }

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
        Vec3 start = Minecraft.getInstance().gameRenderer.getMainCamera().position();
        addModifier(new PathAnimationModifier(start, CameraTrackingTarget.of(destination), durationMillis, easing));
    }

    // --- 網路封包處理 ---

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
            case FADE -> {
                this.fadeR = payload.r();
                this.fadeG = payload.g();
                this.fadeB = payload.b();
                this.fadeInTime = payload.fadeIn();
                this.fadeHoldTime = payload.hold();
                this.fadeOutTime = payload.fadeOut();
                this.fadeStartTime = System.currentTimeMillis();
            }
            case FOV_SET -> {
                this.startFov = (float) Minecraft.getInstance().options.fov().get().doubleValue();
                this.targetFov = payload.fov();
                this.fovStartTime = System.currentTimeMillis();
                this.fovDuration = payload.durationMillis();
                this.fovEasing = payload.easing();
            }
            case FOV_CLEAR -> {
                this.startFov = (float) Minecraft.getInstance().options.fov().get().doubleValue();
                this.targetFov = -1; // -1 表示恢復預設
                this.fovStartTime = System.currentTimeMillis();
                this.fovDuration = payload.durationMillis();
                this.fovEasing = payload.easing();
            }
            case ATTACH -> {
                if (payload.targetEntityId() != null) {
                    this.trackingTarget = CameraTrackingTarget.ofEntityId(payload.targetEntityId()).withOffset(payload.offset());
                    this.setPerspectiveState(CameraPerspective.CUSTOM_OFFSET, false);
                }
            }
            case DETACH -> {
                this.trackingTarget = null;
                this.setPerspectiveState(CameraPerspective.FREE, true);
            }
            case SET_PRESET -> {
                if ("minecraft:free".equals(payload.stringData())) {
                    this.setPerspectiveState(CameraPerspective.FREE, true);
                    this.trackingTarget = null;
                } else if (payload.targetStaticPos() != null || payload.targetEntityId() != null) {
                    // 如果 Preset 帶有目標，則切換到自定義偏移模式
                    this.applyPayload(new CameraActionPayload(
                        ActionType.MOVE_TO, payload.intensity(), payload.durationMillis(),
                        payload.targetStaticPos(), payload.targetEntityId(),
                        payload.lookAtStaticPos(), payload.lookAtEntityId(),
                        payload.offset(), payload.easing(),
                        0, 0, 0, 0, 0, 0, 0, ""
                    ));
                }
            }
            case RESET -> resetState();
        }
    }

    // --- 渲染層每幀呼叫邏輯 ---

    public void applyAll(Camera camera, float tickDelta) {
        long now = System.currentTimeMillis();

        // 1. 更新 Fade 狀態
        updateFade(now);

        if (camera == null || (modifiers.isEmpty() && trackingTarget == null)) return;

        // 2. 建立轉換快照
        CameraTransform transform = CameraTransform.of(
                CameraCompat.getPosition(camera),
                CameraCompat.getYaw(camera),
                CameraCompat.getPitch(camera)
        );

        // 3. 執行所有修改器運算
        for (CameraModifier modifier : modifiers) {
            modifier.apply(transform, tickDelta, now);
        }

        // 4. 套用結果
        CameraCompat.setPosition(camera, transform.position());
        CameraCompat.setRotation(camera, transform.yaw(), transform.pitch());

        modifiers.removeIf(modifier -> modifier.isFinished(now));
    }

    private void updateFade(long now) {
        if (fadeStartTime == 0) return;
        float elapsed = (now - fadeStartTime) / 1000f;
        if (elapsed < fadeInTime) {
            fadeAlpha = elapsed / fadeInTime;
        } else if (elapsed < fadeInTime + fadeHoldTime) {
            fadeAlpha = 1.0f;
        } else if (elapsed < fadeInTime + fadeHoldTime + fadeOutTime) {
            fadeAlpha = 1.0f - (elapsed - (fadeInTime + fadeHoldTime)) / fadeOutTime;
        } else {
            fadeAlpha = 0;
            fadeStartTime = 0;
        }
    }

    /** 計算當前應用的 FOV 修改 */
    public float getModifiedFov(float originalFov) {
        if (fovStartTime == 0) return originalFov;
        long now = System.currentTimeMillis();
        float progress = (float) (now - fovStartTime) / fovDuration;
        if (progress >= 1.0f) {
            if (targetFov == -1) {
                fovStartTime = 0;
                return originalFov;
            }
            return targetFov;
        }
        float easedProgress = fovEasing.apply(progress);
        float endFov = targetFov == -1 ? originalFov : targetFov;
        return startFov + (endFov - startFov) * easedProgress;
    }

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

    public Vec3 clipCameraToSpace(BlockGetter level, Vec3 cameraPos, Vec3 offset) {
        return cameraPos.add(offset);
    }
}
