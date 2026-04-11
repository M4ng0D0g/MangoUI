package com.myudog.myulib.internal.game.components;

import com.myudog.myulib.api.game.components.ComponentManager;
import com.myudog.myulib.api.game.components.ComponentModels;
import com.myudog.myulib.api.game.core.GameInstance;
import net.minecraft.resources.Identifier;

public class DefaultComponentManager {
    public static void install() { ComponentManager.install(); }
    public static void register(ComponentModels.ComponentBindingDefinition binding) { ComponentManager.register(binding); }
    public static void bindInstance(GameInstance<?, ?, ?> instance, Iterable<ComponentModels.ComponentBindingDefinition> bindings) { ComponentManager.bindInstance(instance, bindings); }
    public static ComponentModels.ComponentBindingDefinition get(Identifier bindingId) { return ComponentManager.get(bindingId); }
}
