package com.myudog.myulib.api.ui;

import com.myudog.myulib.api.permission.ScopeLayer;

public interface ConfigurationUiBridge {
    void openFieldEditor(String fieldId);

    void openIdentityGroupEditor(String groupId);

    void openRoleGroupEditor(String groupId);

    void openTeamEditor(String teamId);

    void openPermissionEditor(ScopeLayer layer, String scopeId);
}

