package com.myudog.myulib.api.permission;

import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import java.nio.file.Path;
import java.util.Map;

public interface PermissionStorage {
    void bindServer(MinecraftServer server);
    void ensureLoaded();

    // 取得各層級的 Scope 資料
    PermissionScope getGlobalScope();
    Map<Identifier, PermissionScope> getDimensionScopes();
    Map<Identifier, PermissionScope> getFieldScopes();

    void markDirty(); // 標記需要存檔
}