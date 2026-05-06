package com.myudog.myulib.api.permission.storage;

import com.myudog.myulib.Myulib;
import com.myudog.myulib.api.permission.PermissionAction;
import com.myudog.myulib.api.permission.PermissionDecision;
import com.myudog.myulib.api.permission.PermissionScope;
import com.myudog.myulib.api.permission.PermissionTable;
import com.myudog.myulib.api.storage.DataStorage;
import com.myudog.myulib.api.util.NbtIoHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.server.MinecraftServer;

import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 實作 DataStorage 的 NBT 權限儲存庫。
 * 支援將扁平化 Key (global, dim:xxx, field:xxx) 轉換為階層式 NBT 結構儲存。
 */
public class NbtPermissionStorage implements DataStorage<String, PermissionScope> {

    private static final String FILE_NAME = "permissions.dat";
    private Path storageFile;

    // 檔案鏡像快取：用於單筆 save/delete 時快速重寫檔案
    private final Map<String, PermissionScope> fileMirror = new ConcurrentHashMap<>();

    // =====================================================================
    // 1. DataStorage 介面實作
    // =====================================================================

    @Override
    public void initialize(MinecraftServer server) {
        bindRoot(resolveRootPath(server));
    }

    public void bindRoot(Path root) {
        Path rootPath = root == null ? Paths.get(".") : root.toAbsolutePath().normalize();
        this.storageFile = rootPath.resolve(Myulib.MOD_ID).resolve(FILE_NAME);

        try {
            if (this.storageFile != null && !Files.exists(this.storageFile.getParent())) {
                Files.createDirectories(this.storageFile.getParent());
            }
        } catch (Exception e) {
            System.err.println("[Myulib] 無法建立 Permission 儲存目錄: " + e.getMessage());
        }
    }

    @Override
    public Map<String, PermissionScope> loadAll() {
        fileMirror.clear();
        if (storageFile == null || !Files.exists(storageFile)) {
            return new HashMap<>(); // 檔案不存在，回傳空 Map
        }

        try {
            CompoundTag root = readRoot(storageFile);

            // 讀取 Global Scope
            if (root.contains("global")) {
                fileMirror.put("global", readScope(root.getCompound("global").orElseThrow()));
            }

            // 讀取 Dimension Scopes
            if (root.contains("dimensions")) {
                CompoundTag dimsTag = root.getCompound("dimensions").orElseThrow();
                for (String key : keysOf(dimsTag)) {
                    fileMirror.put("dim:" + key, readScope(dimsTag.getCompound(key).orElseThrow()));
                }
            }

            // 讀取 Field Scopes
            if (root.contains("fields")) {
                CompoundTag fieldsTag = root.getCompound("fields").orElseThrow();
                for (String key : keysOf(fieldsTag)) {
                    fileMirror.put("field:" + key, readScope(fieldsTag.getCompound(key).orElseThrow()));
                }
            }

        } catch (Exception e) {
            throw new IllegalStateException("無法讀取 Permission NBT: " + storageFile, e);
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

    // =====================================================================
    // 2. 內部寫入邏輯與 NBT 序列化轉換
    // =====================================================================

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
            writeRoot(storageFile, root);

        } catch (Exception e) {
            throw new IllegalStateException("無法儲存 Permission NBT: " + storageFile, e);
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
            for (String uuidStr : keysOf(playersTag)) {
                readTable(playersTag.getCompound(uuidStr).orElseThrow(), scope.forPlayer(UUID.fromString(uuidStr)));
            }
        }
        if (tag.contains("groups")) {
            CompoundTag groupsTag = tag.getCompound("groups").orElseThrow();
            for (String groupName : keysOf(groupsTag)) {
                readTable(groupsTag.getCompound(groupName).orElseThrow(), scope.forGroup(groupName));
            }
        }
        return scope;
    }

    private void readTable(CompoundTag tag, PermissionTable targetTable) {
        for (String actionName : keysOf(tag)) {
            try {
                PermissionAction action = PermissionAction.valueOf(actionName);
                PermissionDecision decision = PermissionDecision.valueOf(tag.getString(actionName).orElse("UNSET"));
                targetTable.set(action, decision);
            } catch (IllegalArgumentException ignored) {} // 忽略已廢棄或未知的 Enum
        }
    }

    // =====================================================================
    // 3. NBT 反射底層工具 (安全實作)
    // =====================================================================

    private static CompoundTag readRoot(Path path) throws Exception {
        return NbtIoHelper.readRoot(path);
    }

    private static void writeRoot(Path path, CompoundTag root) throws Exception {
        NbtIoHelper.writeRoot(path, root);
    }

    private static Method findNbtIoMethod(String name, Path path) {
        for (Method method : NbtIo.class.getMethods()) {
            if (!method.getName().equals(name)) continue;
            Class<?>[] params = method.getParameterTypes();
            if (params.length == 1 && params[0].isAssignableFrom(Path.class)) return method;
            if (params.length == 2 && (params[0].isAssignableFrom(Path.class) || params[0].isAssignableFrom(java.io.InputStream.class) || params[0].isAssignableFrom(java.io.OutputStream.class) || params[0].isAssignableFrom(CompoundTag.class))) return method;
        }
        return null;
    }

    private static Object[] buildNbtIoArguments(Method method, Path path, boolean reading) throws Exception {
        Class<?>[] params = method.getParameterTypes();
        if (params.length == 1) return new Object[]{path};
        Object helper = createHelperArgument(params[1]);
        if (helper == null && !params[1].isPrimitive()) helper = null;
        if (params[0].isAssignableFrom(Path.class)) return new Object[]{path, helper};
        if (params[0].isAssignableFrom(java.io.InputStream.class)) return new Object[]{Files.newInputStream(path), helper};
        if (params[0].isAssignableFrom(java.io.OutputStream.class)) return new Object[]{Files.newOutputStream(path), helper};
        return new Object[]{path, helper};
    }

    private static Object createHelperArgument(Class<?> type) {
        try {
            for (Method method : type.getMethods()) {
                if (!java.lang.reflect.Modifier.isStatic(method.getModifiers()) || !type.isAssignableFrom(method.getReturnType())) continue;
                if (method.getParameterCount() == 0) return method.invoke(null);
            }
            try { return type.getDeclaredConstructor().newInstance(); } catch (ReflectiveOperationException ignored) {}
        } catch (Exception ignored) {}
        return null;
    }

    private static List<String> keysOf(CompoundTag compound) {
        return NbtIoHelper.keysOf(compound);
    }

    private static Path resolveRootPath(MinecraftServer server) {
        return NbtIoHelper.resolveRootPath(server);
    }
}