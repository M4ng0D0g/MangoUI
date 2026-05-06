# ECS API
Public ECS Java types:
- `com.myudog.myulib.api.ecs.IComponent`
- `com.myudog.myulib.api.ecs.EcsContainer`
- `com.myudog.myulib.internal.ecs.ComponentStorage`
- `com.myudog.myulib.api.events.ComponentAddedEvent`
- `com.myudog.myulib.api.ecs.lifecycle.Resettable`
- `com.myudog.myulib.api.ecs.lifecycle.DimensionAware`
- `com.myudog.myulib.api.ecs.lifecycle.DimensionChangePolicy`
- `com.myudog.myulib.api.ecs.lifecycle.ComponentLifecycle`
## Quick example
```java
EcsContainer container = new EcsContainer();
int entity = world.createEntity();
world.addComponent(entity, new TransformComponent());
```