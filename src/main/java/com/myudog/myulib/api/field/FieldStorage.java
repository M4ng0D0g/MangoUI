package com.myudog.myulib.api.field;

import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import java.util.Map;

public interface FieldStorage {
    void bindServer(MinecraftServer server);
    void ensureLoaded();

    void add(FieldDefinition field);
    void remove(Identifier id);
    Map<Identifier, FieldDefinition> getAll();

    void markDirty();
}