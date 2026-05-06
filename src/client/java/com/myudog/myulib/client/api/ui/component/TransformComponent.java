package com.myudog.myulib.client.api.ui.component;

import com.myudog.myulib.api.animation.AnimationTarget;
import com.myudog.myulib.api.ecs.IComponent;

public class TransformComponent implements IComponent {
    public float x;
    public float y;
    public float width;
    public float height;
    public float scaleX = 1.0f;
    public float scaleY = 1.0f;
    public float rotation;
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
