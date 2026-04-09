package com.myudog.myulib.client.api.camera;

import com.myudog.myulib.api.camera.CameraActionPayload;
import com.myudog.myulib.api.camera.CameraDispatchBridge;
import net.minecraft.server.level.ServerPlayer;

public final class ClientCameraBridge {
    private ClientCameraBridge() {
    }

    public static void installBridge() {
        CameraDispatchBridge.setDispatchHandler(ClientCameraBridge::dispatch);
        CameraDispatchBridge.setLocalHandler(ClientCameraBridge::applyLocal);
    }

    public static void dispatch(ServerPlayer player, CameraActionPayload payload) {
        // TODO: replace with Fabric custom payload send in a later step.
        applyLocal(payload);
    }

    public static void applyLocal(CameraActionPayload payload) {
        ClientCameraManager.getInstance().applyPayload(payload);
    }
}

