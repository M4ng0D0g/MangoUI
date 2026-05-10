package com.myudog.myulib.mixin;

import com.myudog.myulib.api.core.control.ControlManager;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Mob.class)
public abstract class MixinMobEntity extends LivingEntity {

    protected MixinMobEntity(EntityType<? extends LivingEntity> entityType, Level level) {
        super(entityType, level);
    }

    @Inject(method = "serverAiStep", at = @At("HEAD"), cancellable = true)
    private void myulib_mc$freezeAiWhenPossessed(CallbackInfo ci) {
        // 直接向全域 ControlManager 查詢此實體是否正在被控制
        if (ControlManager.INSTANCE.isControlledTarget(this.getUUID())) {
            ci.cancel(); // 暫停 AI 尋路與決策邏輯
        }
    }
}