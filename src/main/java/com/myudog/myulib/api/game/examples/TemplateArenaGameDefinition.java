package com.myudog.myulib.api.game.examples;

import com.myudog.myulib.api.event.listener.EventListener;
import com.myudog.myulib.api.game.core.GameConfig;
import com.myudog.myulib.api.game.core.GameData;
import com.myudog.myulib.api.game.core.GameDefinition;
import com.myudog.myulib.api.game.core.GameInstance;
import com.myudog.myulib.api.game.event.GameObjectInteractEvent;
import com.myudog.myulib.api.game.object.IGameObject;
import com.myudog.myulib.api.game.object.impl.InteractableObject;
import com.myudog.myulib.api.game.object.impl.RespawnPointObject;
import com.myudog.myulib.api.game.state.BasicGameStateMachine;
import com.myudog.myulib.api.game.state.GameState;
import com.myudog.myulib.api.game.state.GameStateMachine;
import com.myudog.myulib.api.team.TeamColor;
import com.myudog.myulib.api.team.TeamDefinition;
import com.myudog.myulib.internal.event.EventDispatcherImpl;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 可直接複製的 GameDefinition 模板：
 * - 在 Config 內定義 gameObjects()
 * - 透過 bindBehavior()/unbindBehavior() 綁定外界互動規則
 */
public final class TemplateArenaGameDefinition extends GameDefinition<TemplateArenaGameDefinition.TemplateArenaConfig, TemplateArenaGameDefinition.TemplateArenaData, TemplateArenaGameDefinition.TemplateArenaState> {
    private EventListener<GameObjectInteractEvent> listener;

    public TemplateArenaGameDefinition(Identifier id) {
        super(id);
    }

    @Override
    public TemplateArenaData createInitialData(TemplateArenaConfig config) {
        return new TemplateArenaData();
    }

    @Override
    public GameStateMachine<TemplateArenaState> createStateMachine(TemplateArenaConfig config) {
        return new BasicGameStateMachine<>(
                TemplateArenaState.WAITING,
                Map.of(
                        TemplateArenaState.WAITING, Set.of(TemplateArenaState.RUNNING),
                        TemplateArenaState.RUNNING, Set.of(TemplateArenaState.FINISHED)
                )
        );
    }

    @Override
    protected EventDispatcherImpl createEventBus() {
        return new EventDispatcherImpl();
    }

    @Override
    protected void bindBehavior(GameInstance<TemplateArenaConfig, TemplateArenaData, TemplateArenaState> instance) {
        listener = event -> {
            // 這裡放你的遊戲規則：例如按下互動點後切換狀態
            if (instance.getCurrentState() == TemplateArenaState.WAITING) {
                instance.transition(TemplateArenaState.RUNNING);
            }
            return com.myudog.myulib.api.event.ProcessResult.PASS;
        };
        instance.getEventBus().subscribe(GameObjectInteractEvent.class, listener);
    }

    @Override
    protected void unbindBehavior(GameInstance<TemplateArenaConfig, TemplateArenaData, TemplateArenaState> instance) {
        if (listener != null) {
            instance.getEventBus().unsubscribe(GameObjectInteractEvent.class, listener);
            listener = null;
        }
    }

    public enum TemplateArenaState implements GameState {
        WAITING,
        RUNNING,
        FINISHED
    }

    public static final class TemplateArenaData extends GameData {
    }

    public record TemplateArenaConfig(Map<Identifier, IGameObject> gameObjects) implements GameConfig {
        @Override
        public boolean validate() {
            return GameConfig.validateDefinitions(this);
        }

        public static TemplateArenaConfig sample() {
            Identifier spawnId = Identifier.fromNamespaceAndPath("example", "arena/spawn_logic");
            Identifier consoleId = Identifier.fromNamespaceAndPath("example", "arena/match_console");

            RespawnPointObject spawn = new RespawnPointObject(spawnId);
            InteractableObject console = new InteractableObject(consoleId);

            return new TemplateArenaConfig(Map.of(
                    spawnId, spawn,
                    consoleId, console
            ));
        }

        @Override
        public Map<String, Identifier> teams() {
            return Map.of(
                    GameConfig.SPECTATOR_TEAM_KEY, GameConfig.SPECTATOR_TEAM_ID,
                    "red", Identifier.fromNamespaceAndPath("example", "arena/red"),
                    "blue", Identifier.fromNamespaceAndPath("example", "arena/blue")
            );
        }

        @Override
        public List<TeamDefinition> additionalTeamDefinitions() {
            return List.of(
                    new TeamDefinition(
                            Identifier.fromNamespaceAndPath("example", "arena/red"),
                            Component.literal("Arena Red"),
                            TeamColor.RED,
                            Map.of(),
                            0
                    ),
                    new TeamDefinition(
                            Identifier.fromNamespaceAndPath("example", "arena/blue"),
                            Component.literal("Arena Blue"),
                            TeamColor.BLUE,
                            Map.of(),
                            0
                    )
            );
        }
    }

}

