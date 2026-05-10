# Control 系統 (Action Interface Mapping)

本系統實作了一個高度解耦的控制中樞，允許玩家的輸入動態轉移至其他實體。

## 核心概念

### 1. 行為意圖 (Intent)
輸入不再是具體的按鍵，而是抽象的「意圖」。
- `Intent.MOVE_VECTOR`: 帶有絕對世界座標的移動向量 (已根據相機視角轉換)。
- `Intent.USE_PRIMARY`: 主要交互/攻擊。

### 2. 能力介面 (Capability Interfaces)
實體必須實作對應介面才能響應意圖：
- `IControllable`: 基礎控制介面。
- `IControllableMovable`: 響應 `MOVE_VECTOR`。
- `IControllableAttackable`: 響應攻擊意圖。

## 如何擴充

若要讓您的自定義實體支援玩家控制，請實作以下介面：

```java
public class MyBossEntity extends MobEntity implements IControllableMovable {
    @Override
    public void executeMove(Vec3d movementVector) {
        // 自定義移動邏輯
    }
}
```

## API 參考

### PlayerControlManager (透過 PlayerEntity 存取)
- `bindControl(LivingEntity target)`: 綁定控制目標。
- `unbindControl(LivingEntity target)`: 解除綁定。
- `dispatchIntent(Intent intent)`: 發送意圖。

### Client 控制
- `ClientControlManager.INSTANCE.setLockedOn(boolean)`: 開啟鎖定狀態 (劫持滑鼠)。
- `ClientControlManager.INSTANCE.getVirtualCrosshairX()`: 獲取虛擬準心座標。
