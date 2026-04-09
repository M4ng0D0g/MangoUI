package com.myudog.myulib.api.camera;

import net.minecraft.world.phys.Vec3;

public final class CameraTransform {
    private double x;
    private double y;
    private double z;
    private float yaw;
    private float pitch;

    public CameraTransform(double x, double y, double z, float yaw, float pitch) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.yaw = yaw;
        this.pitch = pitch;
    }

    public static CameraTransform of(Vec3 position, float yaw, float pitch) {
        return new CameraTransform(position.x, position.y, position.z, yaw, pitch);
    }

    public Vec3 position() {
        return new Vec3(x, y, z);
    }

    public float yaw() {
        return yaw;
    }

    public float pitch() {
        return pitch;
    }

    public void setPosition(Vec3 position) {
        this.x = position.x;
        this.y = position.y;
        this.z = position.z;
    }

    public void setPosition(double x, double y, double z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public void move(double dx, double dy, double dz) {
        this.x += dx;
        this.y += dy;
        this.z += dz;
    }

    public void setRotation(float yaw, float pitch) {
        this.yaw = yaw;
        this.pitch = pitch;
    }

    public double x() {
        return x;
    }

    public double y() {
        return y;
    }

    public double z() {
        return z;
    }
}

