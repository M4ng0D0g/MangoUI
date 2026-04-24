# GameDefinition

`GameDefinition<C, D, S>` is the extension point where subclasses define how a game is assembled.

## Methods you implement

- `createInitialData(C config)`
  - Called during `GameInstance.init()`.
  - Return your data subclass.
- `createStateMachine(C config)`
  - Define legal state transitions.
- `createEventBus()`
  - Return room-local event bus implementation.

## Optional hooks

- `gameBehaviors()`
  - Return `GameBehavior` list; each behavior is auto-bound at init and auto-unbound at clean.
- `resolveTeamForJoin(...)`
  - Customize join-time team assignment.
- `onStart(instance)`
  - Called when start is triggered after successful init.
- `onEnd(instance)`
  - Called when end is triggered before clean.

## Deprecated bridge

- `bindBehaviors(instance)` is compatibility-only.
- Prefer `gameBehaviors()` for new code.

## Event bus binding guidance

Define listeners in behaviors (`onBind`) or in `onStart`.
Do not manually clear listeners in `onEnd`; instance clean phase clears the event bus.

