package com.myudog.myulib.api.object.behavior;

import com.myudog.myulib.api.event.IEventListener;
import com.myudog.myulib.api.event.ProcessResult;
import com.myudog.myulib.api.game.core.GameInstance;
import com.myudog.myulib.api.object.event.BlockBreakEvent;
import com.myudog.myulib.api.object.event.ObjectMineEvent;
import com.myudog.myulib.api.object.IObjectRt;

public final class MineableBeh implements IBlockBeh {
    private IEventListener<BlockBreakEvent> listener;

    @Override
    public void onInitialize(IObjectRt object, GameInstance<?, ?, ?> instance) {
        listener = event -> {
            if (event.getLevel() != instance.getLevel()) {
                return event.isCanceled() ? ProcessResult.CONSUME : ProcessResult.PASS;
            }

            var objectPos = object.getPosition();
            if (objectPos != null
                    && (int) objectPos.x == event.getPos().getX()
                    && (int) objectPos.y == event.getPos().getY()
                    && (int) objectPos.z == event.getPos().getZ()) {
                event.setCanceled(true);
                instance.getEventBus().dispatch(new ObjectMineEvent(object, event.getPlayer()));
                return ProcessResult.CONSUME;
            }

            return ProcessResult.PASS;
        };

        instance.getEventBus().subscribe(BlockBreakEvent.class, listener);
    }

    @Override
    public void onDestroy(IObjectRt object, GameInstance<?, ?, ?> instance) {
        if (listener != null) {
            instance.getEventBus().unsubscribe(BlockBreakEvent.class, listener);
            listener = null;
        }
    }
}



