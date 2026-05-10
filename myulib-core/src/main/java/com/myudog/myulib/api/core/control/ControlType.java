package com.myudog.myulib.api.core.control;

import java.util.Locale;

public enum ControlType {
    MOVE,        // ordinal 0 → bit 0
    SPRINT,      // ordinal 1 → bit 1
    SNEAK,       // ordinal 2 → bit 2
    CRAWL,       // ordinal 3 → bit 3
    ROTATE,      // ordinal 4 → bit 4
    JUMP,        // ordinal 5 → bit 5
    LEFT_CLICK,  // ordinal 6 → bit 6  (主要攻擊/交互)
    RIGHT_CLICK; // ordinal 7 → bit 7  (次要交互/放置)

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

        String normalized = raw.trim()
                .toUpperCase(Locale.ROOT)
                .replace('-', '_');

        if (normalized.startsWith("PLAYER_")) {
            normalized = normalized.substring("PLAYER_".length());
        }

        // Backward-compatible aliases from previous naming variants.
        if ("MOVEMENT".equals(normalized)) {
            normalized = "MOVE";
        } else if ("LOOK".equals(normalized) || "ROTATION".equals(normalized)) {
            normalized = "ROTATE";
        } else if ("SPRINTING".equals(normalized)) {
            normalized = "SPRINT";
        } else if ("SNEAKING".equals(normalized)) {
            normalized = "SNEAK";
        } else if ("ATTACK".equals(normalized) || "PRIMARY".equals(normalized)) {
            normalized = "LEFT_CLICK";
        } else if ("INTERACT".equals(normalized) || "SECONDARY".equals(normalized)) {
            normalized = "RIGHT_CLICK";
        }

        return ControlType.valueOf(normalized);
    }
}

