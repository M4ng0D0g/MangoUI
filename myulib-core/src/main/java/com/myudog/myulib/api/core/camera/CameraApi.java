package com.myudog.myulib.api.core.camera;

import com.myudog.myulib.api.core.animation.Easing;
import com.myudog.myulib.api.core.debug.DebugFeature;
import com.myudog.myulib.api.core.debug.DebugLogManager;
import net.minecraft.server.level.ServerPlayer;
import java.util.Objects;

import com.myudog.myulib.api.core.camera.network.CameraNetworking;

public final class CameraApi {
    private CameraApi() {
    }

    public static void initServer() {
        CameraNetworking.initServer();
    }

    public static void initClient() {
    }

    public static void shake(ServerPlayer player, float intensity, long durationMillis) {
        if (player == null) {
            return;
        }
        CameraDispatchBridge.dispatch(player, CameraActionPayload.shake(intensity, durationMillis));
        DebugLogManager.INSTANCE.log(DebugFeature.CAMERA,
                "shake player=" + player.getName().getString() + ",intensity=" + intensity + ",duration=" + durationMillis);
    }

    public static void moveTo(ServerPlayer player, CameraTrackingTarget target, long durationMillis, Easing easing) {
        if (player == null || target == null) {
            return;
        }
        // 🌟 修正重點：不再這裡提早 resolvePosition()，因為伺服器端不負責渲染計算！
        // 直接將完整的 target 打包，讓客戶端收到封包後自己每一幀去 resolve 最新座標
        CameraDispatchBridge.dispatch(player, CameraActionPayload.moveTo(target, durationMillis, easing));
        DebugLogManager.INSTANCE.log(DebugFeature.CAMERA,
                "moveTo player=" + player.getName().getString() + ",duration=" + durationMillis + ",easing=" + easing + ",target=" + target);
    }

    public static void reset(ServerPlayer player) {
        if (player == null) {
            return;
        }
        CameraDispatchBridge.dispatch(player, CameraActionPayload.reset());
        DebugLogManager.INSTANCE.log(DebugFeature.CAMERA,
                "reset player=" + player.getName().getString());
    }

    public static void shakeLocal(float intensity, long durationMillis) {
        CameraDispatchBridge.applyLocal(CameraActionPayload.shake(intensity, durationMillis));
        DebugLogManager.INSTANCE.log(DebugFeature.CAMERA,
                "shakeLocal intensity=" + intensity + ",duration=" + durationMillis);
    }

    public static void moveToLocal(CameraTrackingTarget target, long durationMillis, Easing easing) {
        Objects.requireNonNull(target, "target");
        // 🌟 本地端動畫也是一樣，把 target 原封不動交給 Modifier 去即時解析
        CameraDispatchBridge.applyLocal(CameraActionPayload.moveTo(target, durationMillis, easing));
        DebugLogManager.INSTANCE.log(DebugFeature.CAMERA,
                "moveToLocal duration=" + durationMillis + ",easing=" + easing + ",target=" + target);
    }

    public static void resetLocal() {
        CameraDispatchBridge.applyLocal(CameraActionPayload.reset());
        DebugLogManager.INSTANCE.log(DebugFeature.CAMERA, "resetLocal");
    }
}