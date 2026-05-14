# Game 系統 (GameManager & GameInstance)

## GameManager

### 方法列表
- `register(GameDefinition<?, ?, ?> definition)`: 註冊遊戲定義。
- `getDefinition(Identifier id)`: 根據 Identifier 獲取遊戲定義。
- `createInstance(Identifier definitionId, String instanceId)`: 建立遊戲實例。
- `getInstance(String instanceId)`: 根據 ID 獲取遊戲實例。
- `getInstances()`: 獲取所有運行中的遊戲實例。
- `initInstance(String instanceId)`: 初始化遊戲實例。
- `startInstance(String instanceId)`: 啟動遊戲實例。
- `shutdownInstance(String instanceId)`: 關閉遊戲實例。
- `deleteInstance(String instanceId)`: 刪除遊戲實例。
- `joinPlayer(String instanceId, UUID playerUuid, @Nullable UUID teamUuid)`: 玩家加入遊戲。
- `leavePlayer(String instanceId, UUID playerUuid)`: 玩家離開遊戲。
- `leaveAllInstances(UUID playerUuid)`: 玩家離開所有遊戲。
- `onShutDown()`: 伺服器關閉時的清理工作。

## GameInstance

### 方法列表
- `getUuid()`: 獲取實例 UUID。
- `getInstanceId()`: 獲取實例自定義 ID (String)。
- `getConfig()`: 獲取遊戲配置。
- `getData()`: 獲取遊戲數據。
- `getEventBus()`: 獲取事件匯流排。
- `getLevel()`: 獲取所在世界。
- `getDefinition()`: 獲取所屬定義。
- `isInitialized()`: 是否已初始化。
- `isStarted()`: 是否已啟動。
- `getCurrentState()`: 獲取當前狀態。
- `initialize()`: 執行初始化邏輯（驗證 Config，建立 Data）。
- `start()`: 啟動遊戲。
- `shutdown()`: 關閉遊戲。
- `destroy()`: 銷毀實例。
- `transition(S to)`: 狀態切換。
- `forceTransition(S to)`: 強制狀態切換。
- `joinPlayer(UUID playerId, @Nullable UUID teamUuid)`: 處理玩家加入。
- `leavePlayer(UUID playerId)`: 處理玩家離開。
- `bindTimer(TimerDefinition timer)`: 綁定計時器到當前實例。

## GameDefinition

### 方法列表
- `id()`: 獲取定義 Identifier。
- `createConfig()`: 建立預設 Config。
- `createInitialData(C config)`: 根據配置建立初始 Data。
- `createStateMachine(C config)`: 建立狀態機。
- `onStart(GameInstance instance)`: 遊戲啟動回呼。
- `onShutdown(GameInstance instance)`: 遊戲關閉回呼。

## GameData

### 方法列表
- `getEcsContainer()`: 獲取 ECS 容器。
- `bindEntity(UUID mcEntityUuid, int ecsEntityId)`: 綁定 Minecraft 生物到 ECS 實體。
- `getEcsEntity(UUID mcEntityUuid)`: 獲取綁定的 ECS 實體 ID。

## GameConfig

### 方法列表
- `validate()`: 驗證配置合法性。
- `getVariables()`: 獲取可配置的變數列表（用於指令系統）。
- `setVariable(String key, String value)`: 設定變數值。
