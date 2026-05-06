package com.myudog.myulib.api.effect.component;

import com.myudog.myulib.api.ecs.IComponent;
import net.minecraft.core.Holder;
import net.minecraft.world.effect.MobEffect;
import java.util.HashMap;
import java.util.Map;

/**
 * 空間藥水效果組件。
 * 負責記錄單一玩家身上，每一種藥水效果被幾個 AABB「來源」所疊加。
 */
public class SpatialEffectComponent implements IComponent {
    // 記錄格式：<藥水類型, 來源數量>
    // 例如：<MobEffects.POISON, 2> 代表玩家同時站在兩個毒氣方塊的範圍內
    public final Map<Holder<MobEffect>, Integer> sourceCounts = new HashMap<>();
}