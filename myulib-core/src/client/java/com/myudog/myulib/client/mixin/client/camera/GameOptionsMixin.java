package com.myudog.myulib.client.mixin.client.camera;

import com.myudog.myulib.api.core.camera.CameraPerspective;
import com.myudog.myulib.internal.camera.ClientCameraManager;
import net.minecraft.client.CameraType;
import net.minecraft.client.Options;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Options.class)
public abstract class GameOptionsMixin {

    @Shadow public abstract void setCameraType(CameraType cameraType);

    /**
     * 當遊戲試圖設定視角時進行攔截
     */
    @Inject(method = "setCameraType", at = @At("HEAD"), cancellable = true)
    private void myulib_mc$interceptSetPerspective(CameraType cameraType, CallbackInfo ci) {
        CameraPerspective forced = ClientCameraManager.INSTANCE.getCurrentPerspective();
        
        if (forced.isForced()) {
            // 如果目前是強制模式，且目標視角與強制視角不符
            CameraType requiredType = switch (forced) {
                case FIRST_PERSON -> CameraType.FIRST_PERSON;
                case THIRD_PERSON_BACK -> CameraType.THIRD_PERSON_BACK;
                case THIRD_PERSON_FRONT -> CameraType.THIRD_PERSON_FRONT;
                case CUSTOM_OFFSET -> CameraType.THIRD_PERSON_BACK; // 偏移模式通常基於第三人稱背面渲染
                default -> cameraType;
            };

            if (cameraType != requiredType) {
                // 如果不符，且不允許 F5，則取消這次寫入
                if (!ClientCameraManager.INSTANCE.canUseF5()) {
                    ci.cancel();
                }
            }
        }
    }
}