package com.myudog.myulib.api.core.control;

import java.util.Locale;

public enum ControlType {
    // Composite
    MOVE,
    ROTATE,

    // Movement
    MOVE_FORWARD,
    MOVE_BACKWARD,
    MOVE_LEFT,
    MOVE_RIGHT,

    // Rotation
    ROTATE_YAW,
    ROTATE_PITCH,

    // Actions
    SPRINT,
    SNEAK,
    CRAWL,
    JUMP,

    // Clicks
    LEFT_CLICK,
    RIGHT_CLICK,
    MIDDLE_CLICK,

    // Function keys
    F1, F2, F3, F4, F5, F6, F7, F8, F9, F10, F11, F12,
    
    // UI Interaction
    UI_INTERACTION;

    public String token() {
        return name().toLowerCase(Locale.ROOT);
    }

    public String id() {
        return token();
    }

    public static ControlType parse(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new IllegalArgumentException("Control type cannot be blank");
        }

        String normalized = raw.trim().toUpperCase(Locale.ROOT).replace('-', '_');

        // Aliases
        if ("LOOK".equals(normalized) || "ROTATION".equals(normalized)) return ROTATE;
        if ("MOVEMENT".equals(normalized)) return MOVE;
        if ("ATTACK".equals(normalized)) return LEFT_CLICK;
        if ("USE".equals(normalized)) return RIGHT_CLICK;

        return ControlType.valueOf(normalized);
    }
}
