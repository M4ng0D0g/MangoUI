package com.myudog.myulib.api.game.core;

import com.myudog.myulib.api.ecs.EcsContainer;
import com.myudog.myulib.api.field.FieldDefinition;
import com.myudog.myulib.api.field.FieldManager;
import com.myudog.myulib.api.game.object.IGameObject;
import com.myudog.myulib.api.rolegroup.RoleGroupDefinition;
import com.myudog.myulib.api.rolegroup.RoleGroupManager;
import com.myudog.myulib.api.team.TeamDefinition;
import com.myudog.myulib.api.team.TeamManager;
import com.myudog.myulib.api.timer.TimerDefinition;
import com.myudog.myulib.api.timer.TimerManager;
import net.minecraft.resources.Identifier;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public abstract class GameData {

    private Identifier id; // 由 GameManager 分配的 session ID

    private final Map<UUID, Integer> participantToEntity = new ConcurrentHashMap<>();
    private final Map<Identifier, IGameObject> runtimeObjects = new ConcurrentHashMap<>();
    private final Set<Identifier> registeredFieldIds = ConcurrentHashMap.newKeySet();
    private final Set<Identifier> registeredRoleGroupIds = ConcurrentHashMap.newKeySet();
    private final Set<Identifier> registeredTeamIds = ConcurrentHashMap.newKeySet();
    private final Set<Identifier> registeredTimerIds = ConcurrentHashMap.newKeySet();
    private final Map<Identifier, Set<UUID>> teamMembers = new ConcurrentHashMap<>();
    private final EcsContainer ecsContainer;

    protected GameData() {
        this.ecsContainer = new EcsContainer();
    }

    public void setupId(Identifier id) { this.id = id; }
    public Identifier getId() { return this.id; }

    // --- 參與者管理 (會話隔離) ---

    public Integer getParticipantEntity(UUID uuid) { return participantToEntity.get(uuid); }
    public void addParticipant(UUID uuid, int entityId) { participantToEntity.put(uuid, entityId); }
    public void removeParticipant(UUID uuid) { participantToEntity.remove(uuid); }

    public int bindParticipant(UUID uuid) {
        if (uuid == null) {
            throw new IllegalArgumentException("uuid 不得為空");
        }
        int entityId = ecsContainer.createEntity();
        participantToEntity.put(uuid, entityId);
        return entityId;
    }

    public void addRuntimeObject(Identifier id, IGameObject obj) {
        this.runtimeObjects.put(id, obj);
    }

    public Optional<IGameObject> getObject(Identifier id) {
        return Optional.ofNullable(runtimeObjects.get(id));
    }

    public Collection<IGameObject> getRuntimeObjects() {
        return Collections.unmodifiableCollection(runtimeObjects.values());
    }

    public Map<Identifier, Set<UUID>> teamMembersSnapshot() {
        Map<Identifier, Set<UUID>> snapshot = new LinkedHashMap<>();
        for (Map.Entry<Identifier, Set<UUID>> entry : teamMembers.entrySet()) {
            snapshot.put(entry.getKey(), Set.copyOf(entry.getValue()));
        }
        return Collections.unmodifiableMap(snapshot);
    }

    public Optional<Identifier> teamOf(UUID playerId) {
        if (playerId == null) {
            return Optional.empty();
        }
        for (Map.Entry<Identifier, Set<UUID>> entry : teamMembers.entrySet()) {
            if (entry.getValue().contains(playerId)) {
                return Optional.of(entry.getKey());
            }
        }
        return Optional.empty();
    }

    public Set<UUID> membersOf(Identifier teamId) {
        if (teamId == null) {
            return Set.of();
        }
        return Set.copyOf(teamMembers.getOrDefault(teamId, Set.of()));
    }

    public boolean containsPlayer(UUID playerId) {
        return teamOf(playerId).isPresent();
    }

    public int countAllPlayers() {
        Set<UUID> all = new HashSet<>();
        for (Set<UUID> members : teamMembers.values()) {
            all.addAll(members);
        }
        return all.size();
    }

    public int countActivePlayers() {
        Set<UUID> all = new HashSet<>();
        for (Map.Entry<Identifier, Set<UUID>> entry : teamMembers.entrySet()) {
            if (GameConfig.SPECTATOR_TEAM_ID.equals(entry.getKey())) {
                continue;
            }
            all.addAll(entry.getValue());
        }
        return all.size();
    }

    public boolean isTeamRegistered(Identifier teamId) {
        return teamMembers.containsKey(teamId);
    }

    public void removePlayerFromTeams(UUID playerId) {
        if (playerId == null) {
            return;
        }
        for (Set<UUID> members : teamMembers.values()) {
            members.remove(playerId);
        }
    }

    public boolean canJoinTeam(Identifier teamId, UUID playerId, GameConfig config) {
        if (teamId == null || !teamMembers.containsKey(teamId)) {
            return false;
        }

        TeamDefinition definition = teamDefinitionOf(teamId, config);
        if (definition == null || definition.playerLimit() <= 0) {
            return true;
        }

        int members = teamMembers.get(teamId).size();
        Optional<Identifier> current = teamOf(playerId);
        if (current.isPresent() && current.get().equals(teamId)) {
            return true;
        }
        return members < definition.playerLimit();
    }

    public boolean movePlayerToTeam(UUID playerId, Identifier teamId, GameConfig config) {
        if (playerId == null || teamId == null || !teamMembers.containsKey(teamId)) {
            return false;
        }
        if (!canJoinTeam(teamId, playerId, config)) {
            return false;
        }

        removePlayerFromTeams(playerId);
        teamMembers.computeIfAbsent(teamId, ignored -> ConcurrentHashMap.newKeySet()).add(playerId);
        return true;
    }

    public void removeTeam(Identifier teamId) {
        if (teamId == null) {
            return;
        }
        if (GameConfig.SPECTATOR_TEAM_ID.equals(teamId)) {
            throw new IllegalStateException("觀戰隊伍不可刪除");
        }
        teamMembers.remove(teamId);
    }

    public final void init(GameConfig config) {
        if (config == null) {
            return;
        }

        Set<Identifier> stagedFields = new LinkedHashSet<>();
        Set<Identifier> stagedRoles = new LinkedHashSet<>();
        Set<Identifier> stagedTeams = new LinkedHashSet<>();
        Set<Identifier> stagedTimers = new LinkedHashSet<>();

        teamMembers.clear();
        for (Identifier teamId : config.teams().values()) {
            teamMembers.put(teamId, ConcurrentHashMap.newKeySet());
        }
        teamMembers.putIfAbsent(GameConfig.SPECTATOR_TEAM_ID, ConcurrentHashMap.newKeySet());

        try {
            for (FieldDefinition definition : config.fieldDefinitions()) {
                if (!FieldManager.INSTANCE.validate(definition)) {
                    throw new IllegalStateException("FieldDefinition 驗證失敗: " + (definition == null ? "null" : definition.id()));
                }
                FieldManager.INSTANCE.register(definition);
                stagedFields.add(definition.id());
                registeredFieldIds.add(definition.id());
            }
            for (RoleGroupDefinition definition : config.roleGroupDefinitions()) {
                if (!RoleGroupManager.INSTANCE.validate(definition)) {
                    throw new IllegalStateException("RoleGroupDefinition 驗證失敗: " + (definition == null ? "null" : definition.id()));
                }
                RoleGroupManager.INSTANCE.register(definition);
                stagedRoles.add(definition.id());
                registeredRoleGroupIds.add(definition.id());
            }
            for (TeamDefinition definition : config.teamDefinitions()) {
                if (!TeamManager.INSTANCE.validate(definition)) {
                    throw new IllegalStateException("TeamDefinition 驗證失敗: " + (definition == null ? "null" : definition.id()));
                }
                TeamManager.INSTANCE.register(definition);
                stagedTeams.add(definition.id());
                registeredTeamIds.add(definition.id());
            }
            for (TimerDefinition definition : config.timerDefinitions()) {
                if (!TimerManager.INSTANCE.validate(definition)) {
                    throw new IllegalStateException("TimerDefinition 驗證失敗: " + (definition == null ? "null" : definition.id));
                }
                TimerManager.INSTANCE.register(definition);
                stagedTimers.add(definition.id);
                registeredTimerIds.add(definition.id);
            }
        } catch (Exception ex) {
            for (Identifier id : Set.copyOf(stagedTimers)) {
                TimerManager.INSTANCE.unregister(id);
                registeredTimerIds.remove(id);
            }
            for (Identifier id : Set.copyOf(stagedTeams)) {
                TeamManager.INSTANCE.unregister(id);
                registeredTeamIds.remove(id);
            }
            for (Identifier id : Set.copyOf(stagedRoles)) {
                RoleGroupManager.INSTANCE.delete(id);
                registeredRoleGroupIds.remove(id);
            }
            for (Identifier id : Set.copyOf(stagedFields)) {
                FieldManager.INSTANCE.unregister(id);
                registeredFieldIds.remove(id);
            }
            teamMembers.clear();
            throw new RuntimeException("GameData 初始化回滾: " + ex.getMessage(), ex);
        }
    }


    public void reset(GameInstance<?, ?, ?> instance) {
        participantToEntity.clear();
        teamMembers.clear();

        runtimeObjects.values().forEach(obj -> obj.destroy(instance));
        runtimeObjects.clear();

        for (Identifier id : Set.copyOf(registeredFieldIds)) {
            FieldManager.INSTANCE.unregister(id);
            registeredFieldIds.remove(id);
        }
        for (Identifier id : Set.copyOf(registeredRoleGroupIds)) {
            RoleGroupManager.INSTANCE.delete(id);
            registeredRoleGroupIds.remove(id);
        }
        for (Identifier id : Set.copyOf(registeredTeamIds)) {
            TeamManager.INSTANCE.unregister(id);
            registeredTeamIds.remove(id);
        }
        for (Identifier id : Set.copyOf(registeredTimerIds)) {
            TimerManager.INSTANCE.unregister(id);
            registeredTimerIds.remove(id);
        }
    }

    public EcsContainer getEcsContainer() {
        return ecsContainer;
    }

    private TeamDefinition teamDefinitionOf(Identifier teamId, GameConfig config) {
        if (config == null) {
            return null;
        }
        for (TeamDefinition team : config.teamDefinitions()) {
            if (team.id().equals(teamId)) {
                return team;
            }
        }
        return null;
    }
}