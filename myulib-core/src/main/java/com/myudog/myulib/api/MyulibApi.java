package com.myudog.myulib.api;

import com.myudog.myulib.MyulibCore;
import com.myudog.myulib.api.core.control.command.ControlTypeArgument;
import net.fabricmc.fabric.api.command.v2.ArgumentTypeRegistry;
import net.minecraft.commands.synchronization.SingletonArgumentInfo;
import net.minecraft.resources.Identifier;

public final class MyulibApi {

    public static void initCore() {
        // 1. 初始化基礎除錯系統
        com.myudog.myulib.api.core.debug.DebugLogManager.INSTANCE.install();

        // 2. 初始化控制系統 (包含網路接收器與生命週期掛鉤)
        com.myudog.myulib.api.core.control.ControlManager.INSTANCE.install();
        com.myudog.myulib.internal.control.ControlLifecycleListener.register();

        // 3. 註冊網路載荷 (Payloads)
        com.myudog.myulib.api.core.hologram.network.HologramNetworking.registerPayloads();

        // 4. 初始化相機與計時器系統
        com.myudog.myulib.api.core.camera.CameraApi.initServer();
        com.myudog.myulib.api.core.timer.TimerManager.INSTANCE.install();

        // 5. 註冊指令
        net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            com.myudog.myulib.api.core.control.command.ControlCommand.register(dispatcher);
            com.myudog.myulib.api.core.camera.command.CameraCommand.register(dispatcher);
        });

        ArgumentTypeRegistry.registerArgumentType(
                Identifier.fromNamespaceAndPath(MyulibCore.MOD_ID, "control_type"),
                ControlTypeArgument.class,
                SingletonArgumentInfo.contextFree(ControlTypeArgument::type)
        );
    }
}
