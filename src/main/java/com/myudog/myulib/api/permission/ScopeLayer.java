package com.myudog.myulib.api.permission;

public enum ScopeLayer {
    GLOBAL,
    DIMENSION,
    FIELD,
    USER;

    public boolean isHigherThan(ScopeLayer other) {
        return other != null && ordinal() > other.ordinal();
    }
}

