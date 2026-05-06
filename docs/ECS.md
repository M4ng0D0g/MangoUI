# ECS 系統使用說明（完整參考）
本文檔提供專案內 ECS（Entity-Component-System）核心的 Java 版參考：`EcsContainer`、`Component`、`ComponentStorage`、`ComponentAddedEvent`，以及生命週期輔助型別 `Resettable`、`DimensionAware`、`DimensionChangePolicy`、`ComponentLifecycle`。
## 概要
ECS 以資料與行為分離為原則。在本專案中，`EcsContainer` 負責：
- 建立 entity
- 存取 component storage
- 派發 `ComponentAddedEvent`
- 處理 reset / dimension change 生命週期
## 主要類別
### `Component`
Marker interface，所有 ECS component 都應實作它。
### `ComponentStorage<T extends Component>`
內部稠密集合結構，提供：
# ECS (legacy)

This page is kept for backward compatibility.

Canonical ECS docs:
- `docs/ecs/index.md`

```
### 監聽 component 新增
```java
world.eventBus.subscribe(ComponentAddedEvent.class, event -> {
    System.out.println("component added: " + event.getEntityId());
    return com.myudog.myulib.api.event.ProcessResult.PASS;
});
```
## 生命週期
- `resetEntity(entityId)`：對所有實作 `Resettable` 的 component 呼叫 `reset()`。
- `processDimensionChange(entityId)`：根據 `DimensionAware` 的 policy 執行 `KEEP` / `REMOVE` / `RESET`。
## 最佳實務
- component 只放資料。
- system 負責邏輯。
- 熱路徑避免頻繁配置新物件。
- 大量短生命物件請考慮物件池。