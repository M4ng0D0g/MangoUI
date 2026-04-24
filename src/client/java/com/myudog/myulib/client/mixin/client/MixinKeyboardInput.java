package com.myudog.myulib.client.mixin.client;

import com.myudog.myulib.client.api.control.ClientControlManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.KeyboardInput;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(KeyboardInput.class)
public class MixinKeyboardInput {

    // 注入在 tick 方法的最後面 (TAIL)
    // 此時原版已經讀取完 WASD 的狀態了，正好是我們攔截的最佳時機
    @Inject(method = "tick()V", at = @At("TAIL"))
    private void interceptControlInput(CallbackInfo ci) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.options == null) {
            return;
        }

        // Apply client-side control restrictions immediately for local input feel.
        ClientControlManager.INSTANCE.applyClientInputGuards(minecraft);

        // 如果玩家目前正在「遙控」別的生物
        if (ClientControlManager.INSTANCE.isControlling()) {
            // 1. 把真正的按鍵狀態發送給伺服器
            ClientControlManager.INSTANCE.sendInput(
                    minecraft.options.keyUp.isDown(),
                    minecraft.options.keyDown.isDown(),
                    minecraft.options.keyLeft.isDown(),
                    minecraft.options.keyRight.isDown(),
                    minecraft.options.keyJump.isDown(),
                    minecraft.options.keyShift.isDown()
            );
        }
    }
}