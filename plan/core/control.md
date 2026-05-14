# Control 系統

## 1. 系統概述與設計目標
本系統旨在實作一個高度解耦的控制中樞，允許玩家的輸入（鍵盤與滑鼠）動態轉移至遊戲內的其他實體。核心採用「資料導向 (Data-Oriented)」架構，透過全域管理器 `ControlManager` 維護綁定關係與權限遮罩，徹底區分玩家本體行為與受控目標意圖。

### 核心特性
* **全域動態目標轉移**：透過 UUID 綁定關係管理控制權，實體端不持有玩家強引用，防範記憶體洩漏。
* **多重控制支援**：支援 1:N（控制多個生物）與 N:1（多玩家協同控制）關係。
* **權限遮罩系統**：透過 Bitmask 同步玩家當前可執行的動作（移動、旋轉、跳躍等）。
* **物理層攔截**：客戶端在輸入層級直接「閹割」按鍵狀態，防止本體產生預測位移。

---

## 預期功能 (測試項目)

### 🧠 全域核心管理器 (ControlManager - Server)
- [ ] `ControlManager.bind(ServerPlayer player, LivingEntity target): boolean`
  建立玩家與實體的雙向綁定，並為目標添加 `myulib_controlled` 標籤。
- [ ] `ControlManager.unbind(ServerPlayer player, UUID targetUuid): boolean`
  精準解除特定玩家對特定目標的控制權。
- [ ] `ControlManager.unbindFrom(UUID controllerId, MinecraftServer server): void`
  解除該玩家身上所有的控制目標。
- [ ] `ControlManager.unbindTarget(UUID targetId, MinecraftServer server): void`
  解除該目標身上所有的控制者。
- [ ] `ControlManager.isPlayerControlEnabled(ServerPlayer player, ControlType type): boolean`
  查詢玩家目前是否被允許執行特定類型的操作。
- [ ] `ControlManager.updateInput(ServerPlayer player, ServerLevel level, ControlInputPayload input): void`
  更新並暫存受控目標的持續性輸入狀態（如 WASD）。
- [ ] `ControlManager.dispatchIntent(ServerPlayer player, ServerLevel level, Intent intent): void`
  根據意圖類別，將單次動作精準派發給所有受控實體的能力介面。
- [ ] `ControlManager.syncControlState(ServerPlayer player): void`
  計算並向客戶端同步當前的權限遮罩與控制狀態。
- [ ] `ControlManager.effectiveDisabledPlayerControls(UUID playerId): Set<ControlType>`
  計算玩家受環境、控制狀態影響後的最終禁用清單。

### 🌐 客戶端輸入總管 (ClientControlManager - Client)
- [ ] `ClientControlManager.install(): void`
  註冊封包接收器與生命週期監聽。
- [ ] `ClientControlManager.isDenied(ControlType type): boolean`
  利用位元運算極速判斷目前是否被禁止執行該操作。
- [ ] `ClientControlManager.shouldBlockRotation(): boolean`
  判斷是否應因「鎖定模式」或「權限剝奪」攔截滑鼠轉向。
- [ ] `ClientControlManager.sendInput(boolean, boolean, boolean, boolean, boolean, boolean): void`
  將攔截到的 WASD 與跳躍狀態打包發送至伺服器。
- [ ] `ClientControlManager.sendIntent(Intent intent): void`
  發送單次觸發動作（如點擊、技能）意圖。
- [ ] `ClientControlManager.applyClientInputGuards(Minecraft minecraft): void`
  強制清除被禁止的 KeyBinding 狀態，防止客戶端物理預測。
- [ ] `ClientControlManager.updateVirtualCrosshair(double dx, double dy): void`
  在鎖定模式下，將滑鼠位移累加至虛擬準心座標。

### ⚔️ 能力介面派發合約 (Capability Interfaces)
- [ ] `IControllableMovable.myulib_mc$executeMove(Vec3 vector): void`
  接收 `MOVE_VECTOR` 意圖處理移動。
- [ ] `IControllableRotatable.updateRotation(float yaw, float pitch): void`
  接收 `ROTATE` 意圖處理旋轉。
- [ ] `IControllableInteractable.executeInteract(Intent intent): void`
  接收 `RIGHT_CLICK` 意圖處理交互。
- [ ] `IControllableActionable.executeAction(Intent intent): void`
  接收 `JUMP/SNEAK/SPRINT` 意圖處理姿勢變更。

---

## 指令介面

### 控制綁定
`/myulib control bind <source> <target>`
`/myulib control unbind <source> <target>`
`/myulib control unbind_sources`
`/myulib control unbind_targets`

### 控制權限
如果 allow/deny 會發送封包給該 client 當有對應 intent 直接本地攔截

`/myulib control list <player>`
`/myulib control allow <player> <intent>`
`/myulib control deny <player> <intent>`