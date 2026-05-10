package com.myudog.myulib.api.core.control;

import com.myudog.myulib.api.core.control.network.ControlInputPayload;
import com.myudog.myulib.api.core.control.network.ServerControlNetworking;
import com.myudog.myulib.api.core.debug.DebugFeature;
import com.myudog.myulib.api.core.debug.DebugLogManager;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public final class ControlManager {

    public static final ControlManager INSTANCE = new ControlManager();

    private final Map<UUID, Set<UUID>> CONTROLLER_TO_TARGETS = new ConcurrentHashMap<>();
    private final Map<UUID, Set<UUID>> TARGET_TO_CONTROLLERS = new ConcurrentHashMap<>();
    private final Map<UUID, ControlInputPayload> ENTITY_INPUTS = new ConcurrentHashMap<>();
    private final Map<UUID, Integer> PLAYER_PERMISSIONS = new ConcurrentHashMap<>();

    private ControlManager() {}

    public void install() {
        ServerControlNetworking.registerPayloads();
        ServerControlNetworking.registerServerReceivers();
    }

    // --- 權限管理 (Permissions) ---

    public boolean isPlayerControlEnabled(ServerPlayer player, ControlType type) {
        if (player == null) return true;
        return isPlayerControlEnabled(player.getUUID(), type);
    }

    public boolean isPlayerControlEnabled(UUID playerId, ControlType type) {
        if (playerId == null) return true;
        int mask = PLAYER_PERMISSIONS.getOrDefault(playerId, 0);
        return (mask & (1 << type.ordinal())) == 0;
    }

    public void setPlayerControl(ServerPlayer player, ControlType type, boolean enabled) {
        if (player == null) return;
        UUID uuid = player.getUUID();
        int mask = PLAYER_PERMISSIONS.getOrDefault(uuid, 0);
        if (enabled) mask &= ~(1 << type.ordinal());
        else mask |= (1 << type.ordinal());
        PLAYER_PERMISSIONS.put(uuid, mask);
        syncControlState(player);
    }

    // --- 控制綁定 (Binding) ---

    public boolean bind(ServerPlayer player, Entity target) {
        if (player == null || target == null || player.getUUID().equals(target.getUUID())) return false;
        CONTROLLER_TO_TARGETS.computeIfAbsent(player.getUUID(), k -> ConcurrentHashMap.newKeySet()).add(target.getUUID());
        TARGET_TO_CONTROLLERS.computeIfAbsent(target.getUUID(), k -> ConcurrentHashMap.newKeySet()).add(player.getUUID());
        if (target instanceof Mob mob) mob.addTag("myulib_controlled");
        syncControlState(player);
        return true;
    }

    public void unbind(ServerPlayer player) {
        if (player == null) return;
        if (player.level() instanceof ServerLevel serverLevel) {
            unbindFrom(player.getUUID(), serverLevel.getServer());
        }
    }

    public void unbindTo(Entity target) {
        if (target == null) return;
        if (target.level() instanceof ServerLevel serverLevel) {
            unbindTarget(target.getUUID(), serverLevel.getServer());
        }
    }

    public boolean unbind(ServerPlayer player, UUID targetUuid) {
        if (player == null || targetUuid == null) return false;
        UUID pUuid = player.getUUID();
        Set<UUID> targets = CONTROLLER_TO_TARGETS.get(pUuid);
        if (targets != null && targets.remove(targetUuid)) {
            if (targets.isEmpty()) CONTROLLER_TO_TARGETS.remove(pUuid);
            Set<UUID> controllers = TARGET_TO_CONTROLLERS.get(targetUuid);
            if (controllers != null) {
                controllers.remove(pUuid);
                if (controllers.isEmpty()) {
                    TARGET_TO_CONTROLLERS.remove(targetUuid);
                    ENTITY_INPUTS.remove(targetUuid);
                }
            }
            syncControlState(player);
            return true;
        }
        return false;
    }

    public void unbindFrom(UUID controllerId, MinecraftServer server) {
        Set<UUID> targets = CONTROLLER_TO_TARGETS.remove(controllerId);
        if (targets != null) {
            for (UUID tUuid : targets) {
                Set<UUID> controllers = TARGET_TO_CONTROLLERS.get(tUuid);
                if (controllers != null) {
                    controllers.remove(controllerId);
                    if (controllers.isEmpty()) {
                        TARGET_TO_CONTROLLERS.remove(tUuid);
                        ENTITY_INPUTS.remove(tUuid);
                    }
                }
            }
        }
        syncPlayerByUuid(server, controllerId);
    }

    public void unbindTarget(UUID targetId, MinecraftServer server) {
        Set<UUID> controllers = TARGET_TO_CONTROLLERS.remove(targetId);
        ENTITY_INPUTS.remove(targetId);
        if (controllers != null) {
            for (UUID pUuid : controllers) {
                Set<UUID> targets = CONTROLLER_TO_TARGETS.get(pUuid);
                if (targets != null) {
                    targets.remove(targetId);
                    if (targets.isEmpty()) CONTROLLER_TO_TARGETS.remove(pUuid);
                }
                syncPlayerByUuid(server, pUuid);
            }
        }
    }

    // --- 輸入與意圖 (Input & Intent) ---

    public void updateInput(ServerPlayer player, ServerLevel level, ControlInputPayload input) {
        if (player == null || input == null) return;
        Set<UUID> targets = CONTROLLER_TO_TARGETS.get(player.getUUID());
        if (targets != null) {
            for (UUID tUuid : targets) ENTITY_INPUTS.put(tUuid, input);
        }
    }

    public void dispatchIntent(ServerPlayer player, ServerLevel level, Intent intent) {
        if (player == null || intent == null) return;
        Set<UUID> targets = CONTROLLER_TO_TARGETS.get(player.getUUID());
        if (targets == null || targets.isEmpty()) return;

        for (UUID targetId : targets) {
            Entity targetEntity = level.getEntity(targetId);
            if (targetEntity == null) continue;

            switch (intent.type()) {
                case MOVE_VECTOR -> { if (targetEntity instanceof IControllableMovable m) m.myulib_mc$executeMove(intent.vector()); }
                case ROTATE -> { if (targetEntity instanceof IControllableRotatable r) r.updateRotation((float)intent.vector().x, (float)intent.vector().y); }
                case JUMP, SNEAK, SPRINT -> { if (targetEntity instanceof IControllableActionable a) a.executeAction(intent); }
                case LEFT_CLICK, RIGHT_CLICK -> { if (targetEntity instanceof IControllableInteractable i) i.executeInteract(intent); }
                case GENERIC_ACTION -> { if (targetEntity instanceof IControllableCustom c) c.executeCustom(intent); }
            }
        }
    }

    // --- 查詢 (Queries) ---

    public Set<UUID> getControlledEntities(UUID playerId) {
        return CONTROLLER_TO_TARGETS.getOrDefault(playerId, Collections.emptySet());
    }

    public Set<UUID> getControllers(UUID targetId) {
        return TARGET_TO_CONTROLLERS.getOrDefault(targetId, Collections.emptySet());
    }

    public Optional<UUID> targetOfController(UUID playerId) {
        Set<UUID> targets = CONTROLLER_TO_TARGETS.get(playerId);
        return targets == null || targets.isEmpty() ? Optional.empty() : Optional.of(targets.iterator().next());
    }

    public Optional<UUID> controllerOfTarget(UUID targetId) {
        Set<UUID> controllers = TARGET_TO_CONTROLLERS.get(targetId);
        return controllers == null || controllers.isEmpty() ? Optional.empty() : Optional.of(controllers.iterator().next());
    }

    public boolean isController(UUID uuid) { return CONTROLLER_TO_TARGETS.containsKey(uuid); }
    public boolean isControlledTarget(UUID uuid) { return TARGET_TO_CONTROLLERS.containsKey(uuid); }

    public int controlledCount() {
        return CONTROLLER_TO_TARGETS.size();
    }

    public int bufferedInputCount() {
        return ENTITY_INPUTS.size();
    }

    public Set<ControlType> effectiveDisabledPlayerControls(UUID playerId) {
        int mask = PLAYER_PERMISSIONS.getOrDefault(playerId, 0);
        return Arrays.stream(ControlType.values())
                .filter(t -> (mask & (1 << t.ordinal())) != 0)
                .collect(Collectors.toSet());
    }

    // --- 同步 (Sync) ---

    public void syncControlState(ServerPlayer player) {
        if (player == null) return;
        int mask = PLAYER_PERMISSIONS.getOrDefault(player.getUUID(), 0);
        ServerControlNetworking.syncControlState(player, mask, isController(player.getUUID()), isControlledTarget(player.getUUID()));
    }

    private void syncPlayerByUuid(MinecraftServer server, UUID uuid) {
        if (server == null) return;
        ServerPlayer player = server.getPlayerList().getPlayer(uuid);
        if (player != null) syncControlState(player);
    }
}