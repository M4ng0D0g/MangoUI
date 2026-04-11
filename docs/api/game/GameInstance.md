# GameInstance overview
`GameInstance<C, D, S>` lives in `com.myudog.myulib.api.game.core` and represents one running game session.
## Accessors
- `getInstanceId()`
- `getDefinition()`
- `getConfig()`
- `getData()`
- `getStateMachine()`
- `getEventBus()`
- `isEnabled()`
- `getTickCount()`
- `getCurrentState()`
## State control
- `canTransition(...)`
- `transition(...)`
- `transitionUnsafe(...)`
- `resetState()`
- `tick()`
- `destroy()`
## Game object helpers
- `hasGameObject(...)`
- `getGameObjectConfig(...)`
- `requireGameObjectConfig(...)`
