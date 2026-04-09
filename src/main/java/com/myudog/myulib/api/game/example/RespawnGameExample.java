package com.myudog.myulib.api.game.example;

import com.myudog.myulib.api.game.bootstrap.GameBootstrapConfig;
import com.myudog.myulib.api.game.feature.GameFeature;
import com.myudog.myulib.api.game.feature.GameScoreboardFeature;
import com.myudog.myulib.api.game.feature.GameTimerFeature;
import com.myudog.myulib.api.game.instance.GameInstance;
import com.myudog.myulib.api.game.state.GameDefinition;
import com.myudog.myulib.api.game.state.GameStateContext;
import net.minecraft.resources.Identifier;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import com.myudog.myulib.api.game.state.GameState;

public class RespawnGameExample {
    public enum RespawnGameState implements GameState { 
        WAITING {
            @Override
            public <S extends GameState> void onEnter(GameInstance<S> instance, GameStateContext<S> context) {
                instance.getFeatureOrCreate(GameScoreboardFeature.class).setLine(0, "Respawn game ready");
                instance.getFeatureOrCreate(GameScoreboardFeature.class).setValue("players", 0);
            }
        }, 
        COUNTDOWN, 
        ACTIVE, 
        FINISHED;

        @Override
        public <S extends GameState> void onTick(GameInstance<S> instance) {
            instance.getFeatureOrCreate(GameScoreboardFeature.class).setValue("ticks", (int) instance.getTickCount());
        }

        @Override
        public <S extends GameState> void onEnter(GameInstance<S> instance, GameStateContext<S> context) {
            instance.getFeatureOrCreate(GameScoreboardFeature.class).setLine(1, "State: " + context.to());
        }
    }

    public static GameDefinition<RespawnGameState> createDefinition() {
        Map<RespawnGameState, Set<RespawnGameState>> transitions = new EnumMap<>(RespawnGameState.class);
        transitions.put(RespawnGameState.WAITING, Set.of(RespawnGameState.COUNTDOWN, RespawnGameState.ACTIVE));
        transitions.put(RespawnGameState.COUNTDOWN, Set.of(RespawnGameState.ACTIVE, RespawnGameState.FINISHED));
        transitions.put(RespawnGameState.ACTIVE, Set.of(RespawnGameState.FINISHED));
        transitions.put(RespawnGameState.FINISHED, Set.of(RespawnGameState.WAITING));

        return new GameDefinition<>(
            Identifier.fromNamespaceAndPath("myulib", "respawn_game"),
            RespawnGameState.WAITING,
            transitions
        ) {
            @Override
            public Set<Identifier> getRequiredSpecialObjectIds() {
                return Set.of(Identifier.fromNamespaceAndPath("myulib", "respawn_anchor"));
            }

            @Override
            public List<GameFeature> createFeatures(GameBootstrapConfig config) {
                GameScoreboardFeature scoreboard = new GameScoreboardFeature();
                scoreboard.objectiveId = "respawn";
                scoreboard.displayName = "Respawn";
                GameTimerFeature timers = new GameTimerFeature();
                return List.of(scoreboard, timers);
            }
        };
    }
}
