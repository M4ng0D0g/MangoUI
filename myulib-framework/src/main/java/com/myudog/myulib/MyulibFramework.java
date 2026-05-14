package com.myudog.myulib;

import net.fabricmc.api.ModInitializer;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * MyulibFramework
 *
 * 系統：框架層入口 (Framework Layer Entry)
 * 角色：MyuLib 框架層的 Fabric 初始化器，負責啟動高階業務系統。
 * 類型：Entry Point / ModInitializer
 *
 * 此模組依賴於 `myulib-core`，提供了如權限、隊伍、遊戲場地等更具體的遊戲邏輯抽象。
 */
public final class MyulibFramework implements ModInitializer {
    public static final String MOD_ID = "myulib-framework";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    /**
     * 建立基於框架層命名空間的 Identifier。
     *
     * @param path 路徑字串
     * @return 框架層 Identifier
     */
    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MOD_ID, path.trim().toLowerCase());
    }

    /**
     * Fabric 載入時的初始化方法。
     * 調用 {@link com.myudog.myulib.api.MyulibApi#initFramework()} 來啟動所有子系統。
     */
    @Override
    public void onInitialize() {
        LOGGER.info("MyuLib Framework is initializing...");
        com.myudog.myulib.api.MyulibFrameworkApi.initFramework();
        LOGGER.info("MyuLib Framework initialized.");
    }
}
