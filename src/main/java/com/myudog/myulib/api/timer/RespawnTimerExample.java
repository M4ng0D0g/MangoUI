package com.myudog.myulib.api.timer;

import net.minecraft.resources.Identifier;

import java.util.UUID;

public final class RespawnTimerExample {
    private RespawnTimerExample() {
    }

    public static TimerModels.Timer timer() {
        return new TimerModels.Timer(Identifier.fromNamespaceAndPath("myulib", "respawn"), 100L, TimerModels.TimerMode.COUNT_DOWN, true)
                .onRemainingTick(20L, snapshot -> {
                    if (snapshot.payload() instanceof TimerModels.RespawnTimerPayload payload && payload.allowSkip()) {
                        // no-op example hook
                    }
                });
    }

    public static TimerModels.RespawnTimerPayload payload(UUID playerId, boolean allowSkip) {
        return new TimerModels.RespawnTimerPayload(playerId, allowSkip);
    }
}

