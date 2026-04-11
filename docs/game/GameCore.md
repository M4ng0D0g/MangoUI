# Game API overview
Current game creation flows use `GameManager`, `GameDefinition`, `GameInstance`, `GameConfig`, `GameData`, `GameState`, and `GameStateMachine`.
## GameManager methods
- `install()`
- `register(...)` / `unregister(...)`
- `hasDefinition(...)` / `definition(...)`
- `createInstance(...)`
- `getInstance(...)` / `getInstances()` / `getInstances(gameId)`
- `destroyInstance(...)`
- `tickAll()`
## Notes
- `GameConfig.empty()` is the safest default configuration helper in this branch.
