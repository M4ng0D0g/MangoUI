package com.myudog.myulib.api.framework.rolegroup;

import com.myudog.myulib.api.core.debug.DebugFeature;
import com.myudog.myulib.api.core.debug.DebugLogManager;
import com.myudog.myulib.api.framework.rolegroup.storage.NbtRoleGroupStorage;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.UnaryOperator;
import java.nio.charset.StandardCharsets;

/**
 * RoleGroupManager
 *
 * 系統：角色組管理系統 (Framework - RoleGroup)
 * 角色：管理身分標籤（角色組），並將其分配給玩家。
 * 類型：Manager / Identity Provider
 */
public final class RoleGroupManager {

    public static final RoleGroupManager INSTANCE = new RoleGroupManager();

    private final Map<UUID, RoleGroupDefinition> GROUPS = new ConcurrentHashMap<>();
    private final Map<UUID, Set<UUID>> PLAYER_GROUPS = new ConcurrentHashMap<>();
    private RoleGroupStorage storage;

    private RoleGroupManager() {
    }

    public void install() {
        install(new NbtRoleGroupStorage());
    }

    public void install(RoleGroupStorage storageProvider) {
        storage = storageProvider;

        ServerLifecycleEvents.SERVER_STARTING.register(server -> {
            if (storage != null) {
                storage.initialize(server);
                GROUPS.clear();
                PLAYER_GROUPS.clear();

                GROUPS.putAll(storage.loadGroups());
                PLAYER_GROUPS.putAll(storage.loadAssignments());
            }

            // Ensure default everyone group exists
            UUID everyoneUuid = stableUuid("everyone");
            if (!GROUPS.containsKey(everyoneUuid)) {
                MutableComponent translationKey = Component.translatable("myulib.rolegroup.everyone");
                register(new RoleGroupDefinition(everyoneUuid, translationKey, -999, Map.of(), Set.of()));
            }
        });

        ServerLifecycleEvents.SERVER_STOPPING.register(server -> save());
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> clear());
    }

    public RoleGroupDefinition register(RoleGroupDefinition group) {
        if (!validate(group)) {
            throw new IllegalArgumentException("RoleGroupDefinition validation failed: " + (group == null ? "null" : group.uuid()));
        }

        GROUPS.put(group.uuid(), group);
        DebugLogManager.INSTANCE.log(DebugFeature.ROLEGROUP, "register uuid=" + group.uuid() + ",priority=" + group.priority());
        if (storage != null) storage.saveGroup(group);
        return group;
    }

    public boolean validate(RoleGroupDefinition group) {
        return group != null && !GROUPS.containsKey(group.uuid());
    }

    public RoleGroupDefinition update(UUID groupUuid, UnaryOperator<RoleGroupDefinition> updater) {
        RoleGroupDefinition existing = GROUPS.get(groupUuid);
        if (existing == null) return null;
        RoleGroupDefinition updated = updater.apply(existing);
        GROUPS.put(groupUuid, updated);
        if (storage != null) storage.saveGroup(updated);
        return updated;
    }

    public RoleGroupDefinition update(net.minecraft.resources.Identifier groupUuid, UnaryOperator<RoleGroupDefinition> updater) {
        return update(stableUuid(groupUuid.toString()), updater);
    }

    public RoleGroupDefinition delete(UUID groupUuid) {
        DebugLogManager.INSTANCE.log(DebugFeature.ROLEGROUP, "delete uuid=" + groupUuid);
        if (storage != null) storage.deleteGroup(groupUuid);
        PLAYER_GROUPS.values().forEach(set -> set.remove(groupUuid));
        return GROUPS.remove(groupUuid);
    }

    public RoleGroupDefinition delete(net.minecraft.resources.Identifier groupUuid) {
        return delete(stableUuid(groupUuid.toString()));
    }

    public RoleGroupDefinition get(UUID groupUuid) { return GROUPS.get(groupUuid); }
    public RoleGroupDefinition get(net.minecraft.resources.Identifier groupUuid) { return get(stableUuid(groupUuid.toString())); }

    public List<RoleGroupDefinition> groups() { return List.copyOf(GROUPS.values()); }

    public boolean assign(UUID playerId, UUID groupUuid) {
        boolean added = PLAYER_GROUPS.computeIfAbsent(playerId, ignored -> new LinkedHashSet<>()).add(groupUuid);
        if (added) {
            DebugLogManager.INSTANCE.log(DebugFeature.ROLEGROUP, "assign player=" + playerId + " -> group=" + groupUuid);
            if (storage != null) storage.saveAssignments(playerId, PLAYER_GROUPS.get(playerId));
        }
        return added;
    }

    public boolean assign(UUID playerId, net.minecraft.resources.Identifier groupUuid) {
        return assign(playerId, stableUuid(groupUuid.toString()));
    }

    public boolean revoke(UUID playerId, UUID groupUuid) {
        Set<UUID> groups = PLAYER_GROUPS.get(playerId);
        if (groups == null) return false;
        boolean removed = groups.remove(groupUuid);
        if (removed) {
            DebugLogManager.INSTANCE.log(DebugFeature.ROLEGROUP, "revoke player=" + playerId + " <- group=" + groupUuid);
            if (storage != null) storage.saveAssignments(playerId, groups);
        }
        return removed;
    }

    public boolean revoke(UUID playerId, net.minecraft.resources.Identifier groupUuid) {
        return revoke(playerId, stableUuid(groupUuid.toString()));
    }

    public Set<UUID> getAssignedGroups(UUID playerId) {
        Set<UUID> groups = new LinkedHashSet<>(PLAYER_GROUPS.getOrDefault(playerId, Set.of()));
        groups.add(stableUuid("everyone"));
        return groups;
    }

    public List<RoleGroupDefinition> getAssignedDefinitions(UUID playerId) {
        return getAssignedGroups(playerId).stream()
                .map(GROUPS::get)
                .filter(Objects::nonNull)
                .sorted(Comparator.comparingInt(RoleGroupDefinition::priority).reversed())
                .toList();
    }

    public List<String> getSortedGroupIdsOf(UUID playerId) {
        return getAssignedDefinitions(playerId).stream()
                .map(def -> def.uuid().toString())
                .toList();
    }

    public void save() {
        if (storage != null) {
            GROUPS.values().forEach(storage::saveGroup);
            PLAYER_GROUPS.forEach(storage::saveAssignments);
        }
    }

    public void clear() {
        GROUPS.clear();
        PLAYER_GROUPS.clear();
    }

    private static UUID stableUuid(String token) {
        return UUID.nameUUIDFromBytes(token.getBytes(StandardCharsets.UTF_8));
    }
}
