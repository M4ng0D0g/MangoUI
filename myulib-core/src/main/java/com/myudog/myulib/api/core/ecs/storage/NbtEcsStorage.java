package com.myudog.myulib.api.core.ecs.storage;

import com.myudog.myulib.MyulibCore;
import com.myudog.myulib.api.core.storage.DataStorage;
import com.myudog.myulib.api.core.util.NbtIoHelper; // 假設您的反射工具類別名
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class NbtEcsStorage implements DataStorage<Integer, CompoundTag> {
    private static final String FILE_NAME = "ecs_entities.dat";
    private Path storageFile;
    private final Map<Integer, CompoundTag> mirror = new ConcurrentHashMap<>();

    @Override
    public void initialize(MinecraftServer server) {
        // 使用您在 NbtFieldStorage 中實作的 resolveRootPath 邏輯
        Path root = NbtIoHelper.resolveRootPath(server);
        this.storageFile = root.resolve(MyulibCore.MOD_ID).resolve(FILE_NAME);
    }

    @Override
    public Map<Integer, CompoundTag> loadAll() {
        mirror.clear();
        if (storageFile == null || !Files.exists(storageFile)) return new HashMap<>();

        try {
            CompoundTag root = NbtIoHelper.readRoot(storageFile);
            for (String key : NbtIoHelper.keysOf(root)) {
                mirror.put(Integer.parseInt(key), root.getCompound(key).orElseThrow());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return new HashMap<>(mirror);
    }

    @Override
    public void save(Integer id, CompoundTag data) {
        mirror.put(id, data);
        saveToFile();
    }

    @Override
    public void delete(Integer id) {
        if (mirror.remove(id) != null) saveToFile();
    }

    private synchronized void saveToFile() {
        try {
            Files.createDirectories(storageFile.getParent());
            CompoundTag root = new CompoundTag();
            mirror.forEach((id, tag) -> root.put(id.toString(), tag));
            NbtIoHelper.writeRoot(storageFile, root);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}