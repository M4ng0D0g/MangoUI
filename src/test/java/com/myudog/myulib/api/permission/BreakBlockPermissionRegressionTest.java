package com.myudog.myulib.api.permission;

import com.myudog.myulib.api.command.AccessCommandService;
import com.myudog.myulib.api.command.CommandContext;
import com.myudog.myulib.api.command.CommandRegistry;
import com.myudog.myulib.api.command.CommandResult;
import com.myudog.myulib.api.rolegroup.RoleGroupManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class BreakBlockPermissionRegressionTest {

    @BeforeEach
    void reset() {
        PermissionManager.INSTANCE.clear();
        RoleGroupManager.INSTANCE.clear();
        CommandRegistry.clear();
    }

    @Test
    void blockBreakDenyWorksForEvaluateAndCommandPath() {
        UUID playerId = UUID.fromString("00000000-0000-0000-0000-00000000bb01");

        // Mock evaluate path: direct global rule assignment.
        PermissionManager.INSTANCE.global().forGroup("everyone").set(PermissionAction.BLOCK_BREAK, PermissionDecision.DENY);
        PermissionDecision direct = PermissionManager.INSTANCE.evaluate(
                playerId,
                List.of("everyone"),
                PermissionAction.BLOCK_BREAK,
                null,
                null
        );
        assertEquals(PermissionDecision.DENY, direct,
                "Direct evaluate should deny break-block when everyone is DENY");

        // Command path: mirror command writes the same rule via AccessCommandService helpers.
        PermissionManager.INSTANCE.clear();
        AccessCommandService.registerDefaults();
        CommandResult setResult = CommandRegistry.execute(new CommandContext(
                "console",
                "myulib:permission:set-global",
                Map.of(
                        "group", "myulib:everyone",
                        "action", "BLOCK_BREAK",
                        "decision", "DENY"
                )
        ));

        assertTrue(setResult.success(), "Command path should accept permission:set-global");

        PermissionDecision commandConfigured = PermissionManager.INSTANCE.evaluate(
                playerId,
                List.of("everyone"),
                PermissionAction.BLOCK_BREAK,
                null,
                null
        );
        assertEquals(PermissionDecision.DENY, commandConfigured,
                "Command-configured deny should propagate to break-block evaluation");

        // Group normalization regression: myulib prefix input should still match player group path value.
        AccessCommandService.grantGlobalPermission("myulib:test", PermissionAction.BLOCK_BREAK, PermissionDecision.DENY);
        PermissionDecision normalizedGroupDecision = PermissionManager.INSTANCE.evaluate(
                playerId,
                List.of("test", "everyone"),
                PermissionAction.BLOCK_BREAK,
                null,
                null
        );
        assertEquals(PermissionDecision.DENY, normalizedGroupDecision,
                "Namespaced group input should normalize and match path-based player groups");
    }
}

