# GameConfig

`GameConfig` defines static game setup data consumed during `init()`.

## Base responsibilities

- Provide object blueprints via `gameObjects()`.
- Provide team alias map via `teams()`.
- Provide subsystem definitions via:
  - `teamDefinitions()`
  - `fieldDefinitions()`
  - `timerDefinitions()`
  - `roleGroupDefinitions()`
- Validate all setup constraints in `validate()`.

## Current required constraints

- `maxPlayer() > 0`
- `teams()` must contain `spectator` mapped to `myulib:spectator`
- optional `ghost` alias must map to same id as spectator
- at least one playable non-spectator team is required

## Subclass checklist

- Implement `gameObjects()`.
- Usually override `teams()` with playable team aliases.
- Usually override `additionalTeamDefinitions()` to define real teams.
- Override subsystem definition lists when your game needs them.

