# Component / ECS overview
Current component behavior is modeled through ECS.
A thin compatibility layer also exists under `com.myudog.myulib.api.game.components` for internal bridge use.
## Current entry points
- `docs/ecs/index.md`
- `com.myudog.myulib.api.ecs.EcsWorld`
- `com.myudog.myulib.api.ecs.Component`
- `com.myudog.myulib.api.ecs.event.ComponentAddedEvent`
- `com.myudog.myulib.api.ecs.lifecycle.ComponentLifecycle`
- `com.myudog.myulib.api.ecs.lifecycle.Resettable`
- `com.myudog.myulib.api.ecs.lifecycle.DimensionAware`
- `com.myudog.myulib.api.ecs.lifecycle.DimensionChangePolicy`
## Notes
- Prefer the ECS docs for the actual public model.
- The `game.components` compatibility package is intentionally minimal.
