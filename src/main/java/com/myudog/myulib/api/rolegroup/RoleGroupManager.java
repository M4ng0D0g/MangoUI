package com.myudog.myulib.api.rolegroup;

import com.myudog.myulib.Myulib;
import com.myudog.myulib.api.debug.DebugFeature;
import com.myudog.myulib.api.debug.DebugLogManager;
import com.myudog.myulib.api.util.ShortIdRegistry;
import com.myudog.myulib.api.rolegroup.storage.NbtRoleGroupStorage;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.UnaryOperator;

public final class RoleGroupManager {

    public static final RoleGroupManager INSTANCE = new RoleGroupManager();

    

    // 🌟 記憶體狀態
    private final Map<Identifier, RoleGroupDefinition> GROUPS = new ConcurrentHashMap<>();
    private final Map<UUID, Set<Identifier>> PLAYER_GROUPS = new ConcurrentHashMap<>();
    private final ShortIdRegistry ID_REGISTRY = new ShortIdRegistry(6);

    // 🌟 注入你的專用 Storage 介面 (作為 DAO)
    private RoleGroupStorage storage;

    private RoleGroupManager() {}

    public void install() {
        install(new NbtRoleGroupStorage());
    }

    public void install(RoleGroupStorage storageProvider) {
        storage = storageProvider;

        ServerLifecycleEvents.SERVER_STARTING.register(server -> {
            if (storage != null) {
                storage.initialize(server); // 讓 DAO 初始化
                GROUPS.clear();
                PLAYER_GROUPS.clear();
                ID_REGISTRY.clear();

                // 將 DAO 的資料全部載入記憶體
                GROUPS.putAll(storage.loadGroups());
                PLAYER_GROUPS.putAll(storage.loadAssignments());
                for (Identifier id : GROUPS.keySet()) {
                    ID_REGISTRY.generateAndBind(id);
                }
            }

            // 系統啟動時，確保預設的 everyone 身分組存在
            Identifier everyoneId = Identifier.fromNamespaceAndPath(Myulib.MOD_ID, "everyone");
            if (!GROUPS.containsKey(everyoneId)) {
                MutableComponent translationKey = Component.translatable("myulib.rolegroup.everyone");
                register(new RoleGroupDefinition(everyoneId, translationKey, -999, Map.of(), Set.of()));
            }
        });

        ServerLifecycleEvents.SERVER_STOPPING.register(server -> save());
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> clear());
    }

    public RoleGroupDefinition register(RoleGroupDefinition group) {
        if (!validate(group)) {
            throw new IllegalArgumentException("RoleGroupDefinition 驗證失敗: " + (group == null ? "null" : group.id()));
        }

        GROUPS.put(group.id(), group);
        String shortId = ID_REGISTRY.generateAndBind(group.id());
        DebugLogManager.INSTANCE.log(DebugFeature.ROLEGROUP,
                "register id=" + group.id() + ",shortId=" + shortId + ",priority=" + group.priority());
        if (storage != null) storage.saveGroup(group);
        return group;
    }

    public boolean validate(RoleGroupDefinition group) {
        return group != null && group.id() != null && !GROUPS.containsKey(group.id());
    }

    public RoleGroupDefinition update(Identifier groupId, UnaryOperator<RoleGroupDefinition> updater) {
        RoleGroupDefinition existing = GROUPS.get(groupId);
        if (existing == null) return null;
        RoleGroupDefinition updated = updater.apply(existing);
        GROUPS.put(groupId, updated);
        if (storage != null) storage.saveGroup(updated);
        return updated;
    }

    public RoleGroupDefinition delete(Identifier groupId) {
        DebugLogManager.INSTANCE.log(DebugFeature.ROLEGROUP, "delete id=" + groupId + ",shortId=" + ID_REGISTRY.getShortId(groupId));
        if (storage != null) storage.deleteGroup(groupId);
        ID_REGISTRY.unbind(groupId);

        // 同時從所有玩家身上移除該身分組
        PLAYER_GROUPS.values().forEach(set -> set.remove(groupId));
        return GROUPS.remove(groupId);
    }

    public RoleGroupDefinition get(Identifier groupId) { return GROUPS.get(groupId); }
    public List<RoleGroupDefinition> groups() { return List.copyOf(GROUPS.values()); }

    public boolean assign(UUID playerId, Identifier groupId) {
        boolean added = PLAYER_GROUPS.computeIfAbsent(playerId, k -> new LinkedHashSet<>()).add(groupId);
        if (added) {
            DebugLogManager.INSTANCE.log(DebugFeature.ROLEGROUP, "assign player=" + playerId + " -> group=" + groupId);
        }
        if (added && storage != null) storage.saveAssignments(playerId, PLAYER_GROUPS.get(playerId));
        return added;
    }

    public boolean revoke(UUID playerId, Identifier groupId) {
        Set<Identifier> groups = PLAYER_GROUPS.get(playerId);
        if (groups != null && groups.remove(groupId)) {
            DebugLogManager.INSTANCE.log(DebugFeature.ROLEGROUP, "revoke player=" + playerId + " -> group=" + groupId);
            if (storage != null) storage.saveAssignments(playerId, groups);
            return true;
        }
        return false;
    }

    // --- 🎯 雙向查詢系統 (全記憶體運算，瞬間完成) ---

    public Set<UUID> getPlayersInGroup(String groupId) {
        Identifier targetId = toIdentifier(groupId);
        Set<UUID> result = new HashSet<>();
        for (Map.Entry<UUID, Set<Identifier>> entry : PLAYER_GROUPS.entrySet()) {
            if (entry.getValue().contains(targetId)) {
                result.add(entry.getKey());
            }
        }
        return result;
    }

    public List<String> getSortedGroupIdsOf(UUID playerId) {
        Set<Identifier> assignedIds = PLAYER_GROUPS.getOrDefault(playerId, Set.of());

        List<RoleGroupDefinition> assignedGroups = new ArrayList<>();
        for (Identifier id : assignedIds) {
            RoleGroupDefinition def = GROUPS.get(id);
            if (def != null) assignedGroups.add(def);
        }

        // 依據 priority 排序 (需實作或確保 RoleGroupDefinition 有此比較器)
        assignedGroups.sort((a, b) -> Integer.compare(b.priority(), a.priority()));

        List<String> sortedIds = new ArrayList<>();
        for (RoleGroupDefinition def : assignedGroups) {
            sortedIds.add(toGroupName(def.id()));
        }

        sortedIds.remove("everyone");
        sortedIds.add("everyone");

        return sortedIds;
    }

    public Identifier resolveShortId(String shortId) {
        return ID_REGISTRY.getFullId(shortId);
    }

    public String getShortIdOf(Identifier fullId) {
        return ID_REGISTRY.getShortId(fullId);
    }

    private Identifier toIdentifier(String groupId) {
        if (groupId == null || groupId.isBlank()) {
            return Identifier.fromNamespaceAndPath(Myulib.MOD_ID, "everyone");
        }
        if (groupId.contains(":")) {
            return Identifier.parse(groupId);
        }
        return Identifier.fromNamespaceAndPath(Myulib.MOD_ID, groupId);
    }

    private String toGroupName(Identifier id) {
        if (id == null) {
            return "";
        }
        return Myulib.MOD_ID.equals(id.getNamespace()) ? id.getPath() : id.toString();
    }

    public void save() {
        // 如果有批次存檔需求可實作
    }

    public void clear() {
        GROUPS.clear();
        PLAYER_GROUPS.clear();
        ID_REGISTRY.clear();
    }
}