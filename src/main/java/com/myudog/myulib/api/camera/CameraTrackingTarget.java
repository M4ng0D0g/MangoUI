package com.myudog.myulib.api.camera;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

import java.util.Objects;
import java.util.function.Supplier;

public final class CameraTrackingTarget {
    private final Supplier<Vec3> positionSupplier;
    private final Supplier<Vec3> lookAtSupplier;
    private final Vec3 offset;

    private CameraTrackingTarget(Supplier<Vec3> positionSupplier, Supplier<Vec3> lookAtSupplier, Vec3 offset) {
        this.positionSupplier = Objects.requireNonNull(positionSupplier, "positionSupplier");
        this.lookAtSupplier = lookAtSupplier;
        this.offset = offset == null ? Vec3.ZERO : offset;
    }

    public static CameraTrackingTarget of(Entity entity) {
        Objects.requireNonNull(entity, "entity");
        return new CameraTrackingTarget(entity::position, null, Vec3.ZERO);
    }

    public static CameraTrackingTarget of(Vec3 position) {
        Objects.requireNonNull(position, "position");
        return new CameraTrackingTarget(() -> position, null, Vec3.ZERO);
    }

    public CameraTrackingTarget withOffset(Vec3 offset) {
        return new CameraTrackingTarget(positionSupplier, lookAtSupplier, offset);
    }

    public CameraTrackingTarget lookAt(Entity entity) {
        Objects.requireNonNull(entity, "entity");
        return new CameraTrackingTarget(positionSupplier, entity::position, offset);
    }

    public CameraTrackingTarget lookAt(Vec3 point) {
        Objects.requireNonNull(point, "point");
        return new CameraTrackingTarget(positionSupplier, () -> point, offset);
    }

    public Vec3 resolvePosition() {
        Vec3 base = positionSupplier.get();
        if (base == null) {
            return offset;
        }
        return base.add(offset);
    }

    public Vec3 resolveLookAt() {
        return lookAtSupplier == null ? null : lookAtSupplier.get();
    }
}

