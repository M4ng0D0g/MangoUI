package com.myudog.myulib.api.game.components;

import net.minecraft.resources.Identifier;

public final class ComponentModels {
    private ComponentModels() {
    }

    public interface ComponentSignal {
    }

    public record ComponentBindingDefinition(Identifier id) {
    }
}

