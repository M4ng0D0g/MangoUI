package com.myudog.myulib;

import com.myudog.myulib.api.MyulibApi;
import com.myudog.myulib.api.field.FieldManager;
import com.myudog.myulib.api.permission.PermissionManager;
import com.myudog.myulib.api.rolegroup.RoleGroupManager;
import net.fabricmc.api.ModInitializer;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class Myulib implements ModInitializer {
    public static final String MOD_ID = "myulib";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    public static Identifier id(@NotNull String path) {
        return Identifier.fromNamespaceAndPath(MOD_ID, path.trim().toLowerCase());
    }

    @Override
    public void onInitialize() {
        LOGGER.info("MyuLib is initializing...");

        MyulibApi.init();

        LOGGER.info("MyuLib (by MyuDog) has been initialized successfully.");
    }
}


