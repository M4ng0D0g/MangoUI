# Game core overview

The current game API centers on `GameManager`, `GameDefinition`, `GameInstance`, `GameConfig`, `GameData`, `GameState`, `GameStateMachine`, and `GameStateChangeEvent`.

## GameManager

`GameManager` is the registry for definitions and running instances.

### Methods
- `install()`
- `register(definition)` / `unregister(gameId)`
- `hasDefinition(gameId)` / `definition(gameId)`
- `createInstance(gameId, config)`
- `getInstance(instanceId)` / `getInstances()` / `getInstances(gameId)`
- `destroyInstance(instanceId)`
- `tickAll()`

## GameDefinition

`GameDefinition<C, D, S>` defines how to build data, state machines, and the per-instance event bus.

### Key responsibilities
- `createInitialData(config)`
- `createStateMachine(config)`
- `createEventBus()`
- `bindBehaviors(instance)`

## GameInstance

`GameInstance<C, D, S>` is the runtime container for one game session.

### Main accessors
- `getInstanceId()` / `getDefinition()`
- `getConfig()` / `getData()` / `getStateMachine()` / `getEventBus()`
- `isEnabled()` / `getTickCount()` / `getCurrentState()`

### State and lifecycle
- `canTransition(to)`
- `transition(to)` / `transitionUnsafe(to)`
- `resetState()`
- `tick()` / `destroy()`

## Notes
- `GameConfig.empty()` is the safest default configuration helper.
- Game-object configuration lives under `com.myudog.myulib.api.game.object`.

