package com.myudog.myulib.api.permission;

import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;

import java.util.*;

public final class PermissionManager {

    // 核心改變：綁定唯一的儲存層實例，捨棄內部重複的 Map
    private static PermissionStorage STORAGE = new NbtPermissionStorage();

    private PermissionManager() {}

    // --- 初始化管線 ---

    public static void install() {
        STORAGE.ensureLoaded();
    }

    public static void bindServer(MinecraftServer server) {
        STORAGE.bindServer(server);
    }

    // --- 獲取 Scope 以便進行設定 ---

    public static PermissionScope global() {
        STORAGE.ensureLoaded();
        return STORAGE.getGlobalScope();
    }

    public static PermissionScope dimension(Identifier dimensionId) {
        STORAGE.ensureLoaded();
        // 如果該維度還沒有獨立的 Scope，則建立一個新的並放進 Storage 中
        return STORAGE.getDimensionScopes().computeIfAbsent(dimensionId, k -> {
            PermissionScope newScope = new PermissionScope();
            STORAGE.markDirty(); // 標記為需要存檔
            return newScope;
        });
    }

    public static PermissionScope field(Identifier fieldId) {
        STORAGE.ensureLoaded();
        // 如果該區域還沒有獨立的 Scope，則建立一個新的並放進 Storage 中
        return STORAGE.getFieldScopes().computeIfAbsent(fieldId, k -> {
            PermissionScope newScope = new PermissionScope();
            STORAGE.markDirty(); // 標記為需要存檔
            return newScope;
        });
    }

    // --- 輔助查詢 API ---

    public static Map<PermissionAction, PermissionDecision> getScopeMergedTable(
            PermissionScope scope, UUID playerId, List<String> playerGroups) {

        Map<PermissionAction, PermissionDecision> result = new EnumMap<>(PermissionAction.class);

        for (PermissionAction action : PermissionAction.values()) {
            result.put(action, scope.resolve(playerId, playerGroups, action));
        }
        return result;
    }

    public static Map<PermissionAction, PermissionDecision> getFinalPermissions(
            UUID playerId, List<String> playerGroups, Identifier fieldId, Identifier dimensionId) {

        Map<PermissionAction, PermissionDecision> finalRules = new EnumMap<>(PermissionAction.class);

        // 遍歷所有可能的行為
        for (PermissionAction action : PermissionAction.values()) {
            // 利用現有的 evaluate 管線，自動從 Field -> Dimension -> Global 查出最終結果
            PermissionDecision decision = evaluate(playerId, playerGroups, action, fieldId, dimensionId);
            finalRules.put(action, decision);
        }

        return finalRules;
    }

    // --- 終極評估管線 (Mixin 端調用) ---

    /**
     * 評估玩家對某個行為是否有權限
     */
    public static PermissionDecision evaluate(
            UUID playerId,
            List<String> playerGroups,
            PermissionAction action,
            Identifier fieldId,       // 若不在 Field 內則傳 null
            Identifier dimensionId) {

        STORAGE.ensureLoaded();
        PermissionDecision decision;

        // 1. 最高優先級：Field 作用域
        if (fieldId != null && STORAGE.getFieldScopes().containsKey(fieldId)) {
            decision = STORAGE.getFieldScopes().get(fieldId).resolve(playerId, playerGroups, action);
            if (decision != PermissionDecision.UNSET) return decision;
        }

        // 2. 次優先級：Dimension 作用域
        if (dimensionId != null && STORAGE.getDimensionScopes().containsKey(dimensionId)) {
            decision = STORAGE.getDimensionScopes().get(dimensionId).resolve(playerId, playerGroups, action);
            if (decision != PermissionDecision.UNSET) return decision;
        }

        // 3. 基礎優先級：Global 作用域
        decision = STORAGE.getGlobalScope().resolve(playerId, playerGroups, action);
        if (decision != PermissionDecision.UNSET) return decision;

        // 4. 全部都 UNSET 的終極預設值 (通常預設放行，由原版機制接管)
        return PermissionDecision.ALLOW;
    }

    // --- 資源管理 ---

    public static void save() {
        STORAGE.markDirty();
    }

    public static void clear() {
        STORAGE = new NbtPermissionStorage();
    }
}