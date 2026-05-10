package com.myudog.myulib.api.core.control;

import java.util.Set;
import java.util.UUID;

public interface IControllable {

    /** 獲取目前實體的 UUID (實體類通常已實作) */
    UUID myulib_mc$getControllableUuid();

    /** 判斷目前是否被任一玩家控制 */
    default boolean myulib_mc$isPossessed() {
        return ControlManager.INSTANCE.isControlledTarget(myulib_mc$getControllableUuid());
    }

    /** 獲取所有控制者的 ID */
    default Set<UUID> myulib_mc$getControllerIds() {
        return ControlManager.INSTANCE.getControllers(myulib_mc$getControllableUuid());
    }

    /** 實體主動解除所有綁定 */
    default void clearControllers(net.minecraft.server.MinecraftServer server) {
        ControlManager.INSTANCE.unbindTarget(myulib_mc$getControllableUuid(), server);
    }
}