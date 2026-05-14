package com.myudog.myulib.api.core.control;

import com.myudog.myulib.api.core.control.network.ControlInputPayload;
import com.myudog.myulib.api.core.control.network.ServerControlNetworking;
import com.myudog.myulib.api.core.debug.DebugFeature;
import com.myudog.myulib.api.core.debug.DebugLogManager;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * ControlManager
 *
 * 系統：核心控制系統 (Core Control System)
 * 角色：伺服器端的中央控制器，負責管理玩家與實體之間的控制綁定關係、權限管控以及輸入意圖的派發。
 * 類型：Manager / Controller
 *
 * 此類別維護了多對多的控制映射關係 (Controller-to-Target)，並提供介面讓玩家透過 Intent (意圖) 協議
 * 來操作非玩家實體。它與 ServerControlNetworking 協作，將網路封包轉化為具體的遊戲邏輯動作。
 */
public final class ControlManager {

    public static final ControlManager INSTANCE = new ControlManager();

    private final Map<UUID, Set<UUID>> CONTROLLER_TO_TARGETS = new ConcurrentHashMap<>();
    private final Map<UUID, Set<UUID>> TARGET_TO_CONTROLLERS = new ConcurrentHashMap<>();
    private final Map<UUID, ControlInputPayload> ENTITY_INPUTS = new ConcurrentHashMap<>();
    private final Map<UUID, Integer> PLAYER_PERMISSIONS = new ConcurrentHashMap<>();

    private ControlManager() {}

    /**
     * 安裝控制系統。
     * 註冊必要的網路封包與伺服器接收器。
     */
    public void install() {
        ServerControlNetworking.registerPayloads();
        ServerControlNetworking.registerServerReceivers();

        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) ->
            syncControlState(handler.player));

        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) ->
            onPlayerDisconnect(handler.player.getUUID(), server));
    }

    // --- 權限管理 (Permissions) ---

    /**
     * 檢查特定玩家的某種控制類型是否被啟用。
     *
     * @param player 伺服器玩家實例
     * @param type   控制類型 (如移動、相機、互動)
     * @return 若啟用則為 true
     */
    public boolean isPlayerControlEnabled(ServerPlayer player, ControlType type) {
        if (player == null) return true;
        return isPlayerControlEnabled(player.getUUID(), type);
    }

    /**
     * 透過 UUID 檢查控制權限。
     *
     * @param playerId 玩家 UUID
     * @param type     控制類型
     * @return 若啟用則為 true
     */
    public boolean isPlayerControlEnabled(UUID playerId, ControlType type) {
        if (playerId == null) return true;
        int mask = PLAYER_PERMISSIONS.getOrDefault(playerId, 0);
        return (mask & (1 << type.ordinal())) == 0;
    }

    /**
     * 設定玩家的控制權限。
     *
     * @param player  伺服器玩家
     * @param type    控制類型
     * @param enabled 是否啟用
     */
    public void setPlayerControl(ServerPlayer player, ControlType type, boolean enabled) {
        if (player == null) return;
        UUID uuid = player.getUUID();
        int mask = PLAYER_PERMISSIONS.getOrDefault(uuid, 0);
        if (enabled) mask &= ~(1 << type.ordinal());
        else mask |= (1 << type.ordinal());
        PLAYER_PERMISSIONS.put(uuid, mask);
        syncControlState(player);
        DebugLogManager.INSTANCE.log(DebugFeature.CONTROL,
                "Permission: Player[" + player.getName().getString() + "] " + type + "=" + (enabled ? "on" : "off"));
    }

    // --- 控制綁定 (Binding) ---

    /**
     * 將玩家與目標實體綁定。
     * 綁定後，玩家的輸入將會派發給該實體。
     *
     * @param player 負責控制的玩家
     * @param target 被控制的目標實體
     * @return 綁定是否成功 (自我控制會失敗)
     */
    public boolean bind(ServerPlayer player, Entity target) {
        if (player == null || target == null || player.getUUID().equals(target.getUUID())) return false;
        CONTROLLER_TO_TARGETS.computeIfAbsent(player.getUUID(), k -> ConcurrentHashMap.newKeySet()).add(target.getUUID());
        TARGET_TO_CONTROLLERS.computeIfAbsent(target.getUUID(), k -> ConcurrentHashMap.newKeySet()).add(player.getUUID());
        if (target instanceof Mob mob) mob.addTag("myulib_controlled");
        syncControlState(player);
        DebugLogManager.INSTANCE.log(DebugFeature.CONTROL,
                "Bind: Player[" + player.getName().getString() + "] -> Entity[" + target.getUUID() + "]");
        return true;
    }

    /**
     * 解除玩家當前所有的控制關係。
     *
     * @param player 目標玩家
     */
    public void unbind(ServerPlayer player) {
        if (player == null) return;
        if (player.level() instanceof ServerLevel serverLevel) {
            unbindFrom(player.getUUID(), serverLevel.getServer());
        }
        DebugLogManager.INSTANCE.log(DebugFeature.CONTROL,
                "UnbindAll: Player[" + player.getName().getString() + "]");
    }

    /**
     * 解除目標實體被控制的狀態。
     *
     * @param target 目標實體
     */
    public void unbindTo(Entity target) {
        if (target == null) return;
        if (target.level() instanceof ServerLevel serverLevel) {
            unbindTarget(target.getUUID(), serverLevel.getServer());
        }
        DebugLogManager.INSTANCE.log(DebugFeature.CONTROL,
                "UnbindTarget: Entity[" + target.getUUID() + "]");
    }

    /**
     * 解除特定玩家與特定目標的綁定關係。
     *
     * @param player     負責控制的玩家
     * @param targetUuid 目標實體 UUID
     * @return 移除是否成功
     */
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
            DebugLogManager.INSTANCE.log(DebugFeature.CONTROL,
                    "Unbind: Player[" + player.getName().getString() + "] -X- Entity[" + targetUuid + "]");
            return true;
        }
        return false;
    }

    /**
     * 從控制端 (玩家) 角度解除所有綁定。
     *
     * @param controllerId 控制者 UUID
     * @param server       Minecraft 伺服器實例 (用於同步)
     */
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

    /**
     * 從被控制端 (實體) 角度解除所有綁定。
     *
     * @param targetId 被控制者 UUID
     * @param server   Minecraft 伺服器實例
     */
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

    /**
     * 更新玩家的持續性輸入狀態 (如 WASD 狀態)。
     * 此狀態會快取於 ENTITY_INPUTS，供混合控制時查詢。
     *
     * @param player 來源玩家
     * @param level  世界實例
     * @param input  輸入封包資料
     */
    public void updateInput(ServerPlayer player, ServerLevel level, ControlInputPayload input) {
        if (player == null || input == null) return;
        Set<UUID> targets = CONTROLLER_TO_TARGETS.get(player.getUUID());
        if (targets != null) {
            for (UUID tUuid : targets) ENTITY_INPUTS.put(tUuid, input);
        }
    }

    /**
     * 派發一個瞬時意圖 (Intent) 給玩家控制的所有目標。
     *
     * @param player 來源玩家
     * @param level  世界實例
     * @param intent 意圖描述 (包含動作類型與參數)
     */
    public void dispatchIntent(ServerPlayer player, ServerLevel level, Intent intent) {
        if (player == null || intent == null) return;
        
        // 🌟 伺服器端權限強制執行：檢查 PlayerInputGate
        com.myudog.myulib.api.core.control.ControlType type = mapIntentToControl(intent);
        if (type != null && com.myudog.myulib.api.core.control.PlayerInputGate.INSTANCE.isDenied(player.getUUID(), type)) {
            return;
        }

        Set<UUID> targets = CONTROLLER_TO_TARGETS.get(player.getUUID());
        if (targets == null || targets.isEmpty()) return;

        for (UUID targetId : targets) {
            Entity targetEntity = level.getEntity(targetId);
            if (targetEntity == null) continue;

            // 路由意圖至對應的介面
            switch (intent.type()) {
                case MOVE, MOVE_FORWARD, MOVE_BACKWARD, MOVE_LEFT, MOVE_RIGHT -> {
                    if (targetEntity instanceof IControllableMovable m) m.myulib_mc$executeMove(intent.vector());
                }
                case ROTATE, ROTATE_YAW, ROTATE_PITCH -> {
                    if (targetEntity instanceof IControllableRotatable r) {
                        r.myulib_mc$updateRotation((float)intent.vector().x, (float)intent.vector().y);
                    }
                }
                case JUMP, SNEAK, SPRINT, CRAWL -> {
                    if (targetEntity instanceof IControllableActionable a) a.myulib_mc$executeAction(intent);
                }
                case LEFT_CLICK, RIGHT_CLICK, MIDDLE_CLICK -> {
                    if (targetEntity instanceof IControllableInteractable i) i.myulib_mc$executeInteract(intent);
                }
                case GENERIC_ACTION -> {
                    if (targetEntity instanceof IControllableCustom c) c.executeCustom(intent);
                }
                default -> {
                    if (targetEntity instanceof IControllableAttackable attackable) attackable.myulib_mc$executeAttack(intent);
                }
            }
        }
    }

    /**
     * 強制釋放玩家所有按鍵意圖 (用於權限更改時)。
     */
    public void forceReleaseAllIntents(ServerPlayer player) {
        if (player == null || !(player.level() instanceof ServerLevel level)) return;
        for (IntentType type : IntentType.values()) {
            if (type == IntentType.JUMP || type == IntentType.SNEAK || type == IntentType.SPRINT || type == IntentType.CRAWL ||
                type == IntentType.MOVE_FORWARD || type == IntentType.MOVE_BACKWARD || type == IntentType.MOVE_LEFT || type == IntentType.MOVE_RIGHT ||
                type == IntentType.LEFT_CLICK || type == IntentType.RIGHT_CLICK || type == IntentType.MIDDLE_CLICK) {
                dispatchIntent(player, level, Intent.action(type, InputAction.RELEASE));
            }
        }
    }

    // --- 查詢 (Queries) ---

    /**
     * 獲取玩家控制的所有實體 UUID。
     *
     * @param playerId 玩家 UUID
     * @return 實體 UUID 集合 (不可變)
     */
    public Set<UUID> getControlledEntities(UUID playerId) {
        return CONTROLLER_TO_TARGETS.getOrDefault(playerId, Collections.emptySet());
    }

    /**
     * 獲取控制特定實體的所有玩家 UUID。
     *
     * @param targetId 實體 UUID
     * @return 玩家 UUID 集合
     */
    public Set<UUID> getControllers(UUID targetId) {
        return TARGET_TO_CONTROLLERS.getOrDefault(targetId, Collections.emptySet());
    }

    /**
     * 獲取玩家當前控制的第一個目標。
     */
    public Optional<UUID> targetOfController(UUID playerId) {
        Set<UUID> targets = CONTROLLER_TO_TARGETS.get(playerId);
        return targets == null || targets.isEmpty() ? Optional.empty() : Optional.of(targets.iterator().next());
    }

    /**
     * 獲取實體當前被控制的第一個玩家。
     */
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

    /**
     * 獲取玩家當前生效的所有禁用控制類型。
     */
    public Set<ControlType> effectiveDisabledPlayerControls(UUID playerId) {
        int mask = PLAYER_PERMISSIONS.getOrDefault(playerId, 0);
        return Arrays.stream(ControlType.values())
                .filter(t -> (mask & (1 << t.ordinal())) != 0)
                .collect(Collectors.toSet());
    }

    // --- 同步 (Sync) ---

    /**
     * 同步玩家的控制狀態（權限、是否控制中等）到其客戶端。
     *
     * @param player 目標玩家
     */
    public void syncControlState(ServerPlayer player) {
        if (player == null) return;
        java.util.BitSet mask = new java.util.BitSet();
        for (ControlType type : effectiveDisabledPlayerControls(player.getUUID())) {
            mask.set(type.ordinal());
        }
        ServerControlNetworking.syncControlState(player, mask, isController(player.getUUID()), isControlledTarget(player.getUUID()));
    }

    private void onPlayerDisconnect(UUID playerId, MinecraftServer server) {
        if (playerId == null) return;
        clearControllerBindings(playerId);
        PLAYER_PERMISSIONS.remove(playerId);
        ENTITY_INPUTS.remove(playerId);
        if (server != null) {
            unbindTarget(playerId, server);
        }
    }

    public void clearAllBindings(MinecraftServer server) {
        Set<UUID> controllers = new HashSet<>(CONTROLLER_TO_TARGETS.keySet());
        for (UUID id : controllers) {
            unbindFrom(id, server);
        }
        CONTROLLER_TO_TARGETS.clear();
        TARGET_TO_CONTROLLERS.clear();
        ENTITY_INPUTS.clear();
        DebugLogManager.INSTANCE.log(DebugFeature.CONTROL, "Registry: Global Clear All Bindings");
    }

    private void clearControllerBindings(UUID controllerId) {
        Set<UUID> targets = CONTROLLER_TO_TARGETS.remove(controllerId);
        if (targets == null) return;

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

    private void syncPlayerByUuid(MinecraftServer server, UUID uuid) {
        if (server == null) return;
        ServerPlayer player = server.getPlayerList().getPlayer(uuid);
        if (player != null) syncControlState(player);
    }

    private ControlType mapIntentToControl(Intent intent) {
        return switch (intent.type()) {
            case MOVE, MOVE_FORWARD, MOVE_BACKWARD, MOVE_LEFT, MOVE_RIGHT -> ControlType.MOVE;
            case ROTATE, ROTATE_YAW, ROTATE_PITCH -> ControlType.ROTATE;
            case JUMP -> ControlType.JUMP;
            case SNEAK -> ControlType.SNEAK;
            case SPRINT -> ControlType.SPRINT;
            case CRAWL -> ControlType.CRAWL;
            case LEFT_CLICK -> ControlType.LEFT_CLICK;
            case RIGHT_CLICK -> ControlType.RIGHT_CLICK;
            case MIDDLE_CLICK -> ControlType.MIDDLE_CLICK;
            default -> null;
        };
    }
}