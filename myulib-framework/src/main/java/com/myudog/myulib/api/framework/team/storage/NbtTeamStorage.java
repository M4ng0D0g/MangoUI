package com.myudog.myulib.api.framework.team.storage;

import com.myudog.myulib.Myulib;

import com.myudog.myulib.api.core.storage.DataStorage;
import com.myudog.myulib.api.framework.team.TeamDefinition;
import com.myudog.myulib.api.framework.team.TeamColor;
import com.myudog.myulib.api.framework.team.TeamFlag;
import com.myudog.myulib.api.core.util.NbtIoHelper;
import net.minecraft.nbt.*;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.MinecraftServer;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * NbtTeamStorage
 * 
 * 系統：隊伍管理系統 (Framework - Team)
 * 角色：將隊伍定義持久化至 NBT 檔案。
 * 類型：Storage Implementation
 */
public class NbtTeamStorage implements DataStorage<UUID, TeamDefinition> {

    private static final String FILE_NAME = "teams.dat";
    private static final String TEAMS_KEY = "teams";

    private Path storageFile;
    private final Map<UUID, TeamDefinition> fileMirror = new ConcurrentHashMap<>();

    @Override
    public void initialize(MinecraftServer server) {
        Path rootPath = NbtIoHelper.resolveRootPath(server).toAbsolutePath().normalize();
        this.storageFile = rootPath.resolve(Myulib.MOD_ID).resolve(FILE_NAME);

        try {
            if (!Files.exists(this.storageFile.getParent())) {
                Files.createDirectories(this.storageFile.getParent());
            }
        } catch (Exception e) {
            Myulib.LOGGER.error("Failed to create Team storage directory: " + e.getMessage());
        }
    }

    @Override
    public Map<UUID, TeamDefinition> loadAll() {
        fileMirror.clear();
        if (storageFile == null || !Files.exists(storageFile)) {
            return new HashMap<>();
        }

        try {
            CompoundTag root = NbtIoHelper.readRoot(storageFile);
            Tag teamsElement = root.get(TEAMS_KEY);

            if (teamsElement instanceof ListTag list) {
                for (int i = 0; i < list.size(); i++) {
                    TeamDefinition team = readTeam(list.getCompound(i).orElseThrow());
                    fileMirror.put(team.uuid(), team);
                }
            }
        } catch (Exception e) {
            Myulib.LOGGER.error("Failed to load Team NBT: " + storageFile, e);
        }

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

    private synchronized void saveToFile() {
        if (storageFile == null) return;
        try {
            CompoundTag root = new CompoundTag();
            ListTag list = new ListTag();

            for (TeamDefinition team : fileMirror.values()) {
                list.add(writeTeam(team));
            }

            root.put(TEAMS_KEY, list);
            NbtIoHelper.writeRoot(storageFile, root);
        } catch (Exception e) {
            Myulib.LOGGER.error("Failed to save Team NBT: " + storageFile, e);
        }
    }

    private CompoundTag writeTeam(TeamDefinition team) {
        CompoundTag tag = new CompoundTag();
        tag.putString("uuid", team.uuid().toString());
        tag.putString("displayName", team.translationKey().getString());
        tag.putString("color", team.color().name());
        tag.putInt("playerLimit", team.playerLimit());

        CompoundTag flagsTag = new CompoundTag();
        if (team.flags() != null) {
            for (Map.Entry<TeamFlag, Boolean> entry : team.flags().entrySet()) {
                flagsTag.putBoolean(entry.getKey().name(), Boolean.TRUE.equals(entry.getValue()));
            }
        }
        tag.put("flags", flagsTag);

        return tag;
    }

    private TeamDefinition readTeam(CompoundTag tag) {
        String uuidStr = tag.getString("uuid").orElse("");
        UUID teamUuid;
        try {
            if (uuidStr.isBlank()) {
                teamUuid = UUID.randomUUID();
            } else {
                teamUuid = UUID.fromString(uuidStr);
            }
        } catch (IllegalArgumentException e) {
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
            for (String key : NbtIoHelper.keysOf(flagsTag)) {
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
}
