   package com.myudog.myulib.api.team;

import net.minecraft.resources.Identifier;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class TeamManagerTest {

    @Test
    void teamIdsCanBeScopedByGameAndRemovedWithGameCleanup() {
        TeamManager.clear();

        Identifier gameId = Identifier.fromNamespaceAndPath("myulib", "respawn_game");
        TeamDefinition team = new TeamDefinition("blue", "Blue Team", "blue", Map.of("mode", "arena"));
        TeamDefinition scoped = TeamManager.register(gameId, team);

        assertEquals("myulib:respawn_game:blue", scoped.id());
        assertEquals(1, TeamManager.all(gameId).size());
        assertEquals(scoped, TeamManager.get(scoped.id()));

        UUID playerId = UUID.fromString("00000000-0000-0000-0000-000000000999");
        assertTrue(TeamManager.addPlayer(scoped.id(), playerId));
        assertTrue(TeamManager.members(scoped.id()).contains(playerId));

        TeamManager.forEachMember(scoped.id(), member -> assertEquals(playerId, member));

        assertEquals(1, TeamManager.unregisterGame(gameId).size());
        assertNull(TeamManager.get(scoped.id()));
        assertNull(TeamManager.teamOf(playerId));
    }
}

