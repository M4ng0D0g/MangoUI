package com.myudog.myulib.internal.state;
import net.minecraft.world.phys.Vec3;public final class ParticleState {
    public Vec3 pos;
    public Vec3 vel;
    public int age;
    public final int maxAge;
    public final double randomSeed;
    public ParticleState(Vec3 pos, Vec3 vel, int age, int maxAge) {
        this(pos, vel, age, maxAge, Math.random());
    }
    public ParticleState(Vec3 pos, Vec3 vel, int age, int maxAge, double randomSeed) {
        this.pos = pos;
        this.vel = vel;
        this.age = age;
        this.maxAge = maxAge;
        this.randomSeed = randomSeed;
    }
    public float getProgress() {
        if (maxAge <= 0) {
            return 1.0f;
        }
        return Math.min(1.0f, age / (float) maxAge);
    }
    public boolean isDead() {
        return age >= maxAge;
    }
}