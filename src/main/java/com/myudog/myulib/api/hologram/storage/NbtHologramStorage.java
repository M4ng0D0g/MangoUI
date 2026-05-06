package com.myudog.myulib.api.hologram.storage;

import com.myudog.myulib.Myulib;
import com.myudog.myulib.api.hologram.HologramDefinition;
import com.myudog.myulib.api.hologram.HologramStyle;
import com.myudog.myulib.api.storage.DataStorage;
import com.myudog.myulib.api.util.NbtIoHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.phys.AABB;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 實作 DataStorage 的 Hologram 儲存庫。
 * 使用底層 NbtIoHelper 進行直接的檔案讀寫，脫離 Minecraft 原生 SavedData 的限制。
 */
public class NbtHologramStorage implements DataStorage<UUID, HologramDefinition> {

    private static final String FILE_NAME = "holograms.dat";
    private static final String HOLOGRAMS_KEY = "holograms";

    private Path storageFile;
    private final Map<UUID, HologramDefinition> fileMirror = new ConcurrentHashMap<>();

    // =====================================================================
    // 1. DataStorage 介面實作
    // =====================================================================

    @Override
    public void initialize(MinecraftServer server) {
        bindRoot(NbtIoHelper.resolveRootPath(server));
    }

    public void bindRoot(Path root) {
        Path rootPath = root == null ? Paths.get(".") : root.toAbsolutePath().normalize();
        this.storageFile = rootPath.resolve(Myulib.MOD_ID).resolve(FILE_NAME);

        try {
            if (this.storageFile != null && !Files.exists(this.storageFile.getParent())) {
                Files.createDirectories(this.storageFile.getParent());
            }
        } catch (Exception e) {
            System.err.println("[Myulib] 無法建立 Hologram 儲存目錄: " + e.getMessage());
        }
    }

    @Override
    public Map<UUID, HologramDefinition> loadAll() {
        fileMirror.clear();
        if (storageFile == null || !Files.exists(storageFile)) {
            return new HashMap<>();
        }

        try {
            CompoundTag root = NbtIoHelper.readRoot(storageFile);
            Tag hologramsElement = root.get(HOLOGRAMS_KEY);

            if (hologramsElement instanceof ListTag list) {
                for (int i = 0; i < list.size(); i++) {
                    CompoundTag itemTag = list.getCompound(i).orElse(new CompoundTag());
                    HologramDefinition def = readHologram(itemTag);

                    if (def.id() != null) {
                        UUID uuidKey = UUID.nameUUIDFromBytes(def.id().toString().getBytes());
                        fileMirror.put(uuidKey, def);
                    }
                }
            }
        } catch (Exception e) {
            throw new IllegalStateException("無法讀取 Hologram NBT: " + storageFile, e);
        }

        return new HashMap<>(fileMirror);
    }

    @Override
    public void save(UUID id, HologramDefinition data) {
        fileMirror.put(id, data);
        saveToFile();
    }

    @Override
    public void delete(UUID id) {
        if (fileMirror.remove(id) != null) {
            saveToFile();
        }
    }

    // =====================================================================
    // 2. 內部寫入邏輯與 NBT 序列化轉換
    // =====================================================================

    private synchronized void saveToFile() {
        if (storageFile == null) return;
        try {
            CompoundTag root = new CompoundTag();
            ListTag list = new ListTag();

            for (HologramDefinition def : fileMirror.values()) {
                list.add(writeHologram(def));
            }

            root.put(HOLOGRAMS_KEY, list);
            NbtIoHelper.writeRoot(storageFile, root);
        } catch (Exception e) {
            throw new IllegalStateException("無法儲存 Hologram NBT: " + storageFile, e);
        }
    }

    private static HologramDefinition readHologram(CompoundTag itemTag) {
        String idStr = itemTag.getString("id").orElse("");
        String dimStr = itemTag.getString("dimensionId").orElse("");

        Identifier id = Identifier.tryParse(idStr);
        Identifier dimId = Identifier.tryParse(dimStr);
        String label = itemTag.getString("label").orElse(null);

        CompoundTag bTag = itemTag.getCompound("bounds").orElse(new CompoundTag());
        AABB bounds = new AABB(
                bTag.getDouble("minX").orElse(0.0),
                bTag.getDouble("minY").orElse(0.0),
                bTag.getDouble("minZ").orElse(0.0),
                bTag.getDouble("maxX").orElse(0.0),
                bTag.getDouble("maxY").orElse(0.0),
                bTag.getDouble("maxZ").orElse(0.0)
        );

        HologramStyle style = HologramStyle.defaults();
        if (itemTag.contains("color")) {
            style = new HologramStyle(
                    itemTag.getInt("color").orElse(0),
                    itemTag.getByte("flags").orElse((byte) 0)
            );
        }

        return new HologramDefinition(id, dimId, bounds, label, style);
    }

    private static CompoundTag writeHologram(HologramDefinition def) {
        CompoundTag itemTag = new CompoundTag();
        itemTag.putString("id", def.id().toString());
        itemTag.putString("dimensionId", def.dimensionId().toString());
        if (def.label() != null) itemTag.putString("label", def.label());

        CompoundTag bTag = new CompoundTag();
        bTag.putDouble("minX", def.bounds().minX);
        bTag.putDouble("minY", def.bounds().minY);
        bTag.putDouble("minZ", def.bounds().minZ);
        bTag.putDouble("maxX", def.bounds().maxX);
        bTag.putDouble("maxY", def.bounds().maxY);
        bTag.putDouble("maxZ", def.bounds().maxZ);
        itemTag.put("bounds", bTag);

        itemTag.putInt("color", def.style().color());
        itemTag.putByte("flags", def.style().flags());

        return itemTag;
    }
}