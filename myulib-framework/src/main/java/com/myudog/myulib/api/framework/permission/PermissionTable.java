package com.myudog.myulib.api.framework.permission;

import java.util.EnumMap;
import java.util.Map;

public class PermissionTable {
    private final Map<PermissionAction, PermissionDecision> rules = new EnumMap<>(PermissionAction.class);

    public void set(PermissionAction action, PermissionDecision decision) {
        rules.put(action, decision);
    }

    public PermissionDecision get(PermissionAction action) {
        return rules.getOrDefault(action, PermissionDecision.UNSET);
    }

    public Map<PermissionAction, PermissionDecision> rulesSnapshot() {
        return Map.copyOf(rules);
    }

    public void clear() {
        rules.clear();
    }
}
