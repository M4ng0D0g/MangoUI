package com.myudog.myulib.api.framework.rolegroup;

import net.minecraft.network.chat.MutableComponent;
import org.jetbrains.annotations.NotNull;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * RoleGroupDefinition
 *
 * 系統：角色組管理系統 (Framework - RoleGroup)
 * 角色：定義一個角色組的靜態屬性與初始成員。
 * 類型：Record / Data Holder
 *
 * 每個角色組都具備優先級 (Priority)，用於在權限過濾時決定處理順序。
 * 同時可以攜帶任意的元數據 (Metadata)。
 */
public record RoleGroupDefinition(
    /** 角色組的唯一識別碼。 */
    @NotNull UUID uuid,
    /** 角色組的顯示名稱。 */
    @NotNull MutableComponent translationKey,
    /** 角色組的優先級（數值越大優先級越高）。 */
    int priority,
    /** 角色組的附加元數據。 */
    Map<String, String> metadata,
    /** 屬於此角色組的玩家成員名單。 */
    Set<UUID> members
) {
    public static final String ROUTE = "rolegroup";

    public RoleGroupDefinition {
        metadata = metadata == null ? Map.of() : Map.copyOf(metadata);
        members = members == null ? Set.of() : Set.copyOf(members);
    }

    public RoleGroupDefinition(@NotNull String token, @NotNull MutableComponent translationKey, int priority, Map<String, String> metadata, Set<UUID> members) {
        this(stableUuid(token), translationKey, priority, metadata, members);
    }

    public RoleGroupDefinition(@NotNull net.minecraft.resources.Identifier id, @NotNull MutableComponent translationKey, int priority, Map<String, String> metadata, Set<UUID> members) {
        this(stableUuid(id.toString()), translationKey, priority, metadata, members);
    }

    /** 獲取角色組 ID。 */
    public UUID id() {
        return uuid;
    }

    /** 獲取角色組 Token。 */
    public UUID token() {
        return uuid;
    }

    /** 檢查玩家是否屬於此角色組。 */
    public boolean hasMember(UUID playerId) {
        return members != null && members.contains(playerId);
    }

    private static UUID stableUuid(String token) {
        return UUID.nameUUIDFromBytes(token.getBytes(StandardCharsets.UTF_8));
    }
}
