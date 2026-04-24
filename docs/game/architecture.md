# Game Architecture

This document is the canonical runtime architecture for `game`.
It focuses on exact `Class#method` call order and lifecycle gates.

## Lifecycle Contract

- Persistent game id: player-provided game id maps to `GameDefinition#getId()` and stays stable.
- Reusable instance lifecycle: `create -> init -> start -> end -> clean`.
- Init gate: every cycle must complete `init` before `join` or `start`.
- End behavior: `end` runs `onEnd`, performs clean, and leaves the instance reusable but not initialized.

## Call Chain by Phase

### 1) Create

1. `AccessCommandService#registerGameCrud(...)/create` (if command entry)
2. `GameManager#createInstance(gameId, instanceToken, config, level)`
3. `GameManager#definition(gameId)` resolves definition
4. `GameDefinition#createInstance(instanceId, config, level)`
5. `GameDefinition#createStateMachine(config)`
6. `GameDefinition#createEventBus()`
7. `new GameInstance(...)`
8. `GameManager` sets scoped session id via `GameScopeTokens#scoped(...)`
9. `GameInstance#setupIdentity(sessionIdentifier)`
10. `GameManager` writes `INSTANCES` + `INSTANCE_TOKENS`

State after create:

- `enabled=true`
- `initialized=false`
- `started=false`

### 2) Init

1. `GameManager#initInstance(instanceId)`
2. `GameInstance#init()`
3. `GameConfig#validate()`
4. `GameDefinition#createInitialData(config)`
5. `GameData#init(config)`
6. `GameInstance#initializeRuntimeObjects()`
7. `GameDefinition#gameBehaviors()` -> `GameInstance#bindBehavior(...)`
8. `GameDefinition#bindBehaviors(instance)` (compat bridge)
9. `initialized=true`

Init failure path:

- Unbind already bound behaviors
- `GameData#reset(instance)` and clear `data`
- throw `RuntimeException("初始化遊戲實例失敗...")`

### 3) Join

1. `AccessCommandService ... game join ...` (optional command entry)
2. `GameManager#joinPlayer(instanceId, playerId, requestedTeamId)`
3. Guard checks:
   - `instance != null`
   - `instance.isEnabled()`
   - `instance.isInitialized()`
   - not already assigned to another instance
4. `GameInstance#joinPlayer(playerId, requestedTeamId)`
5. `GameDefinition#resolveTeamForJoin(...)`
6. `GameInstance#resolveJoinTeam(...)`
7. `GameData#movePlayerToTeam(...)`
8. On success `playerToInstanceMap[playerId] = instanceId`

Notes:

- Without `init`, join always fails.
- Default no-team join falls back to spectator/ghost team.

### 4) Start

1. `AccessCommandService ... game start ...` (optional command entry)
2. `GameManager#startInstance(instanceId)`
3. Guard checks:
   - `instance != null`
   - `instance.isEnabled()`
   - `instance.isInitialized()`
4. `GameInstance#start()`
5. `GameDefinition#onStart(instance)`
6. `started=true`

### 5) End

1. `AccessCommandService ... game end ...` (optional command entry)
2. `GameManager#endInstance(instanceId)`
3. `GameInstance#end()`
4. `GameDefinition#onEnd(instance)`
5. `GameInstance#clean()`
6. `GameInstance#cleanupRuntimeResources()`
   - reset runtime state flags and tick counter
   - state exit/reset/re-enter initial state
   - unbind all game behaviors
   - `eventBus.clear()`
   - `GameData#reset(instance)` and clear `data`
7. `GameManager#unassignPlayersInInstance(instanceId)`

State after end:

- instance still registered and reusable
- `initialized=false`
- `started=false`
- must call `init` again before any new `join/start`

### 6) Destroy (final removal)

1. `GameManager#destroyInstance(instanceId)`
2. remove token + player mappings
3. `GameInstance#destroy()`
4. internal `clean` then `enabled=false`
5. instance removed from manager map

## Team and Data Invariants

- `GameConfig` must include `spectator` team id = `myulib:spectator`.
- `ghost` alias (if present) must map to the same spectator id.
- At least one playable team (non-spectator/ghost) is required.
- `GameData` owns O(1) participant mapping: `Map<UUID, Integer> participantToEntity`.

## Scoped Token Strategy

For runtime-scoped identifiers, use:

- Namespace: `modToken`
- Path: `gameToken/definitionToken/instanceToken`

Utilities:

- `GameScopeTokens#scoped(mod, game, def, instance)`
- `GameScopeTokens#scoped(gameDefinition, def, instance)`

Example:

- `myulib:chess/white/room_a`

This format is intended for team/field/timer/object and other subsystem ids scoped per game/instance.

