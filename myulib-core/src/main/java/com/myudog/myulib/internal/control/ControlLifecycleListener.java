package com.myudog.myulib.internal.control;

import net.fabricmc.fabric.api.entity.event.v1.ServerEntityLevelChangeEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.world.entity.LivingEntity;

/**
 * 生命週期清理：監聽實體死亡與玩家斷線，自動清理 ControlRegistry。
 * <p>
 * 在模組初始化時呼叫 {@link #register()} 一次即可。
 */
public final class ControlLifecycleListener {

    private ControlLifecycleListener() {}

    public static void register() {
        // 1. 玩家斷線 → 清除其所有綁定與權限狀態
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            java.util.UUID playerId = handler.player.getUUID();
            
            com.myudog.myulib.api.core.debug.DebugLogManager.INSTANCE.log(
                    com.myudog.myulib.api.core.debug.DebugFeature.CONTROL,
                    "Lifecycle: Player Disconnect [" + handler.player.getName().getString() + "], clearing data..."
            );
            
            ControlRegistry.INSTANCE.removeAllByController(playerId);
            com.myudog.myulib.api.core.control.PlayerInputGate.INSTANCE.onPlayerDisconnect(playerId);
            InputSequenceTracker.INSTANCE.clear(playerId);
        });

        // 2. 實體死亡：透過 Mixin 的 clearControllers() 觸發（已在 MixinLivingEntityControl 中委派）
        //    此處另外監聽維度切換（跨維度傳送等同於暫時「重生」）
        ServerEntityLevelChangeEvents.AFTER_ENTITY_CHANGE_LEVEL.register(
                (originalEntity, newEntity, origin, destination) -> {
                    if (originalEntity instanceof LivingEntity) {
                        com.myudog.myulib.api.core.debug.DebugLogManager.INSTANCE.log(
                                com.myudog.myulib.api.core.debug.DebugFeature.CONTROL,
                                "Lifecycle: Entity Dimension Change [" + originalEntity.getUUID() + "]"
                        );
                    }
                }
        );
    }
}
