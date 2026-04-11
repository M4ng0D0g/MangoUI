package com.myudog.myulib.api.game.object;

import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;

import java.util.Map;
import java.util.Objects;

/**
 * 定義遊戲載入時需要生成的實體藍圖
 */
public record GameObjectConfig<T extends Entity>(
        Identifier id,
        EntityType<T> entityType, // 具體的生物類型 (例如：EntityType.ZOMBIE 或您自訂的 ChessPieceEntityType)
        GameObjectKind kind,
        Map<String, Object> properties // 初始屬性，例如血量、陣營
) {
    public GameObjectConfig {
        Objects.requireNonNull(id, "id 不得為空");
        Objects.requireNonNull(entityType, "entityType 不得為空");
        kind = kind == null ? GameObjectKind.CUSTOM : kind;
        properties = properties == null ? Map.of() : Map.copyOf(properties);
    }
}