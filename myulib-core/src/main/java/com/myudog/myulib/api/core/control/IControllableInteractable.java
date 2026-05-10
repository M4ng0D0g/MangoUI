package com.myudog.myulib.api.core.control;

public interface IControllableInteractable extends IControllable {
    /** * 處理右鍵交互意圖
     * @apiNote 未來如果 Intent 擴充了 HitResult (準心指向的目標)，可以在這裡一併處理
     */
    void executeInteract(Intent intent);
}