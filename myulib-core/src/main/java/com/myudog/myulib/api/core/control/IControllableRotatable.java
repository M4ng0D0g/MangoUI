package com.myudog.myulib.api.core.control;

public interface IControllableRotatable extends IControllable {
    /** * 處理頭部與視角的獨立轉向
     * @param yaw 目標偏航角
     * @param pitch 目標俯仰角
     */
    void updateRotation(float yaw, float pitch);
    
    /**
     * 決定該實體是否要訂閱玩家的視角同步
     * @return 若回傳 true，ControlManager 才會發送轉向指令
     */
    boolean shouldSyncRotation();
}