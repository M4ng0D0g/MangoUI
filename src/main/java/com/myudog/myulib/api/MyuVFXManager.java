package com.myudog.myulib.api;

import com.myudog.myulib.api.dsl.EffectBuilder;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;

public final class MyuVFXManager {

    public static final MyuVFXManager INSTANCE = new MyuVFXManager();

    
    private MyuVFXManager() {
    }

    public void spawnSpiral(ServerLevel Level, Vec3 center, ParticleOptions particle) {
        EffectBuilder.spawnEffect(center, builder -> {
            builder.duration(40);
            builder.onTick(tick -> {
                double angle = tick * 0.5;
                double radius = 1.5;
                double x = radius * Math.cos(angle);
                double z = radius * Math.sin(angle);
                double y = tick * 0.1;
                VFXCompat.spawnParticles(Level, particle, center.x + x, center.y + y, center.z + z, 1, 0.0, 0.0, 0.0, 0.0);
            });
        });
    }

    public void spawnShockwave(ServerLevel Level, Vec3 center, ParticleOptions particle) {
        EffectBuilder.spawnEffect(center, builder -> {
            builder.duration(15);
            builder.onTick(tick -> {
                double radius = tick * 0.8;
                double density = Math.max(8.0, radius * 10.0);
                for (Vec3 pos : Shapes.CIRCLE.getOutlinePoints(new Vec3(radius, 0.0, radius), density)) {
                    VFXCompat.spawnParticles(Level, particle, center.x + pos.x, center.y + pos.y, center.z + pos.z, 1, 0.0, 0.1, 0.0, 0.02);
                }
            });
        });
    }
}

