package com.myudog.myulib.api.game.state;

import com.myudog.myulib.api.game.state.GameState;

import net.minecraft.resources.Identifier;

public record GameStateContext<S extends GameState>(Identifier gameId, int instanceId, S from, S to) {
}



