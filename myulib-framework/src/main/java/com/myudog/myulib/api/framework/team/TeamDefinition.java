package com.myudog.myulib.api.framework.team;

import net.minecraft.network.chat.MutableComponent;
import org.jetbrains.annotations.NotNull;

import java.nio.charset.StandardCharsets;
import java.util.EnumMap;
import java.util.Map;
import java.util.UUID;

/**
 * TeamDefinition
 *
 * 系統：隊伍管理系統 (Framework - Team)
 * 角色：定義一個隊伍的所有靜態屬性。
 * 類型：Record / Data Holder
 *
 * 此 Record 封裝了隊伍的唯一識別碼、顯示名稱、代表顏色、行為旗標 (TeamFlag) 以及人數上限。
 * 內部使用 {@link EnumMap} 優化了旗標的儲存空間。
 */
public record TeamDefinition(
    /** 隊伍的唯一識別碼。 */
    @NotNull UUID uuid,
    /** 隊伍的顯示名稱（支援多語言翻譯鍵）。 */
    @NotNull MutableComponent translationKey,
    /** 隊伍的代表顏色。 */
    @NotNull TeamColor color,
    /** 隊伍的行為旗標設置。 */
    Map<TeamFlag, Boolean> flags,
    /** 隊伍的人數上限（0 表示不限制）。 */
    int playerLimit
) {
    public static final String ROUTE = "team";

    public TeamDefinition {
        // 優化 Flags 儲存
        EnumMap<TeamFlag, Boolean> optimizedFlags = new EnumMap<>(TeamFlag.class);
        if (flags != null) optimizedFlags.putAll(flags);
        flags = optimizedFlags;

        if (playerLimit < 0) throw new IllegalArgumentException("playerLimit must be >= 0");
    }

    public TeamDefinition(@NotNull String token, @NotNull MutableComponent translationKey, @NotNull TeamColor color, Map<TeamFlag, Boolean> flags) {
        this(stableUuid(token), translationKey, color, flags, 0);
    }

    public TeamDefinition(@NotNull String token, @NotNull MutableComponent translationKey, @NotNull TeamColor color, Map<TeamFlag, Boolean> flags, int playerLimit) {
        this(stableUuid(token), translationKey, color, flags, playerLimit);
    }

    public TeamDefinition(@NotNull net.minecraft.resources.Identifier id, @NotNull MutableComponent translationKey, @NotNull TeamColor color, Map<TeamFlag, Boolean> flags, int playerLimit) {
        this(stableUuid(id.toString()), translationKey, color, flags, playerLimit);
    }

    /** 獲取隊伍 ID。 */
    public UUID id() {
        return uuid;
    }

    /** 獲取隊伍 Token。 */
    public UUID token() {
        return uuid;
    }

    private static UUID stableUuid(String token) {
        return UUID.nameUUIDFromBytes(token.getBytes(StandardCharsets.UTF_8));
    }
}
