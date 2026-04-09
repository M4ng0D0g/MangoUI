package com.myudog.myulib.api.camera;

import net.minecraft.server.level.ServerPlayer;

import java.util.function.Consumer;

public final class CameraDispatchBridge {
    private static volatile DispatchHandler dispatchHandler;
    private static volatile Consumer<CameraActionPayload> localHandler;

    private CameraDispatchBridge() {
    }

    public static void setDispatchHandler(DispatchHandler handler) {
        dispatchHandler = handler;
    }

    public static void setLocalHandler(Consumer<CameraActionPayload> handler) {
        localHandler = handler;
    }

    public static void dispatch(ServerPlayer player, CameraActionPayload payload) {
        DispatchHandler handler = dispatchHandler;
        if (handler != null && player != null && payload != null) {
            handler.dispatch(player, payload);
        }
    }

    public static void applyLocal(CameraActionPayload payload) {
        Consumer<CameraActionPayload> handler = localHandler;
        if (handler != null && payload != null) {
            handler.accept(payload);
        }
    }

    @FunctionalInterface
    public interface DispatchHandler {
        void dispatch(ServerPlayer player, CameraActionPayload payload);
    }
}

