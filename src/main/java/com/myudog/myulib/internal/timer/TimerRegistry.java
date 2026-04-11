package com.myudog.myulib.internal.timer;

import com.myudog.myulib.api.timer.TimerManager;
import com.myudog.myulib.api.timer.TimerModels;
import net.minecraft.resources.Identifier;

public class TimerRegistry {
    public static void register(TimerModels.Timer timer) { TimerManager.register(timer); }
    public static int createInstance(Identifier timerId, Long ownerEntityId, TimerModels.TimerPayload payload, boolean autoStart, Object Level) { return TimerManager.createInstance(timerId, ownerEntityId, payload, autoStart, Level); }
}
