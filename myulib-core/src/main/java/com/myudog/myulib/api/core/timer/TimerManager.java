package com.myudog.myulib.api.core.timer;

import com.myudog.myulib.api.core.ecs.EcsContainer;
import java.util.*;

/**
 * TimerManager
 *
 * 系統：核心計時系統 (Core Timer System) - ECS 版
 * 角色：驅動所有實體上的 TimerInstance。
 */
public final class TimerManager {

    public static final TimerManager INSTANCE = new TimerManager();
    
    /** 全域 ECS 容器，用於相容舊有的全域計時器。 */
    public final EcsContainer GLOBAL_CONTAINER = new EcsContainer();

    private final Map<UUID, TimerDefinition> DEFINITIONS = new LinkedHashMap<>();

    private TimerManager() {
    }

    public void install() {
        // 全域容器的更新邏輯可以掛載在伺服器 Tick 事件上
        net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents.END_SERVER_TICK.register(server -> {
            update(GLOBAL_CONTAINER);
        });
    }

    public void register(TimerDefinition timer) {
        DEFINITIONS.put(timer.uuid, timer);
    }
    
    public TimerDefinition unregister(UUID uuid) {
        return DEFINITIONS.remove(uuid);
    }
    
    public boolean hasDefinition(UUID uuid) {
        return DEFINITIONS.containsKey(uuid);
    }
    
    public int timerDefinitionCount() {
        return DEFINITIONS.size();
    }
    
    public int timerInstanceCount() {
        // 這裡回傳全域容器中的實例數量
        int[] count = {0};
        GLOBAL_CONTAINER.forAll(TimerInstance.class, (id, inst) -> count[0]++);
        return count[0];
    }

    public void update(EcsContainer container) {
        container.forAll(TimerInstance.class, (entityId, timer) -> {
            if (timer.status != TimerStatus.RUNNING) return;

            timer.elapsedTicks++;
            TimerDefinition def = DEFINITIONS.get(timer.defId);
            if (def == null) return;

            TimerSnapshot snapshot = createSnapshot(entityId, container, timer, def);
            
            // 檢查綁定
            long remaining = Math.max(0L, def.durationTicks - timer.elapsedTicks);
            def.elapsedBindings.values().forEach(b -> {
                if (b.basis() == TimerTickBasis.ELAPSED && b.matches(timer.elapsedTicks)) b.action().invoke(snapshot);
            });
            def.remainingBindings.values().forEach(b -> {
                if (b.basis() == TimerTickBasis.REMAINING && b.matches(remaining)) b.action().invoke(snapshot);
            });

            if (timer.elapsedTicks >= def.durationTicks) {
                timer.status = TimerStatus.COMPLETED;
                def.completedActions.forEach(a -> a.invoke(snapshot));
                if (def.autoStopOnComplete) timer.status = TimerStatus.STOPPED;
            }
        });
    }

    private TimerSnapshot createSnapshot(int entityId, EcsContainer container, TimerInstance timer, TimerDefinition def) {
        long remaining = Math.max(0L, def.durationTicks - timer.elapsedTicks);
        return new TimerSnapshot(timer.instanceId, null, def, timer.status, timer.elapsedTicks, remaining, timer.payload, 0);
    }

    public int createInstance(EcsContainer container, UUID defId, Long ownerEntityId, TimerPayload payload) {
        TimerDefinition def = DEFINITIONS.get(defId);
        if (def == null) throw new IllegalArgumentException("Unknown TimerDefinition: " + defId);

        int id = container.createEntity(UUID.randomUUID());
        TimerInstance instance = new TimerInstance(container.getUuid(id), defId, ownerEntityId, payload);
        instance.status = TimerStatus.RUNNING;
        container.addComponent(id, TimerInstance.class, instance);
        return id;
    }
    
    /** 舊版相容方法 */
    public int createInstance(UUID defId, Long ownerEntityId, TimerPayload payload) {
        return createInstance(GLOBAL_CONTAINER, defId, ownerEntityId, payload);
    }
}