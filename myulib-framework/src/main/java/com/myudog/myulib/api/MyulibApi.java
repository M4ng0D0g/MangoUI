package com.myudog.myulib.api;

import com.myudog.myulib.api.framework.field.FieldManager;
import com.myudog.myulib.api.framework.permission.PermissionManager;
import com.myudog.myulib.api.framework.rolegroup.RoleGroupManager;
import com.myudog.myulib.api.framework.ui.network.ConfigUiNetworking;

public final class MyulibApi {
    /**
     * 初始化框架層系統。
     * 這些系統依賴於 Core 層，但其邏輯屬於 Framework。
     */
    public static void initFramework() {
        // 1. 初始化權限與區域管理系統 (包含 NBT 儲存註冊)
        PermissionManager.INSTANCE.install();
        FieldManager.INSTANCE.install();
        RoleGroupManager.INSTANCE.install();

        // 2. 初始化框架層網路 (UI 等)
        ConfigUiNetworking.registerPayloads();
        ConfigUiNetworking.registerServerReceivers();

        // 3. 初始化高階子系統 (指令、UI 橋接等)
        AccessSystems.init();
    }

    /**
     * 過渡期方法：保留以維持相容性，但核心初始化現在應由 MyulibCore 直接負責。
     * @deprecated 請改用 MyulibCore 或直接呼叫 initFramework()。
     */
    @Deprecated
    public static void initCore() {
        // Core 層現在已在 MyulibCore.onInitialize() 中自我初始化。
    }
}
