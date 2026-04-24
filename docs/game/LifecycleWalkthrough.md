# Lifecycle Walkthrough

This page demonstrates the complete game assembly flow and where each hook should be implemented.

## Stage overview

1. `create`: `GameManager.createInstance(...)`
2. `init`: `GameManager.initInstance(...)`
3. `join`: `GameManager.joinPlayer(...)`
4. `start`: `GameManager.startInstance(...)`
5. `end`: `GameManager.endInstance(...)` -> `GameDefinition.onEnd(...)`
6. `clean`: internal `GameInstance.clean()`
7. next round: run `init` again before any `join/start`

## Full example (definition + data + config)

```java
public final class ChessDefinition extends GameDefinition<ChessConfig, ChessData, ChessState> {
    public static final Identifier ID = Identifier.fromNamespaceAndPath("myulib", "chess");

    public ChessDefinition() {
        super(ID);
    }

    @Override
    public ChessData createInitialData(ChessConfig config) {
        // Initialization override point for room data.
        return new ChessData();
    }

    @Override
    public GameStateMachine<ChessState> createStateMachine(ChessConfig config) {
        return new BasicGameStateMachine<>(
                ChessState.WAITING,
                Map.of(
                        ChessState.WAITING, Set.of(ChessState.RUNNING),
                        ChessState.RUNNING, Set.of(ChessState.FINISHED)
                )
        );
    }

    @Override
    protected EventDispatcherImpl createEventBus() {
        // Event bus is owned by GameInstance and auto-cleared at clean.
        return new EventDispatcherImpl();
    }

    @Override
    protected List<GameBehavior<ChessConfig, ChessData, ChessState>> gameBehaviors() {
        return List.of(new ChessScoreBehavior());
    }

    @Override
    protected void onStart(GameInstance<ChessConfig, ChessData, ChessState> instance) {
        // Start timing: called after init succeeded.
        instance.transition(ChessState.RUNNING);
    }

    @Override
    protected void onEnd(GameInstance<ChessConfig, ChessData, ChessState> instance) {
        // End timing: called before clean.
        instance.transition(ChessState.FINISHED);
    }
}

final class ChessData extends GameData {
    // Put ECS component binding and participant map usage here.
    void bindPlayer(UUID playerId, int entityId) {
        addParticipant(playerId, entityId); // O(1) reverse lookup
    }
}

record ChessConfig(Map<Identifier, IGameObject> gameObjects) implements GameConfig {
    @Override
    public Map<String, Identifier> teams() {
        // spectator/ghost + at least one playable team
        return Map.of(
                GameConfig.SPECTATOR_TEAM_KEY, GameConfig.SPECTATOR_TEAM_ID,
                GameConfig.GHOST_TEAM_KEY, GameConfig.SPECTATOR_TEAM_ID,
                "white", Identifier.fromNamespaceAndPath("myulib", "chess_white"),
                "black", Identifier.fromNamespaceAndPath("myulib", "chess_black")
        );
    }

    @Override
    public List<TeamDefinition> additionalTeamDefinitions() {
        return List.of(
                new TeamDefinition(Identifier.fromNamespaceAndPath("myulib", "chess_white"), Component.literal("White"), TeamColor.WHITE, Map.of(), 16),
                new TeamDefinition(Identifier.fromNamespaceAndPath("myulib", "chess_black"), Component.literal("Black"), TeamColor.BLACK, Map.of(), 16)
        );
    }
}
```

## Runtime usage sequence

```java
GameManager.register(new ChessDefinition());

GameInstance<?, ?, ?> room = GameManager.createInstance(ChessDefinition.ID, "room_a", chessConfig, level);

GameManager.initInstance(room.getInstanceId());
GameManager.joinPlayer(room.getInstanceId(), playerWhite, null);
GameManager.joinPlayer(room.getInstanceId(), playerBlack, null);
GameManager.startInstance(room.getInstanceId());

GameManager.endInstance(room.getInstanceId());

// next round must init again
GameManager.initInstance(room.getInstanceId());
```

## Where to define event bus listeners?

- Preferred: `gameBehaviors()` + `GameBehavior.onBind(...)`
- Compatibility: `bindBehaviors(...)` (deprecated bridge)
- Do not manually clear listeners; clean phase clears the instance event bus.

