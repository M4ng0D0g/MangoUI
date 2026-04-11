package com.myudog.myulib.client.api.ui;

import com.myudog.myulib.api.permission.ScopeLayer;
import com.myudog.myulib.api.ui.ConfigurationUiBridge;
import java.util.Objects;

public final class ConfigurationUiBridgeClient implements ConfigurationUiBridge {
    private String lastFieldId;
    private String lastIdentityGroupId;
    private String lastRoleGroupId;
    private String lastTeamId;
    private ScopeLayer lastPermissionLayer;
    private String lastPermissionScopeId;

    @Override
    public void openFieldEditor(String fieldId) {
        lastFieldId = Objects.requireNonNullElse(fieldId, "");
    }

    @Override
    public void openIdentityGroupEditor(String groupId) {
        lastIdentityGroupId = Objects.requireNonNullElse(groupId, "");
    }

    @Override
    public void openRoleGroupEditor(String groupId) {
        lastRoleGroupId = Objects.requireNonNullElse(groupId, "");
    }

    @Override
    public void openTeamEditor(String teamId) {
        lastTeamId = Objects.requireNonNullElse(teamId, "");
    }

    @Override
    public void openPermissionEditor(ScopeLayer layer, String scopeId) {
        lastPermissionLayer = layer;
        lastPermissionScopeId = Objects.requireNonNullElse(scopeId, "");
    }

    public String lastFieldId() {
        return lastFieldId;
    }

    public String lastIdentityGroupId() {
        return lastIdentityGroupId;
    }

    public String lastRoleGroupId() {
        return lastRoleGroupId;
    }

    public String lastTeamId() {
        return lastTeamId;
    }

    public ScopeLayer lastPermissionLayer() {
        return lastPermissionLayer;
    }

    public String lastPermissionScopeId() {
        return lastPermissionScopeId;
    }
}
