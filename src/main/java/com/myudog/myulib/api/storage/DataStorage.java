package com.myudog.myulib.api.storage;

import net.minecraft.server.MinecraftServer;
import java.util.Map;

/**
 * 通用資料儲存庫介面 (Repository Pattern)
 * @param <K> 資料的主鍵型別 (如 String 或 Identifier)
 * @param <V> 資料的實體型別 (如 TeamDefinition)
 */
public interface DataStorage<K, V> {

    /**
     * 當伺服器啟動時呼叫。
     * 用於初始化資料庫連線、或是向 Server 註冊 NBT SavedData。
     */
    void initialize(MinecraftServer server);

    /**
     * 從儲存媒介載入所有資料。
     */
    Map<K, V> loadAll();

    /**
     * 儲存或更新單筆資料。
     */
    void save(K id, V data);

    /**
     * 刪除單筆資料。
     */
    void delete(K id);
}