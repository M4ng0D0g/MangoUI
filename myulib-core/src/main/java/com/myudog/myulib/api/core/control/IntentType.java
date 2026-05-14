package com.myudog.myulib.api.core.control;

public enum IntentType {
    // ==========================================
    // 移動類 (Movement)
    // ==========================================
    /** 綜合移動指令 (Master Switch / Vector) */
    MOVE,
    MOVE_FORWARD,
    MOVE_BACKWARD,
    MOVE_LEFT,
    MOVE_RIGHT,

    // ==========================================
    // 旋轉類 (Rotation)
    // ==========================================
    /** 綜合旋轉指令 (Master Switch / Combined) */
    ROTATE,
    ROTATE_YAW,   // 水平旋轉
    ROTATE_PITCH, // 垂直旋轉

    // ==========================================
    // 身體動作類 (Stance & Actions)
    // ==========================================
    JUMP,
    SNEAK,
    SPRINT,
    CRAWL,

    // ==========================================
    // 戰鬥與交互類 (Combat & Interaction)
    // ==========================================
    LEFT_CLICK,
    RIGHT_CLICK,
    MIDDLE_CLICK,

    // ==========================================
    // 功能鍵與特殊鍵 (Function Keys)
    // ==========================================
    F1, F2, F3, F4, F5, F6, F7, F8, F9, F10, F11, F12,
    ESCAPE, TAB, CAPS_LOCK, ENTER, BACKSPACE,
    
    // ==========================================
    // 原版常用按鍵 (Alpha-numeric if needed, or Generic)
    // ==========================================
    GENERIC_ACTION,
    
    /** 舊版移動向量支援 */
    MOVE_VECTOR
}