package com.myudog.myulib.api.core.camera;

/**
 * 定義相機動作的類別標籤，作為封包解析的判斷依據。
 */
public enum ActionType {
    /** 觸發相機震動（Shake）效果 */
    SHAKE,
    
    /** 觸發攝影機平滑位移（MoveTo）至目標點或動態實體 */
    MOVE_TO,
    
    /** 強制重置相機狀態，清空所有進行中的動畫與修改器 */
    RESET
}