package com.myudog.myulib.api.camera;

import com.myudog.myulib.api.animation.Easing;

public record CameraActionPayload(
    ActionType action,
    float intensity,
    long durationMillis,
    double targetX,
    double targetY,
    double targetZ,
    Easing easing
) {
    public enum ActionType {
        SHAKE,
        MOVE_TO,
        RESET
    }

    public static CameraActionPayload shake(float intensity, long durationMillis) {
        return new CameraActionPayload(ActionType.SHAKE, intensity, durationMillis, 0.0, 0.0, 0.0, Easing.SMOOTH_STEP);
    }

    public static CameraActionPayload moveTo(double x, double y, double z, long durationMillis, Easing easing) {
        return new CameraActionPayload(ActionType.MOVE_TO, 0.0f, durationMillis, x, y, z, easing == null ? Easing.SMOOTH_STEP : easing);
    }

    public static CameraActionPayload reset() {
        return new CameraActionPayload(ActionType.RESET, 0.0f, 0L, 0.0, 0.0, 0.0, Easing.LINEAR);
    }
}

