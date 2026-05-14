package com.myudog.myulib.api.framework.game.features;

import com.myudog.myulib.api.core.ecs.EcsContainer;
import com.myudog.myulib.api.framework.game.core.GameInstance;
import com.myudog.myulib.api.framework.team.TeamColor;
import com.myudog.myulib.api.framework.team.TeamDefinition;
import com.myudog.myulib.api.framework.team.TeamInstance;
import com.myudog.myulib.api.framework.team.TeamManager;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class TeamFeatureImpl implements TeamFeature {

    private final EcsContainer container;
    private final UUID spectatorTeamUuid;
    private final Map<UUID, Integer> teamEntities = new ConcurrentHashMap<>();

    public TeamFeatureImpl(EcsContainer container, UUID spectatorTeamUuid) {
        this.container = container;
        this.spectatorTeamUuid = spectatorTeamUuid;
    }

    @Override
    public @Nullable UUID getParticipantTeam(@NotNull UUID playerId) {
        for (Map.Entry<UUID, Integer> entry : teamEntities.entrySet()) {
            TeamInstance team = container.getComponent(entry.getValue(), TeamInstance.class);
            if (team != null && team.participants.contains(playerId)) {
                return entry.getKey();
            }
        }
        return null;
    }

    @Override
    public boolean containsParticipant(UUID playerId) {
        return getParticipantTeam(playerId) != null;
    }

    @Override
    public Set<UUID> participantsOf(@Nullable UUID teamId) {
        if (teamId == null) {
            Set<UUID> all = new HashSet<>();
            teamEntities.values().forEach(id -> {
                TeamInstance team = container.getComponent(id, TeamInstance.class);
                if (team != null) all.addAll(team.participants);
            });
            return all;
        }

        Integer entityId = teamEntities.get(teamId);
        if (entityId == null) return Set.of();
        TeamInstance team = container.getComponent(entityId, TeamInstance.class);
        return team != null ? Set.copyOf(team.participants) : Set.of();
    }

    @Override
    public UUID teamOf(@NotNull UUID playerId) {
        return getParticipantTeam(playerId);
    }

    @Override
    public int countAllParticipant(@Nullable UUID teamId) {
        return participantsOf(teamId).size();
    }

    @Override
    public int countActiveParticipant() {
        int count = 0;
        for (Map.Entry<UUID, Integer> entry : teamEntities.entrySet()) {
            if (entry.getKey().equals(spectatorTeamUuid)) continue;
            TeamInstance team = container.getComponent(entry.getValue(), TeamInstance.class);
            if (team != null) count += team.participants.size();
        }
        return count;
    }

    @Override
    public boolean canJoinTeam(@Nullable UUID teamId, @NotNull UUID playerId) {
        if (teamId == null) teamId = spectatorTeamUuid;
        return teamEntities.containsKey(teamId);
    }

    @Override
    public boolean moveParticipantToTeam(@Nullable UUID teamId, @NotNull UUID playerId) {
        if (teamId == null) teamId = spectatorTeamUuid;
        if (!canJoinTeam(teamId, playerId)) return false;

        removeParticipantFromTeams(playerId);
        
        Integer entityId = teamEntities.get(teamId);
        if (entityId != null) {
            TeamManager.INSTANCE.addParticipant(container, entityId, playerId);
            return true;
        }
        return false;
    }

    @Override
    public void removeParticipantFromTeams(@NotNull UUID playerId) {
        teamEntities.values().forEach(id -> TeamManager.INSTANCE.removeParticipant(container, id, playerId));
    }

    @Override
    public void clean(GameInstance<?, ?, ?> instance) {
        teamEntities.values().forEach(container::destroyEntity);
        teamEntities.clear();
    }
    
    public void registerTeam(TeamDefinition definition) {
        int entityId = TeamManager.INSTANCE.createInstance(container, definition);
        teamEntities.put(definition.uuid(), entityId);
    }
}
