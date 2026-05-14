package com.myudog.myulib.api.core.ecs.storage;

import net.minecraft.nbt.Tag;

/**
 * ComponentSerializer
 *
 * 系統：核心 ECS - 儲存層
 * 角色：定義單個組件如何與 Minecraft NBT 格式進行雙向轉換的合約。
 * 類型：Interface / Serializer
 *
 * 實作此介面的類別應確保序列化後的資料能在不同的遊戲會話中完整還原。
 *
 * @param <T> 組件的型別
 */
public interface ComponentSerializer<T> {

    /**
     * 將組件實例序列化為 NBT 標籤。
     *
     * @param component 要序列化的組件實例
     * @return 序列化後的 NBT Tag
     */
    Tag serialize(T component);

    /**
     * 從 NBT 標籤還原組件實例。
     *
     * @param tag 包含組件資料的 NBT Tag
     * @return 還原後的組件實例
     */
    T deserialize(Tag tag);
}