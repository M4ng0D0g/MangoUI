# Myulib 核心系統互動關係文件 (System Interaction Map)

本文件描述 Myulib 各大核心子系統之間的依賴關係、資料流向以及協作機制。

---

## 1. 控制系統 (Control System)
這是玩家與遊戲世界互動的核心層。

### 互動關係：
- **Input (輸入層)**：客戶端監聽原始按鍵（WASD、跳躍等），將其打包為 `ControlInputPayload` 並透過 `ControlManager` 發送至伺服器。
- **Intent (意圖層)**：對於瞬時動作（如左鍵攻擊、特殊技能），系統會生成一個 `Intent` 物件。
- **Dispatch (派發層)**：`ControlManager` (伺服器) 根據綁定關係，將 `Intent` 或 `Input` 派發給目標實體。
- **Enforcement (執行層)**：透過 Mixin (如 `MixinLivingEntityControl`)，將接收到的意圖轉化為 Minecraft 原生動作（如 `setShiftKeyDown`, `jump`）。

### 資料流向：
`Player Input` -> `ClientControlManager` -> `Network` -> `ControlManager (Server)` -> `Mixin (Entity)`

---

## 2. ECS 系統 (Entity-Component-System)
負責高效管理大量非原生或擴充資料。

### 互動關係：
- **Container (容器)**：`EcsContainer` 是核心，持有所有 `ComponentStorage`。
- **Storage (儲存)**：使用 Sparse Set 優化，確保同類組件在記憶體中連續，適合大規模系統迭代。
- **Persistence (持久化)**：掛鉤伺服器生命週期，透過 `DataStorage` 將組件序列化為 NBT。
- **Events (事件)**：組件的新增與移除會觸發 `ComponentAddedEvent`，供其他系統監聽並做出反應。

### 協作範例：
當控制系統偵測到玩家控制了一個 ECS 實體時，可以透過 ECS 屬性組件（如 `StatsComponent`）來調整移動速度。

---

## 3. 相機系統 (Camera System)
負責視覺反饋與過場動畫。

### 互動關係：
- **API (入口)**：`CameraApi` 提供一致的調用介面（震動、移動）。
- **Bridge (橋接)**：`CameraDispatchBridge` 決定是發送網路封包（伺服器端）還是直接套用（本地端）。
- **Modifier (修正器)**：客戶端收到指令後，會在每一幀透過 `CameraModifier` 修改原生相機座標，實現平滑動畫。
- **Target (追蹤目標)**：`CameraTrackingTarget` 可以追蹤特定的座標、偏移或實體 UUID。

### 資料流向：
`Server Logic` -> `CameraApi` -> `CameraNetworking` -> `ClientCameraManager` -> `CameraModifier` -> `Minecraft Rendering`

---

## 4. 物件與行為系統 (Object & Behavior System)
用於定義具備複雜邏輯的虛擬或真實物件。

### 互動關係：
- **Definition (定義)**：`IObjectDef` 定義物件的靜態屬性（外觀、血量、種類）。
- **Behavior (行為)**：`IObjectBeh` 是一組解耦的邏輯處理者（如 `MineableBeh`, `AttackableBeh`）。
- **Runtime (運行時)**：`IObjectRt` 是實例化後的物件，它組合了定義與行為。
- **Events (事件)**：物件狀態改變（受傷、死亡）會透過事件系統通知外部。

### 協作範例：
一個「採集礦點」物件。
1. **定義**：它是 `ObjectKind.BLOCK`。
2. **行為**：掛載 `MineableBeh`。
3. **互動**：當控制系統派發 `LEFT_CLICK` 意圖時，觸發 `ObjectInteractEvent` -> `MineableBeh` 執行採集邏輯。

---

## 5. 權限管理系統 (Permission System)
負責執行細粒度的存取控制與行為限制。

### 互動關係：
- **Decision Engine (裁定引擎)**：`PermissionManager` 是核心，執行階層式（Field > Dimension > Global）的權限查詢。
- **Gatekeeper (守門員)**：`PermissionGate` 是 API 門戶，整合了 `FieldManager` (查位置) 與 `RoleGroupManager` (查身分) 來判斷最終裁定。
- **Storage (儲存層)**：透過 `NbtPermissionStorage` 進行 NBT 持久化，與伺服器生命週期掛鉤。

### 階層解析順序：
1. **Field (區域)**：目標位置是否有專屬權限設定？
2. **Dimension (維度)**：當前世界（主世界、地獄等）是否有設定？
3. **Global (全域)**：若上述皆無，則套用伺服器全域設定。
4. **Default (預設)**：若完全未設定，則預設為 `ALLOW`。

---

## 6. 遊戲場地系統 (Field System)
管理空間區域定義與視覺化。

### 互動關係：
- **Spatial Index (空間索引)**：`FieldManager` 維護所有 `FieldDefinition` 的 AABB 空間索引，支援快速座標查詢。
- **Visualization (視覺化)**：`FieldVisualizationManager` 將伺服器端的邊界資料轉換為 `Hologram` (全息投影)，並根據玩家位置動態同步給客戶端。
- **Integration (整合)**：場地 ID 常被 `PermissionGate` 作為查詢 Key，用來決定該區域內的建築、破壞等行為。

---

## 7. 角色組與隊伍系統 (RoleGroup & Team System)
管理身分標籤與玩家分組。

### 互動關係：
- **RoleGroup (身分組)**：`RoleGroupManager` 提供基於優先級 (Priority) 的身分管理，玩家可屬於多個組別（如 Admin, VIP, Member）。其排序結果直接影響 `PermissionScope` 的解析順序。
- **Team (隊伍)**：`TeamManager` 管理遊戲性的分組。它透過反射與 Minecraft 原生的 `Scoreboard Team` 同步，實現顏色顯示、友軍傷害控制等。
- **Cross-Sync (交叉同步)**：隊伍與角色組可以並存。權限系統主要參考角色組，而遊戲機制（如隊友不互傷）則參考隊伍。

---

## 8. 指令存取系統 (Command System)
對外暴露出系統控制介面。

### 互動關係：
- **Facade (門面)**：`AccessCommandService` 彙整了所有子系統的 CRUD 指令。
- **Dual Registry (雙重註冊)**：同時對接內部 `CommandRegistry` (核心調用) 與原生 Brigadier 指令 (管理員手動輸入)。

---

## 9. 跨系統通訊 (Networking & Events)
- **Event Bus**：提供低耦合的系統間通訊。
- **Payloads**：所有跨端通訊均基於 `CustomPayload` 協議，確保版本相容性與類型安全。
- **Bridge Pattern**：UI 配置（如場地編輯器）透過 `ConfigurationUiBridge` 實現框架與具體介面實作的解耦。
