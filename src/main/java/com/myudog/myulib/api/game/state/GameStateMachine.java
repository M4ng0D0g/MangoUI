package com.myudog.myulib.api.game.state;

import com.myudog.myulib.api.game.state.GameState;

public interface GameStateMachine<S extends GameState> {
    S getCurrentState();

    boolean canTransition(S to);

    boolean transition(S to);

    void reset();
}

