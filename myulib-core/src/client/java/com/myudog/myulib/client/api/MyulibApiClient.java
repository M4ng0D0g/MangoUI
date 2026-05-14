package com.myudog.myulib.client.api;

import com.myudog.myulib.api.core.camera.CameraApi;
import com.myudog.myulib.client.api.camera.ClientCameraManager;
import com.myudog.myulib.client.api.camera.LockOnTargetTracker;
import com.myudog.myulib.client.api.hologram.network.HologramClientNetworking;
import com.myudog.myulib.client.api.control.ClientControlManager;
import com.myudog.myulib.client.api.camera.network.ClientCameraNetworking;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;

public final class MyulibApiClient {
    public static void initCoreClient() {
        CameraApi.initClient();
        com.myudog.myulib.client.api.camera.ClientCameraLifecycle.init();
        ClientCameraNetworking.init();
        ClientControlManager.INSTANCE.install();
        HologramClientNetworking.registerClientReceivers();

        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
            ClientControlManager.INSTANCE.resetState();
            ClientCameraManager.INSTANCE.resetState();
            LockOnTargetTracker.resetOcclusionTime();
        });

        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            ClientControlManager.INSTANCE.resetState();
            ClientCameraManager.INSTANCE.resetState();
            LockOnTargetTracker.resetOcclusionTime();
        });
    }
}
