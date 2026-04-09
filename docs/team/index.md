# Team System

`Team` is now an independent system.
Team entries can be scoped by game and use namespaced IDs such as `myulib:gameId:teamId`.

## Public API
- `com.myudog.myulib.api.team.TeamDefinition`
- `com.myudog.myulib.api.team.TeamManager`
- `com.myudog.myulib.api.team.TeamAdminService`

## Notes
- Use it for gameplay team membership.
- Use `TeamManager.register(gameId, team)` to create a game-scoped team.
- Use `TeamManager.all(gameId)` / `TeamManager.snapshot(gameId)` to inspect a game's team collection.
- Use `TeamManager.forEachMember(teamId, action)` to apply batch operations to a whole team.
- Use `TeamManager.unregisterGame(gameId)` or `TeamAdminService.deleteGameTeams(gameId)` when cleaning up a game.
- `GameDefinition` no longer owns team construction; register teams directly with `TeamManager`.
- Use `TeamAdminService.openEditor(...)` to request a configuration UI.

