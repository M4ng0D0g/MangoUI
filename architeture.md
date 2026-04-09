切換至**開發協作模式** 🛠️。

這是一份為您的 `Myulib` 量身打造的**「攝影機與視角控制系統 (Camera & Perspective API)」**專案提案書 (Proposal)。我已經為您完成了 Minecraft 1.21+ 官方底層機制與 Fabric 社群主流做法的技術調研，並將所有架構與實作策略整理如下。

您可以直接將這份內容複製並儲存為您專案 `docs/api/camera_system.md` 的初稿。

---

# 專案提案：Myulib 攝影機與視角控制系統 (Camera & Perspective API)

## 1. 系統概述
本系統旨在為 Myulib 提供一套強大、平滑且可高度客製化的視角控制 API。開發者能夠透過簡潔的指令，強行接管玩家的第一人稱/第三人稱視角，實現過場動畫、動態實體跟隨、以及基於物理或噪音演算法的螢幕震動特效。系統支援 Client/Server 架構，允許伺服器端直接對特定玩家下達視角修改指令。

## 2. 技術選型與核心機制
根據 Minecraft 1.21+ 的官方架構與 Fabric 社群實踐，本系統採用以下核心技術：

* **視角攔截 (Mixin Hook)：** 注入 `net.minecraft.client.Camera` 的 `setup()` 方法。此為官方計算每幀視角位置與旋轉的核心。我們將在此方法計算完原版邏輯後，強行疊加或覆寫我們的偏移量。
* **3D 數學運算：** 全面採用官方引入的 `org.joml.Vector3f` (位置) 與 `org.joml.Quaternionf` (旋轉) 進行視角矩陣計算，確保性能與兼容性。
* **修改器模式 (Modifier Pattern)：** 將不同的視覺效果（如震動、平移）解耦為獨立的「修改器」，允許同一時間疊加多種效果（例如：一邊平滑移動到定點，同時螢幕還在劇烈震動）。
* **C/S 同步機制：** 使用 Fabric API 的 Custom Payload (1.21+ 的 Record 封包機制)，實現伺服器遙控客戶端視角。

---

## 3. 系統架構與類別職責劃分

系統分為「共用 API 層」、「網路通訊層」、「客戶端實作層」三個核心區塊。

### 📌 共用 API 層 (Common)
提供開發者呼叫的介面與資料結構定義，存在於 `src/main/java/`。

| 類別名稱 | 職責劃分與說明 |
| :--- | :--- |
| `CameraApi` | **系統總入口 (Facade)。** 提供靜態方法供外部呼叫（例如 `CameraApi.shake(player, intensity)`）。內部自動判斷當前處於 Server 或 Client，並決定是發送封包還是直接套用效果。 |
| `CameraModifier` | **修改器介面。** 定義 `apply(Camera camera, float tickDelta)` 方法。所有自定義的視角效果（震動、移動）都必須實作此介面。 |
| `CameraTrackingTarget` | **目標封裝類別。** 用於統一管理跟隨目標，可封裝 `Entity` (實體)、`Vec3` (絕對座標)，並可附加相對偏移量 (Offset) 與鎖定角度 (Pitch/Yaw)。 |

### 📌 網路通訊層 (Network)
負責解決 Server 無法直接控制 Client 視角的限制。

| 類別名稱 | 職責劃分與說明 |
| :--- | :--- |
| `CameraActionPayload` | **自訂封包 (Custom Payload)。** 繼承 1.21+ 的 `CustomPacketPayload`。負責將 `CameraApi` 的指令打包（包含動作類型、參數、持續時間），從伺服器派發給指定玩家。 |

### 📌 客戶端實作層 (Client)
實際執行視角運算的底層邏輯，存在於 `src/client/java/`。

| 類別名稱 | 職責劃分與說明 |
| :--- | :--- |
| `ClientCameraManager` | **客戶端視角管理器。** 單例模式。負責維護一個 `List<CameraModifier>`（當前啟用的修改器清單），並在每幀被 `CameraMixin` 呼叫時，將所有修改器的效果疊加計算並套用。 |
| `CameraMixin` | **底層注入器。** 攔截原版的 `net.minecraft.client.Camera.setup()`。在方法尾端 (`@Inject(at = @At("TAIL"))`) 呼叫 `ClientCameraManager.applyAll()`。 |
| `ShakeModifier` | **震動實作。** 實作 `CameraModifier`。利用時間衰減與偽隨機 (或 Perlin Noise) 產生 `pitch` 與 `yaw` 的高頻率小幅度偏移。 |
| `PathAnimationModifier` | **動畫位移實作。** 結合 Myulib 既有的 `AnimatorComponent` 與 `Easing`。負責在 A 點與 B 點（或目標實體）之間計算當前幀的平滑插值座標與旋轉角度。 |

---

## 4. 功能實作策略 (自然語言描述)

### 策略一：伺服器與客戶端的無縫同步 (C/S Sync)
開發者在伺服器端呼叫 `CameraApi.shake(serverPlayer, 1.0f)` 時，系統不會立刻修改視角（因為做不到）。`CameraApi` 會將 `shake` 指令與強度 `1.0f` 序列化入 `CameraActionPayload` 封包中，並透過 Fabric 的 `ServerPlayNetworking.send` 發送給該 `serverPlayer`。
客戶端收到封包後，透過 `ClientPlayNetworking` 解析指令，並實例化一個 `ShakeModifier` 加入到 `ClientCameraManager` 的佇列中開始運作。

### 策略二：平滑跟隨與過渡動畫 (Smooth Animation)
當指令要求視角從「玩家當前位置」轉移到「指定座標/實體」時，`PathAnimationModifier` 會被啟動。
1. **起點紀錄：** 擷取啟動瞬間的相機座標與旋轉。
2. **終點追蹤：** 每一幀動態獲取 `CameraTrackingTarget` 的最新座標（若目標是實體，座標會實時更新）。
3. **插值計算 (Lerp/Slerp)：** 將總時間傳入您開發的 `AnimatorComponent` 取得當前進度 (0.0 ~ 1.0)，並套用 `Easing` 曲線。位置使用線性插值 (`MathHelper.lerp`)，旋轉角度則使用四元數球面插值 (`Quaternionf.slerp`) 確保視角旋轉自然且不產生萬向鎖 (Gimbal Lock)。

### 策略三：真實的震動特效 (Screen Shake)
為避免傳統隨機數造成的「畫面抽搐」感，`ShakeModifier` 的實作將採用**阻尼正弦波 (Damped Sine Wave)** 或 **1D 柏林噪音 (Perlin Noise)**。
* **衰減機制：** 根據設定的持續時間，計算一個從 1.0 衰減至 0.0 的 `decay` 係數。
* **偏移套用：** 將當前的系統時間 (`Util.getMeasuringTimeMs()`) 乘上震動頻率代入噪音函數，得出 X、Y 軸的偏移量，再乘上強度與衰減係數，最終以 `camera.moveBy()` 或直接修改 `pitch/yaw` 套用到相機上。

### 策略四：原版視角覆寫 (Camera Override)
在 `CameraMixin` 攔截 `setup()` 時，Minecraft 已經根據第三人稱/第一人稱設定好了視角。當 `ClientCameraManager` 發現有「強制覆寫絕對位置」的 Modifier 存在時，會無視原版計算，直接調用 `camera.setPos(x, y, z)` 與 `camera.setRotation(yaw, pitch)`，從而達到完全接管畫面的目的。

---

## 5. 預期 API 使用範例 (Pseudo-code)

開發完成後，模組開發者將能以極簡的語法呼叫：

```java
// 1. 觸發地震特效 (伺服器或客戶端皆可呼叫)
// 參數：目標玩家, 震動強度, 持續時間(ms)
CameraApi.shake(player, 1.5f, 2000L);

// 2. 平滑移動視角去觀看某個實體 (例如 Boss 登場)
CameraTrackingTarget target = CameraTrackingTarget.of(bossEntity)
    .withOffset(new Vec3(0, 5, -10)) // 停在 Boss 後上方 10 格
    .lookAt(bossEntity);             // 鏡頭鎖定 Boss

// 參數：目標玩家, 跟隨目標, 過渡時間(ms), 緩動曲線
CameraApi.moveTo(player, target, 1500L, Easing.EASE_IN_OUT_QUAD);

// 3. 解除所有控制，將視角還給玩家
CameraApi.reset(player);
```