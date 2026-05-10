package com.myudog.myulib.client.api.camera;

import com.myudog.myulib.api.core.camera.CameraTrackingTarget;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public class ClientCameraLifecycle {
    public static void init() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.level != null && client.getCameraEntity() != null) {

                CameraTrackingTarget target = ClientCameraManager.INSTANCE.getTrackingTarget();
                if (target == null) return;

                Integer entityId = target.getEntityId();
                Entity resolvedEntity = entityId != null ? client.level.getEntity(entityId) : null;

                if (entityId != null && (resolvedEntity == null || !resolvedEntity.isAlive())) {
                    LockOnTargetTracker.unlock();
                    return;
                }

                Vec3 cameraPos = client.gameRenderer.getMainCamera().position();
                Vec3 targetPos = resolvedEntity != null ? resolvedEntity.getBoundingBox().getCenter() : target.getStaticPosition();

                if (targetPos == null) return;

                HitResult result = client.level.clip(new ClipContext(
                    cameraPos, targetPos, ClipContext.Block.VISUAL, ClipContext.Fluid.NONE, client.getCameraEntity()
                ));

                if (result.getType() == HitResult.Type.BLOCK) {
                    LockOnTargetTracker.incrementOcclusionTime();
                    if (LockOnTargetTracker.getOcclusionTime() >= 10) { // 10 ticks = 0.5s
                        LockOnTargetTracker.unlock();
                    }
                } else {
                    LockOnTargetTracker.resetOcclusionTime();
                }
            }
        });
    }
}
