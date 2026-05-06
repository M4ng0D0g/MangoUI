package com.myudog.myulib.api.effect.event;

import com.myudog.myulib.api.event.IEvent;
import com.myudog.myulib.api.effect.SpatialEffect;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;

public record PlayerLeaveSpatialZoneEvent(
        Identifier zoneId,
        ServerPlayer player,
        SpatialEffect effect
) implements IEvent {
}

