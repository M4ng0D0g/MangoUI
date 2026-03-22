# 粒子與懸浮物體系統（完整參考）

本文檔為專案內粒子系統與懸浮物體（Floating Objects）的完整參考說明，包含 API、範例、更新/渲染流程與整合建議。

目錄
- 概覽
- 主要類別與 API（假定介面）
- 粒子產生範例
- 懸浮物體（飄字、提示）範例
- 更新與渲染流程
- 與事件系統整合（SpawnParticleEvent 範例）
- 效能建議與實作注意事項

---

概覽

粒子系統與懸浮物體用於短期或中期的視覺效果，通常與遊戲邏輯鬆耦合，透過事件或系統 API 進行溝通。粒子多為大量、短壽命，應使用物件池與批次渲染以節省效能。

主要類別與 API（範例介面）

> 注意：實際類別名稱依專案實作為準，以下為常見且建議的 API 設計，文件中也提供如何整合當前專案的建議。

- ParticleData
  - `x: Float`, `y: Float`, `vx: Float`, `vy: Float`, `lifeMs: Long`, `color: Int`, `size: Float` 等

- ParticleSystem
  - `spawn(data: ParticleData)`
  - `update(deltaMs: Long)`
  - `render(context: DrawContext)`

- FloatingText / FloatingObject
  - `text: String`, `x: Float`, `y: Float`, `durationMs: Long`, `vx/vy` optional

- FloatingManager
  - `spawn(f: FloatingObject)`
  - `update(deltaMs: Long)`
  - `render(context: DrawContext)`

粒子產生範例

```kotlin
val p = ParticleData(x = 120f, y = 60f, vx = (Math.random()-0.5).toFloat()*1.5f, vy = -2f, lifeMs = 600, color = 0xFFFFAA00.toInt(), size = 4f)
ParticleSystem.spawn(p)
```

懸浮物體範例（飄字）

```kotlin
val ft = FloatingText(text = "+10 XP", x = 80f, y = 40f, durationMs = 900)
FloatingManager.spawn(ft)
```

更新與渲染流程

- 每幀（或每固定 tick）呼叫 `ParticleSystem.update(deltaMs)`：更新粒子的生命值、位置，並移除已過期的粒子。
- 在 Render 階段呼叫 `ParticleSystem.render(context)` 進行批次或合併繪製。
- 懸浮物體類似，但通常加入淡出/移動曲線（easing）。

與事件系統整合

建議透過事件系統進行 spawn：

```kotlin
// 註冊
world.eventBus.register(SpawnParticleEvent::class) { e -> ParticleSystem.spawn(e.data) }

// 發佈
world.eventBus.dispatch(SpawnParticleEvent(ParticleData(...)))
```

效能建議與實作注意事項

- 批次渲染：儘可能將粒子合併在同一個 draw call 中（依圖集或著色器分組）。
- 物件池：避免每個粒子都 new / GC，用 pool 重複利用 ParticleData 實例。
- LOD 與限制：在大量粒子情況下根據相機距離或畫面比率限制數量。

---

如需我掃描專案並把真實的 ParticleSystem / FloatingManager 類別與方法寫進文件（包含具體簽名），我可以自動生成更精確的文件。請回覆「掃描並生成真實 API」。
