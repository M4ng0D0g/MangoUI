package com.myudog.myulib.api.control;

import com.myudog.myulib.api.control.network.ControlInputPayload;
import com.myudog.myulib.api.control.network.ServerControlNetworking;
import com.myudog.myulib.api.debug.DebugFeature;
import com.myudog.myulib.api.debug.DebugLogManager;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;

import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 實體操控權管理器 (Possession System)
 * 負責處理玩家對其他實體的 1 對 1 控制權綁定與輸入轉發。
 */
public final class ControlManager {

    public static final ControlManager INSTANCE = new ControlManager();

    

    // 控制者玩家 UUID -> 目標實體 UUID
    private final Map<UUID, UUID> CONTROLLER_TO_TARGET = new ConcurrentHashMap<>();
    // 目標實體 UUID -> 控制者玩家 UUID
    private final Map<UUID, UUID> TARGET_TO_CONTROLLER = new ConcurrentHashMap<>();
    // 實體 UUID -> 最新的按鍵指令
    private final Map<UUID, ControlInputPayload> ENTITY_INPUTS = new ConcurrentHashMap<>();
    // 玩家 UUID -> 被停用的控制項
    private final Map<UUID, Set<ControlType>> PLAYER_DISABLED_CONTROLS = new ConcurrentHashMap<>();

    private ControlManager() {}

    public void install() {
        ServerControlNetworking.registerPayloads();
        ServerControlNetworking.registerServerReceivers();
    }

    public boolean bind(ServerPlayer player, Entity target) {
        if (player == null || target == null) {
            return false;
        }
        Set<UUID> syncTargets = new HashSet<>();
        boolean changed = bindInternal(player.getUUID(), target.getUUID(), syncTargets);
        if (!changed) {
            return false;
        }

        if (target instanceof Mob mob) {
            mob.addTag("myulib_controlled");
        }

        DebugLogManager.INSTANCE.log(DebugFeature.CONTROL,
                "bind player=" + player.getName().getString() + "(" + player.getUUID() + ") -> entity=" + target.getType() + "(" + target.getUUID() + ")");
        syncPlayers(player.level().getServer(), syncTargets);
        return true;
    }

    public void unbind(ServerPlayer player) {
        if (player == null) {
            return;
        }
        Set<UUID> syncTargets = new HashSet<>();
        unbindControllerInternal(player.getUUID(), syncTargets);
        syncPlayers(player.level().getServer(), syncTargets);
    }

    public boolean bind(UUID controllerId, UUID targetId) {
        if (controllerId == null || targetId == null) {
            return false;
        }
        return bindInternal(controllerId, targetId, new HashSet<>());
    }

    public void unbindFrom(UUID controllerId) {
        if (controllerId == null) {
            return;
        }
        unbindControllerInternal(controllerId, new HashSet<>());
    }

    public void unbindTo(UUID targetId) {
        if (targetId == null) {
            return;
        }
        unbindTargetInternal(targetId, new HashSet<>());
    }

    public void unbindTo(Entity target) {
        if (target == null) {
            return;
        }
        Set<UUID> syncTargets = new HashSet<>();
        unbindTargetInternal(target.getUUID(), syncTargets);
        syncPlayers(target.level().getServer(), syncTargets);
    }

    public boolean isControlling(ServerPlayer player) {
        return player != null && CONTROLLER_TO_TARGET.containsKey(player.getUUID());
    }

    public boolean isController(UUID uuid) {
        return uuid != null && CONTROLLER_TO_TARGET.containsKey(uuid);
    }

    public UUID getControlledEntity(ServerPlayer player) {
        return player == null ? null : CONTROLLER_TO_TARGET.get(player.getUUID());
    }

    public Optional<UUID> targetOfController(UUID controllerId) {
        return Optional.ofNullable(controllerId == null ? null : CONTROLLER_TO_TARGET.get(controllerId));
    }

    public Optional<UUID> controllerOfTarget(UUID targetId) {
        return Optional.ofNullable(targetId == null ? null : TARGET_TO_CONTROLLER.get(targetId));
    }

    public boolean isControlledTarget(UUID entityId) {
        return entityId != null && TARGET_TO_CONTROLLER.containsKey(entityId);
    }

    public boolean isControlledByPlayer(Entity entity) {
        return entity != null && TARGET_TO_CONTROLLER.containsKey(entity.getUUID());
    }

    /**
     * 伺服器收到封包時呼叫：更新目標實體的輸入狀態
     */
    public void updateInput(ServerPlayer player, ControlInputPayload input) {
        if (player == null || input == null) {
            return;
        }

        UUID targetId = CONTROLLER_TO_TARGET.get(player.getUUID());
        if (targetId != null) {
            ENTITY_INPUTS.put(targetId, input);
            DebugLogManager.INSTANCE.log(DebugFeature.CONTROL,
                    "input player=" + player.getName().getString() + " -> entity=" + targetId
                            + " [u=" + input.up() + ",d=" + input.down() + ",l=" + input.left() + ",r=" + input.right()
                            + ",j=" + input.jumping() + ",s=" + input.sneaking() + ",yaw=" + input.yaw() + ",pitch=" + input.pitch() + "]");
        }
    }

    /**
     * 供實體每一幀讀取自己的輸入
     */
    public ControlInputPayload getInput(Entity entity) {
        return ENTITY_INPUTS.get(entity.getUUID());
    }

    public int controlledCount() {
        return CONTROLLER_TO_TARGET.size();
    }

    public int bufferedInputCount() {
        return ENTITY_INPUTS.size();
    }

    public boolean setPlayerControl(ServerPlayer player, ControlType type, boolean enabled) {
        if (player == null) {
            return false;
        }
        boolean changed = setPlayerControl(player.getUUID(), type, enabled);
        if (changed) {
            syncControlState(player);
        }
        return changed;
    }

    public boolean setPlayerControl(UUID playerId, ControlType type, boolean enabled) {
        if (playerId == null || type == null) {
            return false;
        }

        if (enabled) {
            Set<ControlType> disabled = PLAYER_DISABLED_CONTROLS.get(playerId);
            if (disabled == null) {
                return false;
            }

            boolean changed = disabled.remove(type);
            if (disabled.isEmpty()) {
                PLAYER_DISABLED_CONTROLS.remove(playerId, disabled);
            }
            return changed;
        }

        Set<ControlType> disabled = PLAYER_DISABLED_CONTROLS.computeIfAbsent(playerId,
                ignored -> Collections.newSetFromMap(new ConcurrentHashMap<>()));
        return disabled.add(type);
    }

    public boolean isPlayerControlEnabled(ServerPlayer player, ControlType type) {
        if (player == null) {
            return true;
        }
        return isPlayerControlEnabled(player.getUUID(), type);
    }

    public boolean isPlayerControlEnabled(UUID playerId, ControlType type) {
        if (playerId == null || type == null) {
            return true;
        }
        return !effectiveDisabledPlayerControls(playerId).contains(type);
    }

    public Set<ControlType> disabledPlayerControls(UUID playerId) {
        if (playerId == null) {
            return Set.of();
        }
        Set<ControlType> disabled = PLAYER_DISABLED_CONTROLS.get(playerId);
        if (disabled == null || disabled.isEmpty()) {
            return Set.of();
        }
        return Set.copyOf(EnumSet.copyOf(disabled));
    }

    public Set<ControlType> effectiveDisabledPlayerControls(UUID playerId) {
        EnumSet<ControlType> disabled = EnumSet.noneOf(ControlType.class);
        disabled.addAll(disabledPlayerControls(playerId));

        if (isController(playerId)) {
            disabled.add(ControlType.MOVE);
            disabled.add(ControlType.SPRINT);
            disabled.add(ControlType.SNEAK);
            disabled.add(ControlType.CRAWL);
            disabled.add(ControlType.JUMP);
        }

        if (isControlledTarget(playerId)) {
            disabled.addAll(EnumSet.allOf(ControlType.class));
        }

        return Set.copyOf(disabled);
    }

    public void clearPlayerControls(UUID playerId) {
        if (playerId == null) {
            return;
        }
        PLAYER_DISABLED_CONTROLS.remove(playerId);
    }

    public void clearAllPlayerControls() {
        PLAYER_DISABLED_CONTROLS.clear();
    }

    public int encodeDisabledMask(Set<ControlType> disabled) {
        if (disabled == null || disabled.isEmpty()) {
            return 0;
        }
        int mask = 0;
        for (ControlType type : disabled) {
            mask |= (1 << type.ordinal());
        }
        return mask;
    }

    public void syncControlState(ServerPlayer player) {
        if (player == null) {
            return;
        }

        UUID playerId = player.getUUID();
        Set<ControlType> disabled = effectiveDisabledPlayerControls(playerId);
        ServerControlNetworking.syncControlState(
                player,
                encodeDisabledMask(disabled),
                isController(playerId),
                isControlledTarget(playerId)
        );
    }

    private boolean bindInternal(UUID controllerId, UUID targetId, Set<UUID> syncTargets) {
        if (controllerId == null || targetId == null || controllerId.equals(targetId)) {
            return false;
        }

        if (isControlledTarget(controllerId)) {
            return false;
        }

        syncTargets.add(controllerId);
        syncTargets.add(targetId);

        UUID oldTarget = CONTROLLER_TO_TARGET.remove(controllerId);
        if (oldTarget != null) {
            TARGET_TO_CONTROLLER.remove(oldTarget, controllerId);
            ENTITY_INPUTS.remove(oldTarget);
            syncTargets.add(oldTarget);
        }

        UUID oldController = TARGET_TO_CONTROLLER.remove(targetId);
        if (oldController != null) {
            CONTROLLER_TO_TARGET.remove(oldController, targetId);
            syncTargets.add(oldController);
        }

        // 如果目標本身是控制者，解除其既有控制關係（被接管者不可再操控他人）。
        UUID targetWasControlling = CONTROLLER_TO_TARGET.remove(targetId);
        if (targetWasControlling != null) {
            TARGET_TO_CONTROLLER.remove(targetWasControlling, targetId);
            ENTITY_INPUTS.remove(targetWasControlling);
            syncTargets.add(targetWasControlling);
        }

        CONTROLLER_TO_TARGET.put(controllerId, targetId);
        TARGET_TO_CONTROLLER.put(targetId, controllerId);
        return true;
    }

    private void unbindControllerInternal(UUID controllerId, Set<UUID> syncTargets) {
        UUID targetId = CONTROLLER_TO_TARGET.remove(controllerId);
        if (targetId != null) {
            TARGET_TO_CONTROLLER.remove(targetId, controllerId);
            ENTITY_INPUTS.remove(targetId);
            syncTargets.add(controllerId);
            syncTargets.add(targetId);
            DebugLogManager.INSTANCE.log(DebugFeature.CONTROL,
                    "unbind controller=" + controllerId + " from target=" + targetId);
        }
    }

    private void unbindTargetInternal(UUID targetId, Set<UUID> syncTargets) {
        UUID controllerId = TARGET_TO_CONTROLLER.remove(targetId);
        if (controllerId != null) {
            CONTROLLER_TO_TARGET.remove(controllerId, targetId);
            ENTITY_INPUTS.remove(targetId);
            syncTargets.add(controllerId);
            syncTargets.add(targetId);
            DebugLogManager.INSTANCE.log(DebugFeature.CONTROL,
                    "unbind target=" + targetId + " from controller=" + controllerId);
        }
    }

    private void syncPlayers(MinecraftServer server, Set<UUID> playerIds) {
        if (server == null || playerIds == null || playerIds.isEmpty()) {
            return;
        }
        for (UUID playerId : playerIds) {
            ServerPlayer online = server.getPlayerList().getPlayer(playerId);
            if (online != null) {
                syncControlState(online);
            }
        }
    }
}