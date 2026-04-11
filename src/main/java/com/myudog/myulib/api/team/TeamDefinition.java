package com.myudog.myulib.api.team;

import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;

public record TeamDefinition(
        String id,
        String displayName,
        TeamColor color,
        EnumMap<TeamFlag, Boolean> flags
) {
}

