package com.myudog.myulib.api.rolegroup;

import com.myudog.myulib.api.permission.PermissionDecision;
import com.myudog.myulib.api.permission.PermissionGrant;
import com.myudog.myulib.api.permission.PermissionLayer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class RoleGroupWorldStorageTest {

    @TempDir
    Path tempDir;

    @Test
    void roleGroupsPersistToBoundWorldRoot() {
        RoleGroupManager.clear();
        RoleGroupManager.bindRoot(tempDir);

        UUID playerId = UUID.fromString("00000000-0000-0000-0000-000000000321");
        RoleGroupDefinition group = new RoleGroupDefinition(
            "builders",
            "Builders",
            10,
            java.util.List.of(new PermissionGrant("group:build", PermissionLayer.USER, "build", PermissionDecision.ALLOW, 50)),
            Map.of("note", "persistent")
        );

        RoleGroupManager.register(group);
        assertTrue(RoleGroupManager.assign(playerId, "builders"));

        Path storageFile = tempDir.resolve("myulib").resolve("rolegroups.dat");
        assertTrue(Files.exists(storageFile));

        RoleGroupManager.clear();
        RoleGroupManager.bindRoot(tempDir);

        assertEquals("Builders", RoleGroupManager.get("builders").displayName());
        assertTrue(RoleGroupManager.groupIdsOf(playerId).contains("builders"));
    }
}
