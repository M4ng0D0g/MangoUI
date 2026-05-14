package com.myudog.myulib.client.mixin.client.camera;

import com.myudog.myulib.api.core.camera.CameraTrackingTarget;
import com.myudog.myulib.client.api.camera.ClientCameraManager;
import net.minecraft.client.Camera;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Camera.class)
public abstract class CameraMixin {

    @Shadow private Vec3 position;
    @Shadow private float xRot; // Pitch
    @Shadow private float yRot; // Yaw

    // 新增 Shadow 以取得類別內部的實體與世界參照
    @Shadow @Nullable private Entity entity;
    @Shadow @Nullable private Level level;

    @Shadow protected abstract void setPosition(Vec3 pos);
    @Shadow protected abstract void setRotation(float yRot, float xRot);

    @Inject(method = "alignWithEntity", at = @At("TAIL"))
    private void myulib_mc$applyCustomTrackingAndClipping(float partialTicks, CallbackInfo ci) {
        // 確保實體與世界已初始化
        if (this.entity == null || this.level == null) return;

        ClientCameraManager manager = ClientCameraManager.INSTANCE;
        CameraTrackingTarget target = manager.getTrackingTarget();

        if (target != null) {
            // 1. 取得目標基準點與無碰撞的期望座標 (傳入 partialTicks 以實現亞像素級平滑追蹤)
            Vec3 desiredPos = target.resolvePosition(entity.level(), partialTicks);
            
            if (desiredPos != null) {
                // === 🌟 防穿牆射線檢測 (Raycast) 開始 ===
                Vec3 startPos = entity.getEyePosition(partialTicks);
                
                ClipContext context = new ClipContext(
                        startPos, 
                        desiredPos, 
                        ClipContext.Block.VISUAL, 
                        ClipContext.Fluid.NONE, 
                        entity
                );
                
                HitResult hitResult = entity.level().clip(context);
                Vec3 finalTargetPos = (hitResult.getType() == HitResult.Type.BLOCK) ? hitResult.getLocation() : desiredPos;

                // 🛡️ 加入安全檢查：防止 NaN 癱瘓渲染引擎
                if (!Double.isNaN(finalTargetPos.x) && !Double.isNaN(finalTargetPos.y) && !Double.isNaN(finalTargetPos.z)) {
                    // 為了消除「來回閃爍」，我們必須確保座標設定是穩定的
                    // 如果距離過大則瞬移，否則直接設定位置 (因為 resolvePosition 已經處理了插值)
                    if (this.position.distanceToSqr(finalTargetPos) > 10000) { 
                        this.setPosition(finalTargetPos);
                    } else {
                        // 這裡不再使用 partialTicks * 0.2f 這種不穩定的倍率，改為直接同步
                        // 若需要更電影感的平滑度，應在 CameraTrackingTarget 內部或 Modifier 處理
                        this.setPosition(finalTargetPos);
                    }
                }
            }

            // 3. 視角解析與動態注視 (LookAt)
            Vec3 lookAtPos = target.resolveLookAt(entity.level(), partialTicks);
            if (lookAtPos != null) {
                double dx = lookAtPos.x - this.position.x;
                double dy = lookAtPos.y - this.position.y;
                double dz = lookAtPos.z - this.position.z;
                double horizontalDistance = Math.sqrt(dx * dx + dz * dz);

                float targetPitch = (float) -(Math.toDegrees(Math.atan2(dy, horizontalDistance)));
                float targetYaw = (float) Math.toDegrees(Math.atan2(dz, dx)) - 90.0F;

                // 視角旋轉同樣使用穩定的插值或直接設定
                this.setRotation(targetYaw, targetPitch);
            }
        }


        // 🌟 核心：套用所有進行中的修改器 (如震動、補間動畫等)
        manager.applyAll((Camera)(Object)this, partialTicks);
    }
}