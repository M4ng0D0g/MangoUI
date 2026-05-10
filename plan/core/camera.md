# Camera 系統

## 1. 系統概述與設計目標
本系統提供高度自定義的相機控制框架，支援平滑路徑、相機震動、以及基於實體或座標的追蹤系統。透過 Modifier 架構，允許疊加多種動態視覺效果，並整合了防穿牆檢測與視角鎖定功能。

### 核心特性
* **修改器堆疊 (Modifiers)**：支援同時執行震動、平滑移動與路徑動畫。
* **防穿牆射線檢測**：在跟隨目標時，自動偵測障礙物並調整相機位置，防止破圖。
* **強制視角狀態機**：可由伺服器強制切換玩家的人稱視角並禁用 F5。
* **動態鎖定 UI**：整合虛擬準心投影，實現類似魂類遊戲的鎖定框視覺效果。

---

## 預期功能 (測試項目)

### 📡 伺服器 API 入口 (CameraApi - Server)
- [ ] `CameraApi.shake(ServerPlayer player, float intensity, long duration): void`
  命令特定玩家的相機進行震動。
- [ ] `CameraApi.moveTo(ServerPlayer player, CameraTrackingTarget target, long duration, Easing): void`
  命令相機平滑移動至特定目標。
- [ ] `CameraApi.reset(ServerPlayer player): void`
  重置所有相機效果並恢復自由視角。

### 🎥 客戶端相機總管 (ClientCameraManager - Client)
- [ ] `ClientCameraManager.applyPayload(CameraActionPayload payload): void`
  解析並執行來自伺服器的相機動作指令。
- [ ] `ClientCameraManager.addModifier(CameraModifier modifier): void`
  向堆疊中添加新的相機效果（如震動）。
- [ ] `ClientCameraManager.setPerspectiveState(CameraPerspective, boolean allowF5): void`
  更新視角狀態機並控制 F5 使用權限。
- [ ] `ClientCameraManager.applyAll(Camera camera, float tickDelta): void`
  每幀核心運算：計算所有 Modifier 與追蹤目標對相機 Transform 的最終影響。
- [ ] `ClientCameraManager.shake(float intensity, long durationMillis): void`
  建立並啟動一個震動修改器。

### 🎯 鎖定與追蹤系統 (LockOnTargetTracker - Client)
- [ ] `LockOnTargetTracker.isLockedOn(): boolean`
  綜合檢查相機與控制狀態，判斷目前是否處於鎖定模式。
- [ ] `LockOnTargetTracker.getCurrentTarget(): Entity`
  根據 `CameraTrackingTarget` 解析並獲取目前追蹤的實體物件。
- [ ] `LockOnTargetTracker.getLockedTargetScreenPos(float tickDelta): Vec2`
  核心投影：將追蹤目標的世界座標轉換為螢幕 2D 座標以繪製鎖定框。
- [ ] `LockOnTargetTracker.getVirtualCrosshairPos(): Vec2`
  獲取基於滑鼠位移計算的二維虛擬準心螢幕座標。
- [ ] `LockOnTargetTracker.unlock(): void`
  清除追蹤目標並同步重置鎖定狀態與 UI。

### 🎞️ 渲染與 Mixin 攔截
- [ ] `MixinCamera.myulib_mc$applyCustomTrackingAndClipping: void`
  攔截原版 `Camera.setup`，注入自定義座標、平滑 Lerp 與防穿牆射線檢測。
- [ ] `GuiMixin.myulib$renderDualCrosshair: void`
  攔截原版準心渲染，執行空間投影鎖定框與虛擬準心的雙重繪製。
- [ ] `KeyboardMixin.myulib_mc$onKeyPress: void`
  攔截 F5 物理按鍵，確保在鎖定模式下視角無法被切換。

---

## 已測試功能
- [x] 伺服器至客戶端相機封包派送
- [x] 基礎震動效果 (Shake)
- [x] Easing 補間移動 (MoveTo)
- [x] 基於實體 Eyes Position 的跟隨邏輯
- [x] 視角強制鎖定 (Perspective Lock)