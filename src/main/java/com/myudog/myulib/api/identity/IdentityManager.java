package com.myudog.myulib.api.identity;

import com.myudog.myulib.api.rolegroup.RoleGroupDefinition;
import com.myudog.myulib.api.rolegroup.RoleGroupManager;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.UnaryOperator;

public final class IdentityManager {
    private IdentityManager() {
    }

    public static void install() {
        RoleGroupManager.install();
    }

    public static IdentityGroupDefinition register(IdentityGroupDefinition group) {
        RoleGroupDefinition created = RoleGroupManager.register(RoleGroupDefinition.fromIdentityDefinition(group));
        return created.toIdentityDefinition();
    }

    public static IdentityGroupDefinition update(String groupId, UnaryOperator<IdentityGroupDefinition> updater) {
        RoleGroupDefinition updated = RoleGroupManager.update(groupId, current -> {
            IdentityGroupDefinition identityCurrent = current.toIdentityDefinition();
            IdentityGroupDefinition next = updater.apply(identityCurrent);
            return next == null ? null : RoleGroupDefinition.fromIdentityDefinition(next);
        });
        return updated == null ? null : updated.toIdentityDefinition();
    }

    public static IdentityGroupDefinition unregister(String groupId) {
        RoleGroupDefinition removed = RoleGroupManager.delete(groupId);
        return removed == null ? null : removed.toIdentityDefinition();
    }

    public static IdentityGroupDefinition get(String groupId) {
        RoleGroupDefinition group = RoleGroupManager.get(groupId);
        return group == null ? null : group.toIdentityDefinition();
    }

    public static List<IdentityGroupDefinition> groups() {
        return RoleGroupManager.groups().stream().map(RoleGroupDefinition::toIdentityDefinition).toList();
    }

    public static Map<String, IdentityGroupDefinition> snapshot() {
        return RoleGroupManager.snapshot().entrySet().stream().collect(java.util.stream.Collectors.toUnmodifiableMap(Map.Entry::getKey, entry -> entry.getValue().toIdentityDefinition()));
    }

    public static boolean assign(UUID playerId, String groupId) {
        return RoleGroupManager.assign(playerId, groupId);
    }

    public static boolean revoke(UUID playerId, String groupId) {
        return RoleGroupManager.revoke(playerId, groupId);
    }

    public static Set<String> groupIdsOf(UUID playerId) {
        return RoleGroupManager.groupIdsOf(playerId);
    }

    public static List<IdentityGroupDefinition> groupsOf(UUID playerId) {
        return RoleGroupManager.groupsOf(playerId).stream().map(RoleGroupDefinition::toIdentityDefinition).toList();
    }

    public static void clear() {
        RoleGroupManager.clear();
    }
}
