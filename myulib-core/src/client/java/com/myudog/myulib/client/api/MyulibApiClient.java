package com.myudog.myulib.client.api;

import com.myudog.myulib.api.core.camera.CameraApi;
import com.myudog.myulib.api.core.control.ControlManager;
import com.myudog.myulib.client.api.hologram.network.HologramClientNetworking;

public final class MyulibApiClient {
    public static void initCoreClient() {
        CameraApi.initClient();
        com.myudog.myulib.client.api.camera.ClientCameraLifecycle.init();
        ControlManager.INSTANCE.install();
        HologramClientNetworking.registerClientReceivers();
    }
}
