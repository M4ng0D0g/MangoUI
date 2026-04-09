# MyuLib-mc Docs

## 系統總覽

每個系統都有自己的資料夾與 `index.md`。閱讀順序：
1. 先看系統 `index.md`
2. 再看該系統底下的 class 詳細頁
3. UI 另外先從 `docs/ui/nodes/index.md` 開始

### 核心系統
- [ECS](docs/ecs/index.md)
- [Event](docs/event/index.md)
- [Component](docs/component/index.md)
- [Access Systems](docs/access/index.md)
- [Field](docs/field/index.md)
- [Region](docs/region/index.md)（舊名／過渡）
- [Game](docs/game/index.md)
- [Timer](docs/timer/index.md)
- [Logic](docs/logic/index.md)
- [Floating](docs/floating/index.md)
- [Object](docs/object/index.md)
- [Camera](docs/camera/index.md)
- [Animation](docs/animation/index.md)
- [UI](docs/ui/index.md)
- [UI Nodes](docs/ui/nodes/index.md)

### 舊版路徑
- `docs/api/`、`docs/systems/`、`docs/ui_nodes/` 保留作相容與過渡參考；新文件以 canonical 路徑為準。

## 目前進度總覽

- **ECS**：核心文件已整理成 canonical index，作為整個專案資料流基底。
- **Event**：事件匯流排與跨系統訊號文件已收斂到 `docs/event/`。
- **Component**：Component / Manager / Binding 關係已集中到 `docs/component/`。
- **Access Systems**：`Field` / `Identity` / `Permission` / `Team` 已拆成獨立系統，入口集中到 `docs/access/`。
- **Region**：舊 `region` 文件保留過渡參考，主線請改看 `docs/field/`。
- **Game**：已完成遊戲物件、隊伍、mixins hooks、測試任務與文件大示範；`Game` 只負責遊戲流程核心。
- **Timer**：Timer core 與 example 已整理到 `docs/timer/`。
- **Logic**：條件 / 動作 / 信號 / facts resolver 的文件已集中。
- **Floating**：懸浮物體與 VFX 文件已集中到 `docs/floating/`。
- **Animation**：已完成手動播放動畫核心、`AnimationSystem`、測試與 UI target adapter 設計。
- **UI**：已完成 docs 重構；runtime layout/render/input 類別目前仍以骨架與逐步補齊為主。
- **UI Nodes**：已建立節點索引與大示範入口，後續逐頁補齊 fields / methods 標準格式。
