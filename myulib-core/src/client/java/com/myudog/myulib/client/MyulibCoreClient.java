package com.myudog.myulib.client;

import com.myudog.myulib.client.api.MyulibApiClient;
import net.fabricmc.api.ClientModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class MyulibCoreClient implements ClientModInitializer {
    public static final Logger LOGGER = LoggerFactory.getLogger("myulib-core-client");

    @Override
    public void onInitializeClient() {
        LOGGER.info("MyuLib Core Client is initializing...");
        MyulibApiClient.initCoreClient();
        LOGGER.info("MyuLib Core Client initialized.");
    }
}
