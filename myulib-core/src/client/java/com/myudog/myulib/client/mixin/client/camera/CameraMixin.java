package com.myudog.myulib.client.mixin.client.camera;

import com.myudog.myulib.api.core.camera.CameraTrackingTarget;
import com.myudog.myulib.internal.camera.ClientCameraManager;
import net.minecraft.client.Camera;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
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

    @Shadow protected abstract void setPosition(Vec3 pos);
    @Shadow protected abstract void setRotation(float yRot, float xRot);

    @Inject(method = "setup", at = @At("TAIL"))
    private void myulib_mc$applyCustomTrackingAndClipping(BlockGetter level, Entity entity, boolean detached, boolean thirdPersonReverse, float partialTick, CallbackInfo ci) {
        
        ClientCameraManager manager = ClientCameraManager.INSTANCE;
        CameraTrackingTarget target = manager.getTrackingTarget();

        if (target != null && entity.level() != null) {
            
            // 1. 取得目標基準點與無碰撞的期望座標 (這裡的 resolvePosition 已經包含了 offset)
            Vec3 desiredPos = target.resolvePosition(entity.level());
            
            if (desiredPos != null) {
                // === 🌟 防穿牆射線檢測 (Raycast) 開始 ===
                
                // 發射起點：實體的眼睛位置
                Vec3 startPos = entity.getEyePosition(partialTick);
                
                // 建立檢測上下文：檢查視覺上會阻擋視線的方塊，不檢查液體
                ClipContext context = new ClipContext(
                        startPos, 
                        desiredPos, 
                        ClipContext.Block.VISUAL, 
                        ClipContext.Fluid.NONE, 
                        entity
                );
                
                // 發射射線！
                HitResult hitResult = entity.level().clip(context);
                
                // 如果撞到方塊，將最終位置設為撞擊點 (可選擇性往前推 0.1 格避免破圖，這裡先用撞擊點)
                Vec3 finalTargetPos = (hitResult.getType() == HitResult.Type.BLOCK) ? hitResult.getLocation() : desiredPos;

                // === 防穿牆檢測結束 ===

                // 2. 座標平滑移動 (Lerp) -> 這裡改用 finalTargetPos
                Vec3 lerpedPos = this.position.lerp(finalTargetPos, partialTick * 0.2f);
                this.setPosition(lerpedPos);
            }

            // 3. 視角解析與動態注視 (LookAt Lerp)
            Vec3 lookAtPos = target.resolveLookAt(entity.level());
            if (lookAtPos != null) {
                double dx = lookAtPos.x - this.position.x;
                double dy = lookAtPos.y - this.position.y;
                double dz = lookAtPos.z - this.position.z;
                double horizontalDistance = Math.sqrt(dx * dx + dz * dz);

                float targetPitch = (float) -(Math.toDegrees(Math.atan2(dy, horizontalDistance)));
                float targetYaw = (float) Math.toDegrees(Math.atan2(dz, dx)) - 90.0F;

                float lerpedYaw = Mth.rotLerp(partialTick * 0.3f, this.yRot, targetYaw);
                float lerpedPitch = Mth.rotLerp(partialTick * 0.3f, this.xRot, targetPitch);

                this.setRotation(lerpedYaw, lerpedPitch);
            }
        }
    }
}