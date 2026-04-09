package com.myudog.myulib.client.api.camera;

import com.myudog.myulib.api.camera.CameraModifier;
import com.myudog.myulib.api.camera.CameraTransform;

final class ShakeModifier implements CameraModifier {
    private final float intensity;
    private final long durationMillis;
    private final long startedAt;

    ShakeModifier(float intensity, long durationMillis) {
        this.intensity = Math.max(0.0f, intensity);
        this.durationMillis = Math.max(1L, durationMillis);
        this.startedAt = System.currentTimeMillis();
    }

    @Override
    public void apply(CameraTransform transform, float tickDelta, long nowMillis) {
        long elapsed = Math.max(0L, nowMillis - startedAt);
        if (elapsed >= durationMillis) {
            return;
        }
        float progress = (float) elapsed / (float) durationMillis;
        float decay = 1.0f - progress;
        double t = nowMillis * 0.045;
        float yawNoise = (float) Math.sin(t * 1.6) * intensity * decay;
        float pitchNoise = (float) Math.cos(t * 1.4) * intensity * decay;
        transform.setRotation(transform.yaw() + yawNoise, transform.pitch() + pitchNoise);
    }

    @Override
    public boolean isFinished(long nowMillis) {
        return nowMillis - startedAt >= durationMillis;
    }
}

