# RoleGroup System

`RoleGroup` is the formal player grouping system used by MyuLib.
It is the canonical name for player grouping and permission templates.
RoleGroup data is stored per-world and persists with the save.
In the current implementation, the canonical storage backend is a world-save/NBT implementation and the data is serialized under the world/server save root as `myulib/rolegroups.dat`.

## Relationship to permission
- A player may have direct permission overrides.
- A player may also inherit permission grants from one or more role groups.
- Role groups are evaluated as part of the broader permission resolution pipeline.

## Management entry points
- Create, update, delete, get, and list are exposed through `RoleGroupManager` and `RoleGroupAdminService`.
- Command-based CRUD entry points are available through the `rolegroup.*` commands.
- The legacy `identity/` package remains available as a compatibility alias.

## Public API
- `com.myudog.myulib.api.rolegroup.RoleGroupDefinition`
- `com.myudog.myulib.api.rolegroup.RoleGroupManager`
- `com.myudog.myulib.api.rolegroup.RoleGroupAdminService`

## Notes
- A player may belong to multiple role groups.
- Role groups can carry permission grants and metadata.
- Use `RoleGroupAdminService.openEditor(...)` to request the configuration UI.
- `identity/` is retained as a compatibility alias for older code.
