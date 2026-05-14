package com.myudog.myulib.api.core.control;

import net.minecraft.world.phys.Vec3;

/**
 * IControllableMovable
 *
 * 系統：核心控制系統 (Core Control System)
 * 角色：定義具備「移動」能力的實體應實現的介面。
 * 類型：Interface / Capability
 *
 * 子類實作說明：
 * 1. 應在實作中處理 movementVector 的方向與量值。
 * 2. 如果目標是 LivingEntity，建議透過修改伺服器端的目標位移 (Target Motion) 或路徑尋找器來達成。
 * 3. 需注意移動向量已由客戶端根據相機朝向轉換為絕對世界座標。
 */
public interface IControllableMovable extends IControllable {

    /**
     * 執行移動動作。
     *
     * @param movementVector 絕對世界座標下的移動向量 (包含方向與強度)
     */
    void myulib_mc$executeMove(Vec3 movementVector);
}
