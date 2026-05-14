package com.myudog.myulib.api.core.floating;

import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

/**
 * IFloatingObject
 *
 * 系統：視覺特效系統 (VFX / Floating System)
 * 角色：定義一個具備三維空間變換（平移、縮放、旋轉）能力的懸浮物件。
 * 類型：Interface / Capability
 *
 * 這些物件通常基於 Minecraft 的 Display Entity 實現，支援伺服器端控制的平滑插值動畫。
 */
public interface IFloatingObject {

    /**
     * 在指定的世界座標生成此懸浮物件。
     *
     * @param pos 生成位置
     */
    void spawn(Vec3 pos);

    /**
     * 從世界中移除此物件。
     */
    void remove();

    /**
     * 將物件移動到新位置。
     *
     * @param pos                   目標座標
     * @param interpolationDuration 插值持續時間 (Tick)
     */
    void moveTo(Vec3 pos, int interpolationDuration);

    /**
     * 設定物件的縮放比例。
     *
     * @param scale                 縮放向量 (x, y, z)
     * @param interpolationDuration 插值持續時間 (Tick)
     */
    void setScale(Vector3f scale, int interpolationDuration);

    /**
     * 設定物件的旋轉角度。
     *
     * @param leftRotation          旋轉向量
     * @param interpolationDuration 插值持續時間 (Tick)
     */
    void setRotation(Vector3f leftRotation, int interpolationDuration);
}

