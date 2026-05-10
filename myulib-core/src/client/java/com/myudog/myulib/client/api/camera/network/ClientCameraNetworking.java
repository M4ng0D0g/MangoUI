package com.myudog.myulib.client.api.camera.network;

import com.myudog.myulib.api.core.camera.CameraDispatchBridge;
import com.myudog.myulib.api.core.camera.CameraActionPayload;
import com.myudog.myulib.client.api.camera.ClientCameraManager;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

public final class ClientCameraNetworking {

    public static void init() {
        // 1. 設置客戶端本地處理橋接
        // 🌟 修正點 1：使用正確的方法名 applyPayload
        CameraDispatchBridge.setLocalHandler(ClientCameraManager.INSTANCE::applyPayload);

        // 2. 註冊封包接收器 (這部分需要 client 依賴)
        ClientPlayNetworking.registerGlobalReceiver(CameraActionPayload.TYPE, (payload, context) -> {
            // 🌟 修正點 2：在具備 client 依賴的專案呼叫執行
            context.client().execute(() -> {
                CameraDispatchBridge.applyLocal(payload);
            });
        });
    }
}