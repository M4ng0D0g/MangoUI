package com.myudog.myulib.api.framework.game.core;

import com.myudog.myulib.api.core.event.EventBus;
import com.myudog.myulib.api.core.state.IStateContext;

public interface IGameContext extends IStateContext<IGameContext> {

    GameConfig getConfig();

    GameData getData();

    EventBus getEventBus();

    long getTickCount();
}
