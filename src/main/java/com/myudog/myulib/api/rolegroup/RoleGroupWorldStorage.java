package com.myudog.myulib.api.rolegroup;

import net.minecraft.server.MinecraftServer;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.UnaryOperator;

@Deprecated(forRemoval = false)
final class RoleGroupWorldStorage {
    private RoleGroupWorldStorage() {
    }

    static void bindServer(MinecraftServer server) {
        RoleGroupManager.bindServer(server);
    }

    static void bindRoot(Path root) {
        RoleGroupManager.bindRoot(root);
    }

    static void ensureLoaded() {
        RoleGroupManager.install();
    }

    static RoleGroupDefinition putGroup(RoleGroupDefinition group) {
        return RoleGroupManager.register(group);
    }

    static RoleGroupDefinition updateGroup(String groupId, UnaryOperator<RoleGroupDefinition> updater) {
        return RoleGroupManager.update(groupId, updater);
    }

    static RoleGroupDefinition removeGroup(String groupId) {
        return RoleGroupManager.delete(groupId);
    }

    static RoleGroupDefinition getGroup(String groupId) {
        return RoleGroupManager.get(groupId);
    }

    static List<RoleGroupDefinition> allGroups() {
        return RoleGroupManager.groups();
    }

    static Map<String, RoleGroupDefinition> snapshotGroups() {
        return RoleGroupManager.snapshot();
    }

    static boolean assign(UUID playerId, String groupId) {
        return RoleGroupManager.assign(playerId, groupId);
    }

    static boolean revoke(UUID playerId, String groupId) {
        return RoleGroupManager.revoke(playerId, groupId);
    }

    static Set<String> groupIdsOf(UUID playerId) {
        return RoleGroupManager.groupIdsOf(playerId);
    }

    static List<RoleGroupDefinition> groupsOf(UUID playerId) {
        return RoleGroupManager.groupsOf(playerId);
    }

    static void clearMemory() {
        RoleGroupManager.clear();
    }
}
