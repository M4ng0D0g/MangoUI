# GameConfig
`GameConfig` is a small interface that supplies validation hooks, object blueprints, and metadata for `GameManager.createInstance(...)`.
## Public methods
- `validate()`
- `gameObjects()`
- `metadata()`
- `empty()`
## Notes
- `gameObjects()` returns `List<GameObjectConfig<?>>`.
- `metadata()` returns a string map used for simple configuration values.
