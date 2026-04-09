# GameStateContracts API 參考

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
- `GameDefinition` 不再承擔 `createXxx()` 模板式裝配；runtime 的組裝由 context 與對應系統入口負責。

## 用法
```java
GameDefinition<RespawnGameExample.RespawnGameState> def = new RespawnGameExample();
```
