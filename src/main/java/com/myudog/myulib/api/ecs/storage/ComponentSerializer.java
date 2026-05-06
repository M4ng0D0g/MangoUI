package com.myudog.myulib.api.ecs.storage;

import net.minecraft.nbt.Tag;

/**
 * 定義單個組件如何與 NBT 互相轉換
 * @param <T> 組件的型別
 */
public interface ComponentSerializer<T> {
    Tag serialize(T component);
    T deserialize(Tag tag);
}