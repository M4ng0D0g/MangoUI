package com.myudog.myulib.api.ecs.lifecycle;

import com.myudog.myulib.api.ecs.IComponent;

public final class ComponentLifecycle {
    private ComponentLifecycle() {
    }

    public static boolean isResettable(IComponent component) {
        return component instanceof Resettable;
    }

    public static DimensionChangePolicy getDimensionPolicy(IComponent component) {
        if (component instanceof DimensionAware aware) {
            return aware.getDimensionPolicy();
        }
        return DimensionChangePolicy.KEEP;
    }
}
