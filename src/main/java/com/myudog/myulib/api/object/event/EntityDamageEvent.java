package com.myudog.myulib.api.object.event;

import com.myudog.myulib.api.event.IEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;

public record EntityDamageEvent(
        LivingEntity victim,
        DamageSource source,
        float amount
) implements IEvent {
}

