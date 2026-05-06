package com.myudog.myulib.client.api.ui.component;

import com.myudog.myulib.api.ecs.IComponent;

public class ComputedTransform implements IComponent {
    public float x;
    public float y;
    public float width;
    public float height;
    public float scaleX = 1.0f;
    public float scaleY = 1.0f;
    public float rotation;
    public float opacity = 1.0f;
}
