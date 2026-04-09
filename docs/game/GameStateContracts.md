# GameStateContracts
## Role
This page is the canonical reference for `GameStateContracts` in the `game` docs area.
## Unique responsibility
It documents the public API, intent, and practical usage of this class without mixing in unrelated systems.
## Practical writing
Use this page when you need the class-level contract, then follow the field and method sections below.
## Fields
- Fields are listed in the existing API content below.
## Methods
- Methods are listed in the existing API content below.
GameStateContracts API 參考

本頁集中說明 `GameStateContext`、`GameTransition`、`GameStateMachine` 與 `GameDefinition`。

## GameStateContext<S>
- 欄位：`gameId`, `instanceId`, `from`, `to`
- 用途：描述一次 state transition 的上下文

## GameTransition<S>
- 欄位：`from`, `to`, `allowed`
- 用途：描述一次狀態轉移

## GameStateMachine<S>
- `getCurrentState()`
- `canTransition(to)`
- `transition(to)`
- `reset()`

## GameDefinition<S>
`com.myudog.myulib.api.game.GameDefinition` 是 `com.myudog.myulib.api.game.state.GameDefinition` 的相容包裝。

### 主要方法
- `getId()`
- `getInitialState()`
- `getAllowedTransitions()`
- `createContext(...)`

### 補充
- `isTransitionAllowed(from, to)`：根據 `allowedTransitions` 判斷是否可轉移
- `GameDefinition` 不再承擔 `createXxx()` 模板式裝配；遊戲自己的 runtime 組裝應交給 context 或對應的系統入口。

## 用法
```java
GameDefinition<RespawnGameExample.RespawnGameState> def = new RespawnGameExample();
```
