package com.myudog.myulib.client.api.ui.component;

import com.myudog.myulib.api.core.animation.AnimationTarget;
import com.myudog.myulib.api.core.ecs.IComponent;

/**
 * TransformComponent
 *
 * 系統：客戶端 UI 系統 (Client UI - Components)
 * 角色：儲存 UI 節點的變換資訊 (位置、大小、縮放、旋轉、不透明度)。
 * 類型：ECS Component / Animation Target
 *
 * 此組件是 UI 佈局與渲染的核心，所有 UI 節點基本都持有此組件。
 * 同時實作了 {@link AnimationTarget} 接口的工廠方法，便於與動畫系統整合。
 */
public class TransformComponent implements IComponent {
    /** X 座標 (相對於父節點)。 */
    public float x;
    /** Y 座標 (相對於父節點)。 */
    public float y;
    /** 寬度。 */
    public float width;
    /** 高度。 */
    public float height;
    /** X 軸縮放比例 (預設 1.0)。 */
    public float scaleX = 1.0f;
    /** Y 軸縮放比例 (預設 1.0)。 */
    public float scaleY = 1.0f;
    /** 旋轉角度。 */
    public float rotation;
    /** 不透明度 (0.0 ~ 1.0)。 */
    public float opacity = 1.0f;

    public AnimationTarget<Float> xTarget() {
        return value -> this.x = value;
    }

    public AnimationTarget<Float> yTarget() {
        return value -> this.y = value;
    }

    public AnimationTarget<Float> widthTarget() {
        return value -> this.width = value;
    }

    public AnimationTarget<Float> heightTarget() {
        return value -> this.height = value;
    }

    public AnimationTarget<Float> scaleXTarget() {
        return value -> this.scaleX = value;
    }

    public AnimationTarget<Float> scaleYTarget() {
        return value -> this.scaleY = value;
    }

    public AnimationTarget<Float> rotationTarget() {
        return value -> this.rotation = value;
    }

    public AnimationTarget<Float> opacityTarget() {
        return value -> this.opacity = value;
    }
}
