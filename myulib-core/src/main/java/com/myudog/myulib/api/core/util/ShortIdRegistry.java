package com.myudog.myulib.api.core.util;

import com.myudog.myulib.api.core.debug.DebugFeature;
import com.myudog.myulib.api.core.debug.DebugLogManager;
import net.minecraft.resources.Identifier;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * ShortIdRegistry
 *
 * 系統：通用工具系統 (Utility System)
 * 角色：短 ID 與完整識別碼 (Identifier) 的映射註冊表。
 * 類型：Utility / Container
 *
 * 提供 Identifier 與 String (Short ID) 之間的雙向查詢功能。
 */
public final class ShortIdRegistry {
    private final Map<String, Identifier> shortToFull = new ConcurrentHashMap<>();
    private final Map<Identifier, String> fullToShort = new ConcurrentHashMap<>();

    private final int idLength;

    public ShortIdRegistry(int idLength) {
        this.idLength = idLength;
    }

    /**
     * 為指定的 Identifier 生成一個絕對不重複的短 ID，並建立綁定。
     */
    public String generateAndBind(Identifier fullId) {
        if (fullToShort.containsKey(fullId)) {
            return fullToShort.get(fullId);
        }

        String shortId;
        do {
            // 🌟 直接使用你提供的高強度、純小寫 Base36 生成器
            shortId = ShortIdGenerator.generate(idLength);
        } while (shortToFull.containsKey(shortId));

        bind(shortId, fullId);
        DebugLogManager.INSTANCE.log(DebugFeature.UTIL,
            "short-id bind full=" + fullId + ",short=" + shortId);
        return shortId;
    }

    public void bind(String shortId, Identifier fullId) {
        // 確保進入記憶體時一律為小寫，防呆
        String safeShortId = shortId.toLowerCase();
        shortToFull.put(safeShortId, fullId);
        fullToShort.put(fullId, safeShortId);
        DebugLogManager.INSTANCE.log(DebugFeature.UTIL,
                "bind short=" + safeShortId + ",full=" + fullId);
    }

    public void unbind(Identifier fullId) {
        String shortId = fullToShort.remove(fullId);
        if (shortId != null) {
            shortToFull.remove(shortId);
            DebugLogManager.INSTANCE.log(DebugFeature.UTIL,
                    "unbind short=" + shortId + ",full=" + fullId);
        }
    }

    public Identifier getFullId(String shortId) {
        // 查詢時自動轉小寫，防止玩家指令輸入大小寫混雜
        return shortToFull.get(shortId != null ? shortId.toLowerCase() : null);
    }

    public String getShortId(Identifier fullId) {
        return fullToShort.get(fullId);
    }

    public void clear() {
        shortToFull.clear();
        fullToShort.clear();
        DebugLogManager.INSTANCE.log(DebugFeature.UTIL, "clear");
    }
}