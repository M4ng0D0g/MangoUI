package com.myudog.myulib.api.command;

import com.myudog.myulib.api.rolegroup.RoleGroupManager;
import com.myudog.myulib.api.ui.ConfigurationUiRegistry;
import com.myudog.myulib.api.ui.NoopConfigurationUiBridge;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class AccessCommandServiceTest {

    @TempDir
    Path tempDir;

    @Test
    void rolegroupCommandsAreRegisteredAndExecuteCrud() {
        RoleGroupManager.clear();
        RoleGroupManager.bindRoot(tempDir);
        ConfigurationUiRegistry.setBridge(NoopConfigurationUiBridge.INSTANCE);
        CommandRegistry.clear();
        AccessCommandService.registerDefaults();

        assertTrue(CommandRegistry.snapshot().containsKey("rolegroup.create"));
        assertTrue(CommandRegistry.snapshot().containsKey("rolegroup.update"));
        assertTrue(CommandRegistry.snapshot().containsKey("rolegroup.delete"));
        assertTrue(CommandRegistry.snapshot().containsKey("rolegroup.get"));
        assertTrue(CommandRegistry.snapshot().containsKey("rolegroup.list"));

        CommandResult created = CommandRegistry.execute(new CommandContext("console", "rolegroup.create", Map.of(
            "id", "builders",
            "name", "Builders",
            "priority", "7"
        )));
        assertTrue(created.success());
        assertEquals("Builders", RoleGroupManager.get("builders").displayName());

        CommandResult updated = CommandRegistry.execute(new CommandContext("console", "rolegroup.update", Map.of(
            "id", "builders",
            "name", "Master Builders",
            "priority", "9"
        )));
        assertTrue(updated.success());
        assertEquals("Master Builders", RoleGroupManager.get("builders").displayName());
        assertEquals(9, RoleGroupManager.get("builders").priority());

        CommandResult listed = CommandRegistry.execute(new CommandContext("console", "rolegroup.list", Map.of()));
        assertTrue(listed.success());
        assertTrue(listed.message().contains("builders"));

        CommandResult deleted = CommandRegistry.execute(new CommandContext("console", "rolegroup.delete", Map.of("id", "builders")));
        assertTrue(deleted.success());
        assertNull(RoleGroupManager.get("builders"));
    }
}
