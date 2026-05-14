# Debug System

Public package: `com.myudog.myulib.api.core.debug`

This area documents debug feature flags, tracing, and runtime diagnostics hooks.

## Runtime logging
- Enable: `/myulib:debug on`
- Disable: `/myulib:debug off`
- Per feature: `/myulib:debug feature <name> on|off`
- Toggle all features: `/myulib:debug all on|off`
- Status: `/myulib:debug status`

## Features
Feature tokens are lowercase and match `DebugFeature.token()`.

- `permission`, `field`, `rolegroup`, `team`, `game`
- `timer`, `control`, `camera`, `command`
- `animation`, `ecs`, `event`, `effect`, `hologram`, `object`, `util`

