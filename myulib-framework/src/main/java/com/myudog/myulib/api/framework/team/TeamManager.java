package com.myudog.myulib.api.framework.team;

import com.myudog.myulib.api.core.ecs.EcsContainer;
import net.minecraft.resources.Identifier;
import java.util.*;
import java.util.function.UnaryOperator;
import java.util.function.Consumer;

public final class TeamManager {

    public static final TeamManager INSTANCE = new TeamManager();
    
    /** 全域 ECS 容器，用於相容舊有的全域隊伍。 */
    public final EcsContainer GLOBAL_CONTAINER = new EcsContainer();

    private TeamManager() {}

    public int createInstance(EcsContainer container, TeamDefinition definition) {
        int id = container.createEntity(definition.uuid());
        container.addComponent(id, TeamInstance.class, new TeamInstance(definition));
        return id;
    }

    public void addParticipant(EcsContainer container, int teamEntityId, UUID playerUuid) {
        TeamInstance team = container.getComponent(teamEntityId, TeamInstance.class);
        if (team != null) {
            team.participants.add(playerUuid);
        }
    }

    public void removeParticipant(EcsContainer container, int teamEntityId, UUID playerUuid) {
        TeamInstance team = container.getComponent(teamEntityId, TeamInstance.class);
        if (team != null) {
            team.participants.remove(playerUuid);
        }
    }

    public boolean hasInstance(EcsContainer container, int teamEntityId) {
        return container.hasEntity(teamEntityId) && container.getComponent(teamEntityId, TeamInstance.class) != null;
    }

    // ==========================================
    // 舊版相容方法 (Legacy API)
    // ==========================================

    public TeamDefinition register(TeamDefinition definition) {
        createInstance(GLOBAL_CONTAINER, definition);
        return definition;
    }
    
    public TeamDefinition register(Identifier gameId, TeamDefinition definition) {
        return register(definition);
    }

    public TeamDefinition unregister(UUID teamUuid) {
        TeamDefinition def = get(teamUuid);
        Integer id = GLOBAL_CONTAINER.getEntityId(teamUuid);
        if (id != null) {
            GLOBAL_CONTAINER.destroyEntity(id);
        }
        return def;
    }
    
    public TeamDefinition unregister(Identifier teamId) {
        return unregister(UUID.nameUUIDFromBytes(teamId.toString().getBytes()));
    }
    
    public List<TeamDefinition> unregisterGame(Identifier gameId) {
        return List.of(); // 暫不支援
    }

    public boolean hasTeam(UUID teamUuid) {
        return GLOBAL_CONTAINER.getEntityId(teamUuid) != null;
    }

    public TeamDefinition get(UUID teamUuid) {
        Integer id = GLOBAL_CONTAINER.getEntityId(teamUuid);
        if (id != null) {
            TeamInstance inst = GLOBAL_CONTAINER.getComponent(id, TeamInstance.class);
            return inst != null ? inst.definition : null;
        }
        return null;
    }

    public TeamDefinition update(UUID teamUuid, UnaryOperator<TeamDefinition> updater) {
        Integer id = GLOBAL_CONTAINER.getEntityId(teamUuid);
        if (id != null) {
            TeamInstance inst = GLOBAL_CONTAINER.getComponent(id, TeamInstance.class);
            if (inst != null) {
                TeamDefinition next = updater.apply(inst.definition);
                GLOBAL_CONTAINER.removeComponent(id, TeamInstance.class);
                GLOBAL_CONTAINER.addComponent(id, TeamInstance.class, new TeamInstance(next));
                return next;
            }
        }
        return null;
    }
    
    public TeamDefinition update(Identifier teamId, UnaryOperator<TeamDefinition> updater) {
        return update(UUID.nameUUIDFromBytes(teamId.toString().getBytes()), updater);
    }

    public boolean addPlayer(Identifier teamId, UUID playerUuid) {
        Integer id = GLOBAL_CONTAINER.getEntityId(UUID.nameUUIDFromBytes(teamId.toString().getBytes()));
        if (id != null) {
            addParticipant(GLOBAL_CONTAINER, id, playerUuid);
            return true;
        }
        return false;
    }

    public boolean removePlayer(UUID playerUuid) {
        final boolean[] removed = {false};
        GLOBAL_CONTAINER.forAll(TeamInstance.class, (id, inst) -> {
            if (inst.participants.remove(playerUuid)) removed[0] = true;
        });
        return removed[0];
    }

    public UUID teamOf(UUID playerUuid) {
        UUID[] found = {null};
        GLOBAL_CONTAINER.forAll(TeamInstance.class, (id, inst) -> {
            if (inst.participants.contains(playerUuid)) found[0] = inst.definition.uuid();
        });
        return found[0];
    }

    public Set<UUID> members(UUID teamUuid) {
        Integer id = GLOBAL_CONTAINER.getEntityId(teamUuid);
        if (id != null) {
            TeamInstance inst = GLOBAL_CONTAINER.getComponent(id, TeamInstance.class);
            return inst != null ? Set.copyOf(inst.participants) : Set.of();
        }
        return Set.of();
    }

    public void forEachMember(UUID teamUuid, Consumer<UUID> action) {
        members(teamUuid).forEach(action);
    }

    public List<TeamDefinition> all() {
        List<TeamDefinition> list = new ArrayList<>();
        GLOBAL_CONTAINER.forAll(TeamInstance.class, (id, inst) -> list.add(inst.definition));
        return list;
    }
    
    public List<TeamDefinition> all(Identifier gameId) {
        return all();
    }

    public Map<UUID, TeamDefinition> snapshot() {
        Map<UUID, TeamDefinition> map = new HashMap<>();
        GLOBAL_CONTAINER.forAll(TeamInstance.class, (id, inst) -> map.put(inst.definition.uuid(), inst.definition));
        return map;
    }
}
