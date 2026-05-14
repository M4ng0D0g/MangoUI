# Control System

`Control` provides player-to-entity possession mapping and input buffering.

## Public API
- `com.myudog.myulib.api.core.control.ControlManager`
- `com.myudog.myulib.api.core.control.network.ControlInputPayload`
- `com.myudog.myulib.api.core.control.network.ServerControlNetworking`

## Notes
- Mapping is one-to-one (`player -> entity`, `entity -> player`).
- Last input packets are buffered per controlled entity.
- Current networking registration is a placeholder until payload codec migration is completed.

## In-game test command
- `/myulib:control status`

