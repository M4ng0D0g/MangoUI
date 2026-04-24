package com.myudog.myulib.api.team;

import com.myudog.myulib.api.ui.ConfigurationUiBridge;
import net.minecraft.resources.Identifier;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.UnaryOperator;

public final class TeamAdminService {
    private TeamAdminService() {
    }

    public static TeamDefinition create(TeamDefinition team) {
        return TeamManager.INSTANCE.register(team);
    }

    public static TeamDefinition create(Identifier gameId, TeamDefinition team) {
        return TeamManager.INSTANCE.register(gameId, team);
    }

    public static TeamDefinition delete(Identifier teamId) {
        return TeamManager.INSTANCE.unregister(teamId);
    }

    public static List<TeamDefinition> deleteGameTeams(Identifier gameId) {
        return TeamManager.INSTANCE.unregisterGame(gameId);
    }

    public static TeamDefinition update(Identifier teamId, UnaryOperator<TeamDefinition> updater) {
        return TeamManager.INSTANCE.update(teamId, updater);
    }

    public static boolean addPlayer(Identifier teamId, UUID playerId) {
        return TeamManager.INSTANCE.addPlayer(teamId, playerId);
    }

    public static boolean removePlayer(UUID playerId) {
        return TeamManager.INSTANCE.removePlayer(playerId);
    }

    public static Identifier teamOf(UUID playerId) {
        return TeamManager.INSTANCE.teamOf(playerId);
    }

    public static Set<UUID> members(Identifier teamId) {
        return TeamManager.INSTANCE.members(teamId);
    }

    public static void forEachMember(Identifier teamId, java.util.function.Consumer<UUID> action) {
        TeamManager.INSTANCE.forEachMember(teamId, action);
    }

    public static List<TeamDefinition> list() {
        return TeamManager.INSTANCE.all();
    }

    public static List<TeamDefinition> list(Identifier gameId) {
        return TeamManager.INSTANCE.all(gameId);
    }

    public static Map<Identifier, TeamDefinition> snapshot() {
        return TeamManager.INSTANCE.snapshot();
    }

    public static Map<Identifier, TeamDefinition> snapshot(Identifier gameId) {
        return TeamManager.INSTANCE.snapshot(gameId);
    }

    public static void openEditor(Identifier teamId, ConfigurationUiBridge ui) {
        if (ui != null) {
            ui.openTeamEditor(teamId);
        }
    }
}

