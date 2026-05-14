package com.myudog.myulib.client.api.control;

import com.myudog.myulib.api.core.control.ControlType;
import com.myudog.myulib.api.core.control.Intent;
import com.myudog.myulib.api.core.control.IntentType;
import com.myudog.myulib.api.core.control.network.ControlInputPayload;
import com.myudog.myulib.api.core.control.network.ControlIntentPayload;
import com.myudog.myulib.api.core.control.network.ServerControlNetworking;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;

public final class ClientControlManager {

    public static final ClientControlManager INSTANCE = new ClientControlManager();

    private java.util.BitSet disabledMask = new java.util.BitSet();
    private boolean isControlling;
    private boolean isControlled;
    private boolean installed;
    private boolean lockedOn;

    private ClientControlManager() {}

    public void install() {
        if (installed) return;
        installed = true;

        ClientPlayNetworking.registerGlobalReceiver(ServerControlNetworking.ControlStatePayload.TYPE,
                (payload, context) -> context.client().execute(() -> {
                    this.disabledMask = payload.disabledMask();
                    this.isControlling = payload.controlling();
                    this.isControlled = payload.controlled();
                }));
    }

    public boolean isControlling() {
        return isControlling;
    }

    public boolean isDenied(ControlType type) {
        return this.disabledMask.get(type.ordinal());
    }

    public boolean isLockedOn() {
        return lockedOn;
    }

    public void setLockedOn(boolean locked) {
        this.lockedOn = locked;
    }

    public void updateVirtualCrosshair(double dx, double dy) {
        // Implementation for virtual crosshair movement when locked on
    }

    public boolean shouldBlockRotation() {
        return lockedOn || isDenied(ControlType.ROTATE) || isDenied(ControlType.ROTATE_YAW) || isDenied(ControlType.ROTATE_PITCH);
    }

    public boolean shouldBlockLeftClick() {
        return isDenied(ControlType.LEFT_CLICK);
    }

    public boolean shouldBlockRightClick() {
        return isDenied(ControlType.RIGHT_CLICK);
    }

    /**
     * 🌟 修正：將攔截到的按鍵過濾後再發送給伺服器。
     * 如果權限被禁止，回傳給伺服器的狀態將永遠是 false。
     */
    public void sendInput(boolean up, boolean down, boolean left, boolean right, boolean jumping, boolean sneaking) {
        if (!isControlling) return;

        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) return;

        // 核心攔截：如果對應操作被禁止，則封包內容強制設為 false
        boolean finalUp = (isDenied(ControlType.MOVE) || isDenied(ControlType.MOVE_FORWARD)) ? false : up;
        boolean finalDown = (isDenied(ControlType.MOVE) || isDenied(ControlType.MOVE_BACKWARD)) ? false : down;
        boolean finalLeft = (isDenied(ControlType.MOVE) || isDenied(ControlType.MOVE_LEFT)) ? false : left;
        boolean finalRight = (isDenied(ControlType.MOVE) || isDenied(ControlType.MOVE_RIGHT)) ? false : right;
        boolean finalJumping = isDenied(ControlType.JUMP) ? false : jumping;
        boolean finalSneaking = isDenied(ControlType.SNEAK) ? false : sneaking;

        ControlInputPayload payload = new ControlInputPayload(
                finalUp, finalDown, finalLeft, finalRight, 
                finalJumping, finalSneaking, 
                player.getYRot(), player.getXRot()
        );
        ClientPlayNetworking.send(payload);
    }

    /**
     * 🌟 修正：攔截單次意圖。若權限不符，則直接丟棄不發送封包。
     */
    public void sendIntent(Intent intent) {
        if (!isControlling) return;

        // 權限映射檢查
        if (isIntentDenied(intent.type())) return;

        ClientPlayNetworking.send(new ControlIntentPayload(
                intent.type(), 
                intent.action(), 
                intent.vector(), 
                intent.keyCode(), 
                intent.customAction(), 
                intent.timestamp()
        ));
    }

    private boolean isIntentDenied(IntentType type) {
        return switch (type) {
            case JUMP -> isDenied(ControlType.JUMP);
            case SNEAK -> isDenied(ControlType.SNEAK);
            case SPRINT -> isDenied(ControlType.SPRINT);
            case CRAWL -> isDenied(ControlType.CRAWL);
            case LEFT_CLICK -> isDenied(ControlType.LEFT_CLICK);
            case RIGHT_CLICK -> isDenied(ControlType.RIGHT_CLICK);
            case MOVE_FORWARD -> isDenied(ControlType.MOVE) || isDenied(ControlType.MOVE_FORWARD);
            case MOVE_BACKWARD -> isDenied(ControlType.MOVE) || isDenied(ControlType.MOVE_BACKWARD);
            case MOVE_LEFT -> isDenied(ControlType.MOVE) || isDenied(ControlType.MOVE_LEFT);
            case MOVE_RIGHT -> isDenied(ControlType.MOVE) || isDenied(ControlType.MOVE_RIGHT);
            case ROTATE_YAW -> isDenied(ControlType.ROTATE) || isDenied(ControlType.ROTATE_YAW);
            case ROTATE_PITCH -> isDenied(ControlType.ROTATE) || isDenied(ControlType.ROTATE_PITCH);
            case ROTATE -> isDenied(ControlType.ROTATE);
            case F1, F2, F3, F4, F5, F6, F7, F8, F9, F10, F11, F12 -> isDenied(ControlType.valueOf(type.name()));
            default -> false;
        };
    }

    /**
     * 強制清除客戶端本地按鍵狀態，防止預測運動。
     */
    public void applyClientInputGuards(Minecraft minecraft) {
        if (minecraft == null) return;

        if (isDenied(ControlType.MOVE)) {
            minecraft.options.keyUp.setDown(false);
            minecraft.options.keyDown.setDown(false);
            minecraft.options.keyLeft.setDown(false);
            minecraft.options.keyRight.setDown(false);
        }
        if (isDenied(ControlType.MOVE_FORWARD)) minecraft.options.keyUp.setDown(false);
        if (isDenied(ControlType.MOVE_BACKWARD)) minecraft.options.keyDown.setDown(false);
        if (isDenied(ControlType.MOVE_LEFT)) minecraft.options.keyLeft.setDown(false);
        if (isDenied(ControlType.MOVE_RIGHT)) minecraft.options.keyRight.setDown(false);

        if (isDenied(ControlType.JUMP)) minecraft.options.keyJump.setDown(false);
        if (isDenied(ControlType.SNEAK)) {
            minecraft.options.keyShift.setDown(false);
            if (minecraft.player != null) minecraft.player.setShiftKeyDown(false);
        }
        if (isDenied(ControlType.SPRINT)) {
            minecraft.options.keySprint.setDown(false);
            if (minecraft.player != null) minecraft.player.setSprinting(false);
        }
        if (isDenied(ControlType.CRAWL)) {
            // Crawl is harder to force off in vanilla without a mixin, 
            // but we can at least stop the key.
        }
    }

    public void resetState() {
        this.disabledMask.clear();
        this.isControlling = false;
        this.isControlled = false;
        this.lockedOn = false;
    }
}