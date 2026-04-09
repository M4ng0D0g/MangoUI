package com.myudog.myulib.api.ui;

import com.myudog.myulib.api.permission.PermissionLayer;

public final class ConfigurationUiRegistry {
    private static volatile ConfigurationUiBridge bridge;

    private ConfigurationUiRegistry() {
    }

    public static void setBridge(ConfigurationUiBridge bridge) {
        ConfigurationUiRegistry.bridge = bridge;
    }

    public static ConfigurationUiBridge bridge() {
        return bridge;
    }

    public static void openFieldEditor(String fieldId) {
        ConfigurationUiBridge current = bridge;
        if (current != null) {
            current.openFieldEditor(fieldId);
        }
    }

    public static void openIdentityGroupEditor(String groupId) {
        ConfigurationUiBridge current = bridge;
        if (current != null) {
            current.openIdentityGroupEditor(groupId);
        }
    }

    public static void openRoleGroupEditor(String groupId) {
        ConfigurationUiBridge current = bridge;
        if (current != null) {
            current.openRoleGroupEditor(groupId);
        }
    }

    public static void openTeamEditor(String teamId) {
        ConfigurationUiBridge current = bridge;
        if (current != null) {
            current.openTeamEditor(teamId);
        }
    }

    public static void openPermissionEditor(PermissionLayer layer, String scopeId) {
        ConfigurationUiBridge current = bridge;
        if (current != null) {
            current.openPermissionEditor(layer, scopeId);
        }
    }
}
