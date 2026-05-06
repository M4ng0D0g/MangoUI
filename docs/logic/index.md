# Logic overview
The current gameplay flow is built from game, timer, event, and permission systems.
A thin compatibility API also exists in `com.myudog.myulib.api.game.logic` for bridge code.
## Current alternatives
- `com.myudog.myulib.api.game.core.GameManager`
- `com.myudog.myulib.api.game.core.GameDefinition`
- `com.myudog.myulib.api.game.core.GameInstance`
- `com.myudog.myulib.api.game.GameState`
- `com.myudog.myulib.api.game.GameStateMachine`
- `com.myudog.myulib.api.object.event.StateChangeEvent`
- `com.myudog.myulib.api.timer.TimerManager`
- `com.myudog.myulib.api.timer.TimerModels`
- `com.myudog.myulib.api.timer.TimerEvents`
- `com.myudog.myulib.api.event.ServerEventBus`
- `com.myudog.myulib.api.permission.PermissionManager`
## Notes
- Keep this area as legacy navigation only.
- The `game.logic` package is compatibility-only.
