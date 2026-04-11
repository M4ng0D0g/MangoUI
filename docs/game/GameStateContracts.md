# Game state notes
State management is represented by `GameState`, `GameStateMachine`, `GameStateChangeEvent`, and `GameDefinition.createStateMachine(...)`.
## Current behavior
- `GameState` marks the state type.
- `GameStateMachine` controls allowed transitions and reset.
- `GameStateChangeEvent` is dispatched by `GameInstance` when the state changes.
