package com.myudog.myulib.api.core.camera.network;

import com.myudog.myulib.api.core.camera.CameraActionPayload;
import com.myudog.myulib.api.core.camera.CameraDispatchBridge;
import com.myudog.myulib.internal.camera.ClientCameraManager;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;

public final class CameraNetworking {

    public static void initServer() {
        // 1. 註冊 S2C (Server-to-Client) 封包
        PayloadTypeRegistry.clientboundPlay().register(CameraActionPayload.TYPE, CameraActionPayload.CODEC);

        // 2. 告訴 Bridge：當伺服器呼叫 dispatch 時，請用 ServerPlayNetworking 發送封包！
        CameraDispatchBridge.setDispatchHandler(ServerPlayNetworking::send);
    }
}