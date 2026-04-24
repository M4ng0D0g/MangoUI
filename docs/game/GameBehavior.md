# GameBehavior

`GameBehavior<C, D, S>` is a lifecycle-bound logic module.

## Contract

- `onBind(instance)` runs during `init()`.
- `onUnbind(instance)` runs during `clean()` / `destroy()`.

## Use cases

- Register event listeners on instance event bus.
- Start/stop timers tied to a round.
- Attach transient runtime mechanics that should be removed at clean.

## Minimal example

```java
final class ChessScoreBehavior implements GameBehavior<ChessConfig, ChessData, ChessState> {
    @Override
    public void onBind(GameInstance<ChessConfig, ChessData, ChessState> instance) {
        instance.getEventBus().subscribe(GameObjectInteractEvent.class, event -> {
            // round-scoped logic
            return ProcessResult.PASS;
        });
    }

    @Override
    public void onUnbind(GameInstance<ChessConfig, ChessData, ChessState> instance) {
        // optional custom cleanup
    }
}
```

