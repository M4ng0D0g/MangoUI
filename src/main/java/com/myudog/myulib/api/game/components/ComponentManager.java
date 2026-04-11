package com.myudog.myulib.api.game.components;

import com.myudog.myulib.api.game.core.GameInstance;
import net.minecraft.resources.Identifier;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public final class ComponentManager {
    private static final Map<Identifier, ComponentModels.ComponentBindingDefinition> BINDINGS = new LinkedHashMap<>();

    private ComponentManager() {
    }

    public static void install() {
    }

    public static void register(ComponentModels.ComponentBindingDefinition binding) {
        if (binding != null && binding.id() != null) {
            BINDINGS.put(binding.id(), binding);
        }
    }

    public static void bindInstance(GameInstance<?, ?, ?> instance, Iterable<ComponentModels.ComponentBindingDefinition> bindings) {
        if (bindings == null) {
            return;
        }
        for (ComponentModels.ComponentBindingDefinition binding : bindings) {
            register(binding);
        }
    }

    public static ComponentModels.ComponentBindingDefinition get(Identifier bindingId) {
        return BINDINGS.get(bindingId);
    }

    public static void publish(ComponentModels.ComponentSignal signal) {
    }

    public static Map<Identifier, ComponentModels.ComponentBindingDefinition> snapshot() {
        return Collections.unmodifiableMap(BINDINGS);
    }
}
