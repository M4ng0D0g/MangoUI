package com.myudog.myulib.api.framework.field;

import com.myudog.myulib.api.core.ecs.IComponent;

public final class FieldInstance implements IComponent {
    public final FieldDefinition definition;

    public FieldInstance(FieldDefinition definition) {
        this.definition = definition;
    }
}
