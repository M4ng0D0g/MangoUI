package com.myudog.myulib.api.ecs.lifecycle;

import com.myudog.myulib.api.ecs.IComponent;

public interface DimensionAware extends IComponent {
    default DimensionChangePolicy getDimensionPolicy() {
        return DimensionChangePolicy.KEEP;
    }
}

