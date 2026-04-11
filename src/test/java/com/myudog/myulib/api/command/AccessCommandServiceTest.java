package com.myudog.myulib.api.command;
import com.myudog.myulib.api.field.FieldDefinition;
import com.myudog.myulib.api.field.FieldManager;
import com.myudog.myulib.api.permission.PermissionAction;
import com.myudog.myulib.api.permission.PermissionDecision;
import com.myudog.myulib.api.permission.PermissionManager;
import com.myudog.myulib.api.rolegroup.RoleGroupManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Path;
import java.util.Map;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.AABB;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
final class AccessCommandServiceTest {
    @TempDir
    Path tempDir;
    @Test
    void accessServiceHelpersCreateAndPersistAccessData() {
        RoleGroupManager.clear();
        PermissionManager.clear();
        FieldManager.clear();
        RoleGroupManager.bindRoot(tempDir);
        assertDoesNotThrow(AccessCommandService::registerDefaults,
                "registerDefaults should only attach the command callback");
        AccessCommandService.createRoleGroup("builders", "Builders", 7);
        assertEquals("Builders", RoleGroupManager.get("builders").displayName(),
                "createRoleGroup should register the new group");
        AccessCommandService.grantGlobalPermission("builders", PermissionAction.BLOCK_PLACE, PermissionDecision.ALLOW);
        assertEquals(PermissionDecision.ALLOW,
                PermissionManager.global().forGroup("builders").get(PermissionAction.BLOCK_PLACE),
                "grantGlobalPermission should update the global permission table");
        Identifier fieldId = Identifier.fromNamespaceAndPath("tests", "spawn");
        FieldDefinition field = new FieldDefinition(
                fieldId,
                Identifier.fromNamespaceAndPath("minecraft", "overworld"),
                new AABB(0, 0, 0, 10, 10, 10),
                Map.of("label", "Spawn")
        );
        AccessCommandService.createField(field);
        assertEquals(field, FieldManager.get(fieldId), "createField should register the field");
        assertTrue(AccessCommandService.listRoleGroups().stream().anyMatch(group -> group.id().equals("builders")),
                "listRoleGroups should include the created group");
        AccessCommandService.deleteField(fieldId);
        AccessCommandService.deleteRoleGroup("builders");
        assertNull(FieldManager.get(fieldId), "deleteField should remove the field");
        assertNull(RoleGroupManager.get("builders"), "deleteRoleGroup should remove the role group");
    }
}
