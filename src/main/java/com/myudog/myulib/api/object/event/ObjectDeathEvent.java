package com.myudog.myulib.api.object.event;

import com.myudog.myulib.api.event.IEvent;
import com.myudog.myulib.api.object.IObjectRt;
import net.minecraft.world.damagesource.DamageSource;

public record ObjectDeathEvent(IObjectRt target, DamageSource source) implements IEvent {}
