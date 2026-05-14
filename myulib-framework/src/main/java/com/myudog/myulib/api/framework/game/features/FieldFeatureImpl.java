package com.myudog.myulib.api.framework.game.features;

import com.myudog.myulib.api.core.ecs.EcsContainer;
import com.myudog.myulib.api.framework.field.FieldDefinition;
import com.myudog.myulib.api.framework.field.FieldInstance;
import com.myudog.myulib.api.framework.field.FieldManager;
import com.myudog.myulib.api.framework.game.core.GameInstance;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class FieldFeatureImpl implements FieldFeature {

    private static final Logger LOGGER = LoggerFactory.getLogger("Myulib-FieldFeature");

    private final EcsContainer container;
    private final Map<UUID, Integer> fieldEntities = new ConcurrentHashMap<>();

    public FieldFeatureImpl(EcsContainer container) {
        this.container = container;
    }

    @Override
    public boolean bindField(@NotNull UUID fieldId) {
        // In ECS architecture, binding might mean tracking an existing definition
        return fieldEntities.containsKey(fieldId);
    }

    @Override
    public boolean isInsideGameBounds(@NotNull Identifier dimensionId, @NotNull Vec3 position) {
        for (Integer entityId : fieldEntities.values()) {
            FieldInstance field = container.getComponent(entityId, FieldInstance.class);
            if (field != null && field.definition.dimensionId().equals(dimensionId) && field.definition.bounds().contains(position)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public Set<UUID> getFieldsAt(@NotNull Identifier dimensionId, @NotNull Vec3 position) {
        Set<UUID> found = new HashSet<>();
        for (Map.Entry<UUID, Integer> entry : fieldEntities.entrySet()) {
            FieldInstance field = container.getComponent(entry.getValue(), FieldInstance.class);
            if (field != null && field.definition.dimensionId().equals(dimensionId) && field.definition.bounds().contains(position)) {
                found.add(entry.getKey());
            }
        }
        return found;
    }

    @Override
    public Set<UUID> getActiveFields() {
        return Set.copyOf(fieldEntities.keySet());
    }

    @Override
    public void unbindField(@NotNull UUID fieldId) {
        Integer entityId = fieldEntities.remove(fieldId);
        if (entityId != null) {
            FieldManager.INSTANCE.destroyInstance(container, entityId);
        }
    }

    @Override
    public void clean(GameInstance<?, ?, ?> instance) {
        fieldEntities.values().forEach(id -> FieldManager.INSTANCE.destroyInstance(container, id));
        fieldEntities.clear();
    }
    
    public void registerField(FieldDefinition definition) {
        int entityId = FieldManager.INSTANCE.createInstance(container, definition);
        fieldEntities.put(definition.uuid(), entityId);
    }
}
