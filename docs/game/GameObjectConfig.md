# GameObjectConfig
`GameObjectConfig<T extends Entity>` describes an object blueprint in the current game API.
## Fields
- `id`: unique identifier.
- `entityType`: the entity type to spawn or bind.
- `kind`: gameplay classification.
- `properties`: immutable custom properties.
## Behavior
- `id` and `entityType` are required.
- `kind` defaults to `CUSTOM`.
- `properties` is copied into an immutable map.
