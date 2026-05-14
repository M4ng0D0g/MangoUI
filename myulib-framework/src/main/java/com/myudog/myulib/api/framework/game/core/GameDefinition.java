package com.myudog.myulib.api.framework.game.core;

import com.myudog.myulib.api.core.event.EventBus;
import com.myudog.myulib.api.core.state.IState;
import com.myudog.myulib.api.core.state.StateMachine;
import com.myudog.myulib.api.framework.team.TeamColor;
import com.myudog.myulib.api.framework.team.TeamDefinition;
import com.myudog.myulib.api.framework.team.TeamManager;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public abstract class GameDefinition<C extends GameConfig, D extends GameData, S extends IState<IGameContext>> {

    private final Identifier defId;

    protected GameDefinition(@NotNull Identifier defId) {
        this.defId = defId;
    }

    public final Identifier id() {
        return defId;
    }

    /**
     * 建立預設 Config。
     */
    public abstract C createConfig();

    /**
     * 建立初始 Data。
     */
    public abstract D createInitialData(C config);

    /**
     * 建立狀態機。
     */
    public abstract StateMachine<S, IGameContext> createStateMachine(C config);

    /**
     * 建立事件匯流排。
     */
    protected abstract EventBus createEventBus();

    protected void bindBehavior(@NotNull GameInstance<C, D, S> instance) {
        TeamDefinition spectatorTeam = new TeamDefinition(
                instance.getConfig().SPECTATOR_TEAM.toString(),
                Component.translatable("team_spectator"),
                TeamColor.DARK_GRAY,
                Map.of()
        );
        TeamManager.INSTANCE.register(spectatorTeam);
    }

    protected void unbindBehavior(@NotNull GameInstance<C, D, S> instance) {
        var config = instance.getConfig();
        TeamManager.INSTANCE.unregister(UUID.fromString(config.SPECTATOR_TEAM.toString()));
    }

    public abstract void onInit(@NotNull GameInstance<C, D, S> instance);

    /**
     * 遊戲啟動回呼。
     */
    public abstract void onStart(@NotNull GameInstance<C, D, S> instance);

    /**
     * 遊戲關閉回呼。
     */
    public abstract void onShutdown(@NotNull GameInstance<C, D, S> instance);

    public final GameInstance<C, D, S> createInstance(@NotNull String instanceId, @NotNull C config, ServerLevel level) {
        StateMachine<S, IGameContext> stateMachine = Objects.requireNonNull(createStateMachine(config), "createStateMachine() returned null");
        EventBus eventBus = Objects.requireNonNull(createEventBus(), "createEventBus() returned null");
        return new GameInstance<>(instanceId, level, this, config, stateMachine, eventBus);
    }
}
