package com.myudog.myulib.api.effect.event;

import com.myudog.myulib.api.effect.SpatialEffect;
import com.myudog.myulib.api.event.IEvent;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;

public record PlayerEnterSpatialZoneEvent(
        Identifier zoneId,
        ServerPlayer player,
        SpatialEffect effect
) implements IEvent {
}

