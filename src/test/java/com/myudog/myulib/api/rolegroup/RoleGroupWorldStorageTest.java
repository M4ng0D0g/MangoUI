package com.myudog.myulib.api.rolegroup;

import com.myudog.myulib.api.permission.PermissionDecision;
import com.myudog.myulib.api.permission.ScopeLayer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class RoleGroupWorldStorageTest {

//    @TempDir
//    Path tempDir;
//
//    @Test
//    void roleGroupsPersistToBoundWorldRoot() {
//        RoleGroupManager.INSTANCE.clear();
//        RoleGroupManager.INSTANCE.bindRoot(tempDir);
//
//        UUID playerId = UUID.fromString("00000000-0000-0000-0000-000000000321");
//        RoleGroupDefinition group = new RoleGroupDefinition(
//            "builders",
//            "Builders",
//            10,
//            java.util.List.of(new PermissionGrant("group:build", ScopeLayer.USER, "build", PermissionDecision.ALLOW, 50)),
//            Map.of("note", "persistent")
//        );
//
//        RoleGroupManager.INSTANCE.register(group);
//        assertTrue(RoleGroupManager.INSTANCE.assign(playerId, "builders"));
//
//        Path storageFile = tempDir.resolve("myulib").resolve("rolegroups.dat");
//        assertTrue(Files.exists(storageFile));
//
//        RoleGroupManager.INSTANCE.clear();
//        RoleGroupManager.INSTANCE.bindRoot(tempDir);
//
//        assertEquals("Builders", RoleGroupManager.INSTANCE.get("builders").displayName());
//        assertTrue(RoleGroupManager.INSTANCE.groupIdsOf(playerId).contains("builders"));
//    }
}
