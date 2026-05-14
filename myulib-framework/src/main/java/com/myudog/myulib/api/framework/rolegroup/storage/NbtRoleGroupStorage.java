package com.myudog.myulib.api.framework.rolegroup.storage;

import com.myudog.myulib.Myulib;

import com.myudog.myulib.api.framework.rolegroup.RoleGroupDefinition;
import com.myudog.myulib.api.framework.rolegroup.RoleGroupStorage;
import com.myudog.myulib.api.core.util.NbtIoHelper;
import net.minecraft.nbt.*;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * NbtRoleGroupStorage
 * 
 * 系統：角色組管理系統 (Framework - RoleGroup)
 * 角色：將角色組定義與成員分配持久化至 NBT 檔案。
 * 類型：Storage Implementation
 */
public class NbtRoleGroupStorage implements RoleGroupStorage {

    private static final String FILE_NAME = "rolegroups.dat";
    private static final String GROUPS_KEY = "groups";

    private Path storageFile;
    private final Map<UUID, RoleGroupDefinition> fileMirror = new ConcurrentHashMap<>();

    @Override
    public void initialize(MinecraftServer server) {
        Path rootPath = NbtIoHelper.resolveRootPath(server).toAbsolutePath().normalize();
        this.storageFile = rootPath.resolve(Myulib.MOD_ID).resolve(FILE_NAME);

        try {
            if (!Files.exists(this.storageFile.getParent())) {
                Files.createDirectories(this.storageFile.getParent());
            }
        } catch (Exception e) {
            Myulib.LOGGER.error("Failed to create RoleGroup storage directory: " + e.getMessage());
        }
    }

    private Map<UUID, RoleGroupDefinition> loadAll() {
        fileMirror.clear();
        if (storageFile == null || !Files.exists(storageFile)) {
            return new HashMap<>();
        }

        try {
            CompoundTag root = NbtIoHelper.readRoot(storageFile);
            Tag groupsElement = root.get(GROUPS_KEY);

            if (groupsElement instanceof ListTag groupList) {
                for (int i = 0; i < groupList.size(); i++) {
                    RoleGroupDefinition group = readGroup(groupList.getCompound(i).orElseThrow());
                    fileMirror.put(group.uuid(), group);
                }
            }

            // Legacy Membership Migration
            if (root.contains("memberships")) {
                CompoundTag membershipsCompound = root.getCompound("memberships").orElseThrow();
                boolean migrated = false;
                for (String playerKey : NbtIoHelper.keysOf(membershipsCompound)) {
                    Tag entry = membershipsCompound.get(playerKey);
                    if (entry instanceof ListTag list) {
                        for (int i = 0; i < list.size(); i++) {
                            UUID groupId = parseGroupIdCompat(list.getString(i).orElseThrow());
                            RoleGroupDefinition def = fileMirror.get(groupId);
                            if (def != null) {
                                Set<UUID> updatedMembers = new LinkedHashSet<>(def.members());
                                updatedMembers.add(UUID.fromString(playerKey));
                                RoleGroupDefinition updatedDef = new RoleGroupDefinition(
                                        def.uuid(), def.translationKey(), def.priority(), def.metadata(), updatedMembers
                                );
                                fileMirror.put(groupId, updatedDef);
                                migrated = true;
                            }
                        }
                    }
                }
                if (migrated) saveToFile();
            }

        } catch (Exception e) {
            Myulib.LOGGER.error("Failed to load RoleGroup NBT: " + storageFile, e);
        }

        return new HashMap<>(fileMirror);
    }

    @Override
    public Map<UUID, RoleGroupDefinition> loadGroups() {
        return loadAll();
    }

    @Override
    public Map<UUID, Set<UUID>> loadAssignments() {
        Map<UUID, Set<UUID>> assignments = new LinkedHashMap<>();
        for (RoleGroupDefinition group : loadAll().values()) {
            for (UUID member : group.members()) {
                assignments.computeIfAbsent(member, ignored -> new LinkedHashSet<>()).add(group.uuid());
            }
        }
        return assignments;
    }

    @Override
    public void saveGroup(RoleGroupDefinition group) {
        fileMirror.put(group.uuid(), group);
        saveToFile();
    }

    @Override
    public void deleteGroup(UUID groupId) {
        if (fileMirror.remove(groupId) != null) {
            saveToFile();
        }
    }

    @Override
    public void saveAssignments(UUID playerId, Set<UUID> groupIds) {
        if (playerId == null) return;
        boolean changed = false;
        for (Map.Entry<UUID, RoleGroupDefinition> entry : fileMirror.entrySet()) {
            RoleGroupDefinition current = entry.getValue();
            Set<UUID> members = new LinkedHashSet<>(current.members());
            boolean shouldContain = groupIds != null && groupIds.contains(entry.getKey());
            
            if (shouldContain && members.add(playerId)) changed = true;
            else if (!shouldContain && members.remove(playerId)) changed = true;
            
            if (changed) {
                fileMirror.put(entry.getKey(), new RoleGroupDefinition(
                        current.uuid(),
                        current.translationKey(),
                        current.priority(),
                        current.metadata(),
                        members
                ));
            }
        }
        if (changed) saveToFile();
    }

    private synchronized void saveToFile() {
        if (storageFile == null) return;
        try {
            CompoundTag root = new CompoundTag();
            ListTag groupList = new ListTag();

            for (RoleGroupDefinition group : fileMirror.values()) {
                groupList.add(writeGroup(group));
            }

            root.put(GROUPS_KEY, groupList);
            NbtIoHelper.writeRoot(storageFile, root);
        } catch (Exception e) {
            Myulib.LOGGER.error("Failed to save RoleGroup NBT: " + storageFile, e);
        }
    }

    private static CompoundTag writeGroup(RoleGroupDefinition group) {
        CompoundTag compound = new CompoundTag();
        compound.putString("id", group.uuid().toString());
        compound.putString("displayName", group.translationKey().getString());
        compound.putInt("priority", group.priority());

        CompoundTag metadataCompound = new CompoundTag();
        if (group.metadata() != null) {
            for (Map.Entry<String, String> entry : group.metadata().entrySet()) {
                metadataCompound.putString(entry.getKey(), entry.getValue());
            }
        }
        compound.put("metadata", metadataCompound);

        CompoundTag membersCompound = new CompoundTag();
        if (group.members() != null) {
            for (UUID member : group.members()) {
                membersCompound.putBoolean(member.toString(), true);
            }
        }
        compound.put("members", membersCompound);

        return compound;
    }

    private static RoleGroupDefinition readGroup(CompoundTag compound) {
        String idStr = compound.getString("id").orElse("");
        String displayName = compound.getString("displayName").orElse(idStr);
        int priority = compound.getInt("priority").orElse(0);

        Map<String, String> metadata = new LinkedHashMap<>();
        Tag metadataElement = compound.get("metadata");
        if (metadataElement instanceof CompoundTag metadataCompound) {
            for (String key : NbtIoHelper.keysOf(metadataCompound)) {
                metadata.put(key, metadataCompound.getString(key).orElse(""));
            }
        }

        Set<UUID> members = new LinkedHashSet<>();
        Tag membersElement = compound.get("members");
        if (membersElement instanceof CompoundTag membersCompound) {
            for (String playerKey : NbtIoHelper.keysOf(membersCompound)) {
                try {
                    members.add(UUID.fromString(playerKey));
                } catch (Exception ignored) {}
            }
        }

        return new RoleGroupDefinition(parseGroupIdCompat(idStr), Component.literal(displayName), priority, metadata, members);
    }

    private static UUID parseGroupIdCompat(String rawId) {
        if (rawId == null || rawId.isBlank()) {
            return UUID.nameUUIDFromBytes(("myulib:everyone").getBytes(StandardCharsets.UTF_8));
        }
        try {
            return UUID.fromString(rawId);
        } catch (IllegalArgumentException e) {
            return UUID.nameUUIDFromBytes(rawId.getBytes(StandardCharsets.UTF_8));
        }
    }
}
