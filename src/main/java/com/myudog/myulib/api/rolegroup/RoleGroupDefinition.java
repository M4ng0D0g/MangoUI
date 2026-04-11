package com.myudog.myulib.api.rolegroup;

import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

public record RoleGroupDefinition(
        String id,
        String displayName,
        int priority, // 數值越大，覆蓋權限的優先級越高
        Map<String, String> metadata,
        Set<UUID> members
) {
    public RoleGroupDefinition {
        id = Objects.requireNonNull(id, "id 不得為空");
        displayName = displayName == null ? id : displayName;
        metadata = metadata == null ? Map.of() : Map.copyOf(metadata);
        members = members == null ? Set.of() : Set.copyOf(members);
    }

    public boolean hasMember(UUID playerId) {
        return members != null && members.contains(playerId);
    }
}