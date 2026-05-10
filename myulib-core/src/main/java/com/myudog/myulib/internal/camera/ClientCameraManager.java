package com.myudog.myulib.internal.camera;

import com.myudog.myulib.api.core.camera.CameraPerspective;
import com.myudog.myulib.api.core.camera.CameraTrackingTarget;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * 客戶端專用的相機狀態總管
 */
public final class ClientCameraManager {
    public static final ClientCameraManager INSTANCE = new ClientCameraManager();

    // --- 視角與權限狀態 ---
    private CameraPerspective currentPerspective = CameraPerspective.FREE;
    private boolean allowF5 = true; // 即使在強制模式下，是否允許玩家用 F5 掙脫？(通常強制模式為 false)

    // --- 追蹤與移動狀態 ---
    @Nullable
    private CameraTrackingTarget currentTarget = null;
    
    // 平滑移動的暫存變數 (供後續 Modifier 計算使用)
    private Vec3 currentLerpPos = Vec3.ZERO;

    private ClientCameraManager() {}

    // --- Setter & Getter ---

    public void setPerspectiveState(CameraPerspective perspective, boolean allowF5) {
        this.currentPerspective = perspective;
        this.allowF5 = allowF5;
    }

    public CameraPerspective getCurrentPerspective() {
        return currentPerspective;
    }

    public boolean canUseF5() {
        return allowF5;
    }

    public void setTrackingTarget(@Nullable CameraTrackingTarget target) {
        this.currentTarget = target;
    }

    @Nullable
    public CameraTrackingTarget getTrackingTarget() {
        return currentTarget;
    }
}