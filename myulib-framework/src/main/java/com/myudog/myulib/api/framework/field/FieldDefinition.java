package com.myudog.myulib.api.framework.field;

import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.nio.charset.StandardCharsets;

/**
 * FieldDefinition
 *
 * 蝟餌絞嚗??脣?啁頂蝯?(Framework - Field)
 * 閫嚗?蝢拐??蝡??/靽風????嗅惇?扼?
 * 憿?嚗ecord / Data Holder
 *
 * 甇?Record 撠?鈭?啁??詨?蝛粹?鞈?嚗雁摨西???嚗誑??????局 (fieldData)??
 * 甈??賊?閮剖??虜摮??`fieldData` 銝哨?銝衣 Permission 蝟餌絞霈??
 */
public record FieldDefinition(
        /** ?游?銝霅蝣潦?*/
        @NotNull UUID uuid,
        /** ??函?蝬剖漲霅蝣潦?*/
        @NotNull Identifier dimensionId,
        /** ?游???寥?蝛粹?????*/
        @NotNull AABB bounds,
        /** ?游?鞈?瑽踝??冽摮憒???????蝐斤??芸?蝢抵???*/
        Map<String, Object> fieldData
) {
    public static final String ROUTE = "field";

    public FieldDefinition {
        fieldData = fieldData == null ? new HashMap<>() : new HashMap<>(fieldData);
    }

    public FieldDefinition(@NotNull String token, @NotNull Identifier dimensionId, @NotNull AABB bounds, Map<String, Object> fieldData) {
        this(stableUuid(token), dimensionId, bounds, fieldData);
    }

    public FieldDefinition(@NotNull Identifier id, @NotNull Identifier dimensionId, @NotNull AABB bounds, Map<String, Object> fieldData) {
        this(stableUuid(id.toString()), dimensionId, bounds, fieldData);
    }

    public UUID id() {
        return uuid;
    }

    public UUID token() {
        return uuid;
    }

    private static UUID stableUuid(String token) {
        return UUID.nameUUIDFromBytes(token.getBytes(StandardCharsets.UTF_8));
    }
}
