package com.myudog.myulib.api.framework.team.storage;

import com.myudog.myulib.api.core.storage.DataStorage;
import com.myudog.myulib.api.framework.team.TeamDefinition;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;

import java.util.Map;

public class SqlTeamStorage implements DataStorage<Identifier, TeamDefinition> {
    @Override
    public void initialize(MinecraftServer server) {
        // 撱箇? HikariCP ???瘙??瑁? CREATE TABLE IF NOT EXISTS ...
    }

    @Override
    public Map<Identifier, TeamDefinition> loadAll() {
        return Map.of();
    }

    @Override
    public void save(Identifier id, TeamDefinition data) {
        // ? 撖血?撱箄降嚗??澈????仿??郊?瑁?蝺?(CompletableFuture.runAsync)
        // ?踹? INSERT/UPDATE 撱園撠 Minecraft 銝餃銵? (TPS) ?⊿?嚗?
    }

    @Override
    public void delete(Identifier id) {

    }
    // ...
}
