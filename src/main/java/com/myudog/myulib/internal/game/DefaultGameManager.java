package com.myudog.myulib.internal.game;

import com.myudog.myulib.api.game.state.GameState;

import com.myudog.myulib.api.game.GameManager;
import com.myudog.myulib.api.game.bootstrap.GameBootstrapConfig;
import com.myudog.myulib.api.game.instance.GameInstance;
import com.myudog.myulib.api.game.state.GameDefinition;
import net.minecraft.resources.Identifier;

public class DefaultGameManager {
    public static void install() { GameManager.install(); }
    public static void register(GameDefinition<?> definition) { GameManager.register(definition); }
    public static GameDefinition<?> unregister(Identifier gameId) { return GameManager.unregister(gameId); }
    public static boolean hasDefinition(Identifier gameId) { return GameManager.hasDefinition(gameId); }
    public static <S extends GameState> GameDefinition<S> definition(Identifier gameId) { return GameManager.definition(gameId); }
    public static <S extends GameState> GameInstance<S> createInstance(Identifier gameId, GameBootstrapConfig config) { return GameManager.createInstance(gameId, config); }
}
