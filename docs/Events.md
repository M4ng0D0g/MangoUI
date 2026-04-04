# 事件系統使用說明（完整參考）

本文檔提供專案內事件匯流排（Event Dispatcher）系統的完整參考，並示範如何在 world 範圍或全域範圍註冊與發佈事件。

目錄
- 概覽
- 主要類別與函式
- 使用範例：註冊、發佈、處理
- 常見事件（ComponentAddedEvent、Timer 事件）與使用情境
- 進階話題：同步/非同步、優先權、解除註冊
- 除錯建議

---

概覽

專案採用簡單的事件匯流排（`EventDispatcherImpl`），設計為能在 `EcsWorld` 的 `eventBus` 中廣播事件。事件通常以 Kotlin data class 的形式定義，系統可訂閱事件類型以接收通告。

主要類別與函式

- `EventDispatcherImpl`（內部實作）
  - `register(type: KClass<T>, handler: (T) -> Unit)` — 註冊一個事件類型的處理函式
  - `dispatch(event: Any)` — 發佈事件，會同步通知所有註冊的處理器

- `EcsWorld.eventBus` — 每個 world 有一個本地的 event bus，用於與該 world 範圍相關的事件通訊。

使用範例

1) 註冊監聽器

```kotlin
world.eventBus.register(MyCustomEvent::class) { evt ->
    // 處理事件
    println("Got event: ${evt}")
}
```

2) 發佈事件

```kotlin
world.eventBus.dispatch(MyCustomEvent("payload"))
```

3) 典型場景：系統間解耦（SpawnParticleEvent）

- 遊戲邏輯層發佈 `SpawnParticleEvent`
- `ParticleSystem` 在自身初始化時註冊該事件並在收到事件時實際產生粒子

```kotlin
// 在 ParticleSystem 初始化時
world.eventBus.register(SpawnParticleEvent::class) { e -> ParticleSystem.spawn(e.data) }

// 在其他地方
world.eventBus.dispatch(SpawnParticleEvent(ParticleData(...)))
```

常見事件

- `ComponentAddedEvent`：`EcsWorld.addComponent` 會在加入 component 後自動派發這個事件，payload 包含 `entityId` 與 `component`。
- `TimerStartedEvent`：timer 開始時觸發，包含 `TimerSnapshot`。
- `TimerPausedEvent`：timer 暫停時觸發。
- `TimerResumedEvent`：timer 恢復時觸發。
- `TimerResetEvent`：timer 重製時觸發。
- `TimerStoppedEvent`：timer 停止時觸發。
- `TimerTickEvent`：每次 timer tick 更新時觸發。
- `TimerCheckpointEvent`：命中特定 tick（elapsed 或 remaining）時觸發。
- `TimerCompletedEvent`：timer 完成時觸發。

### Timer 事件文件
如果你要的是 timer 的完整 API 與 payload 範例，請看：

- `docs/systems/TimerSystem.md`

進階話題

- 同步 vs 非同步：目前 `EventDispatcherImpl` 為同步呼叫（handler 會在 `dispatch` 中被一一執行）。若 handler 內執行重負載工作，請自行將耗時工作移交至 worker/thread pool。
- 優先權：目前若需優先權處理，建議事件管理層再包一層或在註冊時管理 handler 的排序。
- 解除註冊：事件系統應提供解除註冊的機制（例如回傳 token 或 handler 引用），如果目前沒有，建議實作以避免記憶體洩漏。

除錯建議

- 若 handler 沒有被呼叫，檢查：
  - 是否註冊在正確的 `eventBus`（不同 world 有不同 eventBus）
  - 事件類型是否匹配
  - 是否有 exception 被拋出但被吞掉（在 dispatch 中增加錯誤日誌）

---

若你需要，我可以：
- 生成事件類型清單（從 `src` 掃描 `event` package）並把每個事件的參數/用途寫在文件中
- 實作或補充 `EventDispatcherImpl` 的解除註冊/優先權支援（若你想要）
