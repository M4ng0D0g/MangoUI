package com.myudog.myulib.api.core.camera;

import com.myudog.myulib.api.core.animation.Easing;
import com.myudog.myulib.api.core.debug.DebugFeature;
import com.myudog.myulib.api.core.debug.DebugLogManager;
import net.minecraft.server.level.ServerPlayer;
import java.util.Objects;

import com.myudog.myulib.api.core.camera.network.CameraNetworking;
import net.minecraft.world.phys.Vec3;

/**
 * CameraApi
 *
 * 系統：核心相機系統 (Core Camera System)
 * 角色：全域相機操作的靜態入口，提供震動、平滑移動、目標追蹤等功能。
 * 類型：Utility / API Entry
 *
 * 此 API 支援「伺服器端派發 (Server Dispatch)」與「本地端應用 (Local Apply)」兩種模式。
 * 伺服器端調用時會將動作封裝為網路封包發送給目標玩家；
 * 本地端調用（通常用於客戶端邏輯）則會直接作用於當前的渲染相機。
 */
public final class CameraApi {
    private CameraApi() {
    }

    /**
     * 初始化伺服器端相機組件。
     * 註冊相機動作的網路編解碼器與處理器。
     */
    public static void initServer() {
        CameraNetworking.initServer();
    }

    /**
     * 初始化客戶端相機組件。
     * (保留掛鉤)
     */
    public static void initClient() {
    }

    /**
     * 遠程觸發特定玩家的相機震動。
     *
     * @param player         目標玩家
     * @param intensity      震動強度
     * @param durationMillis 持續時間 (毫秒)
     */
    public static void shake(ServerPlayer player, float intensity, long durationMillis) {
        if (player == null) {
            return;
        }
        CameraDispatchBridge.dispatch(player, CameraActionPayload.shake(intensity, durationMillis));
        DebugLogManager.INSTANCE.log(DebugFeature.CAMERA,
                "shake player=" + player.getName().getString() + ",intensity=" + intensity + ",duration=" + durationMillis);
    }

    /**
     * 遠程觸發相機平滑移動。
     * 此方法僅打包移動描述資訊，具體的座標解析與插值計算由接收端的客戶端每一幀執行。
     *
     * @param player         目標玩家
     * @param target         追蹤目標 (包含座標、偏移或實體追蹤)
     * @param durationMillis 移動所需時間
     * @param easing         插值曲線類型
     */
    public static void moveTo(ServerPlayer player, CameraTrackingTarget target, long durationMillis, Easing easing) {
        if (player == null || target == null) {
            return;
        }
        CameraDispatchBridge.dispatch(player, CameraActionPayload.moveTo(target, durationMillis, easing));
        DebugLogManager.INSTANCE.log(DebugFeature.CAMERA,
                "moveTo player=" + player.getName().getString() + ",duration=" + durationMillis + ",easing=" + easing + ",target=" + target);
    }

    /**
     * 遠程觸發相機淡入淡出（Fade）效果。
     *
     * @param player  目標玩家
     * @param r       紅色 (0-255)
     * @param g       綠色 (0-255)
     * @param b       藍色 (0-255)
     * @param in      淡入秒數
     * @param hold    保持秒數
     * @param out     淡出秒數
     */
    public static void fade(ServerPlayer player, int r, int g, int b, float in, float hold, float out) {
        if (player == null) return;
        CameraDispatchBridge.dispatch(player, CameraActionPayload.fade(r, g, b, in, hold, out));
        DebugLogManager.INSTANCE.log(DebugFeature.CAMERA, "fade player=" + player.getName().getString() + ",color=(" + r + "," + g + "," + b + "),times=(" + in + "," + hold + "," + out + ")");
    }

    /**
     * 設定視場角（FOV）。
     *
     * @param player   目標玩家
     * @param fov      FOV 數值
     * @param easeTime 平滑過渡時間 (秒)
     * @param easing   插值類型
     */
    public static void setFov(ServerPlayer player, float fov, float easeTime, Easing easing) {
        if (player == null) return;
        CameraDispatchBridge.dispatch(player, CameraActionPayload.setFov(fov, easeTime, easing));
    }

    /**
     * 清除視場角（FOV）修改，恢復預設值。
     *
     * @param player   目標玩家
     * @param easeTime 平滑過渡時間 (秒)
     * @param easing   插值類型
     */
    public static void clearFov(ServerPlayer player, float easeTime, Easing easing) {
        if (player == null) return;
        CameraDispatchBridge.dispatch(player, CameraActionPayload.clearFov(easeTime, easing));
    }

    /**
     * 設定預設相機模式（Presets）。
     *
     * @param player         目標玩家
     * @param preset         預設名稱 (如 minecraft:free, minecraft:first_person)
     * @param target         追蹤目標
     * @param durationMillis 過渡時間
     * @param easing         過渡曲線
     */
    public static void setPreset(ServerPlayer player, String preset, CameraTrackingTarget target, long durationMillis, Easing easing) {
        if (player == null) return;
        CameraDispatchBridge.dispatch(player, CameraActionPayload.setPreset(preset, target, durationMillis, easing));
    }

    /**
     * 將相機固定於實體（attach_to_entity）。
     *
     * @param player 目標玩家
     * @param entity 要固定的目標實體
     */
    public static void attachToEntity(ServerPlayer player, net.minecraft.world.entity.Entity entity) {
        if (player == null || entity == null) return;
        // 使用特殊的 ActionType 或透過 Preset 實作
        CameraDispatchBridge.dispatch(player, new CameraActionPayload(
                ActionType.ATTACH, 0, 0, null, entity.getId(), null, null, Vec3.ZERO, Easing.LINEAR,
                0, 0, 0, 0, 0, 0, 0, ""
        ));
    }

    /**
     * 從實體分離相機。
     *
     * @param player 目標玩家
     */
    public static void detachFromEntity(ServerPlayer player) {
        if (player == null) return;
        CameraDispatchBridge.dispatch(player, new CameraActionPayload(
                ActionType.DETACH, 0, 0, null, null, null, null, Vec3.ZERO, Easing.LINEAR,
                0, 0, 0, 0, 0, 0, 0, ""
        ));
    }

    /**
     * 重置玩家的相機狀態，移除所有現有的動畫效果。
     *
     * @param player 目標玩家
     */
    public static void reset(ServerPlayer player) {
        if (player == null) {
            return;
        }
        CameraDispatchBridge.dispatch(player, CameraActionPayload.reset());
        DebugLogManager.INSTANCE.log(DebugFeature.CAMERA,
                "reset player=" + player.getName().getString());
    }

    // --- 本地調用版本省略，可視需求擴充 ---
}
