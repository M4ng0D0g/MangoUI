package com.myudog.myulib.api.field;

import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.phys.Vec3;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public final class FieldManager {

    // 綁定持久化儲存層
    private static FieldStorage STORAGE = new NbtFieldStorage();

    private FieldManager() {}

    public static void install() {
        STORAGE.ensureLoaded();
    }

    public static void bindServer(MinecraftServer server) {
        STORAGE.bindServer(server);
    }

    /**
     * 註冊一個新的區域。若與現有區域重疊，將直接拋出異常。
     */
    public static FieldDefinition register(FieldDefinition field) {
        Objects.requireNonNull(field, "field 不得為空");

        // 🛡️ 核心防護：檢查是否有空間重疊 (Overlap)
        // 注意：這裡改為使用 STORAGE.getAll().values() 來遍歷
        for (FieldDefinition existing : STORAGE.getAll().values()) {
            // 只有在同一個維度才需要檢查重疊
            if (existing.dimensionId().equals(field.dimensionId())) {

                // 利用原版 AABB 極高效率的交集判定
                if (existing.bounds().intersects(field.bounds())) {
                    throw new IllegalArgumentException(
                            String.format("註冊失敗！新區域 [%s] 與現有區域 [%s] 發生空間重疊！",
                                    field.id(), existing.id())
                    );
                }
            }
        }

        // 將場地寫入儲存層 (會自動觸發 NBT 存檔)
        STORAGE.add(field);
        return field;
    }

    public static void unregister(Identifier fieldId) {
        STORAGE.remove(fieldId);
    }

    public static FieldDefinition get(Identifier fieldId) {
        return STORAGE.getAll().get(fieldId);
    }

    public static Map<Identifier, FieldDefinition> all() {
        return STORAGE.getAll();
    }

    /**
     * 🎯 尋找包含特定座標的區域。
     * 由於系統嚴格禁止重疊，一個座標最多只會存在於一個區域內。
     * @return 找到的區域 (Optional 封裝以防 Null)
     */
    public static Optional<FieldDefinition> findAt(Identifier dimensionId, Vec3 pos) {
        if (dimensionId == null || pos == null) return Optional.empty();

        for (FieldDefinition field : STORAGE.getAll().values()) {
            if (field.dimensionId().equals(dimensionId) && field.bounds().contains(pos)) {
                return Optional.of(field); // 找到就立刻返回，效能極高
            }
        }
        return Optional.empty();
    }

    public static void save() {
        STORAGE.markDirty();
    }

    public static void clear() {
        STORAGE = new NbtFieldStorage();
    }
}