package com.myudog.myulib.api.storage;

public enum StorageStrategy {
    NONE,
    NBT, // 遊戲引擎內部頻繁讀寫，且玩家絕對不需要(也不應該)直接去修改的狀態資料。
    SQL,
    GSON // 需要讓地圖作者、伺服器管理員手動調整的資料。
}
