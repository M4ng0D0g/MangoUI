package com.myudog.myulib.internal.control;

import com.myudog.myulib.api.core.control.IControllableAttackable;
import com.myudog.myulib.api.core.control.IControllableMovable;
import com.myudog.myulib.api.core.control.Intent;
import com.myudog.myulib.api.core.control.IntentType;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * 玩家對多實體的控制管理器 (1-to-N, ID-Based)
 */
public class PlayerControlManager {

    private final UUID playerUuid;
    private final Set<UUID> boundEntityIds = new HashSet<>();

    public PlayerControlManager(ServerPlayer player) {
        this.playerUuid = player.getUUID();
    }

    public boolean bindControl(UUID targetUuid) {
        if (targetUuid == null || targetUuid.equals(playerUuid)) return false;
        if (boundEntityIds.contains(targetUuid)) return false;
        boundEntityIds.add(targetUuid);

        com.myudog.myulib.api.core.debug.DebugLogManager.INSTANCE.log(
                com.myudog.myulib.api.core.debug.DebugFeature.CONTROL,
                "Bind: Player[" + playerUuid + "] -> Entity[" + targetUuid + "]"
        );
        return true;
    }

    public boolean unbindControl(UUID targetUuid) {
        if (targetUuid == null) return false;
        boolean removed = boundEntityIds.remove(targetUuid);
        if (removed) {
            ControlRegistry.INSTANCE.removeBinding(playerUuid, targetUuid);
            com.myudog.myulib.api.core.debug.DebugLogManager.INSTANCE.log(
                    com.myudog.myulib.api.core.debug.DebugFeature.CONTROL,
                    "Unbind: Player[" + playerUuid + "] -X- Entity[" + targetUuid + "]"
            );
        }
        return removed;
    }

    public Set<UUID> getBoundEntityIds() {
        return Collections.unmodifiableSet(boundEntityIds);
    }

    public void clearBindings(boolean keepSelf) {
        Set<UUID> toRemove = new HashSet<>(boundEntityIds);
        for (UUID id : toRemove) {
            if (keepSelf && id.equals(playerUuid)) continue;
            boundEntityIds.remove(id);
            ControlRegistry.INSTANCE.removeBinding(playerUuid, id);
        }
    }

    /**
     * 分發意圖並記錄輸入序列
     */
    public void dispatchIntent(Intent intent, Level level) {
        // 1. 記錄輸入序列 (用於特殊操作偵測)
        InputSequenceTracker.INSTANCE.track(playerUuid, intent);

        if (!(level instanceof ServerLevel serverWorld)) return;

        // --- Debug Log ---
        if (intent.type() != com.myudog.myulib.api.core.control.IntentType.MOVE_VECTOR && intent.type() != com.myudog.myulib.api.core.control.IntentType.ROTATE) {
             com.myudog.myulib.api.core.debug.DebugLogManager.INSTANCE.log(
                     com.myudog.myulib.api.core.debug.DebugFeature.CONTROL,
                     "Dispatch: Player[" + playerUuid + "] -> Action[" + intent.type() + ":" + intent.action() + "]"
             );
        }

        // 2. 分發給綁定的實體
        for (UUID targetId : boundEntityIds) {
            Entity entity = serverWorld.getEntity(targetId);
            if (!(entity instanceof LivingEntity living)) continue;

            if (intent.type() == IntentType.MOVE_VECTOR && living instanceof IControllableMovable movable) {
                movable.myulib_mc$executeMove(intent.vector());
            } else if (living instanceof IControllableAttackable attackable) {
                attackable.myulib_mc$executeAttack(intent);
            }
        }
    }

    public boolean onEntityRemoved(UUID entityId) {
        return boundEntityIds.remove(entityId);
    }
}
