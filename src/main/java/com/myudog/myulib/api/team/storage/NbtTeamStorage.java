package com.myudog.myulib.api.team.storage;

import com.myudog.myulib.Myulib;
import com.myudog.myulib.api.storage.DataStorage;
import com.myudog.myulib.api.team.TeamDefinition;
import com.myudog.myulib.api.team.TeamColor;
import com.myudog.myulib.api.team.TeamFlag;
import com.myudog.myulib.api.util.NbtIoHelper;
import net.minecraft.nbt.*;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.MinecraftServer;

import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 實作 DataStorage 的 NBT 隊伍儲存庫。
 * 負責將 TeamDefinition 序列化並透過反射安全地寫入實體 NBT 檔案。
 */
public class NbtTeamStorage implements DataStorage<UUID, TeamDefinition> {

    private static final String FILE_NAME = "teams.dat";
    private static final String TEAMS_KEY = "teams";

    private Path storageFile;

    // 檔案鏡像快取：用於在 save/delete 單筆資料時，能快速重寫整個檔案
    private final Map<UUID, TeamDefinition> fileMirror = new ConcurrentHashMap<>();

    // =====================================================================
    // 1. DataStorage 介面實作
    // =====================================================================

    @Override
    public void initialize(MinecraftServer server) {
        Path rootPath = resolveRootPath(server).toAbsolutePath().normalize();
        this.storageFile = rootPath.resolve(Myulib.MOD_ID).resolve(FILE_NAME);

        try {
            if (!Files.exists(this.storageFile.getParent())) {
                Files.createDirectories(this.storageFile.getParent());
            }
        } catch (Exception e) {
            System.err.println("[Myulib] 無法建立 Team 儲存目錄: " + e.getMessage());
        }
    }

    @Override
    public Map<UUID, TeamDefinition> loadAll() {
        fileMirror.clear();
        if (storageFile == null || !Files.exists(storageFile)) {
            return new HashMap<>(); // 檔案不存在，回傳空資料
        }

        try {
            CompoundTag root = readRoot(storageFile);
            Tag teamsElement = root.get(TEAMS_KEY);

            if (teamsElement instanceof ListTag list) {
                for (int i = 0; i < list.size(); i++) {
                    TeamDefinition team = readTeam(list.getCompound(i).orElseThrow());
                    fileMirror.put(team.uuid(), team);
                }
            }
        } catch (Exception e) {
            throw new IllegalStateException("無法讀取 Team NBT: " + storageFile, e);
        }

        // 回傳鏡像的複本給 Manager
        return new HashMap<>(fileMirror);
    }

    @Override
    public void save(UUID uuid, TeamDefinition data) {
        fileMirror.put(uuid, data);
        saveToFile();
    }

    @Override
    public void delete(UUID uuid) {
        if (fileMirror.remove(uuid) != null) {
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

            for (TeamDefinition team : fileMirror.values()) {
                list.add(writeTeam(team));
            }

            root.put(TEAMS_KEY, list);
            writeRoot(storageFile, root);
        } catch (Exception e) {
            throw new IllegalStateException("無法儲存 Team NBT: " + storageFile, e);
        }
    }

    private CompoundTag writeTeam(TeamDefinition team) {
        CompoundTag tag = new CompoundTag();
        tag.putString("uuid", team.uuid().toString());
        tag.putString("displayName", team.translationKey().getString());

        tag.putString("color", team.color().name());
        tag.putInt("playerLimit", team.playerLimit());

        CompoundTag flagsTag = new CompoundTag();
        for (Map.Entry<TeamFlag, Boolean> entry : team.flags().entrySet()) {
            flagsTag.putBoolean(entry.getKey().name(), Boolean.TRUE.equals(entry.getValue()));
        }
        tag.put("flags", flagsTag);

        return tag;
    }

    private TeamDefinition readTeam(CompoundTag tag) {
        String uuidStr = tag.getString("uuid").orElse("");
        UUID teamUuid;
        try {
            if (uuidStr.isBlank()) {
                teamUuid = UUID.randomUUID(); // 或者 return null 讓上層過濾掉
            } else {
                teamUuid = UUID.fromString(uuidStr);
            }
        } catch (IllegalArgumentException e) {
            System.err.println("[MyuLib] 警告：發現無效的 Team UUID，已重新生成：" + uuidStr);
            teamUuid = UUID.randomUUID();
        }

        String displayNameStr = tag.getString("displayName").orElse(uuidStr);
        MutableComponent displayName = Component.literal(displayNameStr);
        TeamColor color = TeamColor.DEFAULT;
        String colorName = tag.getString("color").orElse(TeamColor.DEFAULT.name());
        try {
            color = TeamColor.valueOf(colorName);
        } catch (IllegalArgumentException ignored) {
        }

        Map<TeamFlag, Boolean> flags = new EnumMap<>(TeamFlag.class);

        if (tag.contains("flags")) {
            CompoundTag flagsTag = tag.getCompound("flags").orElseThrow();
            for (String key : keysOf(flagsTag)) {
                boolean value = flagsTag.getBoolean(key).orElse(false);
                try {
                    flags.put(TeamFlag.valueOf(key), value);
                } catch (IllegalArgumentException ignored) {
                }
            }
        }

        int playerLimit = Math.max(0, tag.getInt("playerLimit").orElse(0));
        return new TeamDefinition(teamUuid, displayName, color, flags, playerLimit);
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