package com.myudog.myulib.api.core.debug;

import net.minecraft.server.level.ServerPlayer;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * DebugTraceManager
 *
 * 系統：核心除錯系統 (Core Debug System)
 * 角色：鏈式追蹤管理器，用於記錄並輸出一段連續操作的詳細步驟（如權限校驗流程、複雜事件傳遞）。
 * 類型：Manager / Utility
 *
 * 不同於單行的 `DebugLogManager`，Trace 系統允許開發者開啟一個「會話 (Session)」，
 * 記錄中間的多個步驟 (step)，最後統一輸出完整的流程日誌。
 */
public final class DebugTraceManager {

    public static final DebugTraceManager INSTANCE = new DebugTraceManager();

    /** 已啟用追蹤模式的玩家 ID 集合。 */
    private final Set<UUID> ENABLED = ConcurrentHashMap.newKeySet();

    /** 正在進行中的追蹤會話緩存。 */
    private final Map<UUID, StringBuilder> ACTIVE = new ConcurrentHashMap<>();

    private DebugTraceManager() {
    }

    /**
     * 為指定玩家啟用追蹤模式。
     */
    public void enable(UUID playerId) {
        if (playerId != null) {
            ENABLED.add(playerId);
        }
    }

    /**
     * 為指定玩家停用追蹤模式。
     */
    public void disable(UUID playerId) {
        if (playerId != null) {
            ENABLED.remove(playerId);
            ACTIVE.remove(playerId);
        }
    }

    public boolean isEnabled(UUID playerId) {
        return playerId != null && ENABLED.contains(playerId);
    }

    /**
     * 開始一個新的追蹤會話。
     *
     * @param player 目標玩家
     * @param title  會話標題
     */
    public void begin(ServerPlayer player, String title) {
        if (player == null || !isEnabled(player.getUUID())) {
            return;
        }
        StringBuilder builder = new StringBuilder();
        builder.append("[Trace] ").append(title == null ? "interaction" : title).append('\n');
        ACTIVE.put(player.getUUID(), builder);
    }

    /**
     * 在當前會話中記錄一個步驟。
     *
     * @param player 目標玩家
     * @param line   步驟描述
     */
    public void step(ServerPlayer player, String line) {
        if (player == null || !isEnabled(player.getUUID())) {
            return;
        }
        StringBuilder builder = ACTIVE.computeIfAbsent(player.getUUID(), ignored -> new StringBuilder("[Trace] interaction\n"));
        builder.append("- ").append(line == null ? "-" : line).append('\n');
    }

    /**
     * 結束會話並將完整的追蹤記錄發送給玩家。
     *
     * @param player 目標玩家
     * @param result 最終結果描述
     */
    public void end(ServerPlayer player, String result) {
        if (player == null || !isEnabled(player.getUUID())) {
            return;
        }
        StringBuilder builder = ACTIVE.remove(player.getUUID());
        if (builder == null) {
            builder = new StringBuilder("[Trace] interaction\n");
        }
        builder.append("=> ").append(result == null ? "done" : result);
        player.sendSystemMessage(net.minecraft.network.chat.Component.literal(builder.toString()));
    }
}

