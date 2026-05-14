package com.myudog.myulib.internal.entity;

import com.myudog.myulib.api.core.floating.IFloatingObject;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Display.ItemDisplay;
import net.minecraft.world.item.ItemStack;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;import org.joml.Vector3f;



/**
 * ItemDisplayObject
 *
 * 系統：視覺特效系統 - 內部實現 (Internal VFX Implementation)
 * 角色：基於 Minecraft {@code ItemDisplay} 實體的懸浮物件實現。
 * 類型：Implementation / Wrapper
 *
 * 此類別封裝了原生 ItemDisplay 實體的生成、移除與基本變換操作。
 * 目前部分變換操作（如縮放與旋轉）尚未完全整合原生插值參數，僅更新內部狀態。
 */
public class ItemDisplayObject implements IFloatingObject {
    private final ServerLevel Level;
    private final ItemStack itemStack;
    private ItemDisplay entity;
    private Vector3f scale = new Vector3f(1.0f, 1.0f, 1.0f);
    private Vector3f rotation = new Vector3f();

    /**
     * 建立一個項目顯示物件。
     *
     * @param Level     世界實例
     * @param itemStack 要顯示的物品堆疊
     */
    public ItemDisplayObject(ServerLevel Level, ItemStack itemStack) {
        this.Level = Level;
        this.itemStack = itemStack;
    }

    /**
     * 在世界中生成 ItemDisplay 實體。
     *
     * @param pos 生成位置
     */
    @Override
    public void spawn(Vec3 pos) {
        ItemDisplay display = new ItemDisplay(EntityType.ITEM_DISPLAY, Level);
        display.setItemStack(itemStack.copy());
        display.setPos(pos);
        Level.addFreshEntity(display);
        entity = display;
    }

    @Override
    public void remove() {
        if (entity != null) {
            entity.discard();
            entity = null;
        }
    }

    @Override
    public void moveTo(Vec3 pos, int interpolationDuration) {
        if (entity != null) {
            entity.setPos(pos);
        }
    }

    @Override
    public void setScale(Vector3f scale, int interpolationDuration) {
        this.scale = new Vector3f(scale);
    }

    @Override
    public void setRotation(Vector3f leftRotation, int interpolationDuration) {
        this.rotation = new Vector3f(leftRotation);
    }
}


