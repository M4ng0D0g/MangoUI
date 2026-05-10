package com.myudog.myulib.internal.control;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 全域控制關係雙向索引 (Global Relational Registry)
 * <p>
 * 維護：
 *   controllerToTargets : UUID(Player) → Set<UUID>(Entities)
 *   targetToControllers : UUID(Entity)  → Set<UUID>(Players)
 * <p>
 * 所有操作僅涉及 UUID，無實體強引用，天然規避記憶體洩漏。
 * O(1) 雙向查詢，支援一對多與多對一。
 */
public final class ControlRegistry {

    public static final ControlRegistry INSTANCE = new ControlRegistry();

    // 控制者 → 被控實體集合
    private final Map<UUID, Set<UUID>> controllerToTargets = new ConcurrentHashMap<>();
    // 被控實體 → 控制者集合
    private final Map<UUID, Set<UUID>> targetToControllers = new ConcurrentHashMap<>();

    private ControlRegistry() {}

    // -------------------------------------------------------------------------
    // Mutation
    // -------------------------------------------------------------------------

    /**
     * 登記一筆控制關係。
     *
     * @return 若為新關係回傳 true；已存在則回傳 false
     */
    public boolean addBinding(UUID controllerId, UUID targetId) {
        if (controllerId == null || targetId == null) return false;

        boolean added = controllerToTargets
                .computeIfAbsent(controllerId, k -> ConcurrentHashMap.newKeySet())
                .add(targetId);

        if (added) {
            targetToControllers
                    .computeIfAbsent(targetId, k -> ConcurrentHashMap.newKeySet())
                    .add(controllerId);
        }
        return added;
    }

    /**
     * 移除特定控制者→目標的單一關係。
     */
    public boolean removeBinding(UUID controllerId, UUID targetId) {
        if (controllerId == null || targetId == null) return false;

        boolean removed = false;

        Set<UUID> targets = controllerToTargets.get(controllerId);
        if (targets != null) {
            removed = targets.remove(targetId);
            if (targets.isEmpty()) controllerToTargets.remove(controllerId);
        }

        Set<UUID> controllers = targetToControllers.get(targetId);
        if (controllers != null) {
            controllers.remove(controllerId);
            if (controllers.isEmpty()) targetToControllers.remove(targetId);
        }

        return removed;
    }

    /**
     * 移除某控制者的所有綁定關係（玩家斷線時呼叫）。
     */
    public void removeAllByController(UUID controllerId) {
        if (controllerId == null) return;
        Set<UUID> targets = controllerToTargets.remove(controllerId);
        if (targets == null) return;
        for (UUID targetId : targets) {
            Set<UUID> controllers = targetToControllers.get(targetId);
            if (controllers != null) {
                controllers.remove(controllerId);
                if (controllers.isEmpty()) targetToControllers.remove(targetId);
            }
        }
    }

    /**
     * 移除某目標實體的所有綁定關係（實體死亡/移除時呼叫）。
     */
    public void removeAllByTarget(UUID targetId) {
        if (targetId == null) return;
        Set<UUID> controllers = targetToControllers.remove(targetId);
        if (controllers == null) return;
        for (UUID controllerId : controllers) {
            Set<UUID> targets = controllerToTargets.get(controllerId);
            if (targets != null) {
                targets.remove(targetId);
                if (targets.isEmpty()) controllerToTargets.remove(controllerId);
            }
        }
    }

    // -------------------------------------------------------------------------
    // Query
    // -------------------------------------------------------------------------

    /** 查詢某控制者正在控制的所有目標 UUID */
    public Set<UUID> getTargets(UUID controllerId) {
        Set<UUID> s = controllerToTargets.get(controllerId);
        return s == null ? Set.of() : Collections.unmodifiableSet(s);
    }

    /** 查詢某目標被哪些控制者控制 */
    public Set<UUID> getControllers(UUID targetId) {
        Set<UUID> s = targetToControllers.get(targetId);
        return s == null ? Set.of() : Collections.unmodifiableSet(s);
    }

    /** 快速判斷某實體是否正在被任何玩家控制 (O(1)) */
    public boolean isPossessed(UUID entityId) {
        Set<UUID> s = targetToControllers.get(entityId);
        return s != null && !s.isEmpty();
    }

    /** 快速判斷某玩家是否正在控制任何實體 */
    public boolean isControlling(UUID playerId) {
        Set<UUID> s = controllerToTargets.get(playerId);
        return s != null && !s.isEmpty();
    }
}
