package com.myudog.myulib.api.field;

import com.myudog.myulib.api.field.storage.NbtFieldStorage;
import com.myudog.myulib.api.debug.DebugFeature;
import com.myudog.myulib.api.debug.DebugLogManager;
import com.myudog.myulib.api.storage.DataStorage;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.Vec3;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ConcurrentHashMap;

public final class FieldManager {

    public static final FieldManager INSTANCE = new FieldManager();

    // 🌟 記憶體快取：所有查詢都在此進行，效能極高
    private final Map<UUID, FieldDefinition> FIELDS = new ConcurrentHashMap<>();

    // 🌟 注入的儲存介面
    private DataStorage<UUID, FieldDefinition> storage;

    private FieldManager() {}

    public void install() {
        install(new NbtFieldStorage());
    }

    public void install(DataStorage<UUID, FieldDefinition> storageProvider) {
        storage = storageProvider;

        // 1. 伺服器啟動時，初始化儲存並載入記憶體
        ServerLifecycleEvents.SERVER_STARTING.register(server -> {
            if (storage != null) {
                storage.initialize(server);
                FIELDS.clear();
                Map<UUID, FieldDefinition> loaded = storage.loadAll();
                if (loaded != null) {
                    FIELDS.putAll(loaded);
                }
                System.out.println("[Myulib] FieldManager 已成功載入 " + FIELDS.size() + " 個區域。");
            }
        });

        // 2. 伺服器正在關閉時，強制存檔
        ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
            save();
        });

        // 3. 釋放記憶體
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> {
            clear();
            System.out.println("[Myulib] FieldManager 已成功釋放！");
        });
    }

    public FieldDefinition register(FieldDefinition field) {
        Objects.requireNonNull(field, "field 不得為空");

        if (!validate(field)) {
            throw new IllegalArgumentException("FieldDefinition 驗證失敗: " + field.uuid());
        }

        FIELDS.put(field.uuid(), field);
        if (storage != null) storage.save(field.uuid(), field);
        DebugLogManager.INSTANCE.log(DebugFeature.FIELD,
                "register uuid=" + field.uuid() + ",dim=" + field.dimensionId()
                        + ",min=(" + field.bounds().minX + "," + field.bounds().minY + "," + field.bounds().minZ + ")"
                        + ",max=(" + field.bounds().maxX + "," + field.bounds().maxY + "," + field.bounds().maxZ + ")");

        return field;
    }

    public boolean validate(FieldDefinition field) {
        if (field == null || field.uuid() == null || field.dimensionId() == null || field.bounds() == null) {
            return false;
        }

        if (FIELDS.containsKey(field.uuid())) {
            return false;
        }

        // 🛡️ 空間重疊檢查 (直接遍歷記憶體)
        for (FieldDefinition existing : FIELDS.values()) {
            if (existing.dimensionId().equals(field.dimensionId())) {
                if (existing.bounds().intersects(field.bounds())) {
                    return false;
                }
            }
        }

        return true;
    }

    public void unregister(UUID fieldUuid) {
        DebugLogManager.INSTANCE.log(DebugFeature.FIELD, "unregister uuid=" + fieldUuid);
        if (storage != null) storage.delete(fieldUuid);
        FIELDS.remove(fieldUuid);
    }

    public void unregister(Identifier fieldId) {
        unregister(stableUuid(fieldId.toString()));
    }

    public FieldDefinition get(UUID fieldUuid) {
        return FIELDS.get(fieldUuid);
    }

    public FieldDefinition get(Identifier fieldId) {
        return FIELDS.get(stableUuid(fieldId.toString()));
    }

    public Map<UUID, FieldDefinition> all() {
        return Map.copyOf(FIELDS);
    }

    public Optional<FieldDefinition> findAt(Identifier dimensionId, Vec3 pos) {
        if (dimensionId == null || pos == null) return Optional.empty();

        for (FieldDefinition field : FIELDS.values()) {
            if (field.dimensionId().equals(dimensionId) && field.bounds().contains(pos)) {
                return Optional.of(field);
            }
        }
        return Optional.empty();
    }

    public void save() {
        if (storage != null) {
            for (FieldDefinition field : FIELDS.values()) {
                storage.save(field.uuid(), field);
            }
        }
    }

    public void clear() {
        FIELDS.clear();
    }

    private static UUID stableUuid(String token) {
        return UUID.nameUUIDFromBytes(token.getBytes(StandardCharsets.UTF_8));
    }
}