package com.myudog.myulib.internal.timer;

import com.myudog.myulib.api.core.timer.TimerDefinition;
import com.myudog.myulib.api.core.timer.TimerManager;
import com.myudog.myulib.api.core.timer.TimerPayload;
import net.minecraft.resources.Identifier;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

import com.myudog.myulib.api.core.ecs.EcsContainer;

public class TimerRegistry {
    public static void register(TimerDefinition timer) { TimerManager.INSTANCE.register(timer); }
    public static int createInstance(EcsContainer container, Identifier timerId, Long ownerEntityId, TimerPayload payload) {
        UUID timerUuid = UUID.nameUUIDFromBytes(timerId.toString().getBytes(StandardCharsets.UTF_8));
        return TimerManager.INSTANCE.createInstance(container, timerUuid, ownerEntityId, payload);
    }
}
