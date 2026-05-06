package com.myudog.myulib.api.permission.storage;

import com.myudog.myulib.api.permission.PermissionScope;
import com.myudog.myulib.api.storage.DataStorage;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;

import java.util.Map;

public class SqlPermissionStorage implements DataStorage<Identifier, PermissionScope> {
    @Override
    public void initialize(MinecraftServer server) {
        // 建立 HikariCP 連線池，執行 CREATE TABLE IF NOT EXISTS ...
    }

    @Override
    public Map<Identifier, PermissionScope> loadAll() {
        return Map.of();
    }

    @Override
    public void save(Identifier id, PermissionScope data) {
        // 💡 實務建議：資料庫操作應放入非同步執行緒 (CompletableFuture.runAsync)
        // 避免 INSERT/UPDATE 延遲導致 Minecraft 主執行緒 (TPS) 卡頓！
    }

    @Override
    public void delete(Identifier id) {

    }
    // ...
}