package com.myudog.myulib.mixin;

import com.myudog.myulib.api.core.control.IPlayerController;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;

/**
 * 為 PlayerEntity 注入 IPlayerController 能力。
 * <p>
 * 由於系統已改為資料導向 (Data-Oriented) 的全域 ControlManager 架構，
 * 實體本身不再儲存任何綁定狀態，只需回傳玩家物件即可套用介面的 default 方法。
 */
@Mixin(ServerPlayer.class)
public abstract class MixinPlayerEntity implements IPlayerController {

    @Override
    public ServerPlayer myulib_mc$getControlPlayer() {
        return (ServerPlayer) (Object) this;
    }
}