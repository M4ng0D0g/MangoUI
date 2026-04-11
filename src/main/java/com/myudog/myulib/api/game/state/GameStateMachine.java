package com.myudog.myulib.api.game.state;

public interface GameStateMachine<S extends GameState> {

    S getCurrent();

    boolean canTransition(S to);

    boolean transitionTo(S to);

    /**
     * 強制切換狀態 (無視 canTransition 的規則限制)
     */
    void forceTransition(S to);

    void reset();
}