package com.myudog.myulib.api.system;
import com.myudog.myulib.api.field.FieldDefinition;
import com.myudog.myulib.api.field.FieldManager;
import com.myudog.myulib.api.permission.PermissionAction;
import com.myudog.myulib.api.permission.PermissionDecision;
import com.myudog.myulib.api.permission.PermissionManager;
import com.myudog.myulib.api.rolegroup.RoleGroupDefinition;
import com.myudog.myulib.api.rolegroup.RoleGroupManager;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.AABB;
import org.junit.jupiter.api.Test;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
final class AccessSystemsTest {
    @Test
    void fieldIdentityAndPermissionFlowWorks() {
        FieldManager.INSTANCE.clear();
        RoleGroupManager.INSTANCE.clear();
        PermissionManager.INSTANCE.clear();
        RoleGroupManager.INSTANCE.install();
        Identifier spawnId = Identifier.fromNamespaceAndPath("mygames", "spawn");
        Identifier overworld = Identifier.fromNamespaceAndPath("minecraft", "overworld");
        Identifier nether = Identifier.fromNamespaceAndPath("minecraft", "the_nether");
        AABB spawnBounds = new AABB(0, 0, 0, 10, 10, 10);
        FieldManager.INSTANCE.register(new FieldDefinition(
                spawnId,
                overworld,
                spawnBounds,
                Map.of("label", "Spawn")
        ));
        FieldManager.INSTANCE.unregister(spawnId);
        FieldManager.INSTANCE.register(new FieldDefinition(
                spawnId,
                nether,
                spawnBounds,
                Map.of("label", "Spawn")
        ));
        assertEquals(nether, FieldManager.INSTANCE.get(spawnId).dimensionId(), "Field dimension should update after re-registration");
        UUID playerId = UUID.fromString("00000000-0000-0000-0000-000000000123");
        Identifier builderId = Identifier.fromNamespaceAndPath("myulib", "builder");
        RoleGroupManager.INSTANCE.register(new RoleGroupDefinition(
                builderId,
                Component.literal("Builder"),
                10,
                Map.of(),
                new HashSet<>()
        ));
        RoleGroupManager.INSTANCE.assign(playerId, builderId);
        List<String> playerGroups = RoleGroupManager.INSTANCE.getSortedGroupIdsOf(playerId);
        assertTrue(playerGroups.contains("builder"), "Player should be assigned to the builder role group");
        assertTrue(playerGroups.contains("everyone"), "Player should always include the everyone role group");
        PermissionAction buildAction = PermissionAction.BLOCK_PLACE;
        PermissionAction mineAction = PermissionAction.BLOCK_BREAK;
        PermissionManager.INSTANCE.global().forGroup("builder").set(buildAction, PermissionDecision.ALLOW);
        PermissionManager.INSTANCE.field(spawnId).forGroup("builder").set(buildAction, PermissionDecision.UNSET);
        PermissionManager.INSTANCE.global().forPlayer(playerId).set(mineAction, PermissionDecision.DENY);
        PermissionDecision finalBuildDecision = PermissionManager.INSTANCE.evaluate(
                playerId,
                playerGroups,
                buildAction,
                spawnId,
                nether
        );
        assertEquals(PermissionDecision.ALLOW, finalBuildDecision,
                "Player should inherit the global BUILD allow when field scope is unset");
        PermissionDecision finalMineDecision = PermissionManager.INSTANCE.evaluate(
                playerId,
                playerGroups,
                mineAction,
                spawnId,
                nether
        );
        assertEquals(PermissionDecision.DENY, finalMineDecision,
                "Player-specific DENY should override lower-priority rules");
    }
}
