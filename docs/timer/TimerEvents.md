# Timer events
`TimerEvents` contains the event records emitted from timer lifecycle changes.
## Event records
- `TimerStartedEvent`
- `TimerPausedEvent`
- `TimerResumedEvent`
- `TimerResetEvent`
- `TimerStoppedEvent`
- `TimerTickEvent`
- `TimerCheckpointEvent`
- `TimerCompletedEvent`
## Notes
- Each record wraps a `TimerModels.TimerSnapshot`.
