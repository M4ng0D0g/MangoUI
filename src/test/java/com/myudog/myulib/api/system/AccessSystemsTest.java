package com.myudog.myulib.api.system;

import com.myudog.myulib.api.field.FieldAdminService;
import com.myudog.myulib.api.field.FieldBounds;
import com.myudog.myulib.api.field.FieldDefinition;
import com.myudog.myulib.api.field.FieldManager;
import com.myudog.myulib.api.field.FieldRole;
import com.myudog.myulib.api.identity.IdentityAdminService;
import com.myudog.myulib.api.identity.IdentityGroupDefinition;
import com.myudog.myulib.api.identity.IdentityManager;
import com.myudog.myulib.api.permission.PermissionAdminService;
import com.myudog.myulib.api.permission.PermissionContext;
import com.myudog.myulib.api.permission.PermissionDecision;
import com.myudog.myulib.api.permission.PermissionGrant;
import com.myudog.myulib.api.permission.PermissionLayer;
import com.myudog.myulib.api.permission.PermissionManager;
import com.myudog.myulib.api.team.TeamManager;
import java.util.Map;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;

final class AccessSystemsTest {

    @Test
    void fieldIdentityAndPermissionFlowWorks() {
        FieldManager.clear();
        IdentityManager.clear();
        TeamManager.clear();
        PermissionManager.clear();

        FieldAdminService.create(new FieldDefinition(
                "spawn",
                "server",
                "minecraft:overworld",
                new FieldBounds(0, 0, 0, 10, 10, 10),
                FieldRole.MAIN,
                Map.of("label", "Spawn")
        ));
        FieldAdminService.update("spawn", field -> field.withDimensionId("minecraft:the_nether"));

        UUID playerId = UUID.fromString("00000000-0000-0000-0000-000000000123");
        IdentityAdminService.create(new IdentityGroupDefinition(
                "builder",
                "Builder",
                10,
                java.util.List.of(new PermissionGrant("group:build", PermissionLayer.USER, "build", PermissionDecision.ALLOW, 50)),
                Map.of()
        ));
        IdentityAdminService.assign(playerId, "builder");

        PermissionAdminService.grantField("spawn", new PermissionGrant("field:spawn-unset", PermissionLayer.FIELD, "build", PermissionDecision.UNSET, 0));
        PermissionAdminService.grantUser(playerId, new PermissionGrant("user:mine-deny", PermissionLayer.USER, "mine", PermissionDecision.DENY, 100));

        assertEquals("minecraft:the_nether", FieldAdminService.get("spawn").dimensionId());
        assertEquals(1, IdentityAdminService.groupsOf(playerId).size());
        assertEquals(PermissionDecision.ALLOW, PermissionAdminService.evaluate(new PermissionContext(playerId, "build", "minecraft:the_nether", "spawn", Map.of())).decision());
        assertEquals(PermissionDecision.DENY, PermissionAdminService.evaluate(new PermissionContext(playerId, "mine", "minecraft:the_nether", "spawn", Map.of())).decision());
    }
}
