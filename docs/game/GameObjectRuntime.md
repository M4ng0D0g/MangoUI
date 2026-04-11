# Legacy game object runtime note
The current source tree does not expose a standalone object runtime layer.
Game-object behavior should be implemented through the surrounding game systems and events.
## Use these current types instead
- `GameObjectConfig`
- `GameObjectKind`
- `GameInstance`
- `GameStateChangeEvent`
