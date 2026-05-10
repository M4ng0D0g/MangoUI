package com.myudog.myulib.client.mixin.client.camera;

import net.minecraft.client.Camera;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(Camera.class)
public interface CameraAccessor {
    @Accessor("position")
    Vec3 getPosition();

    @Accessor("position")
    void setPosition(Vec3 position);

    @Accessor("yRot")
    float getYaw();

    @Accessor("xRot")
    float getPitch();

    @Accessor
    void invokeSetRotation(float yRot, float xRot);
}
