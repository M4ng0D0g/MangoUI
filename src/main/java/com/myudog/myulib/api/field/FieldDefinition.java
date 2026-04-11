package com.myudog.myulib.api.field;

import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.AABB;

import java.util.Map;
import java.util.Objects;

/**
 * 定義一個獨立的遊戲/保護區域。
 * 權限相關設定交由外部 Permission 系統處理，可將資料存放於 fieldData 中。
 */
public record FieldDefinition(
        Identifier id,
        Identifier dimensionId, // 例如: minecraft:overworld
        AABB bounds,            // 核心：原版碰撞箱
        Map<String, Object> fieldData // 未來放置權限表 (RoleGroup, Enum 狀態) 的擴充槽
) {
    public FieldDefinition {
        Objects.requireNonNull(id, "id 不得為空");
        Objects.requireNonNull(dimensionId, "dimensionId 不得為空");
        Objects.requireNonNull(bounds, "bounds 不得為空");
        fieldData = fieldData == null ? Map.of() : Map.copyOf(fieldData);
    }
}