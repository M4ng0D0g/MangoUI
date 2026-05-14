package com.myudog.myulib.api.framework.field;

import com.myudog.myulib.api.core.ecs.EcsContainer;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.Vec3;
import java.util.*;

public final class FieldManager {

    public static final FieldManager INSTANCE = new FieldManager();
    
    /** 全域 ECS 容器，用於相容舊有的全域區域。 */
    public final EcsContainer GLOBAL_CONTAINER = new EcsContainer();

    private FieldManager() {}

    public void install() {
        // 全域容器的自動儲存邏輯可以註冊到伺服器生命週期
    }

    public int createInstance(EcsContainer container, FieldDefinition definition) {
        int id = container.createEntity(definition.uuid());
        container.addComponent(id, FieldInstance.class, new FieldInstance(definition));
        return id;
    }

    public void destroyInstance(EcsContainer container, int entityId) {
        container.destroyEntity(entityId);
    }

    // ==========================================
    // 舊版相容方法 (Legacy API)
    // ==========================================

    public void register(FieldDefinition definition) {
        createInstance(GLOBAL_CONTAINER, definition);
    }

    public void unregister(UUID fieldUuid) {
        Integer id = GLOBAL_CONTAINER.getEntityId(fieldUuid);
        if (id != null) GLOBAL_CONTAINER.destroyEntity(id);
    }
    
    public void unregister(Identifier fieldId) {
        unregister(UUID.nameUUIDFromBytes(fieldId.toString().getBytes()));
    }

    public FieldDefinition get(UUID fieldUuid) {
        Integer id = GLOBAL_CONTAINER.getEntityId(fieldUuid);
        if (id != null) {
            FieldInstance inst = GLOBAL_CONTAINER.getComponent(id, FieldInstance.class);
            return inst != null ? inst.definition : null;
        }
        return null;
    }
    
    public FieldDefinition get(Identifier fieldId) {
        return get(UUID.nameUUIDFromBytes(fieldId.toString().getBytes()));
    }

    public Optional<FieldDefinition> findAt(Identifier dimensionId, Vec3 position) {
        FieldDefinition[] found = {null};
        GLOBAL_CONTAINER.forAll(FieldInstance.class, (id, inst) -> {
            if (inst.definition.dimensionId().equals(dimensionId) && inst.definition.bounds().contains(position)) {
                found[0] = inst.definition;
            }
        });
        return Optional.ofNullable(found[0]);
    }

    public Map<UUID, FieldDefinition> all() {
        Map<UUID, FieldDefinition> map = new HashMap<>();
        GLOBAL_CONTAINER.forAll(FieldInstance.class, (id, inst) -> map.put(inst.definition.uuid(), inst.definition));
        return map;
    }

    public void save() {
        // 全域容器的自動儲存邏輯
        GLOBAL_CONTAINER.savePersistentEntities();
    }
}
