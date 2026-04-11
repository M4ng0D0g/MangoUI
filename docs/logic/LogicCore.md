# Legacy logic overview
The current source tree exposes only a thin compatibility API in `com.myudog.myulib.api.game.logic`.
For actual gameplay composition, use the game, timer, event, and permission systems.
## Use these current systems instead
- `GameManager`
- `GameDefinition`
- `GameInstance`
- `GameStateMachine`
- `TimerManager`
- `ServerEventBus`
- `PermissionManager`
## Notes
- Rule storage in the compatibility layer is intentionally minimal.
