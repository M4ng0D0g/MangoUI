package com.myudog.myulib.api.framework.game.core.event;

import com.myudog.myulib.api.core.event.IEvent;
import com.myudog.myulib.api.framework.game.core.GameConfig;
import com.myudog.myulib.api.framework.game.core.GameData;
import com.myudog.myulib.api.framework.game.core.GameInstance;
import com.myudog.myulib.api.framework.game.core.IGameContext;

public class StateTickEvent<C extends GameConfig, D extends GameData, S extends com.myudog.myulib.api.core.state.IState<IGameContext>> implements IEvent {
    private final GameInstance<C, D, S> instance;
    private final S state;
    private final long tickCount;

    public StateTickEvent(GameInstance<C, D, S> instance, S state, long tickCount) {
        this.instance = instance;
        this.state = state;
        this.tickCount = tickCount;
    }

    public GameInstance<C, D, S> getInstance() { return instance; }
    public S getState() { return state; }
    public long getTickCount() { return tickCount; }
}
