package com.myudog.myulib.client.api.camera;

import com.myudog.myulib.api.animation.Easing;
import com.myudog.myulib.api.camera.CameraModifier;
import com.myudog.myulib.api.camera.CameraTrackingTarget;
import com.myudog.myulib.api.camera.CameraTransform;
import net.minecraft.world.phys.Vec3;

final class PathAnimationModifier implements CameraModifier {
    private final Vec3 start;
    private final CameraTrackingTarget target;
    private final long durationMillis;
    private final long startedAt;
    private final Easing easing;

    PathAnimationModifier(Vec3 start, CameraTrackingTarget target, long durationMillis, Easing easing) {
        this.start = start;
        this.target = target;
        this.durationMillis = Math.max(1L, durationMillis);
        this.easing = easing == null ? Easing.SMOOTH_STEP : easing;
        this.startedAt = System.currentTimeMillis();
    }

    @Override
    public void apply(CameraTransform transform, float tickDelta, long nowMillis) {
        long elapsed = Math.max(0L, nowMillis - startedAt);
        float progress = Math.min(1.0f, (float) elapsed / (float) durationMillis);
        float eased = (float) easing.apply(progress);
        Vec3 to = target.resolvePosition();
        double x = start.x + (to.x - start.x) * eased;
        double y = start.y + (to.y - start.y) * eased;
        double z = start.z + (to.z - start.z) * eased;
        transform.setPosition(x, y, z);

        Vec3 lookAt = target.resolveLookAt();
        if (lookAt != null) {
            Vec3 forward = lookAt.subtract(transform.position());
            double horizontal = Math.sqrt(forward.x * forward.x + forward.z * forward.z);
            float yaw = (float) (Math.toDegrees(Math.atan2(forward.z, forward.x)) - 90.0);
            float pitch = (float) (-Math.toDegrees(Math.atan2(forward.y, horizontal)));
            transform.setRotation(yaw, pitch);
        }
    }

    @Override
    public boolean isFinished(long nowMillis) {
        return nowMillis - startedAt >= durationMillis;
    }
}

