package com.myudog.myulib.api.rolegroup;

import com.myudog.myulib.api.ui.ConfigurationUiBridge;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.UnaryOperator;

public final class RoleGroupAdminService {
    private RoleGroupAdminService() {
    }

    public static RoleGroupDefinition create(RoleGroupDefinition group) {
        return RoleGroupManager.register(group);
    }

    public static RoleGroupDefinition delete(String groupId) {
        return RoleGroupManager.delete(groupId);
    }

    public static RoleGroupDefinition update(String groupId, UnaryOperator<RoleGroupDefinition> updater) {
        return RoleGroupManager.update(groupId, updater);
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

    public static List<RoleGroupDefinition> groupsOf(UUID playerId) {
        return RoleGroupManager.groupsOf(playerId);
    }

    public static Map<String, RoleGroupDefinition> list() {
        return RoleGroupManager.snapshot();
    }

    public static void openEditor(String groupId, ConfigurationUiBridge ui) {
        if (ui != null) {
            ui.openRoleGroupEditor(groupId);
        }
    }
}
