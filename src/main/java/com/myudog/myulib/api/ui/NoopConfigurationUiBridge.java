package com.myudog.myulib.api.ui;

import com.myudog.myulib.api.permission.ScopeLayer;

public enum NoopConfigurationUiBridge implements ConfigurationUiBridge {
    INSTANCE;

    @Override
    public void openFieldEditor(String fieldId) {
    }

    @Override
    public void openIdentityGroupEditor(String groupId) {
    }

    @Override
    public void openRoleGroupEditor(String groupId) {
    }

    @Override
    public void openTeamEditor(String teamId) {
    }

    @Override
    public void openPermissionEditor(ScopeLayer layer, String scopeId) {
    }
}
