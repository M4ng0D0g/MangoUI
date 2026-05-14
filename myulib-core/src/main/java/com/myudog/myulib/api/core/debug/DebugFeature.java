package com.myudog.myulib.api.core.debug;

import java.util.Locale;

/**
 * DebugFeature
 *
 * 系統：核心除錯系統 (Core Debug System)
 * 角色：定義可被個別監控的功能模組標籤。
 * 類型：Enum / Metadata
 */
public enum DebugFeature {
    /** 權限系統相關除錯。 */
    PERMISSION,
    /** 戰場/區域相關除錯。 */
    FIELD,
    /** 角色組/身分相關除錯。 */
    ROLEGROUP,
    /** 隊伍系統相關除錯。 */
    TEAM,
    /** 遊戲邏輯與流程相關除錯。 */
    GAME,
    /** 計時器系統相關除錯。 */
    TIMER,
    /** 控制與輸入系統相關除錯。 */
    CONTROL,
    /** 相機與視覺系統相關除錯。 */
    CAMERA,
    /** 命令列指令相關除錯。 */
    COMMAND,
    /** 動畫系統相關除錯。 */
    ANIMATION,
    /** ECS 系統相關除錯。 */
    ECS,
    /** 事件系統相關除錯。 */
    EVENT,
    /** 特效與效果系統相關除錯。 */
    EFFECT,
    /** 全息投影系統相關除錯。 */
    HOLOGRAM,
    /** 物件與行為系統相關除錯。 */
    OBJECT,
    /** 工具與通用輔助系統相關除錯。 */
    UTIL;

    /**
     * 將原始字串解析為 DebugFeature，不分大小寫並自動轉換連字號。
     *
     * @param raw 原始字串 (如 "timer" 或 "CONTROL")
     * @return 對應的 DebugFeature
     */
    public static DebugFeature parse(String raw) {
        return DebugFeature.valueOf(raw.toUpperCase().replace('-', '_'));
    }

    /**
     * 獲取小寫且使用連字號的標籤字串 (例如 "role-group")。
     */
    public String token() {
        return name().toLowerCase(Locale.ROOT).replace('_', '-');
    }

    /**
     * 同 {@link #token()}。
     */
    public String id() {
        return token();
    }
}

