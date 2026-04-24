package com.myudog.myulib.api.field;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
final class FieldManagerTest {
    @BeforeEach
    void reset() {
        FieldManager.INSTANCE.clear();
    }
    @Test
    void registerFindAndOverlapValidationWork() {
        Identifier fieldId = Identifier.fromNamespaceAndPath("tests", "spawn");
        Identifier dimensionId = Identifier.fromNamespaceAndPath("minecraft", "overworld");
        FieldDefinition field = new FieldDefinition(
                fieldId,
                dimensionId,
                new AABB(0, 0, 0, 10, 10, 10),
                Map.of("label", "Spawn")
        );
        FieldManager.INSTANCE.register(field);
        assertEquals(field, FieldManager.INSTANCE.get(fieldId), "Registered field should be retrievable by id");
        assertEquals(1, FieldManager.INSTANCE.all().size(), "Exactly one field should be registered");
        assertTrue(FieldManager.INSTANCE.findAt(dimensionId, new Vec3(1, 1, 1)).isPresent(),
                "Point inside the field should be found");
        assertEquals(field, FieldManager.INSTANCE.findAt(dimensionId, new Vec3(1, 1, 1)).orElseThrow(),
                "findAt should return the same field record");
        IllegalArgumentException overlap = assertThrows(
                IllegalArgumentException.class,
                () -> FieldManager.INSTANCE.register(new FieldDefinition(
                        Identifier.fromNamespaceAndPath("tests", "spawn_overlap"),
                        dimensionId,
                        new AABB(5, 5, 5, 15, 15, 15),
                        Map.of()
                )),
                "Overlapping field registration should be rejected"
        );
        assertTrue(overlap.getMessage().contains("spawn_overlap"),
                "Overlap error should mention the new field id");
        assertTrue(overlap.getMessage().contains("spawn"),
                "Overlap error should mention the existing field id");
    }
}
