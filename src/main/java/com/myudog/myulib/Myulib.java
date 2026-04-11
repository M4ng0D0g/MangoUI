package com.myudog.myulib;

import com.myudog.myulib.api.MyulibApi;
import com.myudog.myulib.api.field.FieldManager;
import com.myudog.myulib.api.permission.PermissionManager;
import com.myudog.myulib.api.rolegroup.RoleGroupManager;
import net.fabricmc.api.ModInitializer;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class Myulib implements ModInitializer {
    private static final Logger LOGGER = LoggerFactory.getLogger("myulib");
//    Identifier id = Identifier.fromNamespaceAndPath("myudog", "myulib");

    @Override
    public void onInitialize() {
        LOGGER.info("Mango UI is initializing...");

        MyulibApi.init();

        // 1. 伺服器啟動時，綁定路徑並讀取
        ServerLifecycleEvents.SERVER_STARTING.register(server -> {
            FieldManager.bindServer(server);
            PermissionManager.bindServer(server);
            RoleGroupManager.bindServer(server);
        });

        // 2. 伺服器正在關閉時 (停止前)，強制存檔
        ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
            // (假設你的 Field 和 RoleGroup 也有對應的 save 方法)
            PermissionManager.save();
            FieldManager.save();
            RoleGroupManager.save();
        });

        // 3. (強烈建議) 掛載伺服器關閉事件，確保記憶體安全釋放
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> {
            System.out.println("[MyuLib] 伺服器已關閉，清理快取資料...");

            // 釋放記憶體，避免下次啟動或切換單人世界時讀到舊資料
            RoleGroupManager.clear();
            PermissionManager.clear();
            FieldManager.clear();
        });

        LOGGER.info("MyuLib (by MyuDog) has been initialized successfully.");
    }
}


