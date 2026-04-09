# Permission System

`Permission` evaluates access with three decision states:

- `ALLOW`
- `UNSET`
- `DENY`

## Resolution order
1. Player-level overrides
2. RoleGroup grants / overrides
3. Field scope
4. Dimension scope
5. World scope

A higher-priority scope may override a lower-priority one.
`UNSET` means the current scope has no rule and evaluation should continue.

## Interception flow
The intended top-level interception path is:

1. A mixin or gameplay hook captures the interaction before the action is applied.
2. The hook builds a `WorldInteractionPermissionContext`.
3. The hook passes that context to `WorldInteractionPermissionHooks.evaluate(...)` or `isDenied(...)`.
4. `WorldInteractionPermissionHooks` converts it into a generic `PermissionContext`.
5. `PermissionManager.evaluate(...)` resolves the decision across player, `RoleGroup`, `Field`, `Dimension`, and `World` scopes.

`DENY` must stop the flow immediately.
`UNSET` means the current scope had no applicable rule.
`ALLOW` grants the action and ends evaluation.

## Action set
- `BLOCK_PLACE`
- `BLOCK_BREAK`
- `ATTACK_PLAYER`
- `ATTACK_FRIENDLY_MOB`
- `ATTACK_HOSTILE_MOB`
- `USE_PROJECTILE`
- `USE_ITEM`
- `INTERACT_BLOCK`
- `INTERACT_ENTITY`
- `TRIGGER_REDSTONE`
- `USE_PORTAL`
- `SEND_MESSAGE`

## Public API
- `PermissionDecision`
- `PermissionLayer`
- `PermissionAction`
- `PermissionGrant`
- `PermissionSeed`
- `PermissionContext`
- `WorldInteractionPermissionContext`
- `PermissionManager`
- `PermissionAdminService`
- `WorldInteractionPermissionHooks`

## Notes
- `DENY` should short-circuit the world-interaction flow.
- `UNSET` delegates to the next layer.
- Role groups are managed in the `RoleGroup` system and contribute to permission resolution.
