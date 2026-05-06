package com.myudog.myulib.api.game.core;

import com.myudog.myulib.api.event.EventBus;
import com.myudog.myulib.api.state.IStateContext;

public interface IGameContext extends IStateContext<IGameContext> {

    GameConfig getConfig();

    GameData getData();

    EventBus getEventBus();

    long getTickCount();
}