package com.myudog.myulib.api.core.debug;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * DebugLogManager
 *
 * 系統：核心除錯系統 (Core Debug System)
 * 角色：全域除錯日誌管理器，負責將特定功能的除錯訊息發送給已訂閱的玩家。
 * 類型：Manager / Utility
 *
 * 支援細粒度的除錯切換：
 * 1. 玩家層級：只有 `enable` 的玩家才會收到訊息。
 * 2. 功能層級：玩家可以選擇訂閱特定的 `DebugFeature` (如 ECS, Timer, Control 等)。
 * 此系統主要用於開發階段的即時反饋，訊息會直接發送到玩家的遊戲聊天視窗中。
 */
public final class DebugLogManager {

    public static final DebugLogManager INSTANCE = new DebugLogManager();

    /** 已啟用除錯模式的玩家 ID 集合。 */
    private final Set<UUID> ENABLED_PLAYERS = ConcurrentHashMap.newKeySet();

    /** 每個玩家個別訂閱的除錯功能集合。 */
    private final Map<UUID, EnumSet<DebugFeature>> PLAYER_FEATURES = new ConcurrentHashMap<>();

    private volatile MinecraftServer server;
    private volatile boolean installed;

    private DebugLogManager() {
    }

    /**
     * 安裝除錯系統，掛載伺服器生命週期事件。
     */
    public void install() {
        if (installed) {
            return;
        }
        installed = true;
        ServerLifecycleEvents.SERVER_STARTED.register(s -> server = s);
        ServerLifecycleEvents.SERVER_STOPPED.register(s -> {
            server = null;
            ENABLED_PLAYERS.clear();
            PLAYER_FEATURES.clear();
        });
    }

    /**
     * 為指定玩家啟用除錯模式。
     * 預設會訂閱所有除錯功能。
     *
     * @param playerId 玩家 UUID
     */
    public void enable(UUID playerId) {
        if (playerId == null) {
            return;
        }
        ENABLED_PLAYERS.add(playerId);
        PLAYER_FEATURES.computeIfAbsent(playerId, ignored -> EnumSet.allOf(DebugFeature.class));
    }

    /**
     * 為指定玩家停用除錯模式。
     *
     * @param playerId 玩家 UUID
     */
    public void disable(UUID playerId) {
        if (playerId == null) {
            return;
        }
        ENABLED_PLAYERS.remove(playerId);
    }

    public boolean isEnabled(UUID playerId) {
        return playerId != null && ENABLED_PLAYERS.contains(playerId);
    }

    /**
     * 切換玩家對特定功能的訂閱狀態。
     *
     * @param playerId 玩家 UUID
     * @param feature  除錯功能
     * @param enabled  是否啟用
     */
    public void setFeature(UUID playerId, DebugFeature feature, boolean enabled) {
        if (playerId == null || feature == null) {
            return;
        }
        EnumSet<DebugFeature> set = PLAYER_FEATURES.computeIfAbsent(playerId, ignored -> EnumSet.noneOf(DebugFeature.class));
        if (enabled) {
            set.add(feature);
        } else {
            set.remove(feature);
        }
    }

    /**
     * 一鍵切換玩家的所有訂閱功能。
     */
    public void setAll(UUID playerId, boolean enabled) {
        if (playerId == null) {
            return;
        }
        if (enabled) {
            PLAYER_FEATURES.put(playerId, EnumSet.allOf(DebugFeature.class));
        } else {
            PLAYER_FEATURES.put(playerId, EnumSet.noneOf(DebugFeature.class));
        }
    }

    /**
     * 獲取玩家目前訂閱的所有功能。
     */
    public Set<DebugFeature> getFeatures(UUID playerId) {
        EnumSet<DebugFeature> set = PLAYER_FEATURES.get(playerId);
        if (set == null) {
            return Set.of();
        }
        return Set.copyOf(set);
    }

    /**
     * 發送一條除錯日誌。
     * 系統會自動過濾未訂閱此功能的玩家。
     *
     * @param feature 訊息所屬的功能類別
     * @param message 訊息內容
     */
    public void log(DebugFeature feature, String message) {
        MinecraftServer current = server;
        if (current == null || feature == null || message == null || message.isBlank()) {
            return;
        }

        for (UUID playerId : Set.copyOf(ENABLED_PLAYERS)) {
            ServerPlayer player = current.getPlayerList().getPlayer(playerId);
            if (player == null) {
                ENABLED_PLAYERS.remove(playerId);
                continue;
            }
            EnumSet<DebugFeature> features = PLAYER_FEATURES.get(playerId);
            if (features == null || !features.contains(feature)) {
                continue;
            }
            player.sendSystemMessage(Component.literal("[Debug:" + feature.token() + "] " + message));
        }
    }
}

