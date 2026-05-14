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
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(KeyboardInput.class)
public class KeyboardInputMixin {

    @Inject(method = "tick", at = @At("TAIL"))
    private void myulib$handleControlAndForwarding(CallbackInfo ci) {
        ClientControlManager manager = ClientControlManager.INSTANCE;
        Minecraft minecraft = Minecraft.getInstance();

        // 🌟 將自己強轉為 Accessor 介面，以便存取父類別變數
        ClientInputAccessor accessor = (ClientInputAccessor) this;

        // 1. 執行實體按鍵保護 (強制清除 KeyBinding 狀態)
        manager.applyClientInputGuards(minecraft);

        // --- A. 遙控意圖轉發 (遙控實體) ---
        if (manager.isControlling() && minecraft.player != null) {
            float yaw = minecraft.player.getYRot();

            // 從 moveVector 取得原始輸入脈衝 (x=左右, y=前後)
            float forwardImpulse = accessor.getMoveVector().y;
            float leftImpulse = accessor.getMoveVector().x;

            // 發送分向移動意圖 (按下時發送)
            if (forwardImpulse > 0) manager.sendIntent(Intent.action(com.myudog.myulib.api.core.control.IntentType.MOVE_FORWARD, com.myudog.myulib.api.core.control.InputAction.PRESS));
            if (forwardImpulse < 0) manager.sendIntent(Intent.action(com.myudog.myulib.api.core.control.IntentType.MOVE_BACKWARD, com.myudog.myulib.api.core.control.InputAction.PRESS));
            if (leftImpulse > 0) manager.sendIntent(Intent.action(com.myudog.myulib.api.core.control.IntentType.MOVE_LEFT, com.myudog.myulib.api.core.control.InputAction.PRESS));
            if (leftImpulse < 0) manager.sendIntent(Intent.action(com.myudog.myulib.api.core.control.IntentType.MOVE_RIGHT, com.myudog.myulib.api.core.control.InputAction.PRESS));

            // 將本地指令轉換為世界座標移動向量
            Vec3 vector = new Vec3(leftImpulse, 0, forwardImpulse).yRot((float) Math.toRadians(-yaw));

            // 發送移動意圖封包
            manager.sendIntent(Intent.move(vector));

            accessor.setMoveVector(Vec2.ZERO);
            accessor.setKeyPresses(new Input(false, false, false, false, false, false, false));
            return; // 結束後續判斷
        }

        // --- B. 本體權限攔截邏輯 ---
        Input currentInput = accessor.getKeyPresses();
        
        boolean blockFwd = manager.isDenied(ControlType.MOVE) || manager.isDenied(ControlType.MOVE_FORWARD);
        boolean blockBwd = manager.isDenied(ControlType.MOVE) || manager.isDenied(ControlType.MOVE_BACKWARD);
        boolean blockLft = manager.isDenied(ControlType.MOVE) || manager.isDenied(ControlType.MOVE_LEFT);
        boolean blockRgt = manager.isDenied(ControlType.MOVE) || manager.isDenied(ControlType.MOVE_RIGHT);
        boolean denyJump = manager.isDenied(ControlType.JUMP);
        boolean denySneak = manager.isDenied(ControlType.SNEAK);
        boolean denySprint = manager.isDenied(ControlType.SPRINT);

        // 重新構造 Input 物件，根據權限決定保留哪些按鍵狀態
        accessor.setKeyPresses(new Input(
                !blockFwd && currentInput.forward(),
                !blockBwd && currentInput.backward(),
                !blockLft && currentInput.left(),
                !blockRgt && currentInput.right(),
                !denyJump && currentInput.jump(),
                !denySneak && currentInput.shift(),
                !denySprint && currentInput.sprint()
        ));

        // 處理向量
        Vec2 currentVec = accessor.getMoveVector();
        accessor.setMoveVector(new Vec2(
                (blockLft && currentVec.x > 0) || (blockRgt && currentVec.x < 0) ? 0 : currentVec.x,
                (blockFwd && currentVec.y > 0) || (blockBwd && currentVec.y < 0) ? 0 : currentVec.y
        ));
    }
}