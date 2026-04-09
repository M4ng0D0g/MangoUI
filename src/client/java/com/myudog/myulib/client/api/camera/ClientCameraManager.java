package com.myudog.myulib.client.api.camera;

import com.myudog.myulib.api.animation.Easing;
import com.myudog.myulib.api.camera.CameraActionPayload;
import com.myudog.myulib.api.camera.CameraModifier;
import com.myudog.myulib.api.camera.CameraTrackingTarget;
import com.myudog.myulib.api.camera.CameraTransform;
import net.minecraft.client.Camera;
import net.minecraft.world.phys.Vec3;

import java.util.Iterator;
import java.util.concurrent.CopyOnWriteArrayList;

public final class ClientCameraManager {
    private static final ClientCameraManager INSTANCE = new ClientCameraManager();

    private final CopyOnWriteArrayList<CameraModifier> modifiers = new CopyOnWriteArrayList<>();

    private ClientCameraManager() {
    }

    public static ClientCameraManager getInstance() {
        return INSTANCE;
    }

    public void addModifier(CameraModifier modifier) {
        if (modifier != null) {
            modifiers.add(modifier);
        }
    }

    public void clearModifiers() {
        modifiers.clear();
    }

    public void shake(float intensity, long durationMillis) {
        addModifier(new ShakeModifier(intensity, durationMillis));
    }

    public void moveTo(Vec3 destination, long durationMillis, Easing easing) {
        if (destination == null) {
            return;
        }
        addModifier(new PathAnimationModifier(destination, CameraTrackingTarget.of(destination), durationMillis, easing));
    }

    public void applyPayload(CameraActionPayload payload) {
        if (payload == null) {
            return;
        }
        switch (payload.action()) {
            case SHAKE -> shake(payload.intensity(), payload.durationMillis());
            case MOVE_TO -> moveTo(new Vec3(payload.targetX(), payload.targetY(), payload.targetZ()), payload.durationMillis(), payload.easing());
            case RESET -> clearModifiers();
        }
    }

    public void applyAll(Camera camera, float tickDelta) {
        if (camera == null || modifiers.isEmpty()) {
            return;
        }
        long now = System.currentTimeMillis();
        CameraTransform transform = CameraTransform.of(CameraCompat.getPosition(camera), CameraCompat.getYaw(camera), CameraCompat.getPitch(camera));

        for (CameraModifier modifier : modifiers) {
            modifier.apply(transform, tickDelta, now);
        }

        CameraCompat.setPosition(camera, transform.position());
        CameraCompat.setRotation(camera, transform.yaw(), transform.pitch());

        for (Iterator<CameraModifier> it = modifiers.iterator(); it.hasNext(); ) {
            CameraModifier modifier = it.next();
            if (modifier.isFinished(now)) {
                modifiers.remove(modifier);
            }
        }
    }
}

