package com.myudog.myulib.api.ui;

import com.myudog.myulib.api.permission.PermissionLayer;

public interface ConfigurationUiBridge {
    void openFieldEditor(String fieldId);

    void openIdentityGroupEditor(String groupId);

    void openRoleGroupEditor(String groupId);

    void openTeamEditor(String teamId);

    void openPermissionEditor(PermissionLayer layer, String scopeId);
}

