# GameData

`GameData` is per-instance mutable runtime state.

## Base responsibilities

- Keep session identity via `setupId(...)` / `getId()`.
- Maintain participant -> ECS entity O(1) map:
  - `addParticipant(UUID, int)`
  - `getParticipantEntity(UUID)`
  - `removeParticipant(UUID)`
- Track runtime object copies and registered subsystem resources.
- Manage team member sets and team migration rules.
- Perform cleanup in `reset(instance)`.

## Team behavior provided by base class

- Keeps default spectator/ghost handling through config team map.
- Supports team switching while respecting team limits.
- Prevents removing spectator team.

## Subclass checklist

- Add game-specific runtime fields (scores, rounds, win condition state).
- Bind ECS components/entities when players or mobs enter the game.
- Keep game-specific cleanup logic in fields that are reset-safe.

