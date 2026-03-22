# UI 系統使用說明（完整參考）

本文檔是 UI 子系統的詳細參考，包含 API、架構說明、繪製與互動流程，以及實務建議。

目錄
- 系統概覽
- 主要類別與 Component 列表
- LayoutSystem（排版）詳細
- RenderSystem（繪製）詳細
- InputSystem（互動）詳細
- Widget 與 Node：API 與範例
- 常見操作範例（建立面板、按鈕、動態清單）
- 渲染最佳實務
- 故障排除

---

系統概覽

UI 採用基於 ECS 的架構：每個 Widget 對應一個 entity，並透過一系列 component（例如 TransformComponent、ComputedTransform、HierarchyComponent、WidgetStateComponent）來記錄狀態。排版（LayoutSystem）先執行，產生 `ComputedTransform`；渲染系統使用 `ComputedTransform` 進行實際繪製；InputSystem 對輸入事件做 hit-testing 與互動管理。

主要類別與 Component（重點）

- `BaseWidget`（`com.myudog.myulib.client.api.ui.BaseWidget`）
  - 初始化時自動註冊：TransformComponent、HierarchyComponent、FlexItemComponent、ComputedTransform、WidgetStateComponent
  - 方法：`onInit()`, `draw(context, mouseX, mouseY, delta)`, `setParent(parent)`

- 常用 Widget：
  - `Box`（容器/裝飾）
  - `ScrollBox`（可捲動容器，包含 ScrollComponent）
  - `LeafWidget`（禁止子元件）
  - `DataBoundList`（綁定資料的列表元件）

- 常見 Component：
  - `TransformComponent`（x,y, offset, size 設置）
  - `ComputedTransform`（最終計算出來的 x,y,w,h）
  - `WidgetStateComponent`（isVisible, isEnabled, isHovered, isFocused）
  - `ClickableComponent`, `TooltipComponent`, `ScrollComponent`, `ItemSlotComponent` 等

LayoutSystem（排版）

- `LayoutSystem.update(world, width, height)`：遍歷 root node，為每個 widget 計算 `ComputedTransform`。
- 支援 flexible layout（類似 flexbox），spacing, padding, margin 與 weight 等屬性。

RenderSystem（繪製）

- `RenderSystem.render(world, context, mouseX, mouseY, delta)`：使用 `ComputedTransform` 進行層級遍歷與繪製。
- 處理額外功能：Tooltip 顯示、手持物品渲染（ItemStack）等。

InputSystem（互動）

- 入口：`InputSystem.onMouseMove(world, rootId, mx, my)`, `onMouseDown`, `onMouseDragged`, `onMouseReleased`, `onMouseScrolled`
- 功能：hover 計算、hit-testing（逆序遍歷 children）、處理拖拽（DraggableComponent）與滑桿（SliderComponent）
- 回傳值：例如 `onMouseDown` 與 `onMouseScrolled` 會回傳 boolean 表示是否被攔截

Widget 與 Node：API 與範例

- 建立自訂 Widget 範例：

```kotlin
class MyBadge(text: String) : LeafWidget() {
    init {
        MyulibApiClient.addComponent(entityId, MyBadgeComponent(text))
    }

    override fun draw(context: DrawContext, mouseX: Int, mouseY: Int, delta: Float) {
        // 取得 computed transform
        val comp = computed
        // 使用 DrawContext 繪製文字或背景
        context.fill(comp.x.toInt(), comp.y.toInt(), comp.x.toInt()+comp.w.toInt(), comp.y.toInt()+comp.h.toInt(), 0x88FFAA00.toInt())
    }
}
```

常見操作範例

- 建立一個簡單面板與按鈕

```kotlin
val panel = Box().apply {
    textureKey = "ui/panel"
    addChild(Label("Title").apply { transform.x = 8f; transform.y = 8f })
    addChild(Button("OK").apply { transform.x = 8f; transform.y = 28f; onClick = { /* action */ } })
}
```

渲染與效能最佳實務

- 緩存重複計算的繪製參數（例如 9-slice draw params）
- 對於大量 UI 元素使用虛擬化技術（例如只為可見範圍建立/渲染子元件）
- 避免在 `draw` 中分配大量臨時物件

故障排除

- 若互動不生效，確保 `InputSystem` 的事件確實從 mapping layer 傳入（檢查是否呼叫 `InputSystem.onMouseDown` 等）。
- 若 layout 計算錯誤，檢查父容器是否正確設置 padding/margin/weight

---

需要我把 `LayoutSystem`, `RenderSystem`, `InputSystem` 的每個 public 方法列成 API 表（名稱、參數、回傳值、示例）並插入到此文件嗎？如要請回覆「列成 API 表」
