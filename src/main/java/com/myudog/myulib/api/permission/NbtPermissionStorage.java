package com.myudog.myulib.api.permission;

import net.minecraft.nbt.*;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;

import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

final class NbtPermissionStorage implements PermissionStorage {
    private static final String FILE_NAME = "permissions.dat";

    private final PermissionScope globalScope = new PermissionScope();
    private final Map<Identifier, PermissionScope> dimensionScopes = new LinkedHashMap<>();
    private final Map<Identifier, PermissionScope> fieldScopes = new LinkedHashMap<>();

    private Path rootPath;
    private Path storageFile;
    private boolean loaded = false;
    private boolean dirty = false;

    // --- 介面實作 ---

    @Override
    public synchronized void bindServer(MinecraftServer server) {
        bindRoot(resolveRootPath(server));
    }

    public synchronized void bindRoot(Path root) {
        Path normalized = root == null ? null : root.toAbsolutePath().normalize();
        if (Objects.equals(rootPath, normalized) && loaded) return;
        if (loaded && dirty) saveInternal();

        rootPath = normalized;
        storageFile = rootPath == null ? null : rootPath.resolve("myulib").resolve(FILE_NAME);
        if (storageFile == null) { loaded = true; return; }

        if (Files.exists(storageFile)) loadInternal();
        else saveInternal();
    }

    @Override
    public synchronized void ensureLoaded() {
        if (loaded) return;
        if (storageFile != null && Files.exists(storageFile)) loadInternal();
        else loaded = true;
    }

    @Override
    public PermissionScope getGlobalScope() { ensureLoaded(); return globalScope; }

    @Override
    public Map<Identifier, PermissionScope> getDimensionScopes() { ensureLoaded(); return dimensionScopes; }

    @Override
    public Map<Identifier, PermissionScope> getFieldScopes() { ensureLoaded(); return fieldScopes; }

    @Override
    public void markDirty() {
        dirty = true;
        if (storageFile != null) saveInternal();
    }

    // --- 內部存取邏輯 ---

    private void loadInternal() {
        dimensionScopes.clear();
        fieldScopes.clear();
        try {
            CompoundTag root = readRoot(storageFile);

            if (root.contains("global")) {
                readScope(root.getCompound("global").orElseThrow(), globalScope);
            }

            if (root.contains("dimensions")) {
                CompoundTag dimsTag = root.getCompound("dimensions").orElseThrow();
                for (String key : keysOf(dimsTag)) {
                    PermissionScope scope = new PermissionScope();
                    readScope(dimsTag.getCompound(key).orElseThrow(), scope);
                    dimensionScopes.put(Identifier.parse(key), scope);
                }
            }

            if (root.contains("fields")) {
                CompoundTag fieldsTag = root.getCompound("fields").orElseThrow();
                for (String key : keysOf(fieldsTag)) {
                    PermissionScope scope = new PermissionScope();
                    readScope(fieldsTag.getCompound(key).orElseThrow(), scope);
                    fieldScopes.put(Identifier.parse(key), scope);
                }
            }

            loaded = true;
            dirty = false;
        } catch (Exception e) {
            throw new IllegalStateException("無法讀取 Permission NBT: " + storageFile, e);
        }
    }

    private void saveInternal() {
        if (storageFile == null) return;
        try {
            Files.createDirectories(storageFile.getParent());
            CompoundTag root = new CompoundTag();

            root.put("global", writeScope(globalScope));

            CompoundTag dimsTag = new CompoundTag();
            dimensionScopes.forEach((id, scope) -> dimsTag.put(id.toString(), writeScope(scope)));
            root.put("dimensions", dimsTag);

            CompoundTag fieldsTag = new CompoundTag();
            fieldScopes.forEach((id, scope) -> fieldsTag.put(id.toString(), writeScope(scope)));
            root.put("fields", fieldsTag);

            writeRoot(storageFile, root);
            loaded = true;
            dirty = false;
        } catch (Exception e) {
            throw new IllegalStateException("無法儲存 Permission NBT: " + storageFile, e);
        }
    }

    // --- NBT 序列化轉換 (階層遞歸) ---

    private CompoundTag writeScope(PermissionScope scope) {
        CompoundTag tag = new CompoundTag();

        CompoundTag playersTag = new CompoundTag();
        // 假設 PermissionScope 提供 getter，若無請在 PermissionScope 加入 getPlayerTables()
        // for (Map.Entry<UUID, PermissionTable> entry : scope.getPlayerTables().entrySet()) { ... }

        CompoundTag groupsTag = new CompoundTag();
        // for (Map.Entry<String, PermissionTable> entry : scope.getGroupTables().entrySet()) { ... }

        tag.put("players", playersTag);
        tag.put("groups", groupsTag);
        return tag;
    }

    private CompoundTag writeTable(PermissionTable table) {
        CompoundTag tag = new CompoundTag();
        // for (Map.Entry<PermissionAction, PermissionDecision> entry : table.getRules().entrySet()) {
        //     tag.putString(entry.getKey().name(), entry.getValue().name());
        // }
        return tag;
    }

    private void readScope(CompoundTag tag, PermissionScope targetScope) {
        if (tag.contains("players")) {
            CompoundTag playersTag = tag.getCompound("players").orElseThrow();
            for (String uuidStr : keysOf(playersTag)) {
                readTable(playersTag.getCompound(uuidStr).orElseThrow(), targetScope.forPlayer(UUID.fromString(uuidStr)));
            }
        }
        if (tag.contains("groups")) {
            CompoundTag groupsTag = tag.getCompound("groups").orElseThrow();
            for (String groupName : keysOf(groupsTag)) {
                readTable(groupsTag.getCompound(groupName).orElseThrow(), targetScope.forGroup(groupName));
            }
        }
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
    // NBT 反射底層工具 (保持獨立運作)
    // =====================================================================

    private static CompoundTag readRoot(Path path) throws Exception {
        Method method = findNbtIoMethod("readCompressed", path);
        if (method == null) method = findNbtIoMethod("read", path);
        if (method == null) throw new NoSuchMethodException("No suitable NbtIo read method found");
        Object[] args = buildNbtIoArguments(method, path, true);
        return (CompoundTag) method.invoke(null, args);
    }

    private static void writeRoot(Path path, CompoundTag root) throws Exception {
        Method method = findNbtIoMethod("writeCompressed", path);
        if (method == null) method = findNbtIoMethod("write", path);
        if (method == null) throw new NoSuchMethodException("No suitable NbtIo write method found");

        Class<?>[] parameters = method.getParameterTypes();
        if (parameters.length == 1) {
            if (parameters[0].isAssignableFrom(CompoundTag.class)) { method.invoke(null, root); return; }
            if (parameters[0].isAssignableFrom(Path.class)) { method.invoke(null, path); return; }
            if (parameters[0].isAssignableFrom(java.io.OutputStream.class)) {
                try (java.io.OutputStream outputStream = Files.newOutputStream(path)) { method.invoke(null, outputStream); }
                return;
            }
        }
        if (parameters.length == 2 && parameters[0].isAssignableFrom(CompoundTag.class)) {
            Object helper = createHelperArgument(parameters[1]);
            if (parameters[1].isAssignableFrom(Path.class)) { method.invoke(null, root, path); return; }
            if (parameters[1].isAssignableFrom(java.io.OutputStream.class)) {
                try (java.io.OutputStream outputStream = Files.newOutputStream(path)) { method.invoke(null, root, outputStream); }
                return;
            }
            method.invoke(null, root, helper);
            return;
        }
        throw new NoSuchMethodException("No suitable NbtIo write method found");
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
        for (String methodName : List.of("getKeys", "getAllKeys", "keySet")) {
            try {
                Method method = compound.getClass().getMethod(methodName);
                Object value = method.invoke(compound);
                if (value instanceof Iterable<?> iterable) {
                    List<String> keys = new ArrayList<>();
                    for (Object entry : iterable) keys.add(String.valueOf(entry));
                    return keys;
                }
            } catch (ReflectiveOperationException ignored) {}
        }
        return List.of();
    }

    private static Path resolveRootPath(MinecraftServer server) {
        if (server == null) return Paths.get(".");
        for (String methodName : List.of("getSavePath", "getRunDirectory", "getServerRunDirectory", "getServerDirectory")) {
            try {
                for (Method method : server.getClass().getMethods()) {
                    if (!method.getName().equals(methodName)) continue;
                    if (method.getParameterCount() == 0 && Path.class.isAssignableFrom(method.getReturnType())) return (Path) method.invoke(server);
                }
            } catch (Exception ignored) {}
        }
        return Paths.get(".");
    }
}