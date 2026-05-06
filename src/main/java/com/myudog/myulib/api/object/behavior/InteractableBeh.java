package com.myudog.myulib.api.object.behavior;

import com.myudog.myulib.api.event.ProcessResult;
import com.myudog.myulib.api.event.IEventListener;
import com.myudog.myulib.api.game.core.GameInstance;
import com.myudog.myulib.api.object.event.BlockInteractEvent;
import com.myudog.myulib.api.object.event.ObjectInteractEvent;
import com.myudog.myulib.api.object.IObjectRt;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class InteractableBeh implements IBlockBeh {
    private final Map<UUID, Long> lastInteractTime = new ConcurrentHashMap<>();
    private IEventListener<BlockInteractEvent> listener;

    @Override
    public void onInitialize(IObjectRt object, GameInstance<?, ?, ?> instance) {
        listener = event -> {
            if (event.getLevel() != instance.getLevel()) {
                return event.isCanceled() ? ProcessResult.CONSUME : ProcessResult.PASS;
            }

            var objectPos = object.getPosition();
            if (objectPos == null
                    || (int) objectPos.x != event.getPos().getX()
                    || (int) objectPos.y != event.getPos().getY()
                    || (int) objectPos.z != event.getPos().getZ()) {
                return ProcessResult.PASS;
            }

            long now = System.currentTimeMillis();
            long lastTime = lastInteractTime.getOrDefault(event.getPlayer().getUUID(), 0L);
            long cooldown = 0L;
            if (now - lastTime < cooldown) {
                return ProcessResult.CONSUME;
            }

            lastInteractTime.put(event.getPlayer().getUUID(), now);
            event.setCanceled(true);
            instance.getEventBus().dispatch(new ObjectInteractEvent(object, event.getPlayer()));
            return ProcessResult.CONSUME;
        };

        instance.getEventBus().subscribe(BlockInteractEvent.class, listener);
    }

    @Override
    public void onDestroy(IObjectRt object, GameInstance<?, ?, ?> instance) {
        if (listener != null) {
            instance.getEventBus().unsubscribe(BlockInteractEvent.class, listener);
            listener = null;
        }
        lastInteractTime.clear();
    }
}


