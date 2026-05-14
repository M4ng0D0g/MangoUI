package com.myudog.myulib.api.core.util;

import com.google.gson.*;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.AABB;

import java.io.IOException;
import java.lang.reflect.Type;

/**
 * GsonProvider
 *
 * 系統：通用工具系統 (Utility System)
 * 角色：JSON 序列化提供者，封裝了預設的 Gson 配置與 Minecraft 特殊類型適配器。
 * 類型：Utility / Factory
 *
 * 提供了一個全域共享的 {@link Gson} 實例，預先註冊了：
 * 1. {@link Identifier}：支援字串與 Identifier 的自動轉換。
 * 2. {@link AABB}：支援碰撞箱座標的 JSON 序列化。
 */
public class GsonProvider {

    // 建立一個帶有客製化規則的 Gson 單例，供整個系統重複使用
    public static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting() // 讓輸出的 JSON 有換行與縮排，方便人類閱讀
            .registerTypeAdapter(Identifier.class, new IdentifierAdapter())
            .registerTypeAdapter(AABB.class, new AABBAdapter())
            .create();

    /**
     * 🌟 1. Identifier 的轉換器 (使用極速的 TypeAdapter)
     * 因為 Identifier 本質上就是一個字串 (例如 "minecraft:overworld")
     */
    private static class IdentifierAdapter extends TypeAdapter<Identifier> {
        @Override
        public void write(JsonWriter out, Identifier value) throws IOException {
            if (value == null) {
                out.nullValue();
            } else {
                out.value(value.toString()); // 轉成字串寫入 JSON
            }
        }

        @Override
        public Identifier read(JsonReader in) throws IOException {
            if (in.peek() == com.google.gson.stream.JsonToken.NULL) {
                in.nextNull();
                return null;
            }
            // 從 JSON 讀取字串，並還原成 Minecraft 的 Identifier 物件
            return Identifier.tryParse(in.nextString());
        }
    }

    /**
     * 🌟 2. AABB 的轉換器 (使用 JsonSerializer/JsonDeserializer)
     * 將 AABB 轉化為帶有 minX, minY, minZ... 的乾淨 JSON Object
     */
    private static class AABBAdapter implements JsonSerializer<AABB>, JsonDeserializer<AABB> {
        @Override
        public JsonElement serialize(AABB src, Type typeOfSrc, JsonSerializationContext context) {
            JsonObject obj = new JsonObject();
            obj.addProperty("minX", src.minX);
            obj.addProperty("minY", src.minY);
            obj.addProperty("minZ", src.minZ);
            obj.addProperty("maxX", src.maxX);
            obj.addProperty("maxY", src.maxY);
            obj.addProperty("maxZ", src.maxZ);
            return obj;
        }

        @Override
        public AABB deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            JsonObject obj = json.getAsJsonObject();
            return new AABB(
                    obj.get("minX").getAsDouble(),
                    obj.get("minY").getAsDouble(),
                    obj.get("minZ").getAsDouble(),
                    obj.get("maxX").getAsDouble(),
                    obj.get("maxY").getAsDouble(),
                    obj.get("maxZ").getAsDouble()
            );
        }
    }
}