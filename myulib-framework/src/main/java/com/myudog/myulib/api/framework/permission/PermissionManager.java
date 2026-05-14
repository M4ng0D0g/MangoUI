package com.myudog.myulib.api.framework.permission;

import com.myudog.myulib.Myulib;

import com.myudog.myulib.api.core.util.ShortIdRegistry;
import com.myudog.myulib.api.framework.permission.storage.NbtPermissionStorage;
import com.myudog.myulib.api.core.storage.DataStorage;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.resources.Identifier;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * PermissionManager
 *
 * 蝟餌絞嚗蕭??嚙賜恣?嚙賜頂嚙?(Framework - Permission)
 * 閫嚗蕭??嚙賢惜?嚙賭葉憭殷蕭??嚙賣?嚙賢嚗蕭?鞎祉恣?嚙踝蕭??嚙賢惜蝝蕭??嚙踝蕭??嚙賜雁摨艾?嚙踝蕭??嚙踝蕭??嚙質身摰蕭?
 * 憿蕭?嚗anager / Evaluator
 *
 * 甇斤頂蝯望?嚙賬蕭?撅歹蕭?閬蕭??嚙踝蕭??嚙踝蕭?甈蕭??嚙踝蕭??嚙賢?嚙踝蕭?摨嚙?
 * 1. ?嚙賢 (Field)嚗摰征?嚙踝蕭??嚙踝蕭??嚙踝蕭?閬蕭???
 * 2. 蝬剖漲 (Dimension)嚗摰蕭??嚙踝蕭?憒?嚙賬蕭??嚙踝蕭??嚙踝蕭??嚙踝蕭?
 * 3. ?嚙踝蕭? (Global)嚗蕭??嚙踝蕭?蝵桅?嚙踝蕭?閮哨蕭??嚙踝蕭?
 *
 * 蝞∴蕭??嚙踝蕭??嚙踝蕭?嚙?{@link ShortIdRegistry} ?嚙賣?嚙踝蕭???ID嚗靘輻摰園蕭??嚙賭誘敹恍蕭?摰雁摨佗蕭??嚙賢??
 */
public final class PermissionManager {

    public static final PermissionManager INSTANCE = new PermissionManager();

    /** ?嚙踝蕭?甈蕭?雿?嚙踝蕭?*/
    private PermissionScope globalScope = new PermissionScope();

    /** 蝬剖漲蝝?嚙踝蕭??嚙踝蕭??嚙踝蕭??嚙踝蕭? (Dimension ID -> Scope)??*/
    private final Map<Identifier, PermissionScope> dimensionScopes = new ConcurrentHashMap<>();

    /** ?嚙賢蝝?嚙踝蕭??嚙踝蕭??嚙踝蕭??嚙踝蕭? (Field ID -> Scope)??*/
    private final Map<Identifier, PermissionScope> fieldScopes = new ConcurrentHashMap<>();

    /** 蝬剖漲??ID 閮鳴蕭?銵剁蕭?*/
    private final ShortIdRegistry DIMENSION_ID_REGISTRY = new ShortIdRegistry(6);

    /** ?嚙賢??ID 閮鳴蕭?銵剁蕭?*/
    private final ShortIdRegistry FIELD_ID_REGISTRY = new ShortIdRegistry(6);

    /** ?嚙踝蕭??嚙賢摮蕭??嚙踝蕭?*/
    private DataStorage<String, PermissionScope> storage;

    private PermissionManager() {
    }

    /**
     * 摰蕭?甈蕭?蝟餌絞嚗蕭?閮凋蝙??NBT ?嚙踝蕭???
     */
    public void install() {
        install(new NbtPermissionStorage());
    }

    /**
     * 摰蕭?甈蕭?蝟餌絞銝佗蕭?摰摮蕭?靘蕭?
     * ?嚙踝蕭?隡綽蕭??嚙踝蕭????嚙賣迫鈭辣嚗蕭??嚙踝蕭??嚙踝蕭??嚙踝蕭?頛?嚙賢摮蕭?
     *
     * @param storageProvider ?嚙踝蕭?撖佗蕭?
     */
    public void install(DataStorage<String, PermissionScope> storageProvider) {
        storage = storageProvider;

        ServerLifecycleEvents.SERVER_STARTING.register(server -> {
            if (storage != null) {
                storage.initialize(server);
                DIMENSION_ID_REGISTRY.clear();
                FIELD_ID_REGISTRY.clear();
                dimensionScopes.clear();
                fieldScopes.clear();

                // 頛銝佗蕭??嚙質撠蕭??嚙踝蕭??嚙踝蕭? Map
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

    /** ?嚙踝蕭??嚙踝蕭?雿?嚙踝蕭?*/
    public PermissionScope global() { return globalScope; }

    /** ?嚙踝蕭??嚙賢遣蝡蕭?摰雁摨佗蕭?雿?嚙踝蕭?*/
    public PermissionScope dimension(Identifier dimensionId) {
        return dimensionScopes.computeIfAbsent(dimensionId, k -> {
            DIMENSION_ID_REGISTRY.generateAndBind(k);
            return new PermissionScope();
        });
    }

    /** ?嚙踝蕭??嚙賢遣蝡蕭?摰?嚙踝蕭?雿?嚙踝蕭?*/
    public PermissionScope field(Identifier fieldId) {
        return fieldScopes.computeIfAbsent(fieldId, k -> {
            FIELD_ID_REGISTRY.generateAndBind(k);
            return new PermissionScope();
        });
    }

    /** 撠 ID 嚙???嚙踝蕭??嚙踝蕭?蝬剖漲霅蝣潘蕭?*/
    public Identifier resolveDimensionShortId(String shortId) {
        return DIMENSION_ID_REGISTRY.getFullId(shortId);
    }

    /** ?嚙踝蕭?蝬剖漲?嚙賜 ID??*/
    public String getDimensionShortIdOf(Identifier fullId) {
        return DIMENSION_ID_REGISTRY.getShortId(fullId);
    }

    /** 撠 ID 嚙???嚙踝蕭??嚙踝蕭??嚙賢霅蝣潘蕭?*/
    public Identifier resolveFieldShortId(String shortId) {
        return FIELD_ID_REGISTRY.getFullId(shortId);
    }

    /** ?嚙踝蕭??嚙賢?嚙賜 ID??*/
    public String getFieldShortIdOf(Identifier fullId) {
        return FIELD_ID_REGISTRY.getShortId(fullId);
    }

    /**
     * ?嚙踝蕭?甈蕭?閰摯?嚙質摩??
     * 靘?嚙賢??> 蝬剖漲 > ?嚙踝蕭??嚙踝蕭??嚙踝蕭?蝝蕭?撠蕭?摰蕭?雿莎蕭?鋆蕭???
     *
     * @param playerId     ?嚙賢振 UUID
     * @param playerGroups ?嚙賢振?嚙賢惇嚙??嚙踝蕭?
     * @param action       閬銵蕭??嚙踝蕭?
     * @param fieldId      ?嚙踝蕭??嚙賢 ID (?嚙賜 null)
     * @param dimensionId  ?嚙踝蕭?蝬剖漲 ID (?嚙賜 null)
     * @return ?嚙賜?嚙??嚙賣捱嚙?(ALLOW, DENY, UNSET)
     */
    public PermissionDecision evaluate(UUID playerId, List<String> playerGroups, PermissionAction action, Identifier fieldId, Identifier dimensionId) {
        PermissionDecision decision;

        // 1. ?嚙踝蕭?瑼Ｘ?嚙賢甈蕭?
        if (fieldId != null && fieldScopes.containsKey(fieldId)) {
            decision = fieldScopes.get(fieldId).resolve(playerId, playerGroups, action);
            if (decision != PermissionDecision.UNSET) return decision;
        }

        // 2. 甈∴蕭?瑼Ｘ蝬剖漲甈蕭?
        if (dimensionId != null && dimensionScopes.containsKey(dimensionId)) {
            decision = dimensionScopes.get(dimensionId).resolve(playerId, playerGroups, action);
            if (decision != PermissionDecision.UNSET) return decision;
        }

        // 3. ?嚙賢?瑼?嚙賢?嚙踝蕭???
        decision = globalScope.resolve(playerId, playerGroups, action);
        if (decision != PermissionDecision.UNSET) return decision;

        // ?嚙質身銵
        return PermissionDecision.ALLOW;
    }

    /**
     * 撠蕭??嚙踝蕭?銝哨蕭??嚙?嚙踝蕭??嚙質身摰摮?嚙踝蕭??嚙踝蕭?隞蕭?
     */
    public void save() {
        if (storage != null) {
            storage.save("global", globalScope);
            dimensionScopes.forEach((k, v) -> storage.save("dim:" + k.toString(), v));
            fieldScopes.forEach((k, v) -> storage.save("field:" + k.toString(), v));
        }
    }

    /**
     * 皜征閮擃蕭??嚙踝蕭?
     */
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

    /**
     * 嚙???嚙踝蕭?甈蕭?蝯?嚙踝蕭?雿?嚙賢惜蝝蕭??嚙踝蕭??嚙踝蕭?
     */
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

    /**
     * 嚙??甈蕭?蝯蕭??嚙踝蕭?撅歹蕭?雿蛛蕭??嚙踝蕭?蝯蕭??嚙踝蕭?
     */
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

    /**
     * 璅蕭??嚙踝蕭??嚙踝蕭??嚙賜迂??
     *
     * @param groupName ?嚙踝蕭?蝯蕭?
     * @return 璅蕭??嚙踝蕭??嚙踝蕭???
     */
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
