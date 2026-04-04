# Timer 系統使用說明（完整參考）

本文件說明 `game/timer` 子系統的設計、公開 API、事件模型、payload 用法，以及如何用它實作玩家復活倒數、音效提醒、技能冷卻等遊戲邏輯。

## 目錄
- 概要
- 設計原則
- 主要類別與職責
- 事件與刻度綁定
- payload 與資料傳遞
- 常見使用範例
- 與 ECS / Server Tick 的整合
- 最佳實務
- FAQ

---

## 概要

Timer 系統是建立在現有 ECS 與事件匯流排之上的遊戲基礎能力。它採用 20 TPS 的伺服器 tick 作為時間基準，並把「共享的 timer 定義」與「執行中的 timer 狀態」拆開，以降低記憶體重複與提高可擴充性。

### 核心特性
- 支援正計時與倒數計時
- 支援暫停、恢復、停止、重製、查詢
- 支援在指定 tick 綁定事件
- 事件可讀取完整 timer 狀態與任意 payload
- 可用於玩家復活、音效提示、冷卻時間、階段切換等邏輯

---

## 設計原則

### 1) 享元模式
`Timer` 是共享定義：
- duration
- mode
- checkpoint bindings
- lifecycle callbacks

`TimerInstance` 是執行狀態：
- current status
- elapsed ticks
- owner entity
- payload
- pause / stop state

這樣可以讓多個 timer instance 共用同一份規則，不會每個實例都複製大量定義資料。

### 2) 邏輯層與商業層分離
Timer 系統只負責：
- 計時
- 派發事件
- 維護狀態

真正的商業邏輯，例如：
- 玩家是否可復活
- 是否要傳送到某個世界
- 是否播音效
- 是否給獎勵

都應該由外部 callback 或業務 service 決定。

### 3) 事件能讀取狀態與任意資料
每個 timer event 都會收到 `TimerSnapshot`，其中包含：
- timer 定義
- timer instance 狀態
- elapsed / remaining ticks
- owner entity
- payload

因此你的 callback 可以根據當下狀態做條件判斷，而不是只能看 tick 數字。

---

## 主要類別與職責

### `TimerManager`
路徑：`com.myudog.myulib.api.game.timer.TimerManager`

對外入口，負責：
- 註冊 timer 定義
- 建立 timer instance
- 查詢狀態
- 暫停 / 恢復 / 停止 / 重製
- 被 `Game.init()` 自動安裝到伺服器 tick

### `Timer`
共享定義物件，包含：
- `id`
- `durationTicks`
- `mode`
- `elapsedBindings`
- `remainingBindings`
- started / paused / resumed / reset / stopped / completed callbacks

### `TimerInstance`
執行狀態物件，通常會被存進 ECS：
- `timerId`
- `ownerEntityId`
- `payload`
- `status`
- `elapsedTicks`
- `pausedTicks`

### `TimerSnapshot`
事件快照，提供事件回呼讀取：
- `status`
- `elapsedTicks`
- `remainingTicks`
- `payload`
- `progress`

### `TimerPayload`
資料慣例 marker，用來標示 payload 類型。

### 範例 payload
- `RespawnTimerPayload`
- `SoundTimerPayload`

---

## 事件與刻度綁定

### Timer 生命週期事件
- `TimerStartedEvent`
- `TimerPausedEvent`
- `TimerResumedEvent`
- `TimerResetEvent`
- `TimerStoppedEvent`
- `TimerTickEvent`
- `TimerCheckpointEvent`
- `TimerCompletedEvent`

### 刻度綁定方式
目前採用：
- `elapsedBindings: Map<Int, List<TimerBinding>>`
- `remainingBindings: Map<Int, List<TimerBinding>>`

這比單純 `Map<tick, List<Function>>` 更實用，因為每個 binding 都有：
- `id`
- `tick`
- `basis`（elapsed / remaining）
- callback

#### 為什麼不直接暴露 raw map？
因為之後你若要：
- 取消某個 binding
- 讓某個 binding 只觸發一次
- 做條件判斷
- 改成 bucket wheel / ring buffer

會比較容易維護。

---

## payload 與資料傳遞

事件回呼可直接從快照讀 payload：

```kotlin
val payload = snapshot.payloadAs<RespawnTimerPayload>() ?: return
```

或者：

```kotlin
val payload = snapshot.requirePayload<SoundTimerPayload>()
```

### 建議用法
- 玩家復活 timer：payload 放 `playerUuid`、`respawnWorld`、`allowSkip`
- 音效 timer：payload 放 `soundId`、`volume`、`pitch`
- 技能冷卻 timer：payload 放 `skillId`、`casterId`、`cooldownGroup`

---

## 常見使用範例

### 1) 註冊一個倒數復活 timer

```kotlin
val timerId = Identifier("myulib", "respawn_timer")

RespawnTimerExample.registerRespawnTimer(
    timerId = timerId,
    durationTicks = 20 * 5,
    onWarning = { snapshot, payload ->
        // 例如：顯示剩餘 3 秒 / 1 秒的提示
        println("Respawn warning: ${payload.playerUuid}, remaining=${snapshot.remainingTicks}")
    },
    onRespawn = { snapshot, payload ->
        // 這裡交給你的商業邏輯：檢查玩家狀態、決定是否重生
        println("Respawn now: ${payload.playerUuid}")
    }
)
```

### 2) 為某位玩家建立實例

```kotlin
val instanceId = RespawnTimerExample.spawnRespawnTimer(
    timerId = timerId,
    ownerEntityId = playerEntityId,
    payload = RespawnTimerPayload(
        playerUuid = player.uuid,
        respawnReason = "death",
        allowSkip = false
    )
)
```

### 3) 播放音效提醒

```kotlin
val soundTimerId = Identifier("myulib", "sound_cue")

RespawnTimerExample.registerSoundCueTimer(
    timerId = soundTimerId,
    durationTicks = 20 * 3,
) { snapshot, payload ->
    println("Play sound ${payload.soundId} at remaining=${snapshot.remainingTicks}")
}
```

### 4) 查詢 timer 狀態

```kotlin
val snapshot = TimerManager.getSnapshot(instanceId)
if (snapshot != null && snapshot.isCompleted()) {
    println("Timer finished for ${snapshot.payload}")
}
```

### 5) 暫停 / 恢復 / 重製

```kotlin
TimerManager.pause(instanceId)
TimerManager.resume(instanceId)
TimerManager.reset(instanceId)
```

---

## 與 ECS / Server Tick 的整合

### 啟動流程
- `Myulib.onInitialize()` 會呼叫 `Game.init()`
- `Game.init()` 會註冊 timer manager
- `TimerManager.install()` 會把 timer 更新掛到 server tick
- 每一個 server tick，timer 會自動更新一次

### 為什麼這樣設計？
這樣 timer 不需要自己維護獨立執行緒，也不需要額外的 scheduler。它直接跟遊戲 tick 同步，最適合 Minecraft 的節奏。

---

## 最佳實務

- **timer 定義與 timer 實例分離**：定義重複使用，實例只放狀態
- **payload 保持純資料**：不要把大量遊戲邏輯塞進 payload
- **事件回呼保持短小**：重工作交給外層 service
- **多 player timer 用 ownerEntityId 分組**：方便查詢與清理
- **需要取消事件時，之後可改用 binding id 做精準管理**

---

## FAQ

### Q1：事件能讀取 timer 狀態嗎？
可以。每個事件都會攜帶 `TimerSnapshot`。

### Q2：不同 timer 可以帶不同參數嗎？
可以。payload 是 `Any?`，可以放 `RespawnTimerPayload`、`SoundTimerPayload` 或你自訂的資料類別。

### Q3：要做玩家復活時，應該把邏輯寫在哪裡？
建議寫在 `onRespawn` callback 或你自己的 business service，不要直接塞進 timer core。

### Q4：可以用來做 cooldown 嗎？
可以。把它當成 count-down timer，完成時發出 `TimerCompletedEvent` 即可。

---

## 源碼導航
- `src/main/kotlin/com/myudog/myulib/api/game/Game.kt`
- `src/main/kotlin/com/myudog/myulib/api/game/timer/TimerModels.kt`
- `src/main/kotlin/com/myudog/myulib/api/game/timer/TimerEvents.kt`
- `src/main/kotlin/com/myudog/myulib/api/game/timer/TimerManager.kt`
- `src/main/kotlin/com/myudog/myulib/api/game/timer/TimerPayloads.kt`
- `src/main/kotlin/com/myudog/myulib/api/game/timer/RespawnTimerExample.kt`

如果你要，我下一步可以把這份文件再拆成「TimerManager / TimerEvents / Payload / Examples」四頁，並在 `ECS.md`、`Events.md` 裡加入更完整的導覽連結。

