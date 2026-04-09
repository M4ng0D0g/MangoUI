package com.myudog.myulib.internal.game.logic;

import com.myudog.myulib.api.game.state.GameState;

import com.myudog.myulib.api.game.logic.LogicContracts;
import com.myudog.myulib.api.game.logic.LogicEngine;

public class DefaultLogicBridge {
    public static <S extends GameState> void register(LogicEngine<S> engine, LogicContracts.LogicRule<S> rule) { engine.register(rule); }
}
