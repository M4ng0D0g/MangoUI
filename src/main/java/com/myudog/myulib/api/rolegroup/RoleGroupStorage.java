package com.myudog.myulib.api.rolegroup;

import net.minecraft.server.MinecraftServer;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.UnaryOperator;

public interface RoleGroupStorage {
    void bindServer(MinecraftServer server);
    void bindRoot(Path root);
    void ensureLoaded();

    RoleGroupDefinition register(RoleGroupDefinition group);
    RoleGroupDefinition update(String groupId, UnaryOperator<RoleGroupDefinition> updater);
    RoleGroupDefinition remove(String groupId);
    RoleGroupDefinition get(String groupId);

    void markDirty();

    List<RoleGroupDefinition> all();

    Map<String, RoleGroupDefinition> snapshot();

    boolean assign(UUID playerId, String groupId);
    boolean revoke(UUID playerId, String groupId);

    Set<String> groupIdsOf(UUID playerId);
    List<RoleGroupDefinition> groupsOf(UUID playerId);

    void clear();

    Set<UUID> getPlayersInGroup(String groupId);
}

