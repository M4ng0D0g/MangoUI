package com.myudog.myulib.api.ui.network;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.myudog.myulib.Myulib;
import com.myudog.myulib.api.field.FieldDefinition;
import com.myudog.myulib.api.field.FieldManager;
import com.myudog.myulib.api.permission.PermissionAction;
import com.myudog.myulib.api.permission.PermissionDecision;
import com.myudog.myulib.api.permission.PermissionManager;
import com.myudog.myulib.api.permission.PermissionScope;
import com.myudog.myulib.api.permission.ScopeLayer;
import com.myudog.myulib.api.rolegroup.RoleGroupDefinition;
import com.myudog.myulib.api.rolegroup.RoleGroupManager;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.Permission;
import net.minecraft.server.permissions.PermissionLevel;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class ConfigUiNetworking {
    private static final Gson GSON = new Gson();
    public static final Identifier SNAPSHOT_REQUEST_CHANNEL = Identifier.fromNamespaceAndPath(Myulib.MOD_ID, "config_snapshot_request");
    public static final Identifier SNAPSHOT_CHANNEL = Identifier.fromNamespaceAndPath(Myulib.MOD_ID, "config_snapshot");
    public static final Identifier APPLY_CHANNEL = Identifier.fromNamespaceAndPath(Myulib.MOD_ID, "config_apply");
    public static final Identifier APPLY_RESULT_CHANNEL = Identifier.fromNamespaceAndPath(Myulib.MOD_ID, "config_apply_result");

    private static boolean payloadsRegistered;
    private static boolean receiversRegistered;

    private ConfigUiNetworking() {
    }

    public record ConfigSnapshotRequestPayload() implements CustomPacketPayload {
        public static final Type<ConfigSnapshotRequestPayload> TYPE = new Type<>(SNAPSHOT_REQUEST_CHANNEL);
        public static final StreamCodec<RegistryFriendlyByteBuf, ConfigSnapshotRequestPayload> CODEC =
                StreamCodec.of((buf, payload) -> {
                }, buf -> new ConfigSnapshotRequestPayload());

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public record ConfigSnapshotPayload(boolean readonly, String snapshotJson) implements CustomPacketPayload {
        public static final Type<ConfigSnapshotPayload> TYPE = new Type<>(SNAPSHOT_CHANNEL);
        public static final StreamCodec<RegistryFriendlyByteBuf, ConfigSnapshotPayload> CODEC =
                StreamCodec.of(ConfigSnapshotPayload::encode, ConfigSnapshotPayload::decode);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }

        private static ConfigSnapshotPayload decode(RegistryFriendlyByteBuf buf) {
            return new ConfigSnapshotPayload(buf.readBoolean(), buf.readUtf());
        }

        private static void encode(RegistryFriendlyByteBuf buf, ConfigSnapshotPayload payload) {
            buf.writeBoolean(payload.readonly);
            buf.writeUtf(payload.snapshotJson == null ? "{}" : payload.snapshotJson);
        }
    }

    public record ConfigApplyPayload(String mutationJson) implements CustomPacketPayload {
        public static final Type<ConfigApplyPayload> TYPE = new Type<>(APPLY_CHANNEL);
        public static final StreamCodec<RegistryFriendlyByteBuf, ConfigApplyPayload> CODEC =
                StreamCodec.of(ConfigApplyPayload::encode, ConfigApplyPayload::decode);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }

        private static ConfigApplyPayload decode(RegistryFriendlyByteBuf buf) {
            return new ConfigApplyPayload(buf.readUtf());
        }

        private static void encode(RegistryFriendlyByteBuf buf, ConfigApplyPayload payload) {
            buf.writeUtf(payload.mutationJson == null ? "{}" : payload.mutationJson);
        }
    }

    public record ConfigApplyResultPayload(boolean success, String message, boolean readonly, String snapshotJson) implements CustomPacketPayload {
        public static final Type<ConfigApplyResultPayload> TYPE = new Type<>(APPLY_RESULT_CHANNEL);
        public static final StreamCodec<RegistryFriendlyByteBuf, ConfigApplyResultPayload> CODEC =
                StreamCodec.of(ConfigApplyResultPayload::encode, ConfigApplyResultPayload::decode);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }

        private static ConfigApplyResultPayload decode(RegistryFriendlyByteBuf buf) {
            return new ConfigApplyResultPayload(buf.readBoolean(), buf.readUtf(), buf.readBoolean(), buf.readUtf());
        }

        private static void encode(RegistryFriendlyByteBuf buf, ConfigApplyResultPayload payload) {
            buf.writeBoolean(payload.success);
            buf.writeUtf(payload.message == null ? "" : payload.message);
            buf.writeBoolean(payload.readonly);
            buf.writeUtf(payload.snapshotJson == null ? "{}" : payload.snapshotJson);
        }
    }

    public static synchronized void registerPayloads() {
        if (payloadsRegistered) {
            return;
        }
        payloadsRegistered = true;

        PayloadTypeRegistry.serverboundPlay().register(ConfigSnapshotRequestPayload.TYPE, ConfigSnapshotRequestPayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(ConfigSnapshotPayload.TYPE, ConfigSnapshotPayload.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(ConfigApplyPayload.TYPE, ConfigApplyPayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(ConfigApplyResultPayload.TYPE, ConfigApplyResultPayload.CODEC);
    }

    public static synchronized void registerServerReceivers() {
        if (receiversRegistered) {
            return;
        }
        receiversRegistered = true;

        ServerPlayNetworking.registerGlobalReceiver(ConfigSnapshotRequestPayload.TYPE, (payload, context) ->
                context.server().execute(() -> {
                    ServerPlayer player = context.player();
                    boolean readonly = isReadonly(player);
                    String snapshotJson = buildSnapshot(player, readonly);
                    ServerPlayNetworking.send(player, new ConfigSnapshotPayload(readonly, snapshotJson));
                }));

        ServerPlayNetworking.registerGlobalReceiver(ConfigApplyPayload.TYPE, (payload, context) ->
                context.server().execute(() -> {
                    ServerPlayer player = context.player();
                    boolean readonly = isReadonly(player);
                    if (!isOperator(player)) {
                        ServerPlayNetworking.send(player,
                                new ConfigApplyResultPayload(false, "config=requires_op", true, buildSnapshot(player, true)));
                        return;
                    }
                    if (readonly) {
                        ServerPlayNetworking.send(player,
                                new ConfigApplyResultPayload(false, "config=readonly_non_creative", true, buildSnapshot(player, true)));
                        return;
                    }

                    try {
                        applyMutation(payload.mutationJson);
                        String snapshotJson = buildSnapshot(player, false);
                        ServerPlayNetworking.send(player,
                                new ConfigApplyResultPayload(true, "config=saved", false, snapshotJson));
                    } catch (Exception ex) {
                        ServerPlayNetworking.send(player,
                                new ConfigApplyResultPayload(false, "config=apply_failed:" + ex.getMessage(), false, buildSnapshot(player, false)));
                    }
                }));
    }

    private static void applyMutation(String mutationJson) {
        JsonObject root = parseObject(mutationJson);

        JsonArray roleGroupOrder = root.getAsJsonArray("roleGroupOrder");
        if (roleGroupOrder != null) {
            int priority = roleGroupOrder.size();
            for (JsonElement element : roleGroupOrder) {
                Identifier id = Identifier.parse(element.getAsString());
                RoleGroupDefinition group = RoleGroupManager.INSTANCE.get(id);
                if (group == null) {
                    continue;
                }
                int updatedPriority = priority--;
                RoleGroupManager.INSTANCE.update(id, old -> new RoleGroupDefinition(
                        old.id(),
                        old.translationKey(),
                        updatedPriority,
                        old.metadata(),
                        old.members()
                ));
            }
            RoleGroupManager.INSTANCE.save();
        }

        JsonObject fieldNames = root.getAsJsonObject("fieldNames");
        if (fieldNames != null) {
            for (Map.Entry<String, JsonElement> entry : fieldNames.entrySet()) {
                Identifier fieldId = Identifier.parse(entry.getKey());
                FieldDefinition existing = FieldManager.INSTANCE.get(fieldId);
                if (existing == null) {
                    continue;
                }
                String fieldName = entry.getValue() == null ? "" : entry.getValue().getAsString();
                var newData = new java.util.HashMap<String, Object>(existing.fieldData());
                if (fieldName.isBlank()) {
                    newData.remove("name");
                } else {
                    newData.put("name", fieldName);
                }
                FieldManager.INSTANCE.unregister(fieldId);
                FieldManager.INSTANCE.register(new FieldDefinition(existing.id(), existing.dimensionId(), existing.bounds(), newData));
            }
            FieldManager.INSTANCE.save();
        }

        JsonArray permissionPatches = root.getAsJsonArray("permissionPatches");
        if (permissionPatches != null) {
            for (JsonElement patchElement : permissionPatches) {
                if (!(patchElement instanceof JsonObject patch)) {
                    continue;
                }

                ScopeLayer scope = ScopeLayer.valueOf(patch.get("scope").getAsString().toUpperCase());
                String scopeIdRaw = patch.has("scopeId") ? patch.get("scopeId").getAsString() : "";
                String group = PermissionManager.INSTANCE.normalizeGroupName(patch.get("group").getAsString());
                PermissionAction action = PermissionAction.valueOf(patch.get("action").getAsString().toUpperCase());
                PermissionDecision decision = PermissionDecision.valueOf(patch.get("decision").getAsString().toUpperCase());

                switch (scope) {
                    case GLOBAL -> PermissionManager.INSTANCE.global().forGroup(group).set(action, decision);
                    case DIMENSION -> {
                        Identifier dimId = parseScopeIdentifier(scopeIdRaw, "minecraft");
                        PermissionManager.INSTANCE.dimension(dimId).forGroup(group).set(action, decision);
                    }
                    case FIELD -> {
                        Identifier fieldId = parseScopeIdentifier(scopeIdRaw, Myulib.MOD_ID);
                        PermissionManager.INSTANCE.field(fieldId).forGroup(group).set(action, decision);
                    }
                    case USER -> {
                    }
                }
            }
            PermissionManager.INSTANCE.save();
        }
    }

    private static String buildSnapshot(ServerPlayer player, boolean readonly) {
        JsonObject root = new JsonObject();
        root.addProperty("readonly", readonly);

        JsonArray roleGroups = new JsonArray();
        List<RoleGroupDefinition> sortedGroups = new ArrayList<>(RoleGroupManager.INSTANCE.groups());
        sortedGroups.sort(Comparator.comparingInt(RoleGroupDefinition::priority).reversed());
        for (RoleGroupDefinition group : sortedGroups) {
            JsonObject item = new JsonObject();
            item.addProperty("id", group.id().toString());
            item.addProperty("path", group.id().getPath());
            item.addProperty("priority", group.priority());
            item.addProperty("memberCount", group.members().size());
            roleGroups.add(item);
        }
        root.add("roleGroups", roleGroups);

        JsonObject permissions = new JsonObject();
        PermissionScope global = PermissionManager.INSTANCE.global();
        JsonArray globalGroups = new JsonArray();
        for (String groupName : global.groupTablesSnapshot().keySet()) {
            globalGroups.add(groupName);
        }
        permissions.add("globalGroups", globalGroups);

        JsonArray dimensions = new JsonArray();
        for (Identifier id : PermissionManager.INSTANCE.dimensionScopeIds()) {
            JsonObject item = new JsonObject();
            item.addProperty("id", id.toString());
            item.addProperty("path", id.getPath());
            dimensions.add(item);
        }
        permissions.add("dimensions", dimensions);

        JsonArray fields = new JsonArray();
        for (Identifier id : PermissionManager.INSTANCE.fieldScopeIds()) {
            JsonObject item = new JsonObject();
            item.addProperty("id", id.toString());
            item.addProperty("path", id.getPath());
            FieldDefinition field = FieldManager.INSTANCE.get(id);
            Object maybeName = field == null ? null : field.fieldData().get("name");
            String resolvedName = maybeName == null ? id.getPath() : Objects.toString(maybeName, id.getPath());
            item.addProperty("name", resolvedName);
            fields.add(item);
        }
        permissions.add("fields", fields);
        root.add("permissions", permissions);

        return GSON.toJson(root);
    }

    private static JsonObject parseObject(String raw) {
        if (raw == null || raw.isBlank()) {
            return new JsonObject();
        }
        JsonElement element = GSON.fromJson(raw, JsonElement.class);
        if (element == null || !element.isJsonObject()) {
            return new JsonObject();
        }
        return element.getAsJsonObject();
    }

    private static Identifier parseScopeIdentifier(String raw, String fallbackNamespace) {
        if (raw == null || raw.isBlank()) {
            return Identifier.fromNamespaceAndPath(fallbackNamespace, "default");
        }
        if (raw.contains(":")) {
            return Identifier.parse(raw);
        }
        return Identifier.fromNamespaceAndPath(fallbackNamespace, raw);
    }

    private static boolean isOperator(ServerPlayer player) {
        return player != null
                && player.createCommandSourceStack().permissions().hasPermission(
                new Permission.HasCommandLevel(PermissionLevel.GAMEMASTERS));
    }

    private static boolean isReadonly(ServerPlayer player) {
        return !isOperator(player) || !player.getAbilities().instabuild;
    }
}



