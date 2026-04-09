package com.myudog.myulib.api.rolegroup;

import com.myudog.myulib.api.permission.PermissionDecision;
import com.myudog.myulib.api.permission.PermissionGrant;
import com.myudog.myulib.api.permission.PermissionLayer;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.server.MinecraftServer;

import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.UnaryOperator;

final class NbtRoleGroupStorage implements RoleGroupStorage {
    private static final String FILE_NAME = "rolegroups.dat";
    private static final String GROUPS_KEY = "groups";
    private static final String MEMBERSHIPS_KEY = "memberships";

    private final Map<String, RoleGroupDefinition> groups = new LinkedHashMap<>();
    private final Map<UUID, Set<String>> playerGroups = new LinkedHashMap<>();

    private Path rootPath;
    private Path storageFile;
    private boolean loaded;
    private boolean dirty;

    @Override
    public synchronized void bindServer(MinecraftServer server) {
        bindRoot(resolveRootPath(server));
    }

    @Override
    public synchronized void bindRoot(Path root) {
        Path normalized = root == null ? null : root.toAbsolutePath().normalize();
        if (Objects.equals(rootPath, normalized) && loaded) {
            return;
        }
        if (loaded && dirty) {
            saveInternal();
        }
        rootPath = normalized;
        storageFile = rootPath == null ? null : rootPath.resolve("myulib").resolve(FILE_NAME);
        if (storageFile == null) {
            loaded = true;
            return;
        }
        if (Files.exists(storageFile)) {
            loadInternal();
        } else {
            saveInternal();
        }
    }

    @Override
    public synchronized void ensureLoaded() {
        if (loaded) {
            return;
        }
        if (storageFile != null && Files.exists(storageFile)) {
            loadInternal();
        } else {
            loaded = true;
        }
    }

    @Override
    public synchronized RoleGroupDefinition register(RoleGroupDefinition group) {
        ensureLoaded();
        Objects.requireNonNull(group, "group");
        groups.put(group.id(), group);
        markDirty();
        return group;
    }

    @Override
    public synchronized RoleGroupDefinition update(String groupId, UnaryOperator<RoleGroupDefinition> updater) {
        ensureLoaded();
        Objects.requireNonNull(groupId, "groupId");
        Objects.requireNonNull(updater, "updater");
        RoleGroupDefinition existing = groups.get(groupId);
        if (existing == null) {
            return null;
        }
        RoleGroupDefinition updated = Objects.requireNonNull(updater.apply(existing), "updated group");
        groups.put(groupId, updated);
        markDirty();
        return updated;
    }

    @Override
    public synchronized RoleGroupDefinition remove(String groupId) {
        ensureLoaded();
        RoleGroupDefinition removed = groups.remove(groupId);
        if (removed == null) {
            return null;
        }
        for (Set<String> memberships : playerGroups.values()) {
            memberships.remove(groupId);
        }
        playerGroups.entrySet().removeIf(entry -> entry.getValue().isEmpty());
        markDirty();
        return removed;
    }

    @Override
    public synchronized RoleGroupDefinition get(String groupId) {
        ensureLoaded();
        return groups.get(groupId);
    }

    @Override
    public synchronized List<RoleGroupDefinition> all() {
        ensureLoaded();
        return List.copyOf(groups.values());
    }

    @Override
    public synchronized Map<String, RoleGroupDefinition> snapshot() {
        ensureLoaded();
        return Map.copyOf(groups);
    }

    @Override
    public synchronized boolean assign(UUID playerId, String groupId) {
        ensureLoaded();
        Objects.requireNonNull(playerId, "playerId");
        Objects.requireNonNull(groupId, "groupId");
        if (!groups.containsKey(groupId)) {
            return false;
        }
        boolean changed = playerGroups.computeIfAbsent(playerId, ignored -> new LinkedHashSet<>()).add(groupId);
        if (changed) {
            markDirty();
        }
        return changed;
    }

    @Override
    public synchronized boolean revoke(UUID playerId, String groupId) {
        ensureLoaded();
        Set<String> memberships = playerGroups.get(playerId);
        if (memberships == null) {
            return false;
        }
        boolean changed = memberships.remove(groupId);
        if (memberships.isEmpty()) {
            playerGroups.remove(playerId);
        }
        if (changed) {
            markDirty();
        }
        return changed;
    }

    @Override
    public synchronized Set<String> groupIdsOf(UUID playerId) {
        ensureLoaded();
        Set<String> memberships = playerGroups.get(playerId);
        return memberships == null ? Set.of() : Set.copyOf(memberships);
    }

    @Override
    public synchronized List<RoleGroupDefinition> groupsOf(UUID playerId) {
        ensureLoaded();
        Set<String> memberships = playerGroups.get(playerId);
        if (memberships == null || memberships.isEmpty()) {
            return List.of();
        }
        List<RoleGroupDefinition> result = new ArrayList<>();
        for (String groupId : memberships) {
            RoleGroupDefinition definition = groups.get(groupId);
            if (definition != null) {
                result.add(definition);
            }
        }
        result.sort((a, b) -> Integer.compare(b.priority(), a.priority()));
        return List.copyOf(result);
    }

    @Override
    public synchronized void clear() {
        groups.clear();
        playerGroups.clear();
        loaded = false;
        dirty = false;
    }

    private void markDirty() {
        dirty = true;
        saveIfBound();
    }

    private void saveIfBound() {
        if (storageFile != null) {
            saveInternal();
        }
    }

    private void loadInternal() {
        groups.clear();
        playerGroups.clear();
        try {
            CompoundTag root = readRoot(storageFile);
            Tag groupsElement = root.get(GROUPS_KEY);
            if (groupsElement instanceof ListTag groupList) {
                for (int i = 0; i < groupList.size(); i++) {
                    CompoundTag groupTag = groupList.getCompound(i).orElseThrow();
                    RoleGroupDefinition group = readGroup(groupTag);
                    groups.put(group.id(), group);
                }
            }
            Tag membershipsElement = root.get(MEMBERSHIPS_KEY);
            if (membershipsElement instanceof CompoundTag membershipsCompound) {
                for (String playerKey : keysOf(membershipsCompound)) {
                    Tag entry = membershipsCompound.get(playerKey);
                    if (entry instanceof ListTag list) {
                        Set<String> groupIds = new LinkedHashSet<>();
                        for (int i = 0; i < list.size(); i++) {
                            groupIds.add(list.getString(i).orElseThrow());
                        }
                        if (!groupIds.isEmpty()) {
                            playerGroups.put(UUID.fromString(playerKey), groupIds);
                        }
                    }
                }
            }
            loaded = true;
            dirty = false;
        } catch (Exception e) {
            throw new IllegalStateException("Unable to load RoleGroup data from " + storageFile, e);
        }
    }

    private void saveInternal() {
        if (storageFile == null) {
            return;
        }
        try {
            Files.createDirectories(storageFile.getParent());
            CompoundTag root = new CompoundTag();
            ListTag groupList = new ListTag();
            for (RoleGroupDefinition group : groups.values()) {
                groupList.add(writeGroup(group));
            }
            root.put(GROUPS_KEY, groupList);
            CompoundTag membershipsCompound = new CompoundTag();
            for (Map.Entry<UUID, Set<String>> entry : playerGroups.entrySet()) {
                ListTag membershipList = new ListTag();
                for (String groupId : entry.getValue()) {
                    membershipList.add(StringTag.valueOf(groupId));
                }
                membershipsCompound.put(entry.getKey().toString(), membershipList);
            }
            root.put(MEMBERSHIPS_KEY, membershipsCompound);
            writeRoot(storageFile, root);
            loaded = true;
            dirty = false;
        } catch (Exception e) {
            throw new IllegalStateException("Unable to save RoleGroup data to " + storageFile, e);
        }
    }

    private static RoleGroupDefinition readGroup(CompoundTag compound) {
        String id = compound.getString("id").orElse("");
        String displayName = compound.getString("displayName").orElse(id);
        int priority = compound.getInt("priority").orElse(0);
        List<PermissionGrant> grants = new ArrayList<>();
        Tag grantsElement = compound.get("grants");
        if (grantsElement instanceof ListTag grantsList) {
            for (int i = 0; i < grantsList.size(); i++) {
                grants.add(readGrant(grantsList.getCompound(i).orElseThrow()));
            }
        }
        Map<String, String> metadata = new LinkedHashMap<>();
        Tag metadataElement = compound.get("metadata");
        if (metadataElement instanceof CompoundTag metadataCompound) {
            for (String key : keysOf(metadataCompound)) {
                metadata.put(key, metadataCompound.getString(key).orElse(""));
            }
        }
        return new RoleGroupDefinition(id, displayName, priority, grants, metadata);
    }

    private static CompoundTag writeGroup(RoleGroupDefinition group) {
        CompoundTag compound = new CompoundTag();
        compound.putString("id", group.id());
        compound.putString("displayName", group.displayName());
        compound.putInt("priority", group.priority());
        ListTag grantsList = new ListTag();
        for (PermissionGrant grant : group.grants()) {
            grantsList.add(writeGrant(grant));
        }
        compound.put("grants", grantsList);
        CompoundTag metadataCompound = new CompoundTag();
        for (Map.Entry<String, String> entry : group.metadata().entrySet()) {
            metadataCompound.putString(entry.getKey(), entry.getValue());
        }
        compound.put("metadata", metadataCompound);
        return compound;
    }

    private static PermissionGrant readGrant(CompoundTag compound) {
        return new PermissionGrant(
            compound.getString("id").orElse(""),
            PermissionLayer.valueOf(compound.getString("layer").orElseThrow()),
            compound.getString("node").orElse("*"),
            PermissionDecision.valueOf(compound.getString("decision").orElseThrow()),
            compound.getInt("priority").orElse(0)
        );
    }

    private static CompoundTag writeGrant(PermissionGrant grant) {
        CompoundTag compound = new CompoundTag();
        compound.putString("id", grant.id());
        compound.putString("layer", grant.layer().name());
        compound.putString("node", grant.node());
        compound.putString("decision", grant.decision().name());
        compound.putInt("priority", grant.priority());
        return compound;
    }

    private static CompoundTag readRoot(Path path) throws Exception {
        Method method = findNbtIoMethod("readCompressed", path);
        if (method == null) {
            method = findNbtIoMethod("read", path);
        }
        if (method == null) {
            throw new NoSuchMethodException("No suitable NbtIo read method found");
        }
        Object[] args = buildNbtIoArguments(method, path, true);
        return (CompoundTag) method.invoke(null, args);
    }

    private static void writeRoot(Path path, CompoundTag root) throws Exception {
        Method method = findNbtIoMethod("writeCompressed", path);
        if (method == null) {
            method = findNbtIoMethod("write", path);
        }
        if (method == null) {
            throw new NoSuchMethodException("No suitable NbtIo write method found");
        }
        Class<?>[] parameters = method.getParameterTypes();
        if (parameters.length == 1) {
            if (parameters[0].isAssignableFrom(CompoundTag.class)) {
                method.invoke(null, root);
                return;
            }
            if (parameters[0].isAssignableFrom(Path.class)) {
                method.invoke(null, path);
                return;
            }
            if (parameters[0].isAssignableFrom(java.io.OutputStream.class)) {
                try (java.io.OutputStream outputStream = Files.newOutputStream(path)) {
                    method.invoke(null, outputStream);
                }
                return;
            }
        }
        if (parameters.length == 2 && parameters[0].isAssignableFrom(CompoundTag.class)) {
            Object helper = createHelperArgument(parameters[1]);
            if (parameters[1].isAssignableFrom(Path.class)) {
                method.invoke(null, root, path);
                return;
            }
            if (parameters[1].isAssignableFrom(java.io.OutputStream.class)) {
                try (java.io.OutputStream outputStream = Files.newOutputStream(path)) {
                    method.invoke(null, root, outputStream);
                }
                return;
            }
            method.invoke(null, root, helper);
            return;
        }
        throw new NoSuchMethodException("No suitable NbtIo write method found");
    }

    private static Method findNbtIoMethod(String name, Path path) {
        for (Method method : NbtIo.class.getMethods()) {
            if (!method.getName().equals(name)) {
                continue;
            }
            Class<?>[] parameters = method.getParameterTypes();
            if (parameters.length == 1 && parameters[0].isAssignableFrom(Path.class)) {
                return method;
            }
            if (parameters.length == 2 && (parameters[0].isAssignableFrom(Path.class) || parameters[0].isAssignableFrom(java.io.InputStream.class) || parameters[0].isAssignableFrom(java.io.OutputStream.class) || parameters[0].isAssignableFrom(CompoundTag.class))) {
                return method;
            }
        }
        return null;
    }

    private static Object[] buildNbtIoArguments(Method method, Path path, boolean reading) throws Exception {
        Class<?>[] parameters = method.getParameterTypes();
        if (parameters.length == 1) {
            return new Object[]{path};
        }
        Object helper = createHelperArgument(parameters[1]);
        if (helper == null && !parameters[1].isPrimitive()) {
            helper = null;
        }
        if (parameters[0].isAssignableFrom(Path.class)) {
            return new Object[]{path, helper};
        }
        if (parameters[0].isAssignableFrom(java.io.InputStream.class)) {
            return new Object[]{Files.newInputStream(path), helper};
        }
        if (parameters[0].isAssignableFrom(java.io.OutputStream.class)) {
            return new Object[]{Files.newOutputStream(path), helper};
        }
        return new Object[]{path, helper};
    }

    private static Object createHelperArgument(Class<?> type) {
        try {
            for (Method method : type.getMethods()) {
                if (!java.lang.reflect.Modifier.isStatic(method.getModifiers())) {
                    continue;
                }
                if (!type.isAssignableFrom(method.getReturnType())) {
                    continue;
                }
                if (method.getParameterCount() == 0) {
                    return method.invoke(null);
                }
                if (method.getParameterCount() == 1) {
                    Class<?> parameterType = method.getParameterTypes()[0];
                    if (parameterType == long.class || parameterType == Long.class) {
                        return method.invoke(null, Long.MAX_VALUE);
                    }
                    if (parameterType == int.class || parameterType == Integer.class) {
                        return method.invoke(null, Integer.MAX_VALUE);
                    }
                }
            }
            for (Method method : type.getDeclaredMethods()) {
                if (!java.lang.reflect.Modifier.isStatic(method.getModifiers())) {
                    continue;
                }
                if (!type.isAssignableFrom(method.getReturnType())) {
                    continue;
                }
                if (method.getParameterCount() == 0) {
                    method.setAccessible(true);
                    return method.invoke(null);
                }
            }
            try {
                return type.getDeclaredConstructor().newInstance();
            } catch (ReflectiveOperationException ignored) {
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    private static List<String> keysOf(CompoundTag compound) {
        for (String methodName : List.of("getKeys", "getAllKeys", "keySet")) {
            try {
                Method method = compound.getClass().getMethod(methodName);
                Object value = method.invoke(compound);
                if (value instanceof Iterable<?> iterable) {
                    List<String> keys = new ArrayList<>();
                    for (Object entry : iterable) {
                        keys.add(String.valueOf(entry));
                    }
                    return keys;
                }
                if (value instanceof Map<?, ?> map) {
                    List<String> keys = new ArrayList<>();
                    for (Object entry : map.keySet()) {
                        keys.add(String.valueOf(entry));
                    }
                    return keys;
                }
            } catch (ReflectiveOperationException ignored) {
            }
        }
        return List.of();
    }

    private static Path resolveRootPath(MinecraftServer server) {
        if (server == null) {
            return Paths.get(".");
        }
        for (String methodName : List.of("getSavePath", "getRunDirectory", "getServerRunDirectory", "getServerDirectory")) {
            Path direct = invokePathMethod(server, methodName);
            if (direct != null) {
                return direct;
            }
        }
        return Paths.get(".");
    }

    private static Path invokePathMethod(MinecraftServer server, String methodName) {
        try {
            for (Method method : server.getClass().getMethods()) {
                if (!method.getName().equals(methodName)) {
                    continue;
                }
                if (method.getParameterCount() == 0 && Path.class.isAssignableFrom(method.getReturnType())) {
                    return (Path) method.invoke(server);
                }
                if (method.getParameterCount() == 1 && Path.class.isAssignableFrom(method.getReturnType())) {
                    Object argument = resolveRootArgument(method.getParameterTypes()[0]);
                    if (argument != null) {
                        return (Path) method.invoke(server, argument);
                    }
                }
            }
        } catch (ReflectiveOperationException ignored) {
        }
        return null;
    }

    private static Object resolveRootArgument(Class<?> parameterType) {
        if (parameterType.isEnum()) {
            for (Object constant : parameterType.getEnumConstants()) {
                if ("ROOT".equals(constant.toString()) || "WORLD".equals(constant.toString())) {
                    return constant;
                }
            }
            Object[] constants = parameterType.getEnumConstants();
            return constants.length > 0 ? constants[0] : null;
        }
        if (Path.class.isAssignableFrom(parameterType)) {
            return Paths.get(".");
        }
        if (String.class.equals(parameterType)) {
            return "ROOT";
        }
        return null;
    }
}



