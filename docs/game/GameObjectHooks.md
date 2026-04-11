# Legacy game object hooks note
The current source tree does not expose a standalone object hooks type.
Use the current game, timer, and ECS systems to wire object-related behavior.
## Use these current types instead
- `GameManager`
- `GameDefinition`
- `GameInstance`
- `GameObjectConfig`
- `GameObjectKind`
- `EcsWorld`
