package com.myudog.myulib.api;

import com.myudog.myulib.api.core.floating.IFloatingObject;
import com.myudog.myulib.internal.entity.ItemDisplayObject;
import net.minecraft.world.item.ItemStack;
import net.minecraft.server.level.ServerLevel;



/**
 * MyuVFX
 *
 * 系統：視覺特效系統 (VFX / Floating System)
 * 角色：VFX 系統的入口點，提供靜態方法用於生成各種視覺特效物件。
 * 類型：API / Factory
 */
public final class MyuVFX {
    private MyuVFX() {
    }

    /**
     * 建立一個基於物品顯示 (Item Display) 的懸浮物件。
     *
     * @param Level     目標世界
     * @param itemStack 要顯示的物品
     * @return 懸浮物件實例
     */
    public static IFloatingObject createItemObject(ServerLevel Level, ItemStack itemStack) {
        return new ItemDisplayObject(Level, itemStack);
    }
}


