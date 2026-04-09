# Identity System (legacy)

`Identity` is the legacy compatibility name for the player grouping system.
The canonical documentation is now `docs/rolegroup/index.md`.

## Legacy API
- `com.myudog.myulib.api.identity.IdentityGroupDefinition`
- `com.myudog.myulib.api.identity.IdentityManager`
- `com.myudog.myulib.api.identity.IdentityAdminService`

## Notes
- Existing code may continue to use the `identity` package.
- New documentation and terminology should prefer `RoleGroup`.
- A player may belong to multiple groups.
- Groups can carry permission grants.
- Use `IdentityAdminService.openEditor(...)` to request a configuration UI.

