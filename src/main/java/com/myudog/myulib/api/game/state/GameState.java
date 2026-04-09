package com.myudog.myulib.api.game.state;

public interface GameState {
    default <S extends GameState> void onEnter(com.myudog.myulib.api.game.instance.GameInstance<S> instance, GameStateContext<S> context) {}
    default <S extends GameState> void onExit(com.myudog.myulib.api.game.instance.GameInstance<S> instance, GameStateContext<S> context) {}
    default <S extends GameState> void onTick(com.myudog.myulib.api.game.instance.GameInstance<S> instance) {}
}
