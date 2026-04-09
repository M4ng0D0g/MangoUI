package com.myudog.myulib.client;

import com.myudog.myulib.client.api.MyulibApiClient;
import net.fabricmc.api.ClientModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class MyulibClient implements ClientModInitializer {
    private static final Logger LOGGER = LoggerFactory.getLogger("myulib");

    @Override
    public void onInitializeClient() {
        MyulibApiClient.init();
        LOGGER.info("Myulib client bootstrap initialized.");
    }
}

