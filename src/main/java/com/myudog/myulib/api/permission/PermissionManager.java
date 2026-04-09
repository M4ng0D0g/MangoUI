package com.myudog.myulib.api.permission;

import com.myudog.myulib.api.identity.IdentityGroupDefinition;
import com.myudog.myulib.api.identity.IdentityManager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.UnaryOperator;

public final class PermissionManager {
    private static final List<PermissionGrant> GLOBAL_RULES = new ArrayList<>();
    private static final Map<String, List<PermissionGrant>> DIMENSION_RULES = new LinkedHashMap<>();
    private static final Map<String, List<PermissionGrant>> FIELD_RULES = new LinkedHashMap<>();
    private static final Map<UUID, List<PermissionGrant>> USER_RULES = new LinkedHashMap<>();

    private PermissionManager() {
    }

    public static void install() {
    }

    public static PermissionGrant grantGlobal(PermissionGrant grant) {
        return grantToList(GLOBAL_RULES, requireLayer(grant, PermissionLayer.GLOBAL));
    }

    public static PermissionGrant grantDimension(String dimensionId, PermissionGrant grant) {
        Objects.requireNonNull(dimensionId, "dimensionId");
        return grantToMap(DIMENSION_RULES, dimensionId, requireLayer(grant, PermissionLayer.DIMENSION));
    }

    public static PermissionGrant grantField(String fieldId, PermissionGrant grant) {
        Objects.requireNonNull(fieldId, "fieldId");
        return grantToMap(FIELD_RULES, fieldId, requireLayer(grant, PermissionLayer.FIELD));
    }

    public static PermissionGrant grantUser(UUID playerId, PermissionGrant grant) {
        Objects.requireNonNull(playerId, "playerId");
        return grantToMap(USER_RULES, playerId, requireLayer(grant, PermissionLayer.USER));
    }

    public static PermissionGrant updateGlobal(String grantId, UnaryOperator<PermissionGrant> updater) {
        return updateInList(GLOBAL_RULES, grantId, PermissionLayer.GLOBAL, updater);
    }

    public static PermissionGrant updateDimension(String dimensionId, String grantId, UnaryOperator<PermissionGrant> updater) {
        return updateInMap(DIMENSION_RULES, dimensionId, grantId, PermissionLayer.DIMENSION, updater);
    }

    public static PermissionGrant updateField(String fieldId, String grantId, UnaryOperator<PermissionGrant> updater) {
        return updateInMap(FIELD_RULES, fieldId, grantId, PermissionLayer.FIELD, updater);
    }

    public static PermissionGrant updateUser(UUID playerId, String grantId, UnaryOperator<PermissionGrant> updater) {
        return updateInMap(USER_RULES, playerId, grantId, PermissionLayer.USER, updater);
    }

    public static PermissionResolution evaluate(PermissionContext context) {
        Objects.requireNonNull(context, "context");
        PermissionResolution resolution = resolveLayer(PermissionLayer.USER, context.playerId().toString(), USER_RULES.get(context.playerId()), context);
        if (!resolution.isUnset()) {
            return resolution;
        }
        resolution = resolveLayer(PermissionLayer.FIELD, context.fieldId(), FIELD_RULES.get(context.fieldId()), context);
        if (!resolution.isUnset()) {
            return resolution;
        }
        resolution = resolveLayer(PermissionLayer.DIMENSION, context.dimensionId(), DIMENSION_RULES.get(context.dimensionId()), context);
        if (!resolution.isUnset()) {
            return resolution;
        }
        resolution = resolveLayer(PermissionLayer.GLOBAL, "global", GLOBAL_RULES, context);
        return resolution.isUnset() ? PermissionResolution.unset() : resolution;
    }

    public static boolean isDenied(PermissionContext context) {
        return evaluate(context).isDenied();
    }

    public static PermissionResolution evaluate(WorldInteractionPermissionContext context) {
        return evaluate(context.toPermissionContext());
    }

    public static boolean isDenied(WorldInteractionPermissionContext context) {
        return isDenied(context.toPermissionContext());
    }

    public static void clear() {
        GLOBAL_RULES.clear();
        DIMENSION_RULES.clear();
        FIELD_RULES.clear();
        USER_RULES.clear();
    }

    public static List<PermissionGrant> globalRules() {
        return List.copyOf(GLOBAL_RULES);
    }

    public static Map<String, List<PermissionGrant>> dimensionRules() {
        return snapshotMap(DIMENSION_RULES);
    }

    public static Map<String, List<PermissionGrant>> fieldRules() {
        return snapshotMap(FIELD_RULES);
    }

    public static Map<UUID, List<PermissionGrant>> userRules() {
        return snapshotMap(USER_RULES);
    }

    private static PermissionGrant requireLayer(PermissionGrant grant, PermissionLayer expected) {
        Objects.requireNonNull(grant, "grant");
        if (grant.layer() != expected) {
            throw new IllegalArgumentException("Grant layer must be " + expected + ": " + grant.layer());
        }
        return grant;
    }

    private static PermissionGrant grantToList(List<PermissionGrant> grants, PermissionGrant grant) {
        grants.add(grant);
        grants.sort((a, b) -> Integer.compare(b.priority(), a.priority()));
        return grant;
    }

    private static <K> PermissionGrant grantToMap(Map<K, List<PermissionGrant>> rules, K key, PermissionGrant grant) {
        List<PermissionGrant> grants = rules.computeIfAbsent(key, ignored -> new ArrayList<>());
        grants.add(grant);
        grants.sort((a, b) -> Integer.compare(b.priority(), a.priority()));
        return grant;
    }

    private static PermissionGrant updateInList(List<PermissionGrant> grants, String grantId, PermissionLayer expectedLayer, UnaryOperator<PermissionGrant> updater) {
        Objects.requireNonNull(grantId, "grantId");
        Objects.requireNonNull(updater, "updater");
        for (int i = 0; i < grants.size(); i++) {
            PermissionGrant existing = grants.get(i);
            if (existing.id().equals(grantId) && existing.layer() == expectedLayer) {
                PermissionGrant updated = requireLayer(Objects.requireNonNull(updater.apply(existing), "updated grant"), expectedLayer);
                grants.set(i, updated);
                grants.sort((a, b) -> Integer.compare(b.priority(), a.priority()));
                return updated;
            }
        }
        return null;
    }

    private static <K> PermissionGrant updateInMap(Map<K, List<PermissionGrant>> rules, K key, String grantId, PermissionLayer expectedLayer, UnaryOperator<PermissionGrant> updater) {
        Objects.requireNonNull(key, "key");
        List<PermissionGrant> grants = rules.get(key);
        if (grants == null) {
            return null;
        }
        return updateInList(grants, grantId, expectedLayer, updater);
    }

    private static <K> Map<K, List<PermissionGrant>> snapshotMap(Map<K, List<PermissionGrant>> source) {
        Map<K, List<PermissionGrant>> snapshot = new LinkedHashMap<>();
        for (Map.Entry<K, List<PermissionGrant>> entry : source.entrySet()) {
            snapshot.put(entry.getKey(), List.copyOf(entry.getValue()));
        }
        return Collections.unmodifiableMap(snapshot);
    }

    private static PermissionResolution resolveLayer(PermissionLayer layer, String sourceId, List<PermissionGrant> grants, PermissionContext context) {
        PermissionGrant best = bestMatch(layer, grants, context.permissionNode());
        if (best != null) {
            return new PermissionResolution(best.decision(), layer, sourceId, best.id());
        }

        PermissionGrant groupBest = null;
        for (IdentityGroupDefinition group : IdentityManager.groupsOf(context.playerId())) {
            PermissionGrant match = bestMatch(layer, group.grants(), context.permissionNode());
            if (match != null && (groupBest == null || match.priority() > groupBest.priority())) {
                groupBest = match;
            }
        }
        return groupBest == null ? PermissionResolution.unset() : new PermissionResolution(groupBest.decision(), layer, sourceId, groupBest.id());
    }

    private static PermissionGrant bestMatch(PermissionLayer layer, List<PermissionGrant> grants, String requestedNode) {
        if (grants == null || grants.isEmpty()) {
            return null;
        }
        PermissionGrant best = null;
        for (PermissionGrant grant : grants) {
            if (grant.layer() == layer && grant.matches(requestedNode) && (best == null || grant.priority() > best.priority())) {
                best = grant;
            }
        }
        return best;
    }
}

