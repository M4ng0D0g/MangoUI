package com.myudog.myulib.api.core.control;

import net.minecraft.world.phys.Vec3;

public interface IControllableMovable extends IControllable {
    /** 根據 Camera 轉換後的絕對世界座標向量進行移動 */
    void myulib_mc$executeMove(Vec3 movementVector);
}
