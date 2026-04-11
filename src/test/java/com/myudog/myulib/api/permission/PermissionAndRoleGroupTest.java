package com.myudog.myulib.api.permission;
import com.myudog.myulib.api.rolegroup.RoleGroupDefinition;
import com.myudog.myulib.api.rolegroup.RoleGroupManager;
import net.minecraft.resources.Identifier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
final class PermissionAndRoleGroupTest {
    @BeforeEach
    void reset() {
        RoleGroupManager.clear();
        PermissionManager.clear();
        RoleGroupManager.install();
    }
    @Test
    void roleGroupDefinitionsNormalizeAndMembersAreResolved() {
        UUID playerId = UUID.fromString("00000000-0000-0000-0000-000000000777");
        RoleGroupDefinition builder = new RoleGroupDefinition(
                "builders",
                null,
                10,
                Map.of("note", "builder"),
                Set.of(playerId)
        );
        assertEquals("builders", builder.displayName(), "Null display names should fall back to the group id");
        assertTrue(builder.hasMember(playerId), "The constructor should preserve the member set");
        RoleGroupManager.register(builder);
        assertTrue(RoleGroupManager.assign(playerId, "builders"), "The player should be assignable to the builders group");
        assertEquals(List.of("builders", "everyone"), RoleGroupManager.getSortedGroupIdsOf(playerId),
                "Builders should be ordered before everyone");
        assertTrue(RoleGroupManager.getPlayersInGroup("builders").contains(playerId),
                "The player should appear in the builders member list");
    }
    @Test
    void permissionResolutionRespectsFieldDimensionAndGlobalPriority() {
        UUID playerId = UUID.fromString("00000000-0000-0000-0000-000000000778");
        List<String> groups = List.of("builders", "everyone");
        Identifier fieldId = Identifier.fromNamespaceAndPath("tests", "spawn");
        Identifier dimensionId = Identifier.fromNamespaceAndPath("minecraft", "overworld");
        PermissionManager.global().forGroup("builders").set(PermissionAction.BLOCK_PLACE, PermissionDecision.ALLOW);
        PermissionManager.field(fieldId).forGroup("builders").set(PermissionAction.BLOCK_PLACE, PermissionDecision.UNSET);
        PermissionManager.dimension(dimensionId).forGroup("builders").set(PermissionAction.BLOCK_BREAK, PermissionDecision.ALLOW);
        PermissionManager.field(fieldId).forPlayer(playerId).set(PermissionAction.BLOCK_BREAK, PermissionDecision.DENY);
        assertEquals(PermissionDecision.ALLOW,
                PermissionManager.evaluate(playerId, groups, PermissionAction.BLOCK_PLACE, fieldId, dimensionId),
                "Unset field rules should fall back to the global allow");
        assertEquals(PermissionDecision.DENY,
                PermissionManager.evaluate(playerId, groups, PermissionAction.BLOCK_BREAK, fieldId, dimensionId),
                "Field-level player deny should override dimension allow");
    }
}
