package com.myudog.myulib.api;

import com.myudog.myulib.api.framework.field.FieldManager;
import com.myudog.myulib.api.framework.permission.PermissionManager;
import com.myudog.myulib.api.framework.rolegroup.RoleGroupManager;
import com.myudog.myulib.api.framework.ui.network.ConfigUiNetworking;

/**
 * MyulibFrameworkApi
 *
 * 系統：框架層入口 (Framework Layer Entry)
 */
public final class MyulibFrameworkApi {

    public static void initFramework() {
        PermissionManager.INSTANCE.install();
        FieldManager.INSTANCE.install();
        RoleGroupManager.INSTANCE.install();
        com.myudog.myulib.api.framework.game.core.GameManager.INSTANCE.install();

        ConfigUiNetworking.registerPayloads();
        ConfigUiNetworking.registerServerReceivers();

        AccessSystems.init();
    }
}
