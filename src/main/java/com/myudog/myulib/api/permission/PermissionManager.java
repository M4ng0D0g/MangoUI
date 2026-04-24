package com.myudog.myulib.api.permission;

import com.myudog.myulib.Myulib;
import com.myudog.myulib.api.util.ShortIdRegistry;
import com.myudog.myulib.api.permission.storage.NbtPermissionStorage;
import com.myudog.myulib.api.storage.DataStorage;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.resources.Identifier;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public final class PermissionManager {

    public static final PermissionManager INSTANCE = new PermissionManager();

    

    // 🌟 記憶體狀態分離：效能最大化
    private PermissionScope globalScope = new PermissionScope();
    private final Map<Identifier, PermissionScope> dimensionScopes = new ConcurrentHashMap<>();
    private final Map<Identifier, PermissionScope> fieldScopes = new ConcurrentHashMap<>();
    private final ShortIdRegistry DIMENSION_ID_REGISTRY = new ShortIdRegistry(6);
    private final ShortIdRegistry FIELD_ID_REGISTRY = new ShortIdRegistry(6);

    // 🌟 使用統一字串 Key 的儲存介面
    private DataStorage<String, PermissionScope> storage;

    private PermissionManager() {}

    public void install() {
        install(new NbtPermissionStorage());
    }

    public void install(DataStorage<String, PermissionScope> storageProvider) {
        storage = storageProvider;

        ServerLifecycleEvents.SERVER_STARTING.register(server -> {
            if (storage != null) {
                storage.initialize(server);
                DIMENSION_ID_REGISTRY.clear();
                FIELD_ID_REGISTRY.clear();
                dimensionScopes.clear();
                fieldScopes.clear();

                // 載入並分配至對應的記憶體 Map
                Map<String, PermissionScope> loaded = storage.loadAll();
                if (loaded != null) {
                    for (Map.Entry<String, PermissionScope> entry : loaded.entrySet()) {
                        String key = entry.getKey();
                        if (key.equals("global")) {
                            globalScope = entry.getValue();
                        } else if (key.startsWith("dim:")) {
                            Identifier id = Identifier.parse(key.substring(4));
                            dimensionScopes.put(id, entry.getValue());
                            DIMENSION_ID_REGISTRY.generateAndBind(id);
                        } else if (key.startsWith("field:")) {
                            Identifier id = Identifier.parse(key.substring(6));
                            fieldScopes.put(id, entry.getValue());
                            FIELD_ID_REGISTRY.generateAndBind(id);
                        }
                    }
                }
            }
        });

        ServerLifecycleEvents.SERVER_STOPPING.register(server -> save());
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> clear());
    }

    public PermissionScope global() { return globalScope; }

    public PermissionScope dimension(Identifier dimensionId) {
        return dimensionScopes.computeIfAbsent(dimensionId, k -> {
            DIMENSION_ID_REGISTRY.generateAndBind(k);
            return new PermissionScope();
        });
    }

    public PermissionScope field(Identifier fieldId) {
        return fieldScopes.computeIfAbsent(fieldId, k -> {
            FIELD_ID_REGISTRY.generateAndBind(k);
            return new PermissionScope();
        });
    }

    public Identifier resolveDimensionShortId(String shortId) {
        return DIMENSION_ID_REGISTRY.getFullId(shortId);
    }

    public String getDimensionShortIdOf(Identifier fullId) {
        return DIMENSION_ID_REGISTRY.getShortId(fullId);
    }

    public Identifier resolveFieldShortId(String shortId) {
        return FIELD_ID_REGISTRY.getFullId(shortId);
    }

    public String getFieldShortIdOf(Identifier fullId) {
        return FIELD_ID_REGISTRY.getShortId(fullId);
    }

    // ... (保留 getScopeMergedTable 與 getFinalPermissions 不變) ...

    public PermissionDecision evaluate(UUID playerId, List<String> playerGroups, PermissionAction action, Identifier fieldId, Identifier dimensionId) {
        PermissionDecision decision;

        if (fieldId != null && fieldScopes.containsKey(fieldId)) {
            decision = fieldScopes.get(fieldId).resolve(playerId, playerGroups, action);
            if (decision != PermissionDecision.UNSET) return decision;
        }

        if (dimensionId != null && dimensionScopes.containsKey(dimensionId)) {
            decision = dimensionScopes.get(dimensionId).resolve(playerId, playerGroups, action);
            if (decision != PermissionDecision.UNSET) return decision;
        }

        decision = globalScope.resolve(playerId, playerGroups, action);
        if (decision != PermissionDecision.UNSET) return decision;

        return PermissionDecision.ALLOW;
    }

    public void save() {
        if (storage != null) {
            storage.save("global", globalScope);
            dimensionScopes.forEach((k, v) -> storage.save("dim:" + k.toString(), v));
            fieldScopes.forEach((k, v) -> storage.save("field:" + k.toString(), v));
        }
    }

    public void clear() {
        globalScope = new PermissionScope();
        dimensionScopes.clear();
        fieldScopes.clear();
        DIMENSION_ID_REGISTRY.clear();
        FIELD_ID_REGISTRY.clear();
    }

    public PermissionScope dimensionIfPresent(Identifier dimensionId) {
        return dimensionScopes.get(dimensionId);
    }

    public PermissionScope fieldIfPresent(Identifier fieldId) {
        return fieldScopes.get(fieldId);
    }

    public PermissionDecision resolveGroupInScope(String groupName, PermissionAction action, ScopeLayer scopeLayer, Identifier scopeId) {
        String normalizedGroup = normalizeGroupName(groupName);
        return switch (scopeLayer) {
            case GLOBAL -> globalScope.forGroup(normalizedGroup).get(action);
            case DIMENSION -> {
                PermissionScope scope = scopeId == null ? null : dimensionScopes.get(scopeId);
                yield scope == null ? PermissionDecision.UNSET : scope.forGroup(normalizedGroup).get(action);
            }
            case FIELD -> {
                PermissionScope scope = scopeId == null ? null : fieldScopes.get(scopeId);
                yield scope == null ? PermissionDecision.UNSET : scope.forGroup(normalizedGroup).get(action);
            }
            case USER -> PermissionDecision.UNSET;
        };
    }

    public PermissionDecision resolveGroupMerged(String groupName, PermissionAction action, Identifier fieldId, Identifier dimensionId) {
        String normalizedGroup = normalizeGroupName(groupName);

        if (fieldId != null) {
            PermissionScope fieldScope = fieldScopes.get(fieldId);
            if (fieldScope != null) {
                PermissionDecision decision = fieldScope.forGroup(normalizedGroup).get(action);
                if (decision != PermissionDecision.UNSET) {
                    return decision;
                }
            }
        }

        if (dimensionId != null) {
            PermissionScope dimensionScope = dimensionScopes.get(dimensionId);
            if (dimensionScope != null) {
                PermissionDecision decision = dimensionScope.forGroup(normalizedGroup).get(action);
                if (decision != PermissionDecision.UNSET) {
                    return decision;
                }
            }
        }

        PermissionDecision globalDecision = globalScope.forGroup(normalizedGroup).get(action);
        if (globalDecision != PermissionDecision.UNSET) {
            return globalDecision;
        }

        return PermissionDecision.UNSET;
    }

    public Set<Identifier> dimensionScopeIds() {
        return Set.copyOf(dimensionScopes.keySet());
    }

    public Set<Identifier> fieldScopeIds() {
        return Set.copyOf(fieldScopes.keySet());
    }

    public Set<String> knownGroupNames() {
        Set<String> names = new LinkedHashSet<>();
        names.addAll(globalScope.groupTablesSnapshot().keySet());
        for (PermissionScope scope : dimensionScopes.values()) {
            names.addAll(scope.groupTablesSnapshot().keySet());
        }
        for (PermissionScope scope : fieldScopes.values()) {
            names.addAll(scope.groupTablesSnapshot().keySet());
        }
        names.add("everyone");
        return Set.copyOf(names);
    }

    public String normalizeGroupName(String groupName) {
        if (groupName == null || groupName.isBlank()) {
            return "everyone";
        }
        String value = groupName.trim();
        if (value.contains(":")) {
            try {
                Identifier parsed = Identifier.parse(value);
                if (Myulib.MOD_ID.equals(parsed.getNamespace())) {
                    return parsed.getPath();
                }
                return parsed.toString();
            } catch (Exception ignored) {
                return value;
            }
        }
        return value;
    }
}