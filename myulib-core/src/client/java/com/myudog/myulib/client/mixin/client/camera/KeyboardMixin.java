package com.myudog.myulib.client.mixin.client.camera;

import com.myudog.myulib.api.core.control.InputAction;
import com.myudog.myulib.api.core.control.Intent;
import com.myudog.myulib.api.core.control.IntentType;
import com.myudog.myulib.client.api.control.ClientControlManager;
import com.myudog.myulib.client.api.camera.ClientCameraManager;
import net.minecraft.client.KeyboardHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.input.KeyEvent;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 相機與控制系統：鍵盤輸入攔截
 */
@Mixin(KeyboardHandler.class)
public class KeyboardMixin {

    @Inject(method = "keyPress", at = @At("HEAD"), cancellable = true)
    private void myulib_mc$onKeyPress(long handle, int action, KeyEvent event, CallbackInfo ci) {
        ClientControlManager controlManager = ClientControlManager.INSTANCE;
        int key = event.key();

        // 1. 相機系統攔截 (F5 等)
        if (key == GLFW.GLFW_KEY_F5 && action == GLFW.GLFW_PRESS) {
            if (ClientCameraManager.INSTANCE.getCurrentPerspective().isForced() 
                && !ClientCameraManager.INSTANCE.canUseF5()) {
                ci.cancel();
                return;
            }
        }

        // 2. 控制系統意圖發送 (PRESS/RELEASE)
        if (controlManager.isControlling()) {
            InputAction inputAction = switch (action) {
                case 1 -> InputAction.PRESS;
                case 0 -> InputAction.RELEASE;
                case 2 -> InputAction.REPEAT;
                default -> null;
            };

            if (inputAction != null) {
                IntentType type = mapKeyToIntent(key);
                if (type != null) {
                    controlManager.sendIntent(Intent.action(type, inputAction));
                }
            }
        }
    }

    @Unique
    private IntentType mapKeyToIntent(int key) {
        Minecraft mc = Minecraft.getInstance();
        if (key == mc.options.keyJump.getDefaultKey().getValue()) return IntentType.JUMP;
        if (key == mc.options.keyShift.getDefaultKey().getValue()) return IntentType.SNEAK;
        if (key == mc.options.keySprint.getDefaultKey().getValue()) return IntentType.SPRINT;

        // F-Keys
        if (key >= GLFW.GLFW_KEY_F1 && key <= GLFW.GLFW_KEY_F12) {
            return IntentType.valueOf("F" + (key - GLFW.GLFW_KEY_F1 + 1));
        }

        // Standard Keys
        return switch (key) {
            case GLFW.GLFW_KEY_ESCAPE -> IntentType.ESCAPE;
            case GLFW.GLFW_KEY_TAB -> IntentType.TAB;
            case GLFW.GLFW_KEY_ENTER -> IntentType.ENTER;
            case GLFW.GLFW_KEY_BACKSPACE -> IntentType.BACKSPACE;
            default -> null;
        };
    }
}