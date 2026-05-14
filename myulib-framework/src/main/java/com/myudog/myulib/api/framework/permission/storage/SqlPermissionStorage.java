package com.myudog.myulib.api.framework.permission.storage;

import com.myudog.myulib.api.framework.permission.PermissionScope;
import com.myudog.myulib.api.core.storage.DataStorage;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;

import java.util.Map;

public class SqlPermissionStorage implements DataStorage<Identifier, PermissionScope> {
    @Override
    public void initialize(MinecraftServer server) {
        // 撱箇? HikariCP ???瘙??瑁? CREATE TABLE IF NOT EXISTS ...
    }

    @Override
    public Map<Identifier, PermissionScope> loadAll() {
        return Map.of();
    }

    @Override
    public void save(Identifier id, PermissionScope data) {
        // ? 撖血?撱箄降嚗??澈????仿??郊?瑁?蝺?(CompletableFuture.runAsync)
        // ?踹? INSERT/UPDATE 撱園撠 Minecraft 銝餃銵? (TPS) ?⊿?嚗?
    }

    @Override
    public void delete(Identifier id) {

    }
    // ...
}
