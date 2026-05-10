package com.myudog.myulib.client.api.camera;

import net.minecraft.client.Camera;
import net.minecraft.world.phys.Vec3;
import com.myudog.myulib.client.mixin.client.camera.CameraAccessor;

/**
 * 處理不同映射下的相機存取相容性。
 */
public final class CameraCompat {
    public static Vec3 getPosition(Camera camera) {
        if (camera instanceof CameraAccessor accessor) {
            return accessor.getPosition();
        }
        return Vec3.ZERO;
    }

    public static float getYaw(Camera camera) {
        if (camera instanceof CameraAccessor accessor) {
            return accessor.getYaw();
        }
        return 0;
    }

    public static float getPitch(Camera camera) {
        if (camera instanceof CameraAccessor accessor) {
            return accessor.getPitch();
        }
        return 0;
    }

    public static void setPosition(Camera camera, Vec3 pos) {
        if (camera instanceof CameraAccessor accessor) {
            accessor.setPosition(pos);
        }
    }

    public static void setRotation(Camera camera, float yaw, float pitch) {
        if (camera instanceof CameraAccessor accessor) {
            accessor.invokeSetRotation(yaw, pitch);
        }
    }
}