package com.myudog.myulib.client.api.control;

import com.myudog.myulib.api.control.ControlType;
import com.myudog.myulib.api.control.network.ControlInputPayload;
import com.myudog.myulib.api.control.network.ServerControlNetworking;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;

import java.util.EnumSet;
import java.util.Set;

public final class ClientControlManager {

    public static final ClientControlManager INSTANCE = new ClientControlManager();

    

    private final Set<ControlType> DISABLED = EnumSet.noneOf(ControlType.class);
    private boolean isControlling;
    private boolean isControlled;
    private boolean installed;

    private ClientControlManager() {}

    public void install() {
        if (installed) {
            return;
        }
        installed = true;

        ClientPlayNetworking.registerGlobalReceiver(ServerControlNetworking.ControlStatePayload.TYPE,
                (payload, context) -> context.client().execute(() -> {
                    synchronized (DISABLED) {
                        DISABLED.clear();
                        for (ControlType type : ControlType.values()) {
                            int mask = 1 << type.ordinal();
                            if ((payload.disabledMask() & mask) != 0) {
                                DISABLED.add(type);
                            }
                        }
                    }
                    isControlling = payload.controlling();
                    isControlled = payload.controlled();
                }));
    }

    public boolean isControlling() {
        return isControlling;
    }

    public boolean isControlled() {
        return isControlled;
    }

    public boolean isControlEnabled(ControlType type) {
        synchronized (DISABLED) {
            return !DISABLED.contains(type);
        }
    }

    public boolean shouldBlockRotation() {
        return !isControlEnabled(ControlType.ROTATE);
    }

    /**
     * 負責將攔截到的按鍵打包並發送給伺服器
     */
    public void sendInput(boolean up, boolean down, boolean left, boolean right, boolean jumping, boolean sneaking) {
        if (!isControlling) {
            return;
        }

        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) return;

        // 取得玩家當前滑鼠轉動的視角
        float yaw = player.getYRot();
        float pitch = player.getXRot();

        ControlInputPayload payload = new ControlInputPayload(
                up, down, left, right, jumping, sneaking, yaw, pitch
        );
        ClientPlayNetworking.send(payload);
    }

    public void applyClientInputGuards(Minecraft minecraft) {
        if (minecraft == null) {
            return;
        }

        if (!isControlEnabled(ControlType.MOVE)) {
            minecraft.options.keyUp.setDown(false);
            minecraft.options.keyDown.setDown(false);
            minecraft.options.keyLeft.setDown(false);
            minecraft.options.keyRight.setDown(false);
        }

        if (!isControlEnabled(ControlType.SPRINT)) {
            minecraft.options.keySprint.setDown(false);
            if (minecraft.player != null) {
                minecraft.player.setSprinting(false);
            }
        }

        if (!isControlEnabled(ControlType.SNEAK)) {
            minecraft.options.keyShift.setDown(false);
            if (minecraft.player != null) {
                minecraft.player.setShiftKeyDown(false);
            }
        }

        if (!isControlEnabled(ControlType.CRAWL) && minecraft.player != null && minecraft.player.isSwimming()) {
            minecraft.options.keyShift.setDown(false);
            minecraft.player.setShiftKeyDown(false);
        }

        if (!isControlEnabled(ControlType.JUMP)) {
            minecraft.options.keyJump.setDown(false);
        }
    }

    public Set<ControlType> disabledSnapshot() {
        synchronized (DISABLED) {
            return Set.copyOf(DISABLED);
        }
    }
}