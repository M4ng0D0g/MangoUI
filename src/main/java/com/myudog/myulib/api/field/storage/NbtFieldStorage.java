package com.myudog.myulib.api.field.storage;

import com.myudog.myulib.Myulib;
import com.myudog.myulib.api.field.FieldDefinition;
import com.myudog.myulib.api.storage.DataStorage;
import com.myudog.myulib.api.util.NbtIoHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.DoubleTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.phys.AABB;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class NbtFieldStorage implements DataStorage<UUID, FieldDefinition> {
    private static final String FILE_NAME = "fields.dat";
    private static final String FIELDS_KEY = "fields";

    private Path storageFile;
    private final Map<UUID, FieldDefinition> fileMirror = new ConcurrentHashMap<>();

    @Override
    public void initialize(MinecraftServer server) {
        bindRoot(NbtIoHelper.resolveRootPath(server));
    }

    public void bindRoot(Path root) {
        Path rootPath = root == null ? Paths.get(".") : root.toAbsolutePath().normalize();
        this.storageFile = rootPath.resolve(Myulib.MOD_ID).resolve(FILE_NAME);
        try {
            Files.createDirectories(this.storageFile.getParent());
        } catch (Exception e) {
            System.err.println("[Myulib] 無法建立 Field 儲存目錄: " + e.getMessage());
        }
    }

    @Override
    public Map<UUID, FieldDefinition> loadAll() {
        fileMirror.clear();
        if (storageFile == null || !Files.exists(storageFile)) {
            return new HashMap<>();
        }
        try {
            CompoundTag root = readRoot(storageFile);
            Tag fieldsElement = root.get(FIELDS_KEY);
            if (fieldsElement instanceof ListTag list) {
                for (int i = 0; i < list.size(); i++) {
                    FieldDefinition field = readField(list.getCompound(i).orElseThrow());
                    fileMirror.put(field.id(), field);
                }
            }
            return new HashMap<>(fileMirror);
        } catch (Exception e) {
            throw new IllegalStateException("無法讀取 Field NBT: " + storageFile, e);
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

    private void saveToFile() {
        if (storageFile == null) {
            return;
        }
        try {
            CompoundTag root = new CompoundTag();
            ListTag list = new ListTag();
            for (FieldDefinition field : fileMirror.values()) {
                list.add(writeField(field));
            }
            root.put(FIELDS_KEY, list);
            writeRoot(storageFile, root);
        } catch (Exception e) {
            throw new IllegalStateException("無法儲存 Field NBT: " + storageFile, e);
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
        for (Map.Entry<String, Object> entry : field.fieldData().entrySet()) {
            dataTag.putString(entry.getKey(), String.valueOf(entry.getValue()));
        }
        tag.put("data", dataTag);
        return tag;
    }

    private FieldDefinition readField(CompoundTag tag) {
        Identifier id = Identifier.parse(tag.getString("id").orElse("myulib:unknown"));
        Identifier dim = Identifier.parse(tag.getString("dim").orElse("minecraft:overworld"));
        ListTag bounds = (ListTag) tag.get("bounds");
        AABB aabb = new AABB(
                bounds.getDouble(0).orElse(0.0), bounds.getDouble(1).orElse(0.0), bounds.getDouble(2).orElse(0.0),
                bounds.getDouble(3).orElse(0.0), bounds.getDouble(4).orElse(0.0), bounds.getDouble(5).orElse(0.0)
        );
        Map<String, Object> fieldData = new LinkedHashMap<>();
        if (tag.contains("data")) {
            CompoundTag dataTag = tag.getCompound("data").orElseThrow();
            for (String key : keysOf(dataTag)) {
                fieldData.put(key, dataTag.getString(key).orElse(""));
            }
        }
        return new FieldDefinition(id, dim, aabb, fieldData);
    }

    private static CompoundTag readRoot(Path path) throws Exception {
        return NbtIoHelper.readRoot(path);
    }

    private static void writeRoot(Path path, CompoundTag root) throws Exception {
        NbtIoHelper.writeRoot(path, root);
    }

    private static List<String> keysOf(CompoundTag compound) {
        return NbtIoHelper.keysOf(compound);
    }
}