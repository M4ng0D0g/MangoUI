package com.myudog.myulib.api.framework.permission.storage;

import com.myudog.myulib.Myulib;

import com.myudog.myulib.api.framework.permission.PermissionAction;
import com.myudog.myulib.api.framework.permission.PermissionDecision;
import com.myudog.myulib.api.framework.permission.PermissionScope;
import com.myudog.myulib.api.framework.permission.PermissionTable;
import com.myudog.myulib.api.core.storage.DataStorage;
import com.myudog.myulib.api.core.util.NbtIoHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * NbtPermissionStorage
 * 
 * 系統：權限管理系統 (Framework - Permission)
 * 角色：將權限範圍 (Scope) 持久化至 NBT 檔案。
 * 類型：Storage Implementation
 */
public class NbtPermissionStorage implements DataStorage<String, PermissionScope> {

    private static final String FILE_NAME = "permissions.dat";
    private Path storageFile;
    private final Map<String, PermissionScope> fileMirror = new ConcurrentHashMap<>();

    @Override
    public void initialize(MinecraftServer server) {
        Path rootPath = NbtIoHelper.resolveRootPath(server).toAbsolutePath().normalize();
        this.storageFile = rootPath.resolve(Myulib.MOD_ID).resolve(FILE_NAME);

        try {
            if (!Files.exists(this.storageFile.getParent())) {
                Files.createDirectories(this.storageFile.getParent());
            }
        } catch (Exception e) {
            Myulib.LOGGER.error("Failed to create Permission storage directory: " + e.getMessage());
        }
    }

    @Override
    public Map<String, PermissionScope> loadAll() {
        fileMirror.clear();
        if (storageFile == null || !Files.exists(storageFile)) {
            return new HashMap<>();
        }

        try {
            CompoundTag root = NbtIoHelper.readRoot(storageFile);

            if (root.contains("global")) {
                fileMirror.put("global", readScope(root.getCompound("global").orElseThrow()));
            }

            if (root.contains("dimensions")) {
                CompoundTag dimsTag = root.getCompound("dimensions").orElseThrow();
                for (String key : NbtIoHelper.keysOf(dimsTag)) {
                    fileMirror.put("dim:" + key, readScope(dimsTag.getCompound(key).orElseThrow()));
                }
            }

            if (root.contains("fields")) {
                CompoundTag fieldsTag = root.getCompound("fields").orElseThrow();
                for (String key : NbtIoHelper.keysOf(fieldsTag)) {
                    fileMirror.put("field:" + key, readScope(fieldsTag.getCompound(key).orElseThrow()));
                }
            }

        } catch (Exception e) {
            Myulib.LOGGER.error("Failed to load Permission NBT: " + storageFile, e);
        }

        return new HashMap<>(fileMirror);
    }

    @Override
    public void save(String id, PermissionScope data) {
        fileMirror.put(id, data);
        saveToFile();
    }

    @Override
    public void delete(String id) {
        if (fileMirror.remove(id) != null) {
            saveToFile();
        }
    }

    private synchronized void saveToFile() {
        if (storageFile == null) return;
        try {
            CompoundTag root = new CompoundTag();
            CompoundTag dimsTag = new CompoundTag();
            CompoundTag fieldsTag = new CompoundTag();

            for (Map.Entry<String, PermissionScope> entry : fileMirror.entrySet()) {
                String key = entry.getKey();
                PermissionScope scope = entry.getValue();

                if (key.equals("global")) {
                    root.put("global", writeScope(scope));
                } else if (key.startsWith("dim:")) {
                    dimsTag.put(key.substring(4), writeScope(scope));
                } else if (key.startsWith("field:")) {
                    fieldsTag.put(key.substring(6), writeScope(scope));
                }
            }

            root.put("dimensions", dimsTag);
            root.put("fields", fieldsTag);
            NbtIoHelper.writeRoot(storageFile, root);

        } catch (Exception e) {
            Myulib.LOGGER.error("Failed to save Permission NBT: " + storageFile, e);
        }
    }

    private CompoundTag writeScope(PermissionScope scope) {
        CompoundTag tag = new CompoundTag();
        CompoundTag playersTag = new CompoundTag();
        for (Map.Entry<UUID, PermissionTable> entry : scope.playerTablesSnapshot().entrySet()) {
            playersTag.put(entry.getKey().toString(), writeTable(entry.getValue()));
        }

        CompoundTag groupsTag = new CompoundTag();
        for (Map.Entry<String, PermissionTable> entry : scope.groupTablesSnapshot().entrySet()) {
            groupsTag.put(entry.getKey(), writeTable(entry.getValue()));
        }

        tag.put("players", playersTag);
        tag.put("groups", groupsTag);
        return tag;
    }

    private CompoundTag writeTable(PermissionTable table) {
        CompoundTag tag = new CompoundTag();
        for (Map.Entry<PermissionAction, PermissionDecision> entry : table.rulesSnapshot().entrySet()) {
            if (entry.getValue() == null || entry.getValue() == PermissionDecision.UNSET) {
                continue;
            }
            tag.putString(entry.getKey().name(), entry.getValue().name());
        }
        return tag;
    }

    private PermissionScope readScope(CompoundTag tag) {
        PermissionScope scope = new PermissionScope();
        if (tag.contains("players")) {
            CompoundTag playersTag = tag.getCompound("players").orElseThrow();
            for (String uuidStr : NbtIoHelper.keysOf(playersTag)) {
                try {
                    readTable(playersTag.getCompound(uuidStr).orElseThrow(), scope.forPlayer(UUID.fromString(uuidStr)));
                } catch (Exception ignored) {}
            }
        }
        if (tag.contains("groups")) {
            CompoundTag groupsTag = tag.getCompound("groups").orElseThrow();
            for (String groupName : NbtIoHelper.keysOf(groupsTag)) {
                try {
                    readTable(groupsTag.getCompound(groupName).orElseThrow(), scope.forGroup(groupName));
                } catch (Exception ignored) {}
            }
        }
        return scope;
    }

    private void readTable(CompoundTag tag, PermissionTable targetTable) {
        for (String actionName : NbtIoHelper.keysOf(tag)) {
            try {
                PermissionAction action = PermissionAction.valueOf(actionName);
                PermissionDecision decision = PermissionDecision.valueOf(tag.getString(actionName).orElse("UNSET"));
                targetTable.set(action, decision);
            } catch (IllegalArgumentException ignored) {}
        }
    }
}
