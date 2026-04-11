# Timer manager
`TimerManager` is the registry and runtime controller for timer definitions and timer instances.
## Public methods
- `install()`
- `register(timer)` / `unregister(timerId)` / `has(timerId)`
- `createInstance(timerId, ownerEntityId, payload, autoStart, level)`
- `getInstance(timerEntityId, level)` / `getSnapshot(timerEntityId)` / `findInstances(ownerEntityId, level)`
- `isRunning(timerEntityId, level)` / `isPaused(timerEntityId, level)` / `isStopped(timerEntityId, level)` / `isCompleted(timerEntityId, level)`
- `start(timerEntityId)` / `pause(timerEntityId)` / `resume(timerEntityId)` / `stop(timerEntityId)`
- `reset(timerEntityId, clearPayload)`
- `setElapsedTicks(timerEntityId, ticks)` / `setRemainingTicks(timerEntityId, ticks)`
- `setPayload(timerEntityId, payload)`
- `update(level)`
