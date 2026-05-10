package com.myudog.myulib.api.core.control;

import com.myudog.myulib.api.core.control.network.ServerControlNetworking;
import net.minecraft.server.level.ServerPlayer;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 玩家輸入權限閘門 (Player Input Gate)
 * <p>
 * 獨立於控制綁定系統之外，專門管理「哪些輸入意圖被允許/拒絕」。
 * 無論玩家目前是否綁定了任何實體，輸入意圖均須先通過此閘門。
 * <p>
 * <b>使用場景：</b>
 * <ul>
 *   <li>「暈眩」：deny(MOVE, ROTATE)</li>
 *   <li>「禁錮」：deny(MOVE, JUMP, SPRINT)</li>
 *   <li>「繳械」：deny(LEFT_CLICK, RIGHT_CLICK)</li>
 *   <li>「過場動畫」：deny(ALL)</li>
 * </ul>
 * <p>
 * <b>底層選擇：</b>
 * <ul>
 *   <li>Server 端以 {@code EnumSet<ControlType>} 儲存，底層為 long bitmask，O(1) 查詢</li>
 *   <li>同步至 Client 端時壓縮為單一 {@code int} bitmask 封包</li>
 * </ul>
 */
public final class PlayerInputGate {

    public static final PlayerInputGate INSTANCE = new PlayerInputGate();

    /** playerUuid → 被拒絕的 ControlType 集合 (EnumSet，底層為 long bitmask) */
    private final ConcurrentHashMap<UUID, EnumSet<ControlType>> denied = new ConcurrentHashMap<>();

    private PlayerInputGate() {}

    // ─────────────────────────────────────────────────────────────────────────
    // Public Mutation API
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * 拒絕指定玩家的特定輸入。
     * 若原本就已被拒絕則為 no-op。
     * <p>自動同步 bitmask 封包至 Client。
     *
     * @param player 目標玩家
     * @param types  要拒絕的輸入類型（可多個）
     */
    public void deny(ServerPlayer player, ControlType... types) {
        if (player == null || types.length == 0) return;
        UUID id = player.getUUID();
        EnumSet<ControlType> set = denied.computeIfAbsent(id, k -> EnumSet.noneOf(ControlType.class));
        for (ControlType t : types) set.add(t);
        sync(player);
    }

    /**
     * 拒絕指定玩家的所有輸入（過場動畫全鎖）。
     */
    public void denyAll(ServerPlayer player) {
        if (player == null) return;
        denied.put(player.getUUID(), EnumSet.allOf(ControlType.class));
        sync(player);
    }

    /**
     * 解除指定玩家的特定輸入拒絕。
     *
     * @param player 目標玩家
     * @param types  要解除拒絕的輸入類型（可多個）
     */
    public void grant(ServerPlayer player, ControlType... types) {
        if (player == null || types.length == 0) return;
        UUID id = player.getUUID();
        EnumSet<ControlType> set = denied.get(id);
        if (set == null) return;
        for (ControlType t : types) set.remove(t);
        if (set.isEmpty()) denied.remove(id);
        sync(player);
    }

    /**
     * 解除指定玩家的所有輸入拒絕（全部恢復）。
     */
    public void grantAll(ServerPlayer player) {
        if (player == null) return;
        denied.remove(player.getUUID());
        sync(player);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Query API  (O(1)，底層 EnumSet 為 long bit operation)
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * 查詢某玩家的特定輸入是否被拒絕。
     * <b>O(1)</b> — 底層是 long 位元運算。
     */
    public boolean isDenied(UUID playerId, ControlType type) {
        EnumSet<ControlType> set = denied.get(playerId);
        return set != null && set.contains(type);
    }

    /** @see #isDenied(UUID, ControlType) */
    public boolean isDenied(ServerPlayer player, ControlType type) {
        return player != null && isDenied(player.getUUID(), type);
    }

    /**
     * 查詢某玩家是否允許特定輸入（isDenied 的語義反轉）。
     */
    public boolean isAllowed(UUID playerId, ControlType type) {
        return !isDenied(playerId, type);
    }

    /**
     * 取得指定玩家目前所有被拒絕的輸入集合（不可修改視圖）。
     */
    public Set<ControlType> getDeniedTypes(UUID playerId) {
        EnumSet<ControlType> set = denied.get(playerId);
        return set == null ? Set.of() : Collections.unmodifiableSet(set);
    }

    /**
     * 判斷玩家是否有任何輸入被拒絕。
     */
    public boolean hasAnyDenied(UUID playerId) {
        EnumSet<ControlType> set = denied.get(playerId);
        return set != null && !set.isEmpty();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Lifecycle
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * 玩家斷線時清理其紀錄（由 ControlLifecycleListener 呼叫）。
     */
    public void onPlayerDisconnect(UUID playerId) {
        denied.remove(playerId);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Bitmask Encoding & Network Sync
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * 將 EnumSet 編碼為單一 int bitmask（ordinal 對應 bit 位）。
     * 最多支援 32 種 ControlType（目前 8 種，充裕）。
     */
    public int encodeMask(UUID playerId) {
        EnumSet<ControlType> set = denied.get(playerId);
        if (set == null || set.isEmpty()) return 0;
        int mask = 0;
        for (ControlType t : set) mask |= (1 << t.ordinal());
        return mask;
    }

    /**
     * 從 bitmask 還原 EnumSet（Client 端解碼用）。
     */
    public static EnumSet<ControlType> decodeMask(int mask) {
        EnumSet<ControlType> result = EnumSet.noneOf(ControlType.class);
        for (ControlType t : ControlType.values()) {
            if ((mask & (1 << t.ordinal())) != 0) result.add(t);
        }
        return result;
    }

    /** 同步當前拒絕狀態到 Client */
    private void sync(ServerPlayer player) {
        UUID id = player.getUUID();
        boolean isControlling = ControlManager.INSTANCE.isController(id);
        boolean isControlled  = ControlManager.INSTANCE.isControlledTarget(id);
        ServerControlNetworking.syncControlState(player, encodeMask(id), isControlling, isControlled);
    }
}
