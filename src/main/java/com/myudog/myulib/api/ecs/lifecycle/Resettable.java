package com.myudog.myulib.api.ecs.lifecycle;

import com.myudog.myulib.api.ecs.IComponent;

public interface Resettable extends IComponent {
    void reset();
}

