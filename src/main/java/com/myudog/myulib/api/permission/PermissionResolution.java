package com.myudog.myulib.api.permission;

public record PermissionResolution(PermissionDecision decision, PermissionLayer layer, String sourceId, String ruleId) {
    public static PermissionResolution unset() {
        return new PermissionResolution(PermissionDecision.UNSET, null, null, null);
    }

    @Deprecated(forRemoval = false)
    public static PermissionResolution pass() {
        return unset();
    }

    public boolean isDenied() {
        return decision == PermissionDecision.DENY;
    }

    public boolean isUnset() {
        return decision == PermissionDecision.UNSET;
    }
}
