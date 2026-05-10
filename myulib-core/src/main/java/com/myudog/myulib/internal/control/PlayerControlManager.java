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
 * <p>
 * 僅以 UUID 儲存綁定關係，不持有任何實體強引用。
 * dispatchIntent 時透過 ServerWorld#getEntity(UUID) 動態解析，
 * 若實體已卸載則靜默跳過，不破壞綁定關係（支援斷線重連）。
 */
public class PlayerControlManager {

    private final UUID playerUuid;
    // 僅存 UUID，無記憶體洩漏風險
    private final Set<UUID> boundEntityIds = new HashSet<>();

    public PlayerControlManager(ServerPlayer player) {
        this.playerUuid = player.getUUID();
    }

    // -------------------------------------------------------------------------
    // Binding API
    // -------------------------------------------------------------------------

    public boolean bindControl(UUID targetUuid) {
        if (targetUuid == null || targetUuid.equals(playerUuid)) {
            return false;
        }
        // 防止重複綁定
        if (boundEntityIds.contains(targetUuid)) {
            return false;
        }
        boundEntityIds.add(targetUuid);

        // 通知目標實體（若當前已在記憶體中）
        // 此步驟為 best-effort，卸載中的實體重新載入後 isPossessed() 依靠 ControlManager 查詢
        return true;
    }

    public boolean unbindControl(UUID targetUuid) {
        if (targetUuid == null) {
            return false;
        }
        boolean removed = boundEntityIds.remove(targetUuid);
        if (removed) {
            // 通知中央 ControlManager 清除反向索引
            ControlRegistry.INSTANCE.removeBinding(playerUuid, targetUuid);
        }
        return removed;
    }

    public Set<UUID> getBoundEntityIds() {
        return Collections.unmodifiableSet(boundEntityIds);
    }

    public void clearBindings(boolean keepSelf) {
        Set<UUID> toRemove = new HashSet<>(boundEntityIds);
        for (UUID id : toRemove) {
            if (keepSelf && id.equals(playerUuid)) {
                continue;
            }
            boundEntityIds.remove(id);
            ControlRegistry.INSTANCE.removeBinding(playerUuid, id);
        }
    }

    // -------------------------------------------------------------------------
    // Intent Dispatch (UUID → Entity resolution happens here)
    // -------------------------------------------------------------------------

    public void dispatchIntent(Intent intent, Level level) {
        if (!(level instanceof ServerLevel serverWorld)) {
            return;
        }

        for (UUID targetId : boundEntityIds) {
            Entity entity = serverWorld.getEntity(targetId);
            // 若實體不在當前已載入的區塊中，靜默跳過（不移除綁定）
            if (!(entity instanceof LivingEntity living)) {
                continue;
            }

            if (intent.type() == IntentType.MOVE_VECTOR && living instanceof IControllableMovable movable) {
                movable.myulib_mc$executeMove(intent.vector());
            } else if (living instanceof IControllableAttackable attackable) {
                attackable.myulib_mc$executeAttack(intent);
            }
        }
    }

    // -------------------------------------------------------------------------
    // Lifecycle: called when entity dies or player disconnects
    // -------------------------------------------------------------------------

    /** 實體死亡或強制移除時，從綁定清單中剔除 */
    public boolean onEntityRemoved(UUID entityId) {
        return boundEntityIds.remove(entityId);
    }
}
