package com.myudog.myulib.api.team;
import net.minecraft.resources.Identifier;
import org.junit.jupiter.api.Test;
import java.util.EnumMap;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
final class TeamManagerTest {
    @Test
    void teamIdsCanBeScopedByGameAndRemovedWithGameCleanup() {
        TeamManager.clear();
        Identifier gameId = Identifier.fromNamespaceAndPath("myulib", "respawn_game");
        TeamDefinition team = new TeamDefinition("blue", "Blue Team", TeamColor.BLUE, new EnumMap<>(TeamFlag.class));
        TeamDefinition scoped = TeamManager.register(gameId, team);
        assertEquals("myulib:respawn_game:blue", scoped.id(), "Scoped team id should include the game namespace");
        assertEquals(1, TeamManager.all(gameId).size(), "Exactly one team should be registered for the game");
        assertEquals(scoped, TeamManager.get(scoped.id()), "Registered scoped team should be retrievable");
        UUID playerId = UUID.fromString("00000000-0000-0000-0000-000000000999");
        assertTrue(TeamManager.addPlayer(scoped.id(), playerId), "Adding the player to the team should succeed");
        assertTrue(TeamManager.members(scoped.id()).contains(playerId), "Player should appear in the team member set");
        TeamManager.forEachMember(scoped.id(), member -> assertEquals(playerId, member, "forEachMember should visit the assigned player"));
        assertEquals(1, TeamManager.unregisterGame(gameId).size(), "Game cleanup should remove exactly one scoped team");
        assertNull(TeamManager.get(scoped.id()), "Removed team should no longer be retrievable");
        assertNull(TeamManager.teamOf(playerId), "Removed player should no longer belong to a team");
    }
}
