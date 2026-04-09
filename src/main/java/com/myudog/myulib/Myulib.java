package com.myudog.myulib;

import com.myudog.myulib.api.MyulibApi;
import net.fabricmc.api.ModInitializer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class Myulib implements ModInitializer {
    private static final Logger LOGGER = LoggerFactory.getLogger("myulib");
//    Identifier id = Identifier.fromNamespaceAndPath("myudog", "myulib");

    @Override
    public void onInitialize() {
        LOGGER.info("Mango UI is initializing...");
        MyulibApi.init();
        LOGGER.info("MyuLib (by MyuDog) has been initialized successfully.");
    }
}


