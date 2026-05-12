package com.myudog.myulib.client.api;

import com.myudog.myulib.api.core.camera.CameraApi;
import com.myudog.myulib.client.api.hologram.network.HologramClientNetworking;
import com.myudog.myulib.client.api.control.ClientControlManager;
import com.myudog.myulib.client.api.camera.network.ClientCameraNetworking;

public final class MyulibApiClient {
    public static void initCoreClient() {
        CameraApi.initClient();
        com.myudog.myulib.client.api.camera.ClientCameraLifecycle.init();
        ClientCameraNetworking.init();
        ClientControlManager.INSTANCE.install();
        HologramClientNetworking.registerClientReceivers();
    }
}
