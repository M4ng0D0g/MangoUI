package com.myudog.myulib.client.api.camera;

import net.minecraft.client.Camera;
import net.minecraft.world.phys.Vec3;

import java.lang.reflect.Method;

final class CameraCompat {
    private CameraCompat() {
    }

    static Vec3 getPosition(Camera camera) {
        try {
            Method m = Camera.class.getMethod("getPosition");
            Object value = m.invoke(camera);
            if (value instanceof Vec3 vec3) {
                return vec3;
            }
        } catch (ReflectiveOperationException ignored) {
        }
        return Vec3.ZERO;
    }

    static float getYaw(Camera camera) {
        try {
            Method m = Camera.class.getMethod("getYRot");
            Object value = m.invoke(camera);
            if (value instanceof Float f) {
                return f;
            }
        } catch (ReflectiveOperationException ignored) {
        }
        return 0.0f;
    }

    static float getPitch(Camera camera) {
        try {
            Method m = Camera.class.getMethod("getXRot");
            Object value = m.invoke(camera);
            if (value instanceof Float f) {
                return f;
            }
        } catch (ReflectiveOperationException ignored) {
        }
        return 0.0f;
    }

    static void setPosition(Camera camera, Vec3 pos) {
        try {
            Method vecMethod = Camera.class.getMethod("setPosition", Vec3.class);
            vecMethod.invoke(camera, pos);
            return;
        } catch (ReflectiveOperationException ignored) {
        }
        try {
            Method xyzMethod = Camera.class.getMethod("setPosition", double.class, double.class, double.class);
            xyzMethod.invoke(camera, pos.x, pos.y, pos.z);
        } catch (ReflectiveOperationException ignored) {
        }
    }

    static void setRotation(Camera camera, float yaw, float pitch) {
        try {
            Method m = Camera.class.getMethod("setRotation", float.class, float.class);
            m.invoke(camera, yaw, pitch);
        } catch (ReflectiveOperationException ignored) {
        }
    }
}

