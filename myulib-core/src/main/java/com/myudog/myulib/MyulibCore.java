package com.myudog.myulib;

import com.myudog.myulib.api.MyulibApi;
import net.fabricmc.api.ModInitializer;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * MyulibCore
 *
 * 系統：核心框架入口 (Core Framework Entry)
 * 角色：Mod 的主要初始化類別，負責依序啟動所有核心子系統（除錯、控制、相機、計時器等）。
 * 類型：Main / Entry Point
 */
public final class MyulibCore implements ModInitializer {
    public static final String MOD_ID = "myulib-core";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    /**
     * 輔助方法：根據路徑生成屬於本 Mod 的 Identifier。
     *
     * @param path 資源路徑 (如 "control_packet")
     * @return 完整的 Identifier
     */
    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MOD_ID, path.trim().toLowerCase());
    }

    /**
     * Fabric Mod 初始化入口。
     * 此方法按順序引導系統啟動，確保依賴關係正確處理。
     */
    @Override
    public void onInitialize() {
        LOGGER.info("MyuLib Core is initializing...");
        MyulibApi.initCore();
        LOGGER.info("MyuLib Core initialized.");
    }
}
