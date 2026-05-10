package com.myudog.myulib.client.mixin.client.control;

import com.myudog.myulib.api.core.control.ControlType;
import com.myudog.myulib.api.core.control.Intent;
import com.myudog.myulib.client.api.control.ClientControlManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.KeyboardInput;
import net.minecraft.world.entity.player.Input;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(KeyboardInput.class)
public class KeyboardInputMixin {

    // 🌟 在 1.21 Mojmap 中，所有按鍵狀態都封裝在 keyPresses 物件中
    @Unique
    public Input keyPresses;
    // 🌟 移動脈衝則封裝在 moveVector 向量中
    @Unique
    public Vec2 moveVector;

    @Inject(method = "tick", at = @At("TAIL"))
    private void myulib$handleControlAndForwarding(CallbackInfo ci) {
        ClientControlManager manager = ClientControlManager.INSTANCE;
        Minecraft minecraft = Minecraft.getInstance();

        // 1. 執行實體按鍵保護 (強制清除 KeyBinding 狀態)
        manager.applyClientInputGuards(minecraft);

        // --- A. 遙控意圖轉發 (遙控實體) ---
        if (manager.isControlling() && minecraft.player != null) {
            float yaw = minecraft.player.getYRot();

            // 從 moveVector 取得原始輸入脈衝 (x=左右, y=前後)
            float forwardImpulse = this.moveVector.y;
            float leftImpulse = this.moveVector.x;

            // 將本地指令轉換為世界座標移動向量
            Vec3 vector = new Vec3(leftImpulse, 0, forwardImpulse).yRot((float) Math.toRadians(-yaw));

            // 發送移動意圖封包
            manager.sendIntent(Intent.move(vector));
        }

        // --- B. 本體權限攔截邏輯 ---
        // 由於 Input 是不可變物件 (或是透過建構子重設)，我們需要重新建立 Input 物件來清除狀態
        boolean denyMove = manager.isDenied(ControlType.MOVE);
        boolean denyJump = manager.isDenied(ControlType.JUMP);
        boolean denySneak = manager.isDenied(ControlType.SNEAK);
        boolean denySprint = manager.isDenied(ControlType.SPRINT);

        if (denyMove || denyJump || denySneak || denySprint) {
            // 重新構造 Input 物件，根據權限決定保留哪些按鍵狀態
            this.keyPresses = new Input(
                    !denyMove && this.keyPresses.forward(),
                    !denyMove && this.keyPresses.backward(),
                    !denyMove && this.keyPresses.left(),
                    !denyMove && this.keyPresses.right(),
                    !denyJump && this.keyPresses.jump(),
                    !denySneak && this.keyPresses.shift(),
                    !denySprint && this.keyPresses.sprint()
            );

            // 如果移動被禁止，強制將移動向量歸零
            if (denyMove) {
                this.moveVector = Vec2.ZERO;
            }
        }
    }
}