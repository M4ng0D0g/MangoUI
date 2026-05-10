package com.myudog.myulib.mixin;

import com.myudog.myulib.api.core.control.ControlManager;
import com.myudog.myulib.api.core.control.IControllableAttackable;
import com.myudog.myulib.api.core.control.IControllableMovable;
import com.myudog.myulib.api.core.control.Intent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.UUID;

/**
 * 為所有 LivingEntity 注入 IControllableMovable / IControllableAttackable 能力。
 * <p>
 * 狀態查詢完全委派給全域 ControlManager，本 Mixin 自身不持有任何玩家強引用。
 * 移動意圖 (movementIntent) 以 @Unique 欄位儲存，
 * 由 executeMove() 寫入，在 aiStep() 中讀取並套用。
 */
@Mixin(LivingEntity.class)
public abstract class MixinLivingEntityControl implements IControllableMovable, IControllableAttackable {

    @Unique
    private Vec3 movementIntent = Vec3.ZERO;

    // ── Shadow 原版欄位 (Mojmap 官方映射) ──────────────────────────────────
    @Shadow public float xxa;
    @Shadow public float zza;
    @Shadow public abstract void setYHeadRot(float yHeadRot);   // 修正: 原 setHeadYaw
    @Shadow public float yBodyRot;                              // 修正: 原 bodyYaw


    // ── IControllable 基礎實作 ──────────────────────────────────────────────

    @Override
    public UUID myulib_mc$getControllableUuid() {
        return selfUuid();
    }
    // 註：isPossessed()、getControllerIds() 等方法已在介面中有 default 實作，
    // 會自動向 ControlManager.INSTANCE 查詢，因此不需要在此覆寫。


    // ── IControllableMovable 實作 ───────────────────────────────────────────

    @Override
    public void myulib_mc$executeMove(Vec3 movementVector) {
        this.movementIntent = movementVector;
    }


    // ── IControllableAttackable 實作 ───────────────────────────────────────

    @Override
    public void myulib_mc$executeAttack(Intent intent) {
        // 由具體子類(或透過其他系統)覆寫以實作攻擊邏輯
        // 若需觸發原版攻擊動畫，可在此呼叫 ((LivingEntity)(Object)this).swing(InteractionHand.MAIN_HAND);
    }


    // ── aiStep 注入：覆寫實體移動與朝向 ──────────────────────────────────────

    @Inject(method = "aiStep", at = @At("HEAD"))
    private void myulib_mc$overrideAiAndMovement(CallbackInfo ci) {
        // 直接向 ControlManager 查詢是否正在被控制
        if (!ControlManager.INSTANCE.isControlledTarget(selfUuid())) return;

        if (movementIntent.lengthSqr() > 0.0001) {
            // 1. 實體獨立旋轉控制 (Decoupled Entity Rotation)
            // 根據移動向量計算目標 Yaw，靜止時保持最後朝向
            float targetYaw = (float) Math.toDegrees(
                    Math.atan2(-movementIntent.x, movementIntent.z));

            ((net.minecraft.world.entity.Entity) (Object) this).setYRot(targetYaw);
            this.setYHeadRot(targetYaw);
            this.yBodyRot = targetYaw;

            // 2. 以移動向量長度作為前進速度
            // movementIntent 已為絕對世界座標，實體朝向已對齊，
            // 因此直接填入 zza (前進/後退的輸入值) 即可沿方向前進
            this.zza = (float) movementIntent.length();

        } else {
            // 停止移動，靜止時保持最後朝向
            this.zza = 0;
        }
        this.xxa = 0;
    }


    // ── die() 注入：實體死亡時自動清理 ControlManager 綁定 ──────────────────

    @Inject(method = "die", at = @At("HEAD"))
    @SuppressWarnings("resource")
    private void myulib_mc$cleanupOnDeath(DamageSource source, CallbackInfo ci) {
        LivingEntity self = (LivingEntity) (Object) this;
        // 實體死亡時，從全域索引中安全移除綁定
        ServerLevel level = (ServerLevel) self.level();
        if (level.getServer() != null) {
            ControlManager.INSTANCE.unbindTarget(self.getUUID(), level.getServer());
        }
    }

    // ── 工具方法 ────────────────────────────────────────────────────────────

    @Unique
    private UUID selfUuid() {
        return ((LivingEntity) (Object) this).getUUID();
    }
}