package com.myudog.myulib.api.framework.field.storage;

import com.myudog.myulib.Myulib;

import com.myudog.myulib.api.framework.field.FieldDefinition;
import com.myudog.myulib.api.core.storage.DataStorage;
import com.myudog.myulib.api.core.util.NbtIoHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.DoubleTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.phys.AABB;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * NbtFieldStorage
 * 
 * 系統：場地管理系統 (Framework - Field)
 * 角色：將場地定義持久化至 NBT 檔案。
 * 類型：Storage Implementation
 */
public class NbtFieldStorage implements DataStorage<UUID, FieldDefinition> {
    private static final String FILE_NAME = "fields.dat";
    private static final String FIELDS_KEY = "fields";

    private Path storageFile;
    private final Map<UUID, FieldDefinition> fileMirror = new ConcurrentHashMap<>();

    @Override
    public void initialize(MinecraftServer server) {
        Path rootPath = NbtIoHelper.resolveRootPath(server).toAbsolutePath().normalize();
        this.storageFile = rootPath.resolve(Myulib.MOD_ID).resolve(FILE_NAME);
        try {
            if (!Files.exists(this.storageFile.getParent())) {
                Files.createDirectories(this.storageFile.getParent());
            }
        } catch (Exception e) {
            Myulib.LOGGER.error("Failed to create Field storage directory: " + e.getMessage());
        }
    }

    @Override
    public Map<UUID, FieldDefinition> loadAll() {
        fileMirror.clear();
        if (storageFile == null || !Files.exists(storageFile)) {
            return new HashMap<>();
        }
        try {
            CompoundTag root = NbtIoHelper.readRoot(storageFile);
            Tag fieldsElement = root.get(FIELDS_KEY);
            if (fieldsElement instanceof ListTag list) {
                for (int i = 0; i < list.size(); i++) {
                    FieldDefinition field = readField(list.getCompound(i).orElseThrow());
                    fileMirror.put(field.uuid(), field);
                }
            }
            return new HashMap<>(fileMirror);
        } catch (Exception e) {
            Myulib.LOGGER.error("Failed to load Field NBT: " + storageFile, e);
            return new HashMap<>();
        }
    }

    @Override
    public void save(UUID id, FieldDefinition data) {
        fileMirror.put(id, data);
        saveToFile();
    }

    @Override
    public void delete(UUID id) {
        if (fileMirror.remove(id) != null) {
            saveToFile();
        }
    }

    private synchronized void saveToFile() {
        if (storageFile == null) return;
        try {
            CompoundTag root = new CompoundTag();
            ListTag list = new ListTag();
            for (FieldDefinition field : fileMirror.values()) {
                list.add(writeField(field));
            }
            root.put(FIELDS_KEY, list);
            NbtIoHelper.writeRoot(storageFile, root);
        } catch (Exception e) {
            Myulib.LOGGER.error("Failed to save Field NBT: " + storageFile, e);
        }
    }

    private CompoundTag writeField(FieldDefinition field) {
        CompoundTag tag = new CompoundTag();
        tag.putString("id", field.id().toString());
        tag.putString("dim", field.dimensionId().toString());

        ListTag bounds = new ListTag();
        bounds.add(DoubleTag.valueOf(field.bounds().minX));
        bounds.add(DoubleTag.valueOf(field.bounds().minY));
        bounds.add(DoubleTag.valueOf(field.bounds().minZ));
        bounds.add(DoubleTag.valueOf(field.bounds().maxX));
        bounds.add(DoubleTag.valueOf(field.bounds().maxY));
        bounds.add(DoubleTag.valueOf(field.bounds().maxZ));
        tag.put("bounds", bounds);

        CompoundTag dataTag = new CompoundTag();
        if (field.fieldData() != null) {
            for (Map.Entry<String, Object> entry : field.fieldData().entrySet()) {
                dataTag.putString(entry.getKey(), String.valueOf(entry.getValue()));
            }
        }
        tag.put("data", dataTag);
        return tag;
    }

    private FieldDefinition readField(CompoundTag tag) {
        Identifier id = Identifier.parse(tag.getString("id").orElse("myulib:unknown"));
        Identifier dim = Identifier.parse(tag.getString("dim").orElse("minecraft:overworld"));
        ListTag bounds = (ListTag) tag.get("bounds");
        AABB aabb = new AABB(0, 0, 0, 0, 0, 0);
        if (bounds != null && bounds.size() >= 6) {
            aabb = new AABB(
                    bounds.getDouble(0).orElse(0.0), bounds.getDouble(1).orElse(0.0), bounds.getDouble(2).orElse(0.0),
                    bounds.getDouble(3).orElse(0.0), bounds.getDouble(4).orElse(0.0), bounds.getDouble(5).orElse(0.0)
            );
        }
        Map<String, Object> fieldData = new LinkedHashMap<>();
        if (tag.contains("data")) {
            CompoundTag dataTag = tag.getCompound("data").orElseThrow();
            for (String key : NbtIoHelper.keysOf(dataTag)) {
                fieldData.put(key, dataTag.getString(key).orElse(""));
            }
        }
        return new FieldDefinition(id, dim, aabb, fieldData);
    }
}
