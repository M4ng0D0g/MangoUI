package com.myudog.myulib.api.game.state;

import com.myudog.myulib.api.game.state.GameState;

public record GameTransition<S extends GameState>(S from, S to, boolean allowed) {
    public GameTransition(S from, S to) {
        this(from, to, true);
    }
}

