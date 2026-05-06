package com.myudog.myulib.api.object.event;

import com.myudog.myulib.api.event.IEvent;
import com.myudog.myulib.api.object.IObjectRt;
import net.minecraft.server.level.ServerPlayer;

public record ObjectInteractEvent(IObjectRt target, ServerPlayer player) implements IEvent {}
