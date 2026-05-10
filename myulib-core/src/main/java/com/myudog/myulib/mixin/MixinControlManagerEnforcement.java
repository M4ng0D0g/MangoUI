package com.myudog.myulib.mixin;

import com.myudog.myulib.api.core.control.ControlManager;
import com.myudog.myulib.api.core.control.ControlType;
import com.myudog.myulib.api.core.control.PlayerInputGate;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.EnumSet;
import java.util.Set;
import java.util.UUID;

/**
 * 透過 Mixin 擴充 ControlManager 的權限計算邏輯。
 * <p>
 * 遵循「不修改現有檔案」原則，透過注入方式將 PlayerInputGate 的限制合併進去。
 * 這樣 ControlManager 發送同步封包時，就會包含來自 Gate 的限制。
 */
@Mixin(ControlManager.class)
public abstract class MixinControlManagerEnforcement {

    /**
     * 在計算玩家有效的「被禁用輸入」集合時，合併來自 PlayerInputGate 的資料。
     */
    @Inject(method = "effectiveDisabledPlayerControls", at = @At("RETURN"), cancellable = true, remap = false)
    private void myulib_mc$mergeGatePermissions(UUID playerId, CallbackInfoReturnable<Set<ControlType>> cir) {
        Set<ControlType> baseDisabled = cir.getReturnValue();
        
        // 如果 PlayerInputGate 有額外限制，則進行合併
        if (PlayerInputGate.INSTANCE.hasAnyDenied(playerId)) {
            EnumSet<ControlType> merged = EnumSet.noneOf(ControlType.class);
            merged.addAll(baseDisabled);
            merged.addAll(PlayerInputGate.INSTANCE.getDeniedTypes(playerId));
            cir.setReturnValue(merged);
        }
    }
}
