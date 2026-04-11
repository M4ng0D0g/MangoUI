package com.myudog.myulib.api.permission;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 代表一個作用域 (例如: 全域、特定維度、特定場地) 內的所有權限資料。
 */
public class PermissionScope {
    // 將玩家與身分組分開管理
    private final Map<UUID, PermissionTable> playerTables = new HashMap<>();
    private final Map<String, PermissionTable> groupTables = new HashMap<>();

    // --- 管理接口 ---
    public PermissionTable forPlayer(UUID playerId) {
        return playerTables.computeIfAbsent(playerId, k -> new PermissionTable());
    }

    public PermissionTable forGroup(String groupName) {
        return groupTables.computeIfAbsent(groupName, k -> new PermissionTable());
    }

    // --- 解析核心 ---
    /**
     * 查詢此作用域內的權限。
     * 順序：Player特例 -> 玩家所屬的身分組 -> everyone預設組
     */
    public PermissionDecision resolve(UUID playerId, Iterable<String> playerGroups, PermissionAction action) {
        // 1. 查詢玩家特例
        if (playerTables.containsKey(playerId)) {
            PermissionDecision decision = playerTables.get(playerId).get(action);
            if (decision != PermissionDecision.UNSET) return decision;
        }

        // 2. 查詢玩家具有的身分組權限 (若有多個群組，依傳入的 Iterable 順序決定優先級)
        for (String group : playerGroups) {
            if (groupTables.containsKey(group)) {
                PermissionDecision decision = groupTables.get(group).get(action);
                if (decision != PermissionDecision.UNSET) return decision;
            }
        }

        // 3. 查詢此作用域的 "everyone" 預設權限
        if (groupTables.containsKey("everyone")) {
            return groupTables.get("everyone").get(action);
        }

        // 4. 若此層級皆未設定，回傳 UNSET，讓上層 Manager 決定往下查
        return PermissionDecision.UNSET;
    }
}