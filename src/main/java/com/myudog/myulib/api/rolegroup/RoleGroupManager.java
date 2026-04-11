package com.myudog.myulib.api.rolegroup;

import net.minecraft.server.MinecraftServer;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.UnaryOperator;

public final class RoleGroupManager {

    private static RoleGroupStorage STORAGE = new NbtRoleGroupStorage();

    private RoleGroupManager() {}

    public static void install() {
        STORAGE.ensureLoaded();

        // 系統啟動時，確保預設的 everyone 身分組存在
        if (get("everyone") == null) {
            register(new RoleGroupDefinition("everyone", "所有人", -999, Map.of(), Set.of()));
        }
    }

    public static void bindServer(MinecraftServer server) { STORAGE.bindServer(server); }
    public static void bindRoot(Path root) { STORAGE.bindRoot(root); }

    public static RoleGroupDefinition register(RoleGroupDefinition group) { return STORAGE.register(group); }
    public static RoleGroupDefinition update(String groupId, UnaryOperator<RoleGroupDefinition> updater) { return STORAGE.update(groupId, updater); }
    public static RoleGroupDefinition delete(String groupId) { return STORAGE.remove(groupId); }
    public static RoleGroupDefinition get(String groupId) { return STORAGE.get(groupId); }
    public static List<RoleGroupDefinition> groups() { return STORAGE.all(); }

    public static boolean assign(UUID playerId, String groupId) { return STORAGE.assign(playerId, groupId); }
    public static boolean revoke(UUID playerId, String groupId) { return STORAGE.revoke(playerId, groupId); }

    // --- 🎯 雙向查詢系統 ---

    /**
     * 1. [群組找玩家] 取得擁有特定身分組的所有玩家 UUID
     */
    public static Set<UUID> getPlayersInGroup(String groupId) {
        return STORAGE.getPlayersInGroup(groupId);
    }

    /**
     * 2. [玩家找群組] 取得玩家擁有的身分組 ID 列表 (供 PermissionManager 使用)
     * 特性：自動依照 priority 由高到低排序，且必定包含 "everyone" 在最後面。
     */
    public static List<String> getSortedGroupIdsOf(UUID playerId) {
        List<RoleGroupDefinition> groups = STORAGE.groupsOf(playerId);

        List<String> sortedIds = new ArrayList<>();
        for (RoleGroupDefinition def : groups) {
            sortedIds.add(def.id());
        }

        // 強制確保 everyone 在列表的最尾端 (最低優先級)
        sortedIds.remove("everyone");
        sortedIds.add("everyone");

        return sortedIds;
    }

    public static void save() {
        STORAGE.markDirty();
    }

    public static void clear() {
        STORAGE = new NbtRoleGroupStorage();
    }
}