package com.myudog.myulib.mixin;

import com.myudog.myulib.api.control.ControlManager;
import com.myudog.myulib.api.control.network.ControlInputPayload;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
public abstract class MixinLivingEntityControl {

    // 取得原版的移動向量欄位
    @Shadow public float xxa; // 左右移動 (Strafe)
    @Shadow public float zza; // 前後移動 (Forward)
    @Shadow public float yya; // 上下移動 (通常是飛行/游泳)
    @Shadow public abstract void setJumping(boolean jumping);

    /**
     * 在實體計算 AI 和移動前注入。
     * 這是我們覆寫大腦的最佳時機！
     */
    @Inject(method = "aiStep", at = @At("HEAD"))
    private void overrideAiAndMovement(CallbackInfo ci) {
        LivingEntity entity = (LivingEntity) (Object) this;

        // 如果這個生物正在被玩家遙控
        if (ControlManager.INSTANCE.isControlledByPlayer(entity)) {
            ControlInputPayload input = ControlManager.INSTANCE.getInput(entity);

            if (input != null) {
                // 1. 🌟 強制同步視角 (讓生物的頭和身體看向玩家滑鼠指的方向)
                entity.setYRot(input.yaw());
                entity.setXRot(input.pitch());
                entity.setYHeadRot(input.yaw());
                entity.yBodyRot = input.yaw(); // 確保身體也轉過去

                // 2. 🌟 轉換 WASD 為底層移動向量
                float forward = 0.0F;
                float strafe = 0.0F;

                if (input.up()) forward += 1.0F;
                if (input.down()) forward -= 1.0F;
                if (input.left()) strafe += 1.0F;
                if (input.right()) strafe -= 1.0F;

                // 如果按著 Shift (潛行)，通常移動速度會變慢
                if (input.sneaking()) {
                    forward *= (float) 0.3;
                    strafe *= (float) 0.3;
                    entity.setShiftKeyDown(true);
                } else {
                    entity.setShiftKeyDown(false);
                }

                // 強制寫入生物的移動神經
                this.zza = forward;
                this.xxa = strafe;
                this.setJumping(input.jumping());

                // 💡 提示：因為我們直接改寫了 xxa 和 zza，Minecraft 原版的 travel() 方法
                // 會自動幫我們處理這隻生物的走路動畫、碰撞箱、掉落傷害，一切都會完美運作！
            }
        }
    }
}