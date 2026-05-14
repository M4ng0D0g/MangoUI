package com.myudog.myulib.client.mixin.client.camera;

import com.myudog.myulib.api.core.camera.CameraTrackingTarget;
import com.myudog.myulib.client.api.camera.ClientCameraManager;
import com.myudog.myulib.client.api.camera.LockOnTargetTracker;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.world.phys.Vec2;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Gui.class)
public abstract class GuiMixin {

    @Unique
    private static final Identifier MYULIB$LOCK_ON_TEXTURE = Identifier.fromNamespaceAndPath("myulib", "textures/gui/lock_on.png");
    @Unique
    private static final Identifier MYULIB$VIRTUAL_CROSSHAIR_TEXTURE = Identifier.fromNamespaceAndPath("myulib", "textures/gui/virtual_crosshair.png");

    @Inject(method = "extractCrosshair", at = @At("HEAD"), cancellable = true)
    private void myulib$renderDualCrosshair(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker, CallbackInfo ci) {
        if (LockOnTargetTracker.isLockedOn()) {
            float tickDelta = deltaTracker.getGameTimeDeltaTicks();
            Vec2 lockedTargetScreenPos = LockOnTargetTracker.getLockedTargetScreenPos(tickDelta);
            Vec2 virtualCrosshairPos = LockOnTargetTracker.getVirtualCrosshairPos();

            // 在新版架構中，切換繪製層級需呼叫 nextStratum()
            graphics.nextStratum();

            // 🌟 修正：移除 RenderSystem 呼叫，直接使用 blit 並指定 RenderPipelines。
            // 顏色與透明度透過 ARGB.white(alpha) 傳遞。

            // 繪製鎖定框
            graphics.blit(
                    RenderPipelines.GUI_TEXTURED,
                    MYULIB$LOCK_ON_TEXTURE,
                    (int)lockedTargetScreenPos.x - 8,
                    (int)lockedTargetScreenPos.y - 8,
                    0.0F, 0.0F,
                    16, 16,
                    16, 16,
                    ARGB.white(1.0F)
            );

            // 繪製虛擬準星
            graphics.blit(
                    RenderPipelines.GUI_TEXTURED,
                    MYULIB$VIRTUAL_CROSSHAIR_TEXTURE,
                    (int)virtualCrosshairPos.x - 4,
                    (int)virtualCrosshairPos.y - 4,
                    0.0F, 0.0F,
                    8, 8,
                    8, 8,
                    ARGB.white(1.0F)
            );

            // 取消原版準星渲染
            ci.cancel();
        }
    }

    @Inject(method = "extractRenderState", at = @At("TAIL"))
    private void myulib$renderFade(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker, CallbackInfo ci) {
        float alpha = ClientCameraManager.INSTANCE.getFadeAlpha();
        if (alpha > 0.001f) {
            int r = ClientCameraManager.INSTANCE.getFadeR();
            int g = ClientCameraManager.INSTANCE.getFadeG();
            int b = ClientCameraManager.INSTANCE.getFadeB();
            int color = ARGB.color((int)(alpha * 255), r, g, b);
            
            graphics.nextStratum();
            graphics.fill(0, 0, 4000, 4000, color); // 使用足夠大的範圍覆蓋全螢幕
        }
    }
}