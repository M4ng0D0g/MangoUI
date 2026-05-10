package com.myudog.myulib.client.mixin.client.camera;

import com.myudog.myulib.internal.camera.ClientCameraManager;
import net.minecraft.client.KeyboardHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.input.KeyEvent;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(KeyboardHandler.class)
public class KeyboardMixin {

    /**
     * 依照你提供的官方簽名進行注入。
     * @param handle GLFW 窗口句柄
     * @param action 動作代碼 (1=按下, 0=放開)
     * @param event 包含按鍵詳細資訊的物件
     */
    @Inject(method = "keyPress", at = @At("HEAD"), cancellable = true)
    private void myulib_mc$onKeyPress(long handle, int action, KeyEvent event, CallbackInfo ci) {
        // 偵測 F5 鍵 (GLFW_KEY_F5)
        if (event.key() == GLFW.GLFW_KEY_F5 && action == GLFW.GLFW_PRESS) {
            // 如果處於強制模式且禁止 F5
            if (ClientCameraManager.INSTANCE.getCurrentPerspective().isForced() 
                && !ClientCameraManager.INSTANCE.canUseF5()) {
                
                // 可以在這裡加入小提示，告知玩家目前視角被鎖定
                // Minecraft.getInstance().player.sendMessage(Text.literal("視角已鎖定"), true);
                
                ci.cancel(); // 徹底攔截按鍵事件，不傳遞給原版邏輯
            }
        }
    }
}