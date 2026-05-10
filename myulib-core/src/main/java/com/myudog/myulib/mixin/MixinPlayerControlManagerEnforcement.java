package com.myudog.myulib.mixin;

import com.myudog.myulib.api.core.control.ControlType;
import com.myudog.myulib.api.core.control.Intent;
import com.myudog.myulib.api.core.control.PlayerInputGate;
import com.myudog.myulib.internal.control.PlayerControlManager;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.UUID;

/**
 * 透過 Mixin 在 PlayerControlManager 的分發層級強制執行輸入閘門權限。
 * <p>
 * 確保即使 Client 端被破解發送了非法封包，Server 端仍能根據 PlayerInputGate 拒絕意圖。
 */
@Mixin(PlayerControlManager.class)
public abstract class MixinPlayerControlManagerEnforcement {

    @Shadow @Final private UUID playerUuid;

    /**
     * 在意圖分發前檢查權限。
     */
    @Inject(method = "dispatchIntent", at = @At("HEAD"), cancellable = true, remap = false)
    private void myulib_mc$enforceInputGate(Intent intent, Level level, CallbackInfo ci) {
        // 根據意圖類型對應到 ControlType 進行檢查
        ControlType requiredType = mapIntentToControl(intent);
        
        if (requiredType != null && PlayerInputGate.INSTANCE.isDenied(playerUuid, requiredType)) {
            // 權限不足，直接攔截，不再分發給綁定的實體
            ci.cancel();
        }
    }

    /**
     * 輔助方法：將 Intent 對應到權限系統的 ControlType
     */
    @Unique
    private ControlType mapIntentToControl(Intent intent) {
        return switch (intent.type()) {
            case MOVE_VECTOR -> ControlType.MOVE;
            case ROTATE -> ControlType.ROTATE;
            case JUMP -> ControlType.JUMP;
            case SNEAK -> ControlType.SNEAK;
            case SPRINT -> ControlType.SPRINT;
            case LEFT_CLICK -> ControlType.LEFT_CLICK;
            case RIGHT_CLICK -> ControlType.RIGHT_CLICK;
            default -> null;
        };
    }
}
