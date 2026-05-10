package com.myudog.myulib;

import net.fabricmc.api.ModInitializer;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class MyulibCore implements ModInitializer {
    public static final String MOD_ID = "myulib-core";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MOD_ID, path.trim().toLowerCase());
    }

    @Override
    public void onInitialize() {
        LOGGER.info("MyuLib Core is initializing...");
        com.myudog.myulib.internal.control.ControlLifecycleListener.register();
        LOGGER.info("MyuLib Core initialized.");
    }
}
