package com.myudog.myulib.api.game.core;

import com.myudog.myulib.api.debug.DebugFeature;
import com.myudog.myulib.api.debug.DebugLogManager;
import com.myudog.myulib.api.effect.ISpatialEffectManager;
import com.myudog.myulib.api.effect.SpatialEffectEvents;
import com.myudog.myulib.api.effect.SpatialEffectManager;
import com.myudog.myulib.api.game.event.GameBlockBreakEvent;
import com.myudog.myulib.api.game.event.GameBlockInteractEvent;
import com.myudog.myulib.api.game.event.GameEntityDamageEvent;
import com.myudog.myulib.api.game.event.GameEntityDeathEvent;
import com.myudog.myulib.api.game.event.GameEntityInteractEvent;
import com.myudog.myulib.api.game.object.IGameObject;
import com.myudog.myulib.api.game.object.impl.BlockGameObject;
import com.myudog.myulib.api.game.state.GameState;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 核心遊戲管理中心。
 * [v0.4.2] 重構修正：
 * 1. 統一採用 Integer ID 作為唯一的互動識別碼，徹底移除 ShortIdRegistry。
 * 2. 負責分配遞增的 Session Identifier (如 myulib:session_1) 並注入資料層。
 * 3. 整合 GameInstance 的資源清理機制。
 */
public final class GameManager {
    private static final Map<Identifier, GameDefinition<?, ?, ?>> DEFINITIONS = new LinkedHashMap<>();
    private static final Map<Integer, GameInstance<?, ?, ?>> INSTANCES = new ConcurrentHashMap<>();
    private static final Map<String, Integer> INSTANCE_TOKENS = new ConcurrentHashMap<>();
    private static final Map<UUID, Integer> playerToInstanceMap = new ConcurrentHashMap<>();

    // 🌟 效能優化：實體反向對應表 (Entity UUID -> Instance ID)
    // 當物件生成或玩家加入時寫入，死亡或離開時移除，O(1) 複雜度極速查詢
    private static final Map<UUID, Integer> entityToInstanceMap = new ConcurrentHashMap<>();
    private static final ISpatialEffectManager GLOBAL_EFFECT_MANAGER = new SpatialEffectManager();

    private GameManager() {
        // 工具類別，禁止實例化
    }

    public static void install() {
        SpatialEffectEvents.register(GLOBAL_EFFECT_MANAGER);

        ServerPlayerEvents.LEAVE.register((player) -> {
            Integer instanceId = playerToInstanceMap.get(player.getUUID());
            if (instanceId == null) {
                return;
            }

            GameInstance<?, ?, ?> instance = INSTANCES.get(instanceId);
            if (instance == null) {
                playerToInstanceMap.remove(player.getUUID());
                return;
            }

            if (!instance.isStarted()) {
                unassignPlayer(player.getUUID());
            }
        });
    }

    public static ISpatialEffectManager getGlobalEffectManager() {
        return GLOBAL_EFFECT_MANAGER;
    }

    // --- Definition 管理 ---

    public static void register(GameDefinition<?, ?, ?> definition) {
        Objects.requireNonNull(definition, "GameDefinition 不能為空");
        DEFINITIONS.put(definition.getId(), definition);
        DebugLogManager.INSTANCE.log(DebugFeature.GAME, "register definition id=" + definition.getId());
    }

    public static GameDefinition<?, ?, ?> unregister(Identifier gameId) {
        DebugLogManager.INSTANCE.log(DebugFeature.GAME, "unregister definition id=" + gameId);
        return DEFINITIONS.remove(gameId);
    }

    public static boolean hasDefinition(Identifier gameId) {
        return DEFINITIONS.containsKey(gameId);
    }

    @SuppressWarnings("unchecked")
    public static <C extends GameConfig, D extends GameData, S extends GameState> GameDefinition<C, D, S> definition(Identifier gameId) {
        return (GameDefinition<C, D, S>) DEFINITIONS.get(gameId);
    }

    // --- Instance 管理 ---

    @SuppressWarnings("unchecked")
    public static <C extends GameConfig, D extends GameData, S extends GameState> GameInstance<C, D, S> createInstance(Identifier gameId, String instanceToken, C config, ServerLevel level) {
        GameDefinition<C, D, S> definition = definition(gameId);
        if (definition == null) {
            throw new IllegalArgumentException("找不到該遊戲藍圖 (Unknown game definition): " + gameId);
        }

        String normalizedToken = normalizeInstanceToken(instanceToken);
        if (normalizedToken.isBlank()) {
            throw new IllegalArgumentException("GameInstanceId 不得為空");
        }

        if (INSTANCE_TOKENS.containsKey(normalizedToken)) {
            throw new IllegalArgumentException("GameInstanceId 已存在: " + normalizedToken);
        }

        Objects.requireNonNull(config, "建立遊戲必須提供 GameConfig 實例");
        Objects.requireNonNull(level, "建立遊戲必須提供 ServerLevel");

        int instanceId = resolveRequestedInstanceId(normalizedToken);
        if (INSTANCES.containsKey(instanceId)) {
            throw new IllegalArgumentException("GameInstanceId 衝突: " + normalizedToken + " -> " + instanceId);
        }

        GameInstance<C, D, S> instance = definition.createInstance(instanceId, config, level);

        Identifier sessionIdentifier = GameScopeTokens.scoped(
                definition.modToken(),
                definition.gameToken(),
                "session",
                normalizedToken
        );
        instance.setupIdentity(sessionIdentifier);

        INSTANCES.put(instanceId, instance);
        INSTANCE_TOKENS.put(normalizedToken, instanceId);

        DebugLogManager.INSTANCE.log(DebugFeature.GAME,
                "create instance id=" + instanceId + ", token=" + normalizedToken + ", definition=" + gameId + ", sessionIdentifier=" + sessionIdentifier);
        return instance;
    }

    @SuppressWarnings("unchecked")
    public static <C extends GameConfig, D extends GameData, S extends GameState> GameInstance<C, D, S> createInstance(Identifier gameId, String instanceToken, ServerLevel level) {
        GameDefinition<C, D, S> definition = definition(gameId);
        if (definition == null) {
            throw new IllegalArgumentException("找不到該遊戲藍圖 (Unknown game definition): " + gameId);
        }
        return createInstance(gameId, instanceToken, (C) GameConfig.empty(), level);
    }

    public static <C extends GameConfig> boolean setInstanceConfig(String instanceToken, C config) {
        Objects.requireNonNull(config, "config 不得為空");
        GameInstance<?, ?, ?> instance = getInstance(instanceToken);
        if (instance == null || instance.isStarted()) {
            return false;
        }

        @SuppressWarnings("unchecked")
        GameInstance<C, ?, ?> typed = (GameInstance<C, ?, ?>) instance;
        typed.setConfig(config);
        return true;
    }

    public static OptionalInt resolveInstanceId(String instanceToken) {
        String normalizedToken = normalizeInstanceToken(instanceToken);
        if (normalizedToken.isBlank()) {
            return OptionalInt.empty();
        }

        Integer fromToken = INSTANCE_TOKENS.get(normalizedToken);
        if (fromToken != null) {
            return OptionalInt.of(fromToken);
        }

        Integer numeric = parseNumericInstanceId(normalizedToken);
        if (numeric != null && INSTANCES.containsKey(numeric)) {
            return OptionalInt.of(numeric);
        }

        return OptionalInt.empty();
    }

    public static List<String> instanceTokens() {
        pruneOrphanedTokens();
        return List.copyOf(INSTANCE_TOKENS.keySet());
    }

    public static GameInstance<?, ?, ?> getInstance(int instanceId) {
        return INSTANCES.get(instanceId);
    }

    public static GameInstance<?, ?, ?> getInstance(String instanceToken) {
        OptionalInt resolved = resolveInstanceId(instanceToken);
        return resolved.isPresent() ? INSTANCES.get(resolved.getAsInt()) : null;
    }

    public static List<GameInstance<?, ?, ?>> getInstances() {
        return List.copyOf(INSTANCES.values());
    }

    public static List<GameInstance<?, ?, ?>> getInstances(Identifier gameId) {
        return INSTANCES.values().stream()
                .filter(instance -> instance.getDefinition().getId().equals(gameId))
                .toList();
    }

    /**
     * 銷毀指定的遊戲實例。
     * 🌟 修正：直接觸發 GameInstance 的 Scoped 資源自動清理機制。
     */
    public static boolean destroyInstance(int instanceId) {
        GameInstance<?, ?, ?> instance = INSTANCES.remove(instanceId);
        if (instance != null) {
            removeTokenByInstanceId(instanceId);
            unassignPlayersInInstance(instanceId);
            // 觸發內部清理，包含資料重置與 Scoped 外部資源 (隊伍、區域) 註銷
            instance.destroy();
            DebugLogManager.INSTANCE.log(DebugFeature.GAME, "destroy instance id=" + instanceId);
            return true;
        }
        DebugLogManager.INSTANCE.log(DebugFeature.GAME, "destroy miss instance id=" + instanceId);
        return false;
    }

    public static boolean destroyInstance(String instanceToken) {
        OptionalInt resolved = resolveInstanceId(instanceToken);
        return resolved.isPresent() && destroyInstance(resolved.getAsInt());
    }

    public static boolean startInstance(int instanceId) {
        GameInstance<?, ?, ?> instance = INSTANCES.get(instanceId);
        if (instance == null || !instance.isEnabled() || !instance.isInitialized()) {
            return false;
        }
        return instance.start();
    }

    public static boolean initInstance(int instanceId) {
        GameInstance<?, ?, ?> instance = INSTANCES.get(instanceId);
        if (instance == null || !instance.isEnabled()) {
            return false;
        }
        return instance.init();
    }

    public static boolean startInstance(String instanceToken) {
        OptionalInt resolved = resolveInstanceId(instanceToken);
        return resolved.isPresent() && startInstance(resolved.getAsInt());
    }

    public static boolean initInstance(String instanceToken) {
        OptionalInt resolved = resolveInstanceId(instanceToken);
        return resolved.isPresent() && initInstance(resolved.getAsInt());
    }

    public static boolean shutdownInstance(int instanceId) {
        GameInstance<?, ?, ?> instance = INSTANCES.get(instanceId);
        if (instance == null || !instance.isEnabled()) {
            return false;
        }

        boolean ended = instance.shutdown();
        if (ended) {
            unassignPlayersInInstance(instanceId);
        }
        return ended;
    }

    public static boolean shutdownInstance(String instanceToken) {
        OptionalInt resolved = resolveInstanceId(instanceToken);
        return resolved.isPresent() && shutdownInstance(resolved.getAsInt());
    }

    public static OptionalInt instanceOf(UUID playerId) {
        Integer instanceId = playerId == null ? null : playerToInstanceMap.get(playerId);
        return instanceId == null ? OptionalInt.empty() : OptionalInt.of(instanceId);
    }

    public static boolean joinPlayer(int instanceId, UUID playerId, Identifier requestedTeamId) {
        GameInstance<?, ?, ?> instance = INSTANCES.get(instanceId);
        if (instance == null || !instance.isEnabled() || !instance.isInitialized()) {
            return false;
        }

        if (playerId == null) {
            return false;
        }

        Integer existing = playerToInstanceMap.get(playerId);
        if (existing != null && existing != instanceId) {
            return false;
        }

        boolean joined = instance.joinPlayer(playerId, requestedTeamId);
        if (joined) {
            playerToInstanceMap.put(playerId, instanceId);
        }
        return joined;
    }

    public static boolean joinPlayer(String instanceToken, UUID playerId, Identifier requestedTeamId) {
        OptionalInt resolved = resolveInstanceId(instanceToken);
        return resolved.isPresent() && joinPlayer(resolved.getAsInt(), playerId, requestedTeamId);
    }

    public static void unassignPlayer(UUID playerId) {
        Integer instanceId = playerToInstanceMap.remove(playerId);
        if (instanceId == null) {
            return;
        }

        GameInstance<?, ?, ?> instance = INSTANCES.get(instanceId);
        if (instance == null) {
            return;
        }

        try {
            instance.getData().removePlayerFromTeams(playerId);
        } catch (IllegalStateException ignored) {
            // Data is not initialized yet; only index cleanup is required.
        }
    }

    public static boolean resetInstance(String instanceToken) {
        OptionalInt resolved = resolveInstanceId(instanceToken);
        if (resolved.isEmpty()) {
            return false;
        }

        GameInstance<?, ?, ?> instance = INSTANCES.get(resolved.getAsInt());
        if (instance == null) {
            return false;
        }

        instance.resetState();
        return true;
    }

    // --- 生命週期 ---

    public static void tickAll() {
        for (Map.Entry<Integer, GameInstance<?, ?, ?>> entry : INSTANCES.entrySet()) {
            GameInstance<?, ?, ?> instance = entry.getValue();
            if (!instance.isEnabled()) {
                // 惰性清理已停用的實例
                INSTANCES.remove(entry.getKey());
                removeTokenByInstanceId(entry.getKey());
                DebugLogManager.INSTANCE.log(DebugFeature.GAME, "auto-prune disabled instance id=" + entry.getKey());
                continue;
            }
            instance.tick();
        }
    }

    public static void handleEntityHurt(LivingEntity victim, DamageSource source) {
        handleEntityDamage(victim, source, 0.0f);
    }

    public static void handleEntityDamage(LivingEntity victim, DamageSource source, float amount) {
        Integer victimInstanceId = entityToInstanceMap.get(victim.getUUID());
        if (victimInstanceId == null) {
            return;
        }

        GameInstance<?, ?, ?> instance = INSTANCES.get(victimInstanceId);
        if (instance == null || !instance.isEnabled() || !instance.isStarted()) {
            entityToInstanceMap.remove(victim.getUUID());
            return;
        }

        instance.getEventBus().dispatch(new GameEntityDamageEvent(victim, source, amount));
    }

    public static void handleEntityDeath(LivingEntity victim, DamageSource source) {
        Integer victimInstanceId = entityToInstanceMap.get(victim.getUUID());
        if (victimInstanceId == null) {
            return;
        }

        GameInstance<?, ?, ?> instance = INSTANCES.get(victimInstanceId);
        if (instance == null || !instance.isEnabled() || !instance.isStarted()) {
            entityToInstanceMap.remove(victim.getUUID());
            return;
        }

        instance.getEventBus().dispatch(new GameEntityDeathEvent(victim, source));
        entityToInstanceMap.remove(victim.getUUID());
    }

    public static boolean handleBlockBreak(ServerPlayer player, BlockPos pos, ServerLevel level) {
        GameInstance<?, ?, ?> instance = resolveInstanceForBlock(player, pos, level);
        if (instance == null || !instance.isEnabled() || !instance.isStarted()) {
            return false;
        }

        GameBlockBreakEvent event = new GameBlockBreakEvent(player, pos, level);
        instance.getEventBus().dispatch(event);
        return event.isCanceled();
    }

    public static boolean handleBlockInteract(ServerPlayer player, BlockPos pos, ServerLevel level) {
        GameInstance<?, ?, ?> instance = resolveInstanceForBlock(player, pos, level);
        if (instance == null || !instance.isEnabled() || !instance.isStarted()) {
            return false;
        }

        GameBlockInteractEvent event = new GameBlockInteractEvent(player, pos, level);
        instance.getEventBus().dispatch(event);
        return event.isCanceled();
    }

    public static boolean handleEntityInteract(ServerPlayer player, Entity target, InteractionHand hand) {
        Integer instanceId = entityToInstanceMap.get(target.getUUID());
        if (instanceId == null) {
            return false;
        }

        GameInstance<?, ?, ?> instance = INSTANCES.get(instanceId);
        if (instance == null || !instance.isEnabled() || !instance.isStarted()) {
            return false;
        }

        GameEntityInteractEvent event = new GameEntityInteractEvent(player, target, hand);
        instance.getEventBus().dispatch(event);
        return event.isCanceled();
    }

    private static GameInstance<?, ?, ?> resolveInstanceForBlock(ServerPlayer player, BlockPos pos, ServerLevel level) {
        Integer mappedInstanceId = entityToInstanceMap.get(player.getUUID());
        if (mappedInstanceId != null) {
            return INSTANCES.get(mappedInstanceId);
        }

        for (GameInstance<?, ?, ?> candidate : INSTANCES.values()) {
            if (!candidate.isEnabled() || !candidate.isStarted() || candidate.getLevel() != level) {
                continue;
            }

            for (IGameObject runtimeObject : candidate.getData().getRuntimeObjects()) {
                if (runtimeObject instanceof BlockGameObject blockObject && blockObject.containsPos(pos)) {
                    return candidate;
                }
            }
        }

        return null;
    }



    // --- 給 GameObject 註冊實體用的 API ---
    public static void registerEntity(UUID entityUuid, int instanceId) {
        if (!INSTANCES.containsKey(instanceId)) {
            throw new IllegalArgumentException("instance 不存在: " + instanceId);
        }
        Integer existing = entityToInstanceMap.putIfAbsent(entityUuid, instanceId);
        if (existing != null && existing != instanceId) {
            throw new IllegalStateException("entity 已註冊於不同 instance: " + entityUuid + " -> " + existing + ", attempted=" + instanceId);
        }
    }

    public static void unregisterEntity(UUID entityUuid) {
        entityToInstanceMap.remove(entityUuid);
    }

    private static String normalizeInstanceToken(String token) {
        return token == null ? "" : token.trim().toLowerCase();
    }

    private static Integer parseNumericInstanceId(String token) {
        try {
            int parsed = Integer.parseInt(token);
            return parsed > 0 ? parsed : null;
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private static int resolveRequestedInstanceId(String normalizedToken) {
        Integer numeric = parseNumericInstanceId(normalizedToken);
        if (numeric != null) {
            return numeric;
        }

        int hash = normalizedToken.hashCode();
        if (hash == Integer.MIN_VALUE) {
            hash = 1;
        }
        int derived = Math.abs(hash);
        return derived == 0 ? 1 : derived;
    }

    private static void removeTokenByInstanceId(int instanceId) {
        INSTANCE_TOKENS.entrySet().removeIf(entry -> entry.getValue() == instanceId);
    }

    private static void pruneOrphanedTokens() {
        INSTANCE_TOKENS.entrySet().removeIf(entry -> !INSTANCES.containsKey(entry.getValue()));
    }

    private static void unassignPlayersInInstance(int instanceId) {
        playerToInstanceMap.entrySet().removeIf(entry -> entry.getValue() == instanceId);
    }
}

