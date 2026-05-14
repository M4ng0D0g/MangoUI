package com.myudog.myulib.api.core;

import com.myudog.myulib.MyulibCore;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.NotNull;

/**
 * Tokenable
 *
 * 系統：核心基礎類型 (Core API - Messaging/ID)
 * 角色：定義具備「識別令牌」特徵的物件介面。支援層級化路徑與 Identifier 轉換。
 * 類型：Interface / Contract
 */
public interface Tokenable {

    /**
     * 獲取物件本身的標籤 (例如 "red_team")。
     *
     * @return Token 字串
     */
    @NotNull String getToken();

    /**
     * 獲取物件所屬的層級路徑 (例如 "game/room_1")。
     *
     * @return 路徑字串 (不含 Token 本身)
     */
    @NotNull String getPath();

    /**
     * 將 Token 轉換為 Minecraft 原生的 Identifier。
     * 預設實作會將 path 與 token 組合，並強制使用 Mod 的命名空間。
     *
     * @return 完整的 Identifier 實例
     */
    default Identifier toIdentifier() {
        return MyulibCore.id(getPath() + "/" + getToken());
    }
}