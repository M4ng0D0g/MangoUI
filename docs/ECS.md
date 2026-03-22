# ECS 系統使用說明（完整參考）

本文檔提供專案內 ECS（Entity-Component-System）子系統的詳細參考，目標讀者為想使用或擴充 ECS、撰寫 system、或整合 UI/事件系統的開發者。

目錄
- 概要
- 關鍵概念
- 主要類別與 API 參考
  - EcsWorld
  - Component（約定）
  - ComponentStorage（內部實作要點）
  - 事件（ComponentAddedEvent、World.eventBus）
- 常見使用情境與完整範例
  - 創建 World 與 Entity
  - 新增/取得/查詢 Component
  - 撰寫 System 的範例
- 進階：生命週期、維度政策與重置策略
- 最佳實務與效能建議
- 常見問題與排錯（FAQ）

---

概要

ECS 是把資料（Components）與邏輯（Systems）分離的架構。在本專案中，ECS 用於驅動 UI、互動、拖放、粒子等子系統。每個 `EcsWorld` 同時提供一個本地事件匯流排 (`eventBus`)，方便系統內部或外部模組以事件方式溝通。

關鍵概念

- Entity：單純的整數 ID（由 `EcsWorld.createEntity()` 產生），代表一個遊戲物件或 UI 元件實例。
- Component：純資料容器（例如 TransformComponent、WidgetStateComponent），不應包含大量行為。
- System：在每個遊戲循環或特定時機讀取 component 並進行邏輯處理（例如 LayoutSystem、RenderSystem、InputSystem）。

主要類別與 API 參考

EcsWorld
- 路徑：`com.myudog.myulib.api.ecs.EcsWorld`
- 重要屬性：
  - `val eventBus` — 用於在 world 範圍內廣播/監聽事件（`EventDispatcherImpl`）。
- 重要方法：
  - `fun createEntity(): Int` — 建立並回傳新的 entity id。
  - `inline fun <reified T : Component> addComponent(entityId: Int, component: T)` — 使用 reified 泛型，將 component 加入對應的 `ComponentStorage`。
  - `inline fun <reified T : Component> getComponent(entityId: Int): T?` — 取得實體資料。
  - `fun <T : Component> getComponent(entityId: Int, type: KClass<T>): T?` — 類型版本（供 Java 互作或反射使用）。
  - `fun query(type: KClass<out Component>): List<Int>` — 回傳所有擁有該 component 的實體。
  - `fun destroyEntity(entityId: Int)` — 刪除實體與其 components。
  - `fun resetEntity(entityId: Int)` — 對所有實作 `Resettable` 的 component 呼叫 reset（軟重置）。
  - `fun processDimensionChange(entityId: Int)` — 根據 component 的 `DimensionAware` 策略處理維度切換。

約定：Component
- Component 類型在專案中多會以資料類（data class 或 class）定義，應避免在 component 中放置複雜行為。
- 若 component 需要在加入時被初始化，系統可監聽 `ComponentAddedEvent` 來執行初始化步驟。

ComponentStorage（內部）
- 實作位置：`com.myudog.myulib.internal.ecs.ComponentStorage`
- 要點：
  - 使用稠密陣列（dense array）保存 component，支援快速索引與遍歷。
  - 提供 `add(entityId, component)`, `remove(entityId)`, `get(entityId)` 等操作。

事件（Event）
- 每次 `addComponent` 都會觸發 `ComponentAddedEvent`，可在系統中接收以進行延後初始化或觸發相關流程。
- `EcsWorld.eventBus` 提供 `register` / `dispatch` 介面（參見 Events.md 更詳內容）。

常見使用情境與完整範例

1) 建立 World、Entity、加入 Component

```kotlin
val world = EcsWorld()
val e = world.createEntity()
world.addComponent(e, TransformComponent().apply { x = 10f; y = 20f })
world.addComponent(e, WidgetStateComponent())
```

2) 取得/修改 Component

```kotlin
val t = world.getComponent<TransformComponent>(e)
if (t != null) {
    t.x += 5f
}
```

3) 使用 `query` 撰寫 System（示例：簡易移動系統）

```kotlin
fun movementSystem(world: EcsWorld, dt: Float) {
    val entities = world.query(VelocityComponent::class)
    for (id in entities) {
        val vel = world.getComponent<VelocityComponent>(id) ?: continue
        val pos = world.getComponent<TransformComponent>(id) ?: continue
        pos.x += vel.vx * dt
        pos.y += vel.vy * dt
    }
}
```

4) 監聽 component 被加入的事件

```kotlin
world.eventBus.register(ComponentAddedEvent::class) { evt ->
    // evt.entityId, evt.component
}
```

進階：生命週期、維度政策與重置策略

- `Resettable` 接口：component 可實作 `reset()` 以在 `resetEntity` 時回復初始狀態。
- `DimensionAware` / `DimensionChangePolicy`：component 可指定當實體切換「維度」時採取：REMOVE（刪除）、RESET（重置）、KEEP（保留）。
- `processDimensionChange(entityId)` 會遍歷所有 storages，根據 component 的 `dimensionPolicy` 處理。

最佳實務與效能建議

- 將資料與行為分離：component 只放資料，system 負責邏輯。
- 使用 `reified` 泛型存取 component，減少 `KClass` 的明示使用。
- 系統內盡量避免在熱路徑頻繁建立新物件（尤其在 Render/Update loop 中）。
- 若要頻繁 spawn/destroy 大量短生命物件（例如粒子），請使用物件池或專門的 storage 來降低分配/回收成本。

常見問題與排錯（FAQ）

Q1: `getComponent<T>` 回傳 null？
- 確認 entity 拿到的是正確的 id，component 是否已被加入，或是不是加入於不同的 world 上。

Q2: `query` 似乎不包含剛加入的 component
- `addComponent` 會立即加入 storage，除非有跨執行緒問題或錯誤覆寫 storage 實作。

Q3: 事件 handler 沒有被呼叫
- 檢查是否註冊在正確的 `world.eventBus`（每個 world 有各自 eventBus）。

---

附錄：源碼導航（快速索引）
- `src/main/kotlin/com/myudog/myulib/api/ecs/EcsWorld.kt`
- `src/main/kotlin/com/myudog/myulib/internal/ecs/ComponentStorage.kt`
- `src/main/kotlin/com/myudog/myulib/api/ecs/event`（事件定義）

如需我掃描專案並針對你實際的 component 與 system 自動產生完整 API 表（包含每個成員的參數、預設值與範例），我可以進一步自動化此處理。請回覆「自動產生 API 表」即可。
