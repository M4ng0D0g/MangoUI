# Game 系統核心 API 參考

本頁整理 `Game`、`GameManager`、`GameDefinition`、`GameStateMachine`、`GameStateContext`、`GameTransition` 與 `GameInstance` 的 Java API。

## Game

`Game` 是模組初始化入口，負責遊戲流程核心；`team` / `timer` / `object` 與 access 類系統請看各自的獨立文件。

### 方法
- `init()`：註冊 `GameManager`、`ComponentManager`、`TimerManager`。

### 用法
```java
Game.init();
```

## GameManager

`GameManager` 是遊戲定義與實例的中央註冊表。

### 方法
- `install()`
- `register(definition)` / `unregister(gameId)`
- `hasDefinition(gameId)` / `definition(gameId)`
- `createInstance(gameId, config)`
- `getInstance(instanceId)` / `getInstances()` / `getInstances(gameId)`
- `destroyInstance(instanceId)`
- `transition(instanceId, to)`
- `tickAll()`

### 用法
```java
GameManager.register(new RespawnGameExample());
var instance = GameManager.createInstance(
    Identifier.of("myulib", "respawn_game"),
    new GameBootstrapConfig()
);
```

## GameStateContracts

- `GameStateContext<S>`：state 轉移上下文，包含 `gameId`、`instanceId`、`from`、`to`
- `GameTransition<S>`：描述一次狀態轉移是否允許
- `GameStateMachine<S>`：狀態機介面，提供 `getCurrentState()`、`canTransition()`、`transition()`、`reset()`
- `GameDefinition<S>`：只定義遊戲 id、初始狀態、允許轉移與 `createContext(...)`；不再負責模板式 feature / logic / component 裝配

## GameInstance

`GameInstance<S>` 位於 `com.myudog.myulib.api.game.instance`，是實際執行中的遊戲容器。

### 主要方法
- `getInstanceId()` / `getDefinition()` / `getBootstrapConfig()`
- `getSpecialObjects()` / `getFeatures()` / `isEnabled()` / `getCurrentState()` / `getTickCount()`
- `feature(type)` / `requireFeature(type)` / `putFeature(feature)` / `removeFeature(type)` / `getFeatureOrCreate(type)`
- `timers()` / `scoreboard()` / `objectBindings()` / `regions()` / `components()` / `logicOrNull()` / `logic()`
- `canTransition(to)` / `transition(to)` / `transitionUnsafe(to)` / `resetState()`
- `tick()` / `destroy()`
- `requireSpecialObject(id)` / `hasSpecialObject(id)`

### 用法
```java
GameInstance<?> instance = GameManager.createInstance(
    Identifier.of("myulib", "respawn_game"),
    new GameBootstrapConfig()
);
instance.transition(RespawnGameExample.RespawnGameState.COUNTDOWN);
```

