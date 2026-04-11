package com.myudog.myulib.api.field;

import net.minecraft.nbt.*;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.phys.AABB;

import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

final class NbtFieldStorage implements FieldStorage {
    private static final String FILE_NAME = "fields.dat";
    private static final String FIELDS_KEY = "fields";

    private final Map<Identifier, FieldDefinition> fields = new LinkedHashMap<>();

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
    public synchronized void add(FieldDefinition field) {
        ensureLoaded();
        fields.put(field.id(), field);
        markDirty();
    }

    @Override
    public synchronized void remove(Identifier id) {
        ensureLoaded();
        if (fields.remove(id) != null) {
            markDirty();
        }
    }

    @Override
    public synchronized Map<Identifier, FieldDefinition> getAll() {
        ensureLoaded();
        return Map.copyOf(fields);
    }

    // --- 內部存取邏輯 ---

    public void markDirty() {
        dirty = true;
        if (storageFile != null) saveInternal();
    }

    private void loadInternal() {
        fields.clear();
        try {
            CompoundTag root = readRoot(storageFile);
            Tag fieldsElement = root.get(FIELDS_KEY);
            if (fieldsElement instanceof ListTag list) {
                for (int i = 0; i < list.size(); i++) {
                    FieldDefinition field = readField(list.getCompound(i).orElseThrow());
                    fields.put(field.id(), field);
                }
            }
            loaded = true;
            dirty = false;
        } catch (Exception e) {
            throw new IllegalStateException("無法讀取 Field NBT: " + storageFile, e);
        }
    }

    private void saveInternal() {
        if (storageFile == null) return;
        try {
            Files.createDirectories(storageFile.getParent());
            CompoundTag root = new CompoundTag();
            ListTag list = new ListTag();
            for (FieldDefinition field : fields.values()) {
                list.add(writeField(field));
            }
            root.put(FIELDS_KEY, list);
            writeRoot(storageFile, root);
            loaded = true;
            dirty = false;
        } catch (Exception e) {
            throw new IllegalStateException("無法儲存 Field NBT: " + storageFile, e);
        }
    }

    // --- NBT 序列化轉換 ---

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
        String idStr = tag.getString("id").orElse("");
        String dimStr = tag.getString("dim").orElse("minecraft:overworld");

        Identifier id = Identifier.parse(idStr);
        Identifier dim = Identifier.parse(dimStr);

        ListTag bounds = (ListTag) tag.get("bounds");

        assert bounds != null;
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
        // 🔧 修正：將 dimStr 改為 dim，符合 FieldDefinition 的 Identifier 型別需求
        return new FieldDefinition(id, dim, aabb, fieldData);
    }

    // =====================================================================
    // NBT 反射底層工具 (與 RoleGroupStorage 共用的安全實作)
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