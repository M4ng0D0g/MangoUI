package com.myudog.myulib.api.permission;
import com.myudog.myulib.api.rolegroup.RoleGroupDefinition;
import com.myudog.myulib.api.rolegroup.RoleGroupManager;
import net.minecraft.network.chat.Component;
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
        RoleGroupManager.INSTANCE.clear();
        PermissionManager.INSTANCE.clear();
        RoleGroupManager.INSTANCE.install();
    }
    @Test
    void roleGroupDefinitionsNormalizeAndMembersAreResolved() {
        UUID playerId = UUID.fromString("00000000-0000-0000-0000-000000000777");
        Identifier builderId = Identifier.fromNamespaceAndPath("myulib", "builders");
        RoleGroupDefinition builder = new RoleGroupDefinition(
                builderId,
                Component.literal("Builders"),
                10,
                Map.of("note", "builder"),
                Set.of(playerId)
        );
        assertEquals("Builders", builder.translationKey().getString(), "Display component should be preserved");
        assertTrue(builder.hasMember(playerId), "The constructor should preserve the member set");
        RoleGroupManager.INSTANCE.register(builder);
        assertTrue(RoleGroupManager.INSTANCE.assign(playerId, builderId), "The player should be assignable to the builders group");
        assertEquals(List.of("builders", "everyone"), RoleGroupManager.INSTANCE.getSortedGroupIdsOf(playerId),
                "Builders should be ordered before everyone");
        assertTrue(RoleGroupManager.INSTANCE.getPlayersInGroup("builders").contains(playerId),
                "The player should appear in the builders member list");
    }
    @Test
    void permissionResolutionRespectsFieldDimensionAndGlobalPriority() {
        UUID playerId = UUID.fromString("00000000-0000-0000-0000-000000000778");
        List<String> groups = List.of("builders", "everyone");
        Identifier fieldId = Identifier.fromNamespaceAndPath("tests", "spawn");
        Identifier dimensionId = Identifier.fromNamespaceAndPath("minecraft", "overworld");
        PermissionManager.INSTANCE.global().forGroup("builders").set(PermissionAction.BLOCK_PLACE, PermissionDecision.ALLOW);
        PermissionManager.INSTANCE.field(fieldId).forGroup("builders").set(PermissionAction.BLOCK_PLACE, PermissionDecision.UNSET);
        PermissionManager.INSTANCE.dimension(dimensionId).forGroup("builders").set(PermissionAction.BLOCK_BREAK, PermissionDecision.ALLOW);
        PermissionManager.INSTANCE.field(fieldId).forPlayer(playerId).set(PermissionAction.BLOCK_BREAK, PermissionDecision.DENY);
        assertEquals(PermissionDecision.ALLOW,
                PermissionManager.INSTANCE.evaluate(playerId, groups, PermissionAction.BLOCK_PLACE, fieldId, dimensionId),
                "Unset field rules should fall back to the global allow");
        assertEquals(PermissionDecision.DENY,
                PermissionManager.INSTANCE.evaluate(playerId, groups, PermissionAction.BLOCK_BREAK, fieldId, dimensionId),
                "Field-level player deny should override dimension allow");
    }
}
