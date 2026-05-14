package com.myudog.myulib.api.core.control;

import com.myudog.myulib.api.core.control.network.ControlInputPayload;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

import java.util.Set;
import java.util.UUID;

public interface IPlayerController {

    /** 獲取玩家物件以取得 UUID */
    ServerPlayer myulib_mc$getControlPlayer();

    /** 綁定目標實體 */
    @SuppressWarnings("resource")
    default boolean myulib_mc$bindControl(UUID targetUuid) {
        ServerPlayer player = myulib_mc$getControlPlayer();
        if (player.level() instanceof ServerLevel serverLevel) {
            Entity target = serverLevel.getEntity(targetUuid);
            if (target instanceof LivingEntity living) {
                return ControlManager.INSTANCE.bind(player, living);
            }
        }
        return false;
    }

    default boolean myulib_mc$unbindControl(UUID targetUuid) {
        ServerPlayer player = myulib_mc$getControlPlayer();
        if (player != null) {
            // 呼叫 ControlManager 的精準單一解除方法
            return ControlManager.INSTANCE.unbind(player, targetUuid);
        }
        return false;
    }

    /** 解除目前所有的綁定關係 */
    @SuppressWarnings("resource")
    default void myulib_mc$clearBindings() {
        // 修正 1: 26.1 使用 getUuid() (小寫)
        // 修正 2: 確保透過正確方式獲取 server
        ServerPlayer player = myulib_mc$getControlPlayer();
        if (player != null) {
            ServerLevel level = player.level();
            ControlManager.INSTANCE.unbindFrom(player.getUUID(), level.getServer());
        }
    }

    /** 獲取目前正在控制的所有實體 ID */
    default Set<UUID> myulib_mc$getBoundEntityIds() {
        // 修正 3: 同樣使用 getUuid()
        return ControlManager.INSTANCE.getControlledEntities(myulib_mc$getControlPlayer().getUUID());
    }

    /** 發送抽象意圖 (Intent) 給所有受控實體 */
    @SuppressWarnings("resource")
    default void myulib_mc$dispatchIntent(Intent intent) {
        ServerPlayer player = myulib_mc$getControlPlayer();
        // 修正 4: 獲取當前玩家所在的 ServerLevel 並轉發
        if (player != null && player.level() instanceof ServerLevel serverLevel) {
            ControlManager.INSTANCE.dispatchIntent(player, serverLevel, intent);
        }
    }

    /**
     * 補全：更新持續性輸入狀態 (如 WASD)
     */
    @SuppressWarnings("resource")
    default void myulib_mc$updateControlInput(ControlInputPayload payload) {
        ServerPlayer player = myulib_mc$getControlPlayer();
        if (player != null && player.level() instanceof ServerLevel serverLevel) {
            ControlManager.INSTANCE.updateInput(player, serverLevel, payload);
        }
    }

    /**
     * 強制釋放所有意圖狀態
     */
    default void myulib_mc$forceReleaseAllIntents() {
        ServerPlayer player = myulib_mc$getControlPlayer();
        if (player != null) {
            ControlManager.INSTANCE.forceReleaseAllIntents(player);
        }
    }

}