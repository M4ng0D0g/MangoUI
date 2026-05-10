package com.myudog.myulib.api.core.control;

public interface IControllableCustom extends IControllable {
    /** * 處理未被標準化的擴充意圖
     */
    void executeCustom(Intent intent);
}