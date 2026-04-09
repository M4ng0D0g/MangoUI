package com.myudog.myulib.api.camera;

import com.myudog.myulib.api.animation.Easing;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

import java.util.Objects;

public final class CameraApi {
    private CameraApi() {
    }

    public static void initServer() {
    }

    public static void shake(ServerPlayer player, float intensity, long durationMillis) {
        if (player == null) {
            return;
        }
        CameraDispatchBridge.dispatch(player, CameraActionPayload.shake(intensity, durationMillis));
    }

    public static void moveTo(ServerPlayer player, CameraTrackingTarget target, long durationMillis, Easing easing) {
        if (player == null || target == null) {
            return;
        }
        Vec3 resolved = target.resolvePosition();
        CameraDispatchBridge.dispatch(player, CameraActionPayload.moveTo(resolved.x, resolved.y, resolved.z, durationMillis, easing));
    }

    public static void reset(ServerPlayer player) {
        if (player == null) {
            return;
        }
        CameraDispatchBridge.dispatch(player, CameraActionPayload.reset());
    }

    public static void shakeLocal(float intensity, long durationMillis) {
        CameraDispatchBridge.applyLocal(CameraActionPayload.shake(intensity, durationMillis));
    }

    public static void moveToLocal(CameraTrackingTarget target, long durationMillis, Easing easing) {
        Objects.requireNonNull(target, "target");
        Vec3 resolved = target.resolvePosition();
        CameraDispatchBridge.applyLocal(CameraActionPayload.moveTo(resolved.x, resolved.y, resolved.z, durationMillis, easing));
    }

    public static void resetLocal() {
        CameraDispatchBridge.applyLocal(CameraActionPayload.reset());
    }
}


