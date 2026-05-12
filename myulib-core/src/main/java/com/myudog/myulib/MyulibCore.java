package com.myudog.myulib;

import net.fabricmc.api.ModInitializer;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class MyulibCore implements ModInitializer {
    public static final String MOD_ID = "myulib-core";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MOD_ID, path.trim().toLowerCase());
    }

    @Override
    public void onInitialize() {
        LOGGER.info("MyuLib Core is initializing...");
        
        // 1. 初始化基礎除錯系統
        com.myudog.myulib.api.core.debug.DebugLogManager.INSTANCE.install();

        // 2. 初始化控制系統 (包含網路接收器)
        com.myudog.myulib.api.core.control.ControlManager.INSTANCE.install();
        com.myudog.myulib.internal.control.ControlLifecycleListener.register();

        // 3. 註冊網路載荷 (Payloads)
        com.myudog.myulib.api.core.hologram.network.HologramNetworking.registerPayloads();

        // 4. 初始化相機與計時器系統
        com.myudog.myulib.api.core.camera.CameraApi.initServer();
        com.myudog.myulib.api.core.timer.TimerManager.INSTANCE.install();

        LOGGER.info("MyuLib Core initialized.");
    }
}
