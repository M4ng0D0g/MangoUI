package com.myudog.myulib.api.core.camera;

/**
 * 定義相機動作的類別標籤，作為封包解析的判斷依據。
 */
public enum ActionType {
    /** 觸發相機震動（Shake）效果 */
    SHAKE,
    
    /** 觸發攝影機平滑位移（MoveTo）至目標點或動態實體 */
    MOVE_TO,
    
    /** 觸發相機淡入淡出（Fade）效果 */
    FADE,

    /** 設定視場角（FOV） */
    FOV_SET,

    /** 清除視場角（FOV）修改 */
    FOV_CLEAR,

    /** 將相機固定於實體 */
    ATTACH,

    /** 從實體分離相機 */
    DETACH,

    /** 播放路徑樣條（Spline） */
    PLAY_SPLINE,

    /** 設定預設相機模式（Presets） */
    SET_PRESET,
    
    /** 強制重置相機狀態，清空所有進行中的動畫與修改器 */
    RESET
}