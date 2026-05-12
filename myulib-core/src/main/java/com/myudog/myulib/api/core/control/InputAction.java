package com.myudog.myulib.api.core.control;

/**
 * 玩家輸入行為類型
 */
public enum InputAction {
    /** 按下 */
    PRESS,
    /** 放開 */
    RELEASE,
    /** 持續按住 */
    REPEAT,
    /** 移動 (通常用於視角旋轉或位置向量更新) */
    MOVE
}
