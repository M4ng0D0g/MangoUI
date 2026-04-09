# Access Systems

This index covers the independent access-related systems introduced by MyuLib.

## Systems
- [Field](../field/index.md)
- [RoleGroup](../rolegroup/index.md)
- [Identity](../identity/index.md) (legacy)
- [Permission](../permission/index.md)
- [Team](../team/index.md)

## Responsibilities
- `Field`: long-lived world regions / areas.
- `RoleGroup`: player grouping and shared permission templates.
- `Identity`: legacy compatibility name for `RoleGroup`.
- `Permission`: layered allow / deny / pass decision engine.
- `Team`: gameplay team membership, isolated from core game logic.

## UI entry
Use `ConfigurationUiBridge` to open the editor for the target system from a command or an in-game action.
