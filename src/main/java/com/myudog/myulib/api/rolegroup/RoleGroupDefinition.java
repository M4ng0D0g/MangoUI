package com.myudog.myulib.api.rolegroup;

import com.myudog.myulib.api.identity.IdentityGroupDefinition;
import com.myudog.myulib.api.permission.PermissionGrant;

import java.util.List;
import java.util.Map;
import java.util.Objects;

public record RoleGroupDefinition(
    String id,
    String displayName,
    int priority,
    List<PermissionGrant> grants,
    Map<String, String> metadata
) {
    public RoleGroupDefinition {
        id = Objects.requireNonNull(id, "id");
        displayName = displayName == null ? id : displayName;
        grants = grants == null ? List.of() : List.copyOf(grants);
        metadata = metadata == null ? Map.of() : Map.copyOf(metadata);
    }

    public IdentityGroupDefinition toIdentityDefinition() {
        return new IdentityGroupDefinition(id, displayName, priority, grants, metadata);
    }

    public static RoleGroupDefinition fromIdentityDefinition(IdentityGroupDefinition definition) {
        Objects.requireNonNull(definition, "definition");
        return new RoleGroupDefinition(definition.id(), definition.displayName(), definition.priority(), definition.grants(), definition.metadata());
    }
}
