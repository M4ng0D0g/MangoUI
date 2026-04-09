package com.myudog.myulib.api.rolegroup;

import net.minecraft.server.MinecraftServer;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.UnaryOperator;

public final class RoleGroupManager {
    private static volatile RoleGroupStorage STORAGE = new NbtRoleGroupStorage();

    private RoleGroupManager() {
    }

    public static void install() {
        STORAGE.ensureLoaded();
    }

    public static void bindServer(MinecraftServer server) {
        STORAGE.bindServer(server);
    }

    public static void bindRoot(Path root) {
        STORAGE.bindRoot(root);
    }

    public static RoleGroupDefinition register(RoleGroupDefinition group) {
        return STORAGE.register(group);
    }

    public static RoleGroupDefinition update(String groupId, UnaryOperator<RoleGroupDefinition> updater) {
        return STORAGE.update(groupId, updater);
    }

    public static RoleGroupDefinition delete(String groupId) {
        return STORAGE.remove(groupId);
    }

    public static RoleGroupDefinition get(String groupId) {
        return STORAGE.get(groupId);
    }

    public static List<RoleGroupDefinition> groups() {
        return STORAGE.all();
    }

    public static Map<String, RoleGroupDefinition> snapshot() {
        return STORAGE.snapshot();
    }

    public static boolean assign(UUID playerId, String groupId) {
        return STORAGE.assign(playerId, groupId);
    }

    public static boolean revoke(UUID playerId, String groupId) {
        return STORAGE.revoke(playerId, groupId);
    }

    public static Set<String> groupIdsOf(UUID playerId) {
        return STORAGE.groupIdsOf(playerId);
    }

    public static List<RoleGroupDefinition> groupsOf(UUID playerId) {
        return STORAGE.groupsOf(playerId);
    }

    public static void clear() {
        STORAGE.clear();
    }

    public static RoleGroupDefinition create(RoleGroupDefinition group) {
        return register(group);
    }

    public static RoleGroupDefinition deleteRoleGroup(String groupId) {
        return delete(groupId);
    }

    public static List<RoleGroupDefinition> list() {
        return groups();
    }

    public static Map<String, RoleGroupDefinition> listMap() {
        return snapshot();
    }
}
