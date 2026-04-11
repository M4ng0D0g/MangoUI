# Timer System
The timer subsystem is currently provided by `TimerManager`, `TimerModels`, `TimerEvents`, `TimerPayloads`, and `RespawnTimerExample`.
## Current entry points
- `TimerManager`
- `TimerModels`
- `TimerEvents`
- `TimerPayloads`
- `RespawnTimerExample`
## Notes
- `TimerModels` contains the nested timer enums, records, and instance class.
- `TimerPayloads` is currently a namespace holder; the concrete payload records live in `TimerModels`.
- Timer instances are updated through `TimerManager.update(...)`.
