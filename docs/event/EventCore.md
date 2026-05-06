# EventCore
## Role
This page is the canonical reference for `EventCore` in the `event` docs area.
## Unique responsibility
It documents the public API, intent, and practical usage of this class without mixing in unrelated systems.
## Practical writing
Use this page when you need the class-level contract, then follow the field and method sections below.
## Fields
- Fields are listed in the existing API content below.
## Methods
- Methods are listed in the existing API content below.
Event API
Public event-layer Java types:
- `com.myudog.myulib.api.event.IEvent`
- `com.myudog.myulib.api.event.IFailableEvent`
- `com.myudog.myulib.api.event.EventPriority`
- `com.myudog.myulib.api.event.ProcessResult`
- `com.myudog.myulib.api.event.IEventListener`
- `com.myudog.myulib.api.event.EventBus`
- `com.myudog.myulib.api.event.ServerEventBus`
- `com.myudog.myulib.api.events.EntitySpawnEvent`
- `com.myudog.myulib.api.events.ServerTickEvent`
- `com.myudog.myulib.api.events.ComponentAddedEvent`
## Quick example
```java
EventDispatcherImpl dispatcher = new EventDispatcherImpl();
dispatcher.subscribe(ServerTickEvent.class, event -> ProcessResult.PASS);
```