package com.myudog.myulib.api.camera;

public interface CameraModifier {
    void apply(CameraTransform transform, float tickDelta, long nowMillis);

    default boolean isFinished(long nowMillis) {
        return false;
    }
}

