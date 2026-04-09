package com.myudog.myulib.api.game.logic.facts;

import com.myudog.myulib.api.game.instance.GameInstance;
import com.myudog.myulib.api.team.TeamManager;
import net.minecraft.resources.Identifier;

public interface LogicFactsResolver {
    LogicFactsResolver DEFAULT = new LogicFactsResolver() {
    };

    default int playerCount(GameInstance<?> instance) {
        return instance == null ? 0 : TeamManager.all(instance.getDefinition().getId()).stream().mapToInt(team -> TeamManager.members(team.id()).size()).sum();
    }

    default int playerCountInTeam(GameInstance<?> instance, Identifier teamId) {
        return instance == null ? 0 : TeamManager.members(teamId.toString()).size();
    }

    default boolean isOnTeam(GameInstance<?> instance, Identifier playerId, Identifier teamId) {
        java.util.UUID uuid = toUuid(playerId);
        return instance != null && uuid != null && teamId.toString().equals(TeamManager.teamOf(uuid));
    }

    default boolean isRedTeam(GameInstance<?> instance, Identifier playerId) {
        if (instance == null) {
            return false;
        }
        java.util.UUID uuid = toUuid(playerId);
        if (uuid == null) {
            return false;
        }
        String teamId = TeamManager.teamOf(uuid);
        if (teamId == null) {
            return false;
        }
        com.myudog.myulib.api.team.TeamDefinition definition = TeamManager.get(teamId);
        return definition != null && "red".equalsIgnoreCase(definition.color());
    }

    default int gameTimeTicks(GameInstance<?> instance) {
        return instance == null ? 0 : (int) instance.getTickCount();
    }

    default boolean hasSpecialObject(GameInstance<?> instance, Identifier objectId) {
        return instance != null && instance.hasSpecialObject(objectId);
    }

    private static java.util.UUID toUuid(Identifier value) {
        if (value == null) {
            return null;
        }
        try {
            return java.util.UUID.fromString(value.toString());
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }
}
