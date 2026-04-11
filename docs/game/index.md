# Game System
`com.myudog.myulib.api.game` is the current public entry point for game registration and instance management.
## Core types
- `GameManager`
- `GameDefinition`
- `GameInstance`
- `GameConfig`
- `GameData`
- `GameState`
- `GameStateMachine`
- `GameStateChangeEvent`
- `GameObjectConfig`
- `GameObjectKind`
- `IGameEntity`
## Related systems
- `timer`
- `event`
- `ecs`
- `field`
- `identity`
- `permission`
- `team`
- `rolegroup`
## Notes
- Game objects are modeled through `com.myudog.myulib.api.game.object`.
- The old feature-heavy state-contract pages are no longer the source of truth.
