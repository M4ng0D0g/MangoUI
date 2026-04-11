# Respawn timer example
This example shows how to build a countdown timer and its payload with the current timer API.
## Public helpers
- `timer()` returns a countdown timer that triggers a remaining-tick callback.
- `payload(UUID, boolean)` builds a respawn payload.
## Example
```java
TimerModels.Timer timer = RespawnTimerExample.timer();
TimerModels.RespawnTimerPayload payload = RespawnTimerExample.payload(playerId, true);
```
